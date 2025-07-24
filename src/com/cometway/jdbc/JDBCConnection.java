
package com.cometway.jdbc;


import com.cometway.ak.ServiceManager;
import com.cometway.swing.LightBox;
import com.cometway.util.ReporterInterface;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/** These imports are required for JDK 1.6 */

import java.sql.Array;
import java.sql.SQLClientInfoException;
import java.sql.Struct;

/**
* This class implements a pool-able JDBCConnection.
*/

public class JDBCConnection implements Connection
{
	private final static SimpleDateFormat SDF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z");
	private final static String VALIDATE_SQL = "SELECT 1";

	protected static int uniqueIndex = 0;

	protected static final int DEFAULT_MAX_ROW_SIZE = 255;

	protected ReporterInterface reporter;

	private int index = 0;
	private String uniqueID;
	private String jdbcURL;
	private JDBCConnectionPool ownerPool;
	private boolean inuse;
	private long leaseTimeStamp;

	private Driver driver;
	private Connection connection;
	private DatabaseMetaData dbmd;
	private List typeInfo;


	/**
	* Users will probably not need to call the constructor directly;
	* the connection is typically created by a ConnectionPoolManager.
	* @param jdbcURL The JDBC URL used to open the Connection.
	* @param connection The JDBC Connection to use
	* @param ownerPool The ThreadPool to use when performing lazy database modifications
	* @param reporter The ReporterInterface to use for reporting trace and diagnostic information.
	*/

	public JDBCConnection(String jdbcURL, Connection connection, JDBCConnectionPool ownerPool, ReporterInterface reporter)
	{
		this.jdbcURL = jdbcURL;
		this.index = uniqueIndex++;
		this.uniqueID = "JDBCConnection[" + index + "] " + jdbcURL;
		this.connection = connection;
		this.ownerPool = ownerPool;
		this.reporter = reporter;
		this.inuse = false;
		this.leaseTimeStamp = 0;

		LightBox box = (LightBox) ServiceManager.getService("light_box");

		if (box != null)
		{
			box.setCellState(index, LightBox.GREEN);
			box.drawChangedCells();
		}

		if (connection != null)
		{
			try
			{
				dbmd = getMetaData();
			}
			catch (SQLException e)
			{
				error("Could create JDBCConnection", e);
			}
		}
	}


	/**
	* @param jdbcDriver The JDBC Driver to use
	* @param jdbcURL The JDBC URL to use
	* @param username The username to use for DB access
	* @param password The password to use for DB access
	*/

	public JDBCConnection(String jdbcDriver, String jdbcURL, String username, String password)
	{ 
	    openDatabase(jdbcDriver, jdbcURL, username, password);

		if (connection != null)
		{
			try
			{
				dbmd = getMetaData();
			}
			catch (SQLException e)
			{
				error("Could create JDBCConnection", e);
			}
		}
	    
	}


	/**
	* Called by the JDBCConnectionPool.
	*/

	public synchronized boolean lease() 
	{
		if (inuse)
		{
			return (false);
		}
		else
		{
			LightBox box = (LightBox) ServiceManager.getService("light_box");

			if (box != null)
			{
				box.setCellState(index, LightBox.RED);
				box.drawChangedCells();
			}

			inuse = validate();

			if (inuse)
			{
				leaseTimeStamp = System.currentTimeMillis();

				debug("Leased at " + SDF.format(new Date(leaseTimeStamp)));
			}

			return (true);
		}
	}


    /**
     * Called by the ConnectionReaper to determine if
     * this connection should be freed.
     */
    
    public boolean validate() 
    {
		LightBox box = (LightBox) ServiceManager.getService("light_box");

	    try
		{
			Statement statement	= connection.createStatement();
			ResultSet result = statement.executeQuery(VALIDATE_SQL);
			statement.close();

			if (box != null)
			{
				box.setCellState(index, LightBox.BLUE);
				box.drawChangedCells();
			}
		}
		catch (Exception e)
		{
			if (box != null)
			{
				box.setCellState(index, LightBox.YELLOW);
				box.drawChangedCells();
			}

	        return false;
	    }

	    return true;
    }


	/**
	* Returns true if the JDBCConnection is currently in use; false otherwise.
	*/

    public boolean inUse() 
    {
        return inuse;
    }


	/**
	* Returns a long containing the timestamp when this connection was leased.
	*/

    public long getLastUse() 
    {
        return (leaseTimeStamp);
    }


	/**
	* It is really important to call close when you are done
	* using a pooled JDBCConnection because un-closed connections
	* will be unusable until the ConnectionReaper gets around to 
	* freeing them.
	* 
	* @exception java.sql.SQLException if there was a problem closing the connection
	*/

    public void close() throws SQLException 
    {
        ownerPool.returnConnection(this);
    }


	/**
	* Called by the ConnectionReaper to forcibly expire this lease.
	*/

    protected void expireLease() 
    {
		LightBox box = (LightBox) ServiceManager.getService("light_box");

		if (box != null)
		{
			box.setCellState(index, LightBox.GREEN);
			box.drawChangedCells();
		}

        inuse = false;
    }


	/**
	* Should return the maximum row size supported by the current
	* database but JDBC drivers have problems reporting MAX_ROW_SIZE
	* accurately so MAX_ROW_SIZE is hard-coded to be 255.
	*/

    public int getMaxRowSize()
    {
        int retval = 0;

        try
		{
            retval = dbmd.getMaxRowSize();
		}
		catch (SQLException e)
		{
			warning("getMaxRowSize: JDBCDriver doesn't know the maximum row size for this database (default: " + DEFAULT_MAX_ROW_SIZE + ")", e);

			retval = DEFAULT_MAX_ROW_SIZE;
		}

        return (retval);
    }


	/**
	* Builds a type information list specific to this driver connection.
	* The type information is used internally to construct dynamic queries.
	* This method only needs to be called once.
	* See: DatabaseMetaData.getTypeInfo()
	*/

	public void buildDataTypeInfo()
	{
		DataTypeInfo info;
		
		if (typeInfo == null)
		{
			typeInfo = new Vector();

			try
			{
				ResultSet rs = dbmd.getTypeInfo();

				while (rs.next())
				{
					info = new DataTypeInfo();

					info.typeName = rs.getString("TYPE_NAME");
					info.dataType = (int) rs.getShort("DATA_TYPE");
					info.literalPrefix = rs.getString("LITERAL_PREFIX");
					info.literalSuffix = rs.getString("LITERAL_SUFFIX");
					info.createParams = rs.getString("CREATE_PARAMS");

					typeInfo.add(info);
				}
				
				rs.close();
			}
			catch (SQLException e)
			{
				error("buildDataTypeInfo", e);
			}
		}
		
		// typeInfo should NOT be null by the time the program flow gets here;
		// if typeInfo is null, the jdbc driver doesn't implement the dbmd api.
		
		if (typeInfo == null) throw new NullPointerException("typeInfo is null (dbmd.getTypeInfo() isn't supported).");
	}


	/**
	 * Executes the query contained in the argument, returning results as a String.
	 * @param sql The query to execute.
	 */


	public String executeQuery(String sql)
	{
		StringBuffer s = new StringBuffer("% " + sql + "\n");

		try
		{
			Statement statement	= connection.createStatement();
			ResultSet result = statement.executeQuery(sql);

			while (result.next())
			{
				s.append(result.getString(1) + "\n");
			}

			statement.close();
		}
		catch (SQLException e)
		{
			s.append("# " + e);
		}

		return (s.toString());
	}


	/**
	* Executes the update contained in the argument,
	* returning results as a String.
	* @param sql The update to execute.
	*/

	public String executeUpdate(String sql)
	{
		StringBuffer s = new StringBuffer("% " + sql + "\n");

		try
		{
			Statement statement = connection.createStatement();;
			int rowsAffected = statement.executeUpdate(sql);

			s.append(rowsAffected + " rows affected.\n");

			statement.close();

		}
		catch (SQLException e)
		{
			s.append("# " + e);
		}

		return (s.toString());
	}


	/**
	* Returns the actual java.sql.Connection object
	* used by this JDBCConnection.
	*/
    
	public Connection getConnection()
	{
		return (connection);
	}


	/**
	* Returns the String used to load the JDBC driver.
	*/

	public Driver getDriver()
	{
		return (driver);
	}


	/**
	* Returns the current status information and performance
	* data for this connection.
	*/

	public String getStatus()
	{
		StringBuffer b = new StringBuffer();
        
		if (connection == null)
		{
			b.append("        Connected: false\n");
		}
		else
		{
			try
			{
				b.append("        Connected: true\n");
				b.append("        User Name: " + dbmd.getUserName() + "\n");
				b.append("     Database URL: " + dbmd.getURL() + "\n");
				b.append("         Database: " + dbmd.getDatabaseProductName() + "\n");
				b.append(" Database Version: " + dbmd.getDatabaseProductVersion() + "\n");
				b.append("           Driver: " + dbmd.getDriverName() + "\n");
				b.append("   Driver Version: " + dbmd.getDriverVersion() + "\n");
				b.append("  Max Connections: " + dbmd.getMaxConnections() + "\n");
				b.append("   Max Statements: " + dbmd.getMaxStatements() + "\n");
				b.append("Stored Procedures: " + dbmd.supportsStoredProcedures() + "\n");
			}
			catch (SQLException e)
			{
				error("getStatus", e);
				b.append(e);
			}
		}

		return (b.toString());
	}


	/**
	* Get the String name for the DataType with this ID.
	* @param dataType
	*/

	public String getTypeName(int dataType)
	{
		String name = "UNKNOWN";
		DataTypeInfo info;

		buildDataTypeInfo();

		// Find the string for the requested data type.

		int count = typeInfo.size();

		for (int i = 0; i < count ; i++)
		{
			info = (DataTypeInfo) typeInfo.get(i);

			if (info.dataType == dataType)
			{
				name = info.typeName;
				break;
			}
		}

		return (name);
	}


	/**
	* Opens a connection to a database using the specified connection parameters.
	*/

	private boolean openDatabase(String jdbcDriver, String jdbcURL, String username, String password)
	{
		boolean result = false;

		debug("openDatabase( " + jdbcDriver + ", " + jdbcURL + ", " + username + " )");

		try
		{
        	driver = (Driver) Class.forName(jdbcDriver).newInstance();

            debug("Connecting to " + jdbcURL);
            
			Properties p = new Properties();
			p.put("username", username);
			p.put("password", password);

			index = uniqueIndex++;
			uniqueID = "JDBCConnection[" + index + "] " + jdbcURL;

			setConnection(driver.connect(jdbcURL, p));

			debug("Connection established.");
			
			result = true;
		}
		catch (Exception e)
		{
			error("Unable to open database", e);
		}

		return (result);
	}

	/**
	* Sets the JDBC Connection and initializes the database MetaData
	* used by this class.
	*/

	protected void setConnection(Connection connection)
	{
		this.connection = connection;

		driver = null;
		typeInfo = null;
        dbmd = null;
        
		if (connection != null)
		{
			try
			{
				dbmd = getMetaData();
			}
			catch (SQLException e)
			{
				error("Could not set connection", e);
			}
		}
	}


	/**
	* Prints a debug message tagged with this Connection's identity to the output stream.
	*/

	public void debug(String message)
	{
		if (reporter != null)
		{
			reporter.debug(this, message);
		}
	}


	/**
	* Prints a warning message tagged with this Connection's identity to the error stream.
	* If the Exception is type java.lang.reflect.InvocationTargetException
	* that exception's target exception is also stack traced.
	*/

	public void warning(String message)
	{
		if (reporter != null)
		{
			reporter.warning(this, message);
		}
	}


	/**
	* Prints an warning message tagged with this Connection's identity
	* followed by a stack trace of the passed Exception to error stream.
	* If the Exception is type java.lang.reflect.InvocationTargetException
	* that exception's target exception is also stack traced.
	*/

	public void warning(String message, Exception e)
	{
		if (reporter != null)
		{
			reporter.warning(this, message, e);
		}
	}


	/**
	* Prints an error message tagged with this Connection's identity to the error stream.
	* If the Exception is type java.lang.reflect.InvocationTargetException
	* that exception's target exception is also stack traced.
	*/

	public void error(String message)
	{
		if (reporter != null)
		{
			reporter.error(this, message);
		}
	}


	/**
	* Prints an error message tagged with this Connection's identity
	* followed by a stack trace of the passed Exception to the error stream.
	* If the Exception is type java.lang.reflect.InvocationTargetException
	* that exception's target exception is also stack traced.
	*/

	public void error(String message, Exception e)
	{
		if (reporter != null)
		{
			if (e instanceof SQLException)
			{
				SQLException x = (SQLException) e;

				reporter.error(this, message);

				do
				{
					reporter.error(this, "   SQLState = " + x.getSQLState(), x);

					x = x.getNextException();
				}
				while (x != null);
			}
			else
			{
				reporter.error(this, message, e);
			}
		}
	}


	/**
	* Prints a message tagged with this connection's identity to the output stream.
	*/

	public void println(String message)
	{
		if (reporter != null)
		{
			reporter.println(this, message);
		}
	}


	/**
	* Sets the reporter for this Connection.
	*/

	public void setReporter(ReporterInterface reporter)
	{
		this.reporter = reporter;
	}


	/**
	* Returns this connection as a String.
	*/

	public String toString()
	{
		return (uniqueID);
	}


	//--------------------------------------------------------------------------

	
    /*
    * The rest of the methods in this class are required
    * to make the class behave like a java.sql.Connection.
    */
    
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{ 
	    return (connection.prepareStatement(sql));
	}


	public PreparedStatement prepareStatement(String sql, int parm2, int parm3) throws SQLException
	{ 
	    return (connection.prepareStatement(sql, parm2, parm3));
	}


    public CallableStatement prepareCall(String sql) throws SQLException
	{
        return connection.prepareCall(sql);
    }


    public CallableStatement prepareCall(String sql, int parm2, int parm3) throws SQLException
	{
        return connection.prepareCall(sql, parm2, parm3);
    }


    public Statement createStatement() throws SQLException
	{
        return connection.createStatement();
    }


    public Statement createStatement(int parm1, int parm2) throws SQLException
	{
        return connection.createStatement(parm1, parm2);
    }


	public String nativeSQL(String sql) throws SQLException
	{
        return connection.nativeSQL(sql);
    }


	public Map getTypeMap() throws SQLException
	{
        return connection.getTypeMap();
    }


	public void setTypeMap(Map map) throws SQLException
	{
        connection.setTypeMap(map);
    }


    public void setAutoCommit(boolean autoCommit) throws SQLException
	{
        connection.setAutoCommit(autoCommit);
    }


    public boolean getAutoCommit() throws SQLException
	{
        return connection.getAutoCommit();
    }


    public void commit() throws SQLException
	{
        connection.commit();
    }


    public void rollback() throws SQLException
	{
        connection.rollback();
    }

/*
    public void rollback(Savepoint spoint) throws SQLException {
        connection.rollback(spoint);
    }
*/

    public boolean isClosed() throws SQLException
	{
        return connection.isClosed();
    }


    public DatabaseMetaData getMetaData() throws SQLException
	{
        return connection.getMetaData();
    }


    public void setReadOnly(boolean readOnly) throws SQLException
	{
        connection.setReadOnly(readOnly);
    }


    public boolean isReadOnly() throws SQLException
	{
        return connection.isReadOnly();
    }


    public void setCatalog(String catalog) throws SQLException
	{
        connection.setCatalog(catalog);
    }


    public String getCatalog() throws SQLException
	{
        return connection.getCatalog();
    }


    public void setTransactionIsolation(int level) throws SQLException
	{
        connection.setTransactionIsolation(level);
    }


    public int getTransactionIsolation() throws SQLException
	{
        return connection.getTransactionIsolation();
    }


    public SQLWarning getWarnings() throws SQLException
	{
        return connection.getWarnings();
    }


    public void clearWarnings() throws SQLException
	{
        connection.clearWarnings();
    }


	// These methods are needed when using JDK 1.4 or later

    public CallableStatement prepareCall(String sql, int parm2, int parm3, int parm4) throws SQLException
	{
        return connection.prepareCall(sql, parm2, parm3, parm4);
    }


	public PreparedStatement prepareStatement(String sql, String[] parm2) throws SQLException
	{ 
	    return (connection.prepareStatement(sql, parm2));
	}


	public PreparedStatement prepareStatement(String sql, int parm2) throws SQLException
	{
	    return (connection.prepareStatement(sql, parm2));
	}


	public PreparedStatement prepareStatement(String sql, int[] parm2) throws SQLException
	{
	    return (connection.prepareStatement(sql, parm2));
	}


	public PreparedStatement prepareStatement(String sql, int parm2, int parm3, int parm4) throws SQLException
	{
	    return (connection.prepareStatement(sql, parm2, parm3, parm4));
	}


    public Statement createStatement(int parm1, int parm2, int parm3) throws SQLException
	{
        return connection.createStatement(parm1, parm2, parm3);
    }


	public int getHoldability() throws SQLException
	{
        return connection.getHoldability();
    }


	public void setHoldability(int parm1) throws SQLException
	{
        connection.setHoldability(parm1);
    }


    public Savepoint setSavepoint() throws SQLException
	{
        return connection.setSavepoint();
    }


    public Savepoint setSavepoint(String savepnt) throws SQLException
	{
        return connection.setSavepoint(savepnt);
    }


    public void releaseSavepoint(Savepoint savepnt) throws SQLException
	{
        connection.releaseSavepoint(savepnt);
    }


	public void rollback(java.sql.Savepoint sp) throws SQLException
	{
		connection.rollback(sp);
	}


	//--------------------------------------------------------------------------

	
	// The following methods are neccessary for compiling under JDK 1.6.x:

/* begin JDK 1.6 added code */
	public Struct createStruct(String s, Object[] o) throws SQLException
	{
		return connection.createStruct(s,o);
	}

	public Array createArrayOf(String s, Object[] o) throws SQLException
	{
		return connection.createArrayOf(s,o);
	}


	public Properties getClientInfo() throws SQLException
	{
		return connection.getClientInfo();
	}

	public void setClientInfo(java.util.Properties ps) throws SQLClientInfoException
	{
		connection.setClientInfo(ps);
	}

	
	public void setClientInfo(String s1, String s2) throws SQLClientInfoException
	{
		connection.setClientInfo(s1, s2);
	}


	public boolean isValid(int i) throws SQLException
	{
		return connection.isValid(i);
	}


	public java.sql.SQLXML createSQLXML() throws SQLException
	{
		return connection.createSQLXML();
	}


	public java.sql.NClob createNClob() throws SQLException
	{
		return connection.createNClob();
	}


	public java.sql.Blob createBlob() throws SQLException
	{
		return connection.createBlob();
	}	


	public java.sql.Clob createClob() throws SQLException
	{
		return connection.createClob();
	}


	public boolean isWrapperFor(Class iface) throws SQLException
	{
		return connection.isWrapperFor(iface);
	}


	public Object unwrap(Class iface) throws SQLException
	{
		return connection.unwrap(iface);
	}

	public String getClientInfo(String s) throws SQLException
	{
		return connection.getClientInfo(s);
	}
/* end JDK 1.6 added code */

	/* Java 21 required methods */
	public int getNetworkTimeout() throws SQLException
	{
		return connection.getNetworkTimeout();
	}

	public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException
	{
		connection.setNetworkTimeout(executor, milliseconds);
	}

	public void abort(java.util.concurrent.Executor executor) throws SQLException
	{
		connection.abort(executor);
	}

	public String getSchema() throws SQLException
	{
		return connection.getSchema();
	}

	public void setSchema(String schema) throws SQLException
	{
		connection.setSchema(schema);
	}

/* end Java 21 added code */

	//--------------------------------------------------------------------------
	// Inner classes start here.
	//--------------------------------------------------------------------------


	class DataTypeInfo
	{
		int dataType;
		int maxRowSize;
		String typeName;
		String literalPrefix;
		String literalSuffix;
		String createParams;


		public String toString()
		{
			return (dataType + ":" + typeName);
		}
	}
}


