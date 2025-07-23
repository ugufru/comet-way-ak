package com.cometway.jdbc;

	/**
	* This class implements an ObjectManager using JDBCPropsContainers
	* for storage of Properties in a relational database. The database is 
	* accessed via JDBC. This code has been tested with Oracle, Microsoft
	* SQL Server and the open-source MySQL RDBMS.
	*/

import java.io.*;
import java.sql.*;
import java.util.*;

import com.cometway.ak.AK;
import com.cometway.om.*;
import com.cometway.props.Props;

/**
 * This is the JDBC implementation of IObjectManager. Object types are stored
 * as tables in a SQL database, their data represented by table schemas which
 * allow for fast and convenient access to data.
 */


/*
 * TO DO:
 * Set the threadgroup priority lower
 * deal with the shitty MSSQL 6.5 problem below:
 * ** SQL Error ***
 * JDBCPropsContainer.setProperty[text](oldStories):
 * SQLState   = 37000
 * Error Code = 1105
 * [Microsoft][ODBC SQL Server Driver][SQL Server]Can't allocate space for object '
 * Syslogs' in database 'viaDB' because the 'logsegment' segment is full. If you ra
 * n out of space in Syslogs, dump the transaction log. Otherwise, use ALTER DATABA
 * SE or sp_extendsegment to increase the size of the segment.
 * 
 */

public class JDBCObjectManager implements IObjectManager
{
	protected Hashtable				typesTable;
	protected PreparedStatement     updateUniqueIDStmt;
	private boolean					debug;
	private static int				NUM_WRITE_THREADS = 20;	// This pool of ObjectWriter threads will be used by ALL JDBCPropsContainers
														    // created in this OM. It should be set to at least 20.

	private static int				DEFAULT_MAX_FIELD_SIZE = 255;	// Maximum field size to use if JDBCDriver can't figure it out.
	private Vector					writeThreads;					// Vector of object writer threads.
	private int						maxRowSize;						// maximum size of a varbinary/varchar type (in bytes - this is DB dependent)
	private JDBCConnectionDriver    connectionMgr;					// Manages the pool of DB Connections.
	private ThreadGroup				myThreadGroup;


	/**
	 * Creates a new instance of this object manager and connects to a database using
	 * the supplied parameters. This class uses the pooled JDBC Connection functionality
	 * found in com.cometway.jdbc
	 * 
	 * @param jdbcDriver the class name of the JDBC driver to use for this connection (ie: "sun.jdbc.odbc.JdbcOdbcDriver").
	 * @param jdbcURL the connection URL for this driver and database (ie: "jdbc:odbc:database").
	 * @param user the account username used to access the database.
	 * @param password the account password used to access the database.
	 */

	public JDBCObjectManager(String jdbcDriver, String jdbcURL, String user, String password)
	{
		setDebug(false);
		JDBCPropsContainer.setDebug(false);


		// Initialize the pool of DB Connections.

		openConnection(jdbcDriver, jdbcURL, user, password);


		// Initialize the writer threads.

		writeThreads = new Vector();
		myThreadGroup = new ThreadGroup("Database I/O Threads");

		myThreadGroup.setMaxPriority(5);

		for (int i = 0; i < NUM_WRITE_THREADS; i++)
		{
			ObjectWriterThread      t = new ObjectWriterThread(myThreadGroup);

			writeThreads.addElement(t);
		}
	}


	/**
	 * Creates a new object in the object manager of the specified object type.
	 * <P>
	 * <I>Note: Currently, only the PropsType is supported.</I>
	 * 
	 * @param type a reference to an ObjectType representing the type of object to create.
	 * @return a reference to a valid ObjectID if successful; null otherwise.
	 */

	public ObjectID createObject(ObjectType type)
	{
		ObjectID		objectID;
		TypeInfo		info;
		JDBCPropsContainer      p;

		objectID = null;
		info = getTypeInfo(type);

		if (info != null)
		{
			objectID = info.getUniqueID();

			if (objectID != null)
			{
				String  newType = objectID.getType();
				String  newID = objectID.getID();

				p = new JDBCPropsContainer(connectionMgr, writeThreads, newType, newID);

				p.setProperty("type", newType);
				p.setProperty("id", newID);
			}
		}

		return (objectID);
	}


	/**
	 * Called internally to create prepared SQL statements for later frequent use.
	 */

	private void createPreparedStatements()
	{
		com.cometway.jdbc.JDBCConnection    myconn = null;

		try
		{
			myconn = connectionMgr.connect();
			updateUniqueIDStmt = myconn.prepareStatement("update types set uniqueSeed = ? where name = ?");

			myconn.close();
		}
		catch (SQLException e)
		{
			sqlError("createPreparedStatements", e);
		}
	}


	/**
	 * Called internally to create the main "types" table which stores critical information
	 * about objects in the database created by JDBCObjectManager.
	 */

	private void createTypesTable()
	{


		// beginSession();

		com.cometway.jdbc.JDBCConnection    myconn = null;

		try
		{
			myconn = connectionMgr.connect();

			try
			{
				String		sql = "create table IF NOT EXISTS types (name varchar(64) not null, uniqueSeed int not null)";
				Statement       statement = myconn.createStatement();

				try
				{
					statement.executeUpdate(sql);
				}
				catch (SQLException e)
				{
					String  s = e.getSQLState();

					if (s.equals("S0001") == false)		// Not a problem if the table already exists.
					{
						sqlError("createTypesTable", e);
					}
				}

				statement.close();
			}
			catch (SQLException e)
			{
				sqlError("createTypesTable", e);
			}

			try
			{
				String		sql = "grant ALL on types to PUBLIC";
				Statement       statement = myconn.createStatement();

				statement.executeUpdate(sql);
				statement.close();
			}
			catch (SQLException e)
			{
				String  s = e.getSQLState();

				if (s.equals("S0001") == false)		// Not a problem if the table already exists.
				{
					sqlError("createTypesTable", e);
				}
			}

			myconn.close();
		}
		catch (SQLException e)
		{
			sqlError("createTypesTable", e);
		}
	}


	/**
	 * Called internally to output debugging information to the System.out stream.
	 */

	protected void debug(String methodName, String message)
	{
		if (debug)
		{
			System.out.println("JDBCObjectManager." + methodName + ": " + message);
		}
	}


	/**
	 * Called internally to output error messages to the System.err stream.
	 */

	protected void error(String methodName, String message, Exception e)
	{
		System.err.println("JDBCObjectManager." + methodName + ": " + message + "\n" + e);
	}


	/**
	 * Could be called internally (currently unused) to drop tables and information associated with an object type.
	 */

	private boolean dropType(ObjectType type)
	{
		boolean		result;
		String		typeName;
		String		sql;
		ResultSet       resultSet;

		result = false;
		typeName = type.getType();


		// beginSession();

		com.cometway.jdbc.JDBCConnection    myconn = null;

		try
		{
			myconn = connectionMgr.connect();


			// Remove this type from the TYPES table.

			Statement       statement = myconn.createStatement();

			sql = "delete types where name='" + typeName + "'";
			resultSet = statement.executeQuery(sql);

			resultSet.next();

			if (resultSet.getInt(1) > 0)
			{
				JDBCPropsContainer.dropTable(connectionMgr, typeName);
			}

			myconn.close();

			result = true;
		}
		catch (SQLException e)
		{
			sqlError("dropType", e);
		}

		return (result);
	}


	/**
	 * Called by the garbage collector; close prepared statements.
	 */

	@Deprecated
	protected void finalize() throws Throwable
	{
		try
		{
			updateUniqueIDStmt.close();
		}
		catch (SQLException e)
		{
			sqlError("finalize", e);
		}


		// myThreadGroup.setDaemon(true);
		// Note: ThreadGroup.destroy() is deprecated and should not be used
		// myThreadGroup.destroy();
	}


	/**
	 * Retrieves the object corresponding to an object ID.
	 * 
	 * @param id a reference to an ObjectID representing a valid object in the object manager.
	 * @return a reference to an Object if one was found; null otherwise.
	 */

	public Object getObject(ObjectID id)
	{
		Object		o = null;
		String		typeName = id.getType();
		TypeInfo	typeInfo = (TypeInfo) typesTable.get(typeName);

		if (typeInfo != null)
		{
			o = typeInfo.getObject(connectionMgr, id);
		}
		else
		{
			debug("getObject", "invalid object type: " + typeName);
		}

		return (o);
	}


	/**
	 * Returns a string containing current status information about this object manager.
	 */

	public String getStatus()
	{
		StringBuffer				b = new StringBuffer();
		com.cometway.jdbc.JDBCConnection    connection = null;

		try
		{
			connection = connectionMgr.connect();

			b.append("JDBCObjectManager Status\n");
			b.append("          Version: 1.0a\n");
			b.append(connection.getStatus());
			b.append(JDBCPropsContainer.getStatus());
			connection.close();
		}
		catch (SQLException e)
		{
			sqlError("getStatus", e);
		}

		return (b.toString());
	}


	/**
	 * Returns the TypeInfo for the specified ObjectType, adding it to the types
	 * table and creating a new table as necessary.
	 * @return a reference to a TypeInfo.
	 */

	private TypeInfo getTypeInfo(ObjectType type)
	{
		TypeInfo	info = (TypeInfo) typesTable.get(type.getType());

		if (info == null)
		{
			info = new TypeInfo(type);

			if (type.getType().startsWith(PropsType.TYPE_STR))
			{
				JDBCPropsContainer.createTable(connectionMgr, info.typeName);
			}
		}

		return (info);
	}


	/**
	 * Returns a Vector of objects designated by the <TT>objectQuery</TT> parameter.
	 * Valid objects for this parameter are:
	 * <TABLE>
	 * <TR><TD>IObjectManager.LIST_TYPES            <TD>Lists object types that already exist.
	 * <TR><TD>IObjectManager.LIST_SUPPORTED_TYPES  <TD>Lists objects which can be passed to createObject.
	 * <TR><TD>ObjectType                           <TD>Lists all existing objects of the same ObjectType.
	 * <TR><TD>PropsQuery                           <TD>Lists Props based on data from the PropsQuery object.
	 * </TABLE>
	 * <I>Note: Some object managers may support additional values for access to non-standard features.<I>
	 * 
	 * @param objectQuery any valid object from the list above.
	 * @return a Vector containing Objects based on the query; null if the query wasn't recognized.
	 */

	public Vector listObjects(Object objectQuery)
	{
		Vector  v = new Vector();

		if (objectQuery == LIST_TYPES)
		{
			Enumeration     e = typesTable.keys();

			while (e.hasMoreElements())
			{
				v.addElement(new ObjectType((String) e.nextElement()));
			}
		}
		else if (objectQuery == LIST_SUPPORTED_TYPES)
		{
			v.addElement("com.cometway.om.PropsType");
		}
		else if (objectQuery instanceof ObjectType)
		{
			TypeInfo	info = getTypeInfo((ObjectType) objectQuery);

			v = info.listObjects();
		}
		else if (objectQuery instanceof PropsQuery)
		{
			PropsQuery      q = (PropsQuery) objectQuery;
			TypeInfo	info = getTypeInfo(new ObjectType(q.typeName));

			v = info.propsQuery(q);
		}

		return (v);
	}


	/**
	 * Called internally to load information from the types table.
	 */

	private void loadTypeInfo()
	{
		TypeInfo	info;

		debug("loadTypeInfo", "Loading type information...");

		typesTable = new Hashtable();


		// beginSession();

		com.cometway.jdbc.JDBCConnection    conn = null;

		try
		{
			conn = connectionMgr.connect();
		}
		catch (SQLException e)
		{
			sqlError("loadTypeInfo", e);
		}

		try
		{
			String		sql = "select name,uniqueSeed from types";
			Statement       statement = conn.createStatement();
			ResultSet       resultSet = statement.executeQuery(sql);

			while (resultSet.next())
			{
				info = new TypeInfo(resultSet.getString(1), resultSet.getInt(2));

				if (info.typeName.startsWith(PropsType.TYPE_STR))
				{
					typesTable.put(info.typeName, info);
				}
			}

			statement.close();
			conn.close();
		}
		catch (SQLException e)
		{
			sqlError("loadTypeInfo", e);
		}

		Enumeration     e = typesTable.elements();

		while (e.hasMoreElements())
		{


			// Make sure a table exists for this type.

			info = (TypeInfo) e.nextElement();

			if (info.typeName.startsWith(PropsType.TYPE_STR))
			{
				JDBCPropsContainer.createTable(connectionMgr, info.typeName);
			}
		}
	}


	/**
	 * Called internally to connect to the database. Sets up the connection manager,
	 * then gets a connection to perform DB initialization work as necessary.
	 */

	private boolean openConnection(String jdbcDriver, String jdbcURL, String user, String password)
	{
		boolean result = false;

		try
		{
			connectionMgr = new JDBCConnectionDriver(jdbcDriver, jdbcURL, user, password, AK.getDefaultReporter());
		}
		catch (ClassNotFoundException ex)
		{
			System.err.println("Could not find the JDBCDriver:" + ex.toString());
		}
		catch (InstantiationException ex2)
		{
			System.err.println("Could not instantiate the JDBCDriver:" + ex2.toString());
		}
		catch (IllegalAccessException ex3)
		{
			System.err.println("Could not access the JDBCDriver:" + ex3.toString());
		}
		catch (SQLException ex4)
		{
			System.err.println("Could not create connectionPool:" + ex4.toString());
			sqlError("openConnection", ex4);
		}

		try
		{
			com.cometway.jdbc.JDBCConnection    myConn = connectionMgr.connect();

			myConn.buildDataTypeInfo();
			myConn.close();
		}
		catch (SQLException e)
		{
			sqlError("openConnection", e);
		}

		createPreparedStatements();
		createTypesTable();
		loadTypeInfo();

		result = true;

		return (result);
	}


	/**
	 * Deletes the object corresponding to an object ID from the object manager.
	 * 
	 * @param id a reference to an ObjectID representing a valid object in the object manager.
	 * @return true if the object was successfully deleted; false otherwise.
	 */

	public boolean removeObject(ObjectID id)
	{
		boolean		result = false;
		String		typeName = id.getType();
		TypeInfo	typeInfo = (TypeInfo) typesTable.get(typeName);

		if (typeInfo != null)
		{
			typeInfo.objectList.remove(id.getID());


			// beginSession();

			com.cometway.jdbc.JDBCConnection    connection = null;

			try
			{
				connection = connectionMgr.connect();

				PreparedStatement       deleteObjectStmt = connection.prepareStatement("delete from " + typeName + " where name = ?");

				deleteObjectStmt.setString(1, id.getID());
				deleteObjectStmt.executeUpdate();
				deleteObjectStmt.close();
				connection.close();

				result = true;
			}
			catch (SQLException e)
			{
				sqlError("removeObject", e);
			}
		}
		else
		{
			debug("removeObject", "invalid object type: " + typeName);
		}

		return (result);
	}


	/**
	 * Enables debugging messages that are printed to the System.out stream.
	 * This feature is turned off by default.
	 * 
	 * @param state true enables debugging; false disables it.
	 */

	public void setDebug(boolean state)
	{
		debug = state;
	}


	/**
	 * Called internally to report JDBC related exceptions.
	 */

	private void sqlError(String methodName, SQLException e)
	{
		System.err.println("*** SQL Error ***");
		System.err.println("JDBCObjectManager." + methodName + ":");

		while (e != null)
		{
			System.err.println("SQLState   = " + e.getSQLState());
			System.err.println("Error Code = " + e.getErrorCode());
			System.err.println(e.getMessage());
			System.err.println();
			System.err.flush();

			e = e.getNextException();
		}
	}


	/**
	 * Changes the objectID of an object.
	 */

	public boolean changeObjectID(ObjectID oldID, ObjectID newID)
	{
		debug("changeObjectID", "Changing ID of Object, old ID: " + oldID + " new ID:" + newID);

		boolean rval = false;

		try
		{
			TypeInfo	t = (TypeInfo) typesTable.get(oldID.getType());

			if (t == null)
			{
				System.err.println("ObjectManager.changeObjectID: Unknown type \"" + oldID.getType() + "\".");
			}
			else
			{
				Object  o = t.objectList.get(oldID.getID());

				if (!oldID.getType().equals(newID.getType()))
				{
					TypeInfo	newtype = (TypeInfo) typesTable.get(newID.getType());

					if (newtype == null)
					{
						newtype = getTypeInfo(new ObjectType(newID.getType()));

						typesTable.put(newID.getType(), newtype);
					}

					newtype.objectList.put(newID.getID(), o);
					t.objectList.remove(oldID.getID());

					if (t.objectList.size() == 0)
					{
						typesTable.remove(oldID.getType());
					}
				}
				else
				{
					t.objectList.remove(oldID.getID());
					t.objectList.put(newID.getID(), o);
				}

				if (o instanceof Props)
				{
					Props   p = (Props) o;

					p.setProperty("type", newID.getType());
					p.setProperty("id", newID.getID());
				}
			}

			rval = true;
		}
		catch (Exception e)
		{
			System.err.println("ObjectManager.changeObjectID: Exception caught: " + e);
			e.printStackTrace();
		}

		return (rval);
	}


	// Inner classes

	class TypeInfo
	{
		String		typeName;
		int		uniqueSeed;
		Hashtable       objectList;


		/**
		 * This constructor used when loading data from the types table.
		 */

		public TypeInfo(String typeName, int startID)
		{
			this.typeName = typeName;
			this.uniqueSeed = startID;
			objectList = new Hashtable();
		}


		/**
		 * This constructor is used to create a new type. It inserts a row into the types
		 * table and initializes the value of the unique seed to 1000.
		 */

		public TypeInfo(ObjectType type)
		{
			this.typeName = type.getType();
			this.uniqueSeed = 1000;
			objectList = new Hashtable();


			// beginSession();

			com.cometway.jdbc.JDBCConnection    connection = null;

			try
			{
				connection = connectionMgr.connect();

				PreparedStatement       statement = connection.prepareStatement("insert types values(?, ?)");

				statement.setString(1, typeName);
				statement.setInt(2, uniqueSeed);
				statement.executeUpdate();
				statement.close();
				typesTable.put(typeName, this);
				connection.close();
			}
			catch (SQLException e)
			{
				sqlError("$TypeInfo", e);
			}
		}


		public Object getObject(JDBCConnectionDriver connDriver, ObjectID objectID)
		{
			Object  o = null;
			String  typeName = objectID.getType();
			String  id = objectID.getID();

			o = objectList.get(id);

			if (o == null)
			{


				// System.out.println("JDBCObjectManager.$TypeInfo.getObject: Props created for " + objectID + ".");

				if (typeName.startsWith(PropsType.TYPE_STR))
				{
					o = new Props(new JDBCPropsContainer(connDriver, writeThreads, typeName, id));

					objectList.put(id, o);
				}
				else
				{
					debug("$TypeInfo.getObject", "invalid object type: " + typeName);
				}
			}
			else
			{


				// System.out.println("JDBCObjectManager.$TypeInfo.getObject: " + objectID + " found in cache.");

			}

			return (o);
		}


		/**
		 * Returns a unique object ID based on the current unique seed value.
		 */

		protected synchronized ObjectID getUniqueID()
		{
			String		str = "ID" + uniqueSeed++;
			ObjectID	id = new ObjectID(typeName, str);


			// beginSession();

			try
			{
				updateUniqueIDStmt.setInt(1, uniqueSeed);
				updateUniqueIDStmt.setString(2, typeName);
				updateUniqueIDStmt.executeUpdate();
			}
			catch (SQLException e)
			{
				sqlError("$TypeInfo.getUniqueID", e);

				id = null;
			}


			// endSession();

			return (id);
		}


		/**
		 * Returns a vector of object IDs for this type.
		 */

		Vector listObjects()
		{
			Vector					v = new Vector();
			com.cometway.jdbc.JDBCConnection    connection = null;

			try
			{
				connection = connectionMgr.connect();

				String		sql = "select name from " + typeName + " where keyname = 'id'";
				Statement       statement = connection.createStatement();
				ResultSet       resultSet = statement.executeQuery(sql);

				while (resultSet.next())
				{
					v.addElement(new ObjectID(typeName, resultSet.getString(1)));
				}

				statement.close();
				connection.close();
			}
			catch (SQLException e)
			{
				sqlError("$TypeInfo.listObjects", e);
			}
			catch (Exception ex)
			{

			}

			return (v);
		}


		/**
		 * Returns a vector of object IDs which match the query parameters.
		 */

		Vector propsQuery(PropsQuery q)
		{
			Vector					v = new Vector();
			Hashtable				resultsToCheck = new Hashtable();
			com.cometway.jdbc.JDBCConnection    conn = null;

			try
			{
				conn = connectionMgr.connect();

				//danik: mysql doesnt like this
				//conn.setAutoCommit(false);


				// First, check to see which values are in multiple rows.

				String		sql;
				Statement       statement;
				ResultSet       resultSet;
				String		name = null;

				if(q.key!=null) {
				    sql = "select name from " + q.typeName + " where keyname '" + q.key + "' and write_order = 1";
				}
				else {
				    sql = "select name from " + q.typeName + " where keyname = 'type'";// and write_order = 1";
				}
				//				System.out.println(sql);
				statement = conn.createStatement();
				resultSet = statement.executeQuery(sql);

				Vector  multiRowNames = new Vector();

				while (resultSet.next())
				{
					name = resultSet.getString(1);

					multiRowNames.addElement(name);
				}

				debug("$typeInfo.propsQuery", "Retreived multi-row names:" + multiRowNames.toString());
				statement.close();


				// If there are values in multiple rows, get the data
				// for each of these values, put it together, and add it
				// to our results vector.

				for (int i = 0; i < multiRowNames.size(); i++)
				{
					Vector  textValues = new Vector();
					Vector  binValues = new Vector();

					sql = "select text_value, value from " + typeName + " where name = '" + (String) multiRowNames.elementAt(i) + "' order by write_order";
					statement = conn.createStatement();
					resultSet = statement.executeQuery(sql);

					while (resultSet.next())
					{
						String  text_val = resultSet.getString(1);
						byte[]  b = null;

						try
						{
							b = resultSet.getBytes(2);
						}
						catch (NegativeArraySizeException e)
						{
							;
						}		// Thrown when the binary data is null.

						if (text_val != null)		// The value is stored as a text string.
						{
							textValues.addElement(text_val);
						}
						else if (b != null)		// The value is stored as a serialized object.
						{
							binValues.addElement(b);
						}
						else
						{
							continue;
						}
					}

					statement.close();

					if (textValues.size() > 0)		// Build our object by concat'ing all the text values.
					{
						StringBuffer    concatValue = new StringBuffer();

						for (int idx = 0; idx < textValues.size(); idx++)
						{
							concatValue.append(textValues.elementAt(idx));
						}

						Object  ob = concatValue.toString();

						debug("$typeInfo.propsQuery", "Built multi-row text value:" + ob);
						resultsToCheck.put(name, ob);
					}
					else if (binValues.size() > 0)		// Build the object by concating all bin data and de-serializing it.
					{
						Object  ob = null;

						try
						{
							PipedInputStream	pin = new PipedInputStream();


							// Spawn a seperate thread to work with the piped data.

							boolean foundAThread = false;

							while (true)
							{
								for (int y = 0; y < NUM_WRITE_THREADS; y++)
								{
									ObjectWriterThread      t = (ObjectWriterThread) writeThreads.elementAt(y);

									if (!t.isBusy())
									{
										foundAThread = true;

										t.connect(pin);
										t.setByteData(binValues);
										t.start();

										y = NUM_WRITE_THREADS;
									}
									else
									{
										debug("$typeInfo.propsQuery", "Waiting for an ObjectWriter thread.");
									}
								}

								if (foundAThread)
								{
									break;
								}
								else
								{
									debug("$typeInfo.propsQuery", "All ObjectWriter threads are busy, waiting for a thread to become available.");
								}

								try
								{
									wait(1000);
								}
								catch (Exception e)
								{
									debug("$typeInfo.propsQuery", "Exception:" + e.toString());
								}
							}

							ObjectInputStream       oin = new ObjectInputStream(pin);

							ob = oin.readObject();

							debug("$typeInfo.propsQuery", "value (from binary data):" + ob);
							pin.close();
							oin.close();
						}
						catch (Exception e)
						{
							debug("$typeInfo.propsQuery", "Data:" + binValues + " Exception:" + e.toString());
							e.printStackTrace();
						}

						if (ob != null)
						{
							resultsToCheck.put(name, ob);
						}
					}
				}


				// When all of the multi-row data has been dealt with, get the single row data.

				sql = "select name, text_value, value from " + typeName + " where keyname = '" + q.key + "' and write_order = 0";
				statement = conn.createStatement();
				resultSet = statement.executeQuery(sql);

				while (resultSet.next())
				{
					Object  ob = null;

					name = null;

					try
					{
						name = resultSet.getString(1);

						if (!(resultsToCheck.containsKey(name)))
						{


							// Only do the work if we do * not * have a
							// value for this name already.

							String  text_val = resultSet.getString(2);
							byte[]  b = null;

							try
							{
								b = resultSet.getBytes(3);
							}
							catch (NegativeArraySizeException e)
							{
								;
							}		// Thrown when there is no binary data.

							if (text_val != null)		// The value is stored as a text string.
							{
								ob = text_val;


								// System.out.println("***************got text_val:"+text_val);

								resultsToCheck.put(name, ob);
							}
							else if (b != null)		// The value is stored as a serialized object.
							{


								// System.out.println("***************got single-row binary data:"+new String(b));

								Vector  binaryData = new Vector();

								binaryData.addElement(b);

								try
								{
									PipedInputStream	pin = new PipedInputStream();
									boolean foundAThread = false;

									while (true)
									{
										for (int i = 0; i < NUM_WRITE_THREADS; i++)
										{
											ObjectWriterThread      t = (ObjectWriterThread) writeThreads.elementAt(i);

											if (!t.isBusy())
											{
												foundAThread = true;

												t.connect(pin);
												t.setByteData(binaryData);
												t.start();

												i = NUM_WRITE_THREADS;
											}
											else
											{
												debug("$typeInfo.propsQuery", "Waiting for an ObjectWriter thread.");
											}
										}

										if (foundAThread)
										{
											break;
										}
										else
										{
											debug("$typeInfo.propsQuery", "All ObjectWriter threads are busy, waiting for a thread to become available.");
										}

										try
										{
											wait(1000);
										}
										catch (Exception e)
										{
											debug("$typeInfo.propsQuery", "Exception:" + e.toString());
										}
									}

									ObjectInputStream       oin = new ObjectInputStream(pin);

									ob = oin.readObject();

									debug("$typeInfo.propsQuery", "value (from binary data):" + ob);
									pin.close();
									oin.close();
								}
								catch (Exception e)
								{
									debug("$typeInfo.propsQuery", "Data:" + binaryData + " Exception:" + e.toString());
									e.printStackTrace();
								}
							}
							else
							{


								// System.out.println("***************no values to add from this row.");

								continue;
							}
						}
					}
					catch (Exception e)
					{
						error("$TypeInfo.propsQuery", "Exception caught.", e);
						e.printStackTrace();
					}

					if ((ob != null) && (name != null))
					{
						resultsToCheck.put(name, ob);
					}
				}

				statement.close();

				if(q.value!=null) {
				    Enumeration     n = resultsToCheck.keys();
				    
				    while (n.hasMoreElements())
					{
					    Object  key = n.nextElement();
					    Object  obj = resultsToCheck.get(key);
					    
					    if (obj != null)
						{
						    if (q.value.equals(obj))
							{
							    v.addElement(new ObjectID(typeName, (String) key));
							}
						}
					}
				}
				else {
				    Enumeration     n = resultsToCheck.keys();
				    
				    while (n.hasMoreElements())
					{
					    Object  key = n.nextElement();
					    v.addElement(new ObjectID(typeName, (String) key));
					}
				}
				    
				conn.close();
			}
			catch (SQLException e)
			{
				sqlError("$TypeInfo.propsQuery", e);
			}


			// debug("$typeInfo.propsQuery","***** Returning vector: "+v.toString());

			return (v);
		}


	}

}

