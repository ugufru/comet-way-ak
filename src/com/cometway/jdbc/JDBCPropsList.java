package com.cometway.jdbc;

import java.io.IOException;

import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.sql.Blob;
import java.sql.Timestamp;

import javax.sql.rowset.serial.SerialBlob;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.Date;
import java.util.Calendar;

import java.text.SimpleDateFormat;

import com.cometway.util.FlushInterface;
import com.cometway.util.ReporterInterface;
import com.cometway.util.ObjectSerializer;

import com.cometway.props.Props;
import com.cometway.props.PropsListInterface;
import com.cometway.props.IPropsChangeListener;



/**
 * A PropsList interface for querying and adding rows to a table in a database.
 * Any changes to the Props listed by this PropsList will not be reflected in the database,
 * there is no PropsChangeListener listening to any changes in any of the Props listed or added.
 * Rows in the database cannot be updated, only new rows can be added. No local list of
 * Props are stored.
 */
public class JDBCPropsList implements PropsListInterface
{
	public static final String TABLENAME = "___INTERNAL_TABLE_NAME___";
	protected JDBCAgentInterface jdbcAgent;
	protected String table;
	protected Hashtable tableDescription;
	protected List columnNames;
	protected DatabaseMetaData metaData;
	protected ReporterInterface reporter;
	protected String indexColumn;

	public JDBCPropsList(JDBCAgentInterface agent, String tableName) throws SQLException
	{
		jdbcAgent = agent;
		table = tableName;
		tableDescription = new Hashtable();
		columnNames = new Vector();

		Connection con = agent.getConnection();

		metaData = con.getMetaData();

		// Try to get the index from the DatabaseMetaData

		ResultSet metaResults = metaData.getIndexInfo(null,null,table,true,false);

		while (metaResults.next())
		{
			String columnName = metaResults.getString(9);
			
			if(columnName!=null)
			{
				indexColumn = columnName;
				break;
			}
		}

		metaResults = metaData.getColumns(null,null,table,"%");

		try
		{
			while (metaResults.next())
			{
				String column_name = metaResults.getString(4);
				String type_name = metaResults.getString(6);
//System.out.println("***********************\nadding " + column_name + " (" + type_name + ")");

				columnNames.add(column_name);

				// Make the type lowercase to avoid different SQL casing

				tableDescription.put(column_name, type_name.toLowerCase());

				if(indexColumn==null)
				{
					// This may not be the right way to do this
					if(type_name.equalsIgnoreCase("int identity"))
					{
						indexColumn = column_name;
					}
				}
			}
		}
		catch(Exception e)
		{
			jdbcAgent.closeConnection(con);
			throw(new JDBCPropsListException("Could not fetch table column information",e));
		}

		if (columnNames.size() == 0)
		{
			jdbcAgent.closeConnection(con);
			throw(new JDBCPropsListException("Could not fetch table column information"));
		}

		jdbcAgent.closeConnection(con);
	}

	/**
	* Creates a new row in the table given by the TABLENAME property. It is up to the caller to ensure
	* This doesn't overwrite the contents of a row handled by another Props.
	*/

	public void addProps(Props p)
	{
		debug("Adding Props:\n"+p);

		SimpleDateFormat DB_DATE_FORMAT = null;

		if(!p.hasProperty(TABLENAME))
		{
			p.setProperty(TABLENAME,table);
		}

		Vector v = p.getKeys();

		StringBuffer sql = new StringBuffer();
		StringBuffer values = new StringBuffer();

		sql.append("INSERT INTO ");
		sql.append(table);
		sql.append(" (");
		values.append(" VALUES (");

		boolean first = true;

		if (v.size() > 0)
		{
			for (int x = 0; x < v.size() ; x++)
			{
				String key = (String) v.elementAt(x);
				String typeInfo = (String) tableDescription.get(key);

//debug("adding: " + key + " (" + typeInfo + ")");

				if (typeInfo!=null)
				{
					if (!first)
					{
						sql.append(",");
						values.append(",");
					}

					sql.append(key);

					if (typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1)
					{
						values.append("?");
					}
					else if (typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1)
					{
						//						values.append("?");
						// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate

						if (DB_DATE_FORMAT==null)
						{
							DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						}

						values.append("'");
						values.append(DB_DATE_FORMAT.format(p.getDate(key)));
						values.append("'");
					}
					else if (typeInfo.equals("bit"))
					{
						values.append(""+p.getBoolean(key));
					}
					else if (typeInfo.indexOf("int") != -1)
					{
						values.append(""+p.getInteger(key));
					}
					else
					{
						values.append("'");
						values.append(escape(p.getString(key)));
						values.append("'");
					}

					first = false;
				}
			}		
		}

		sql.append(")");
		values.append(")");
		sql.append(values);

		PreparedStatement statement = null;
		Connection connection = null;
		try {
			connection = jdbcAgent.getConnection();
			statement = connection.prepareStatement(sql.toString());
			debug("Creating PreparedStatement for addProps(): "+sql);
			int count = 1;
			for(int x=0;x<v.size();x++) {
				String key = (String)v.elementAt(x);
				String typeInfo = (String)tableDescription.get(key);
				if(typeInfo!=null) {
					try {
						if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
							Object o = p.getProperty(key);
							if(o instanceof byte[]) {
								statement.setBlob(count++,new SerialBlob((byte[])o));
							}
							else {
								try {
									statement.setBlob(count++,new SerialBlob(ObjectSerializer.serialize(o)));
								}
								catch(Exception e) {
									error("Fatal error, cannot serialize property '"+key+"' into SerialBlob",e);
									throw(new JDBCPropsListException("Key '"+key+"' could not be serialized for the binary column type in table: "+table,e));
								}
							}
						}
						// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
						//						else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
						//							statement.setTimestamp(count++,new Timestamp(((Date)p.getProperty(key)).getTime()));
						//						}
					}
					catch(ClassCastException e) {
						error("Key type ("+key+") incompatible with database column for table: "+table,e);
						throw(new JDBCPropsListException("Key type ("+key+") incompatible with database column for table: "+table,e));
					}
				}
			}		
			
			// This will always return false
			statement.execute();

			//			if(!statement.execute()) {
			//				error("Unable to insert into database table: "+table);
			//				throw(new JDBCPropsListException("Unable to insert into database table: "+table));
			//			}
		}
		catch(SQLException e) {
			error("Unable to insert into database table: "+table,e);
			error("Statement: "+sql);
			throw(new JDBCPropsListException("Unable to insert into database table: "+table,e));
		}
		finally {
			try {
				statement.close();
				jdbcAgent.closeConnection(connection);
			}
			catch(Exception e) {;}
		}
	}

	
	/**
	 * Not implemented, you must use addProps to create a row in a table.
	 */
	//	public Props createProps()
	//	{
	//		return(null);
	//	}


	/**
	 * Not implemented, changes take immediate effect.
	 */
	public void flush()
	{
		return;
	}


	/**
	 * Returns the first matching Props
	 */
	public Props getProps(String key, Object value)
	{
		debug("getProps("+key+","+value+")");
		Props rval = null;
		String typeInfo = null;
		SimpleDateFormat DB_DATE_FORMAT = null;

		// do SQL select
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT ");
		for(int x=0;x<columnNames.size();x++) {
			if(x!=0) {
				sql.append(",");
			}
			sql.append(columnNames.get(x).toString());
		}
		sql.append(" FROM ");
		sql.append(table);
		if(key!=null) {
			sql.append(" WHERE ");
			sql.append(key);
			if(value!=null) {
				typeInfo = (String)tableDescription.get(key);
				if(typeInfo!=null) {
					if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
						sql.append("=?");
					}
					else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
						//						sql.append("=?");
						// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
						if(DB_DATE_FORMAT==null) {
							DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						}
						sql.append("='");
						if(value instanceof Date) {
							sql.append(DB_DATE_FORMAT.format((Date)value));
						}
						else {
							sql.append(value.toString());
						}
						sql.append("'");
					}
					else {
						sql.append("='");
						sql.append(escape(value.toString()));
						sql.append("'");
					}
				}
			}
			else {
				throw(new JDBCPropsListException("The Key provided ("+key+") for the table '"+table+"' is given a null Value"));
			}
		}
		// mssql seems to get pissy about this line
		//		sql.append(" LIMIT 1");

// The MS SQL eqivalent of MySQL's LIMIT clause is TOP. Example:
//    SELECT TOP 10 * FROM stuff;
// Will return the top ten rows, effectively doing the same thing as
//    SELECT * FROM stuff LIMIT 10;

		// We add this in the event that there's a 'created' field
		if(columnNames.contains("created") && (((String)tableDescription.get("created")).indexOf("date")!=-1 || ((String)tableDescription.get("created")).indexOf("time")!=-1)) {
			sql.append(" ORDER BY created DESC");
		}

		PreparedStatement statement = null;
		Connection connection = null;
		try {
			connection = jdbcAgent.getConnection();
			statement = connection.prepareStatement(sql.toString());
			debug("Creating PreparedStatement for getProps(): "+sql);
			if(typeInfo!=null) {
				if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
					if(value instanceof byte[]) {
						statement.setBlob(1,new SerialBlob((byte[])value));
					}
					else {
						try {
							statement.setBlob(1,new SerialBlob(ObjectSerializer.serialize(value)));
						}
						catch(Exception e) {
							error("Fatal error, cannot serialize property '"+key+"' into SerialBlob",e);
							throw(new JDBCPropsListException("Key '"+key+"' could not be serialized for the binary column type in table: "+table,e));
						}
					}
					//					statement.setBlob(1,new SerialBlob((byte[])value));
				}
				// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
				//				else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
				//					statement.setTimestamp(1,new Timestamp(((Date)value).getTime()));
				//				}
			}
			if(statement.execute()) {
				ResultSet set = statement.getResultSet();
				List list = createPropsList(set);
				if(list.size()>0) {
					rval = (Props)list.get(0);
				}
			}
			else {
				error("Cannot select from table: "+table);
				throw(new JDBCPropsListException("Cannot select from table: "+table));
			}
		}
		catch(ClassCastException e) {
			error("Key type ("+key+") incompatible with database column for table: "+table,e);
			throw(new JDBCPropsListException("Key type ("+key+") incompatible with database column for table: "+table,e));
		}
		catch(SQLException e) {
			error("Unable to select from database table: "+table,e);
			error("Statement: "+sql);
			throw(new JDBCPropsListException("Unable to select from database table: "+table,e));
		}
		finally {
			try {
				statement.close();
				jdbcAgent.closeConnection(connection);
			}
			catch(Exception e) {;}
		}

		return(rval);
	}

	public List listProps()
	{
		return(listProps(null,null,null));
	}

	public List listProps(String key, Object value)
	{
		return(listProps(key,value,null));
	}

	/**
	 * Returns a list of matching Props
	 */
	public List listProps(String key, Object value, String sortBy)
	{
		debug("listProps("+key+","+value+")");

		List rval = new Vector();
		String typeInfo = null;
      SimpleDateFormat DB_DATE_FORMAT = null;

		// do SQL select

		StringBuffer sql = new StringBuffer();
		sql.append("SELECT ");
		for(int x=0;x<columnNames.size();x++) {
			if(x!=0) {
				sql.append(",");
			}
			sql.append(columnNames.get(x).toString());
		}
		sql.append(" FROM ");
		sql.append(table);
		if(key!=null) {
			sql.append(" WHERE ");
			sql.append(key);
			if(value!=null) {
				typeInfo = (String)tableDescription.get(key);
				if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
					sql.append("=?");
				}
				else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
					//						sql.append("=?");
					// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
					if(DB_DATE_FORMAT==null) {
						DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					}
					sql.append("='");
					if(value instanceof Date) {
						sql.append(DB_DATE_FORMAT.format((Date)value));
					}
					else {
						sql.append(value.toString());
					}
					sql.append("'");
				}
				else {
					sql.append("='");
					sql.append(escape(value.toString()));
					sql.append("'");
				}
			}
		}

		if(sortBy!=null) {
			sql.append(" ORDER BY ");
			sql.append(sortBy);
			sql.append(" ASC");
		}
		else {
			// We add this in the event that there's a 'created' field
			if(columnNames.contains("created") && (((String)tableDescription.get("created")).indexOf("date")!=-1 || ((String)tableDescription.get("created")).indexOf("time")!=-1)) {
				sql.append(" ORDER BY created DESC");
			}
		}

		PreparedStatement statement = null;
		Connection connection = null;
		try {
			connection = jdbcAgent.getConnection();
			statement = connection.prepareStatement(sql.toString());
			debug("Creating PreparedStatement for listProps(): "+sql);
			if(typeInfo!=null) {
				if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
					if(value instanceof byte[]) {
						statement.setBlob(1,new SerialBlob((byte[])value));
					}
					else {
						try {
							statement.setBlob(1,new SerialBlob(ObjectSerializer.serialize(value)));
						}
						catch(Exception e) {
							error("Fatal error, cannot serialize property '"+key+"' into SerialBlob",e);
							throw(new JDBCPropsListException("Key '"+key+"' could not be serialized for the binary column type in table: "+table,e));
						}
					}
					//					statement.setBlob(1,new SerialBlob((byte[])value));
				}
				// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
				//				else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
				//					statement.setTimestamp(1,new Timestamp(((Date)value).getTime()));
				//				}
			}
			if(statement.execute()) {
				ResultSet set = statement.getResultSet();
				rval = createPropsList(set);
			}
			else {
				error("Cannot select from table: "+table);
				throw(new JDBCPropsListException("Cannot select from table: "+table));
			}
		}
		catch(ClassCastException e) {
			error("Key type ("+key+") incompatible with database column for table: "+table,e);
			throw(new JDBCPropsListException("Key type ("+key+") incompatible with database column for table: "+table,e));
		}
		catch(SQLException e) {
			error("Unable to select from database table: "+table+"\n"+sql.toString(),e);
			error("Statement: "+sql);
			throw(new JDBCPropsListException("Unable to select from database table: "+table,e));
		}
		finally {
			try {
				statement.close();
				jdbcAgent.closeConnection(connection);
			}
			catch(Exception e) {;}
		}

		return(rval);
	}

	/**
	 * Returns a list of loosely matching Props using the SQL keyword 'LIKE'
	 */
	public List listPropsRegExMatching(String key, String sqlExpression)
	{
		debug("listPropsRegExMatching("+key+","+sqlExpression+")");

		List rval = new Vector();

		// do SQL select
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT ");
		for(int x=0;x<columnNames.size();x++) {
			if(x!=0) {
				sql.append(",");
			}
			sql.append(columnNames.get(x).toString());
		}
		sql.append(" FROM ");
		sql.append(table);
		if(key!=null) {
			sql.append(" WHERE ");
			sql.append(key);
			if(sqlExpression!=null) {
				sql.append(" LIKE '");
				sql.append(escape(sqlExpression));
				sql.append("'");
			}
		}

		// We add this in the event that there's a 'created' field
		if(columnNames.contains("created") && (((String)tableDescription.get("created")).indexOf("date")!=-1 || ((String)tableDescription.get("created")).indexOf("time")!=-1)) {
			sql.append(" ORDER BY created DESC");
		}

		Statement statement = null;
		Connection connection = null;
		try {
			connection = jdbcAgent.getConnection();
			statement = connection.createStatement();
			if(statement.execute(sql.toString())) {
				ResultSet set = statement.getResultSet();
				rval = createPropsList(set);
			}
			else {
				error("Cannot select from table: "+table);
				throw(new JDBCPropsListException("Cannot select from table: "+table));
			}
		}
		catch(SQLException e) {
			error("Unable to select from database table: "+table,e);
			error("Statement: "+sql);
			throw(new JDBCPropsListException("Unable to select from database table: "+table,e));
		}
		finally {
			try {
				statement.close();
				jdbcAgent.closeConnection(connection);
			}
			catch(Exception e) {;}
		}

		return(rval);
	}

	/**
	 * Identical to listPropsRegExMaching(key,sqlExpression)
	 */
	public List listPropsMatchingRegEx(String key, String sqlExpression)
	{
		return(listPropsRegExMatching(key,sqlExpression));
	}


	/**
	 * Deletes the row from the table represented by the Props, note that depending on the Props,
	 * it's possible to delete more than one row.
	 */
	public boolean removeProps(Props p)
	{
		debug("Removing Props: "+p);
		int count = 0;
      SimpleDateFormat DB_DATE_FORMAT = null;

		boolean rval = false;
		if(p.hasProperty(TABLENAME) && p.getString(TABLENAME).equals(table)) {
			// do SQL delete
			Vector specialKeys = new Vector();
			StringBuffer sql = new StringBuffer();
			sql.append("DELETE FROM ");
			sql.append(table);
			sql.append(" WHERE ");

			p.dump();
			if(indexColumn!=null && p.hasProperty(indexColumn)) {
				// The index column is guaranteed to be unique
				sql.append(indexColumn);
				String typeInfo = (String)tableDescription.get(indexColumn);
				if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
					specialKeys.addElement(indexColumn);
					sql.append("=?");
				}
				else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
					//					specialKeys.addElement(indexColumn);
					//					sql.append("=?");
					// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
					if(DB_DATE_FORMAT==null) {
						DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					}
					sql.append("='");
					sql.append(DB_DATE_FORMAT.format(p.getDate(indexColumn)));
					sql.append("'");
				}
				else {
					sql.append("='");
					sql.append(escape(p.getString(indexColumn)));
					sql.append("'");
				}
				count ++;
			}
			else {
				for(int x=0;x<columnNames.size();x++) {
					String name = columnNames.get(x).toString();
					if(p.hasProperty(name)) {
						if(count!=0) {
							sql.append(" AND ");
						}
						String typeInfo = (String)tableDescription.get(name);
						if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
							specialKeys.addElement(name);
							sql.append(name);
							sql.append("=?");
						}
						else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
							//							specialKeys.addElement(name);
							//							sql.append(name);
							//							sql.append("=?");
							// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
							if(DB_DATE_FORMAT==null) {
								DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							}
							sql.append("='");
							sql.append(DB_DATE_FORMAT.format(p.getDate(name)));
							sql.append("'");
						}
						else {
							sql.append(name);
							sql.append("='");
							sql.append(escape(p.getString(name)));
							sql.append("'");
						}
						count ++;
					}
				}
			}

			if(count>0) {
				PreparedStatement statement = null;
				Connection connection = null;
				try {
					connection = jdbcAgent.getConnection();
					statement = connection.prepareStatement(sql.toString());
					debug("Creating PreparedStatement for removeProps(): "+sql);
					for(int x=0;x<specialKeys.size();x++) {
						String key = (String)specialKeys.elementAt(x);
						String typeInfo = (String)tableDescription.get(key);
						if(typeInfo!=null) {
							try {
								if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
									Object o = p.getProperty(key);
									if(o instanceof byte[]) {
										statement.setBlob(x+1,new SerialBlob((byte[])o));
									}
									else {
										try {
											statement.setBlob(x+1,new SerialBlob(ObjectSerializer.serialize(o)));
										}
										catch(Exception e) {
											error("Fatal error, cannot serialize property '"+key+"' into SerialBlob",e);
											throw(new JDBCPropsListException("Key '"+key+"' could not be serialized for the binary column type in table: "+table,e));
										}
									}
									//									statement.setBlob(x+1,new SerialBlob((byte[])p.getProperty(key)));
								}
								// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
								//								else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
								//									statement.setTimestamp(x+1,new Timestamp(((Date)p.getProperty(key)).getTime()));
								//								}
							}
							catch(ClassCastException e) {
								error("Key type ("+key+") incompatible with database column for table: "+table,e);
								throw(new JDBCPropsListException("Key type ("+key+") incompatible with database column for table: "+table,e));
							}
						}
					}
					
					if(statement.executeUpdate()>0) {
						rval = true;
					}
					else {
						error("Cannot delete from table: "+table);
						throw(new JDBCPropsListException("Cannot delete from table: "+table));
					}
				}
				catch(SQLException e) {
					error("Unable to delete from database table: "+table+"\n"+sql.toString(),e);
					error("Statement: "+sql);
					throw(new JDBCPropsListException("Unable to delete from database table: "+table,e));
				}
				finally {
					try {
						statement.close();
						jdbcAgent.closeConnection(connection);
					}
					catch(Exception e) {;}
				}
			}
		}
		return(rval);
	}

	/**
	 * Deletes the row or rows from the table that has the given key/value.
	 */
	public boolean removeProps(String key, Object value)
	{
		Props p = new Props();
		p.setProperty(TABLENAME,table);
		p.setProperty(key,value);
		return(removeProps(p));
	}
	

	/**
	 * Updates all Props with matching key and value using the specificed Props.
	 * Returns true if Props were found and updated; false otherwise.
	 */
	public boolean updateProps(Props p, String key, Object value)
	{
		debug("updateProps("+key+","+value+")");
		boolean rval = false;
      SimpleDateFormat DB_DATE_FORMAT = null;

		if(columnNames.contains(key)) {
			List specialKeys = new Vector();
			int count = 0;
			StringBuffer sql = new StringBuffer();
			sql.append("UPDATE ");
			sql.append(table);
			sql.append(" SET ");
		
			for(int x=0;x<columnNames.size();x++) {
				String column = (String)columnNames.get(x);
				if(!column.equalsIgnoreCase(key) && p.hasProperty(column)) {
					String typeInfo = (String)tableDescription.get(column);
					if(typeInfo!=null) {
						if(count!=0) {
							sql.append(", ");
						}
						sql.append(column);
						if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
							specialKeys.add(column);
							sql.append("=? ");
						}
						else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
							//							specialKeys.add(column);
							//							sql.append("=? ");
							// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
							if(DB_DATE_FORMAT==null) {
								DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							}
							sql.append("='");
							sql.append(DB_DATE_FORMAT.format(p.getDate(column)));
							sql.append("'");
						}
						else if (typeInfo.equals("bit"))
						{
							sql.append("="+p.getBoolean(column));
							sql.append(" ");
						}
						else if (typeInfo.indexOf("int")!=-1)
						{
							sql.append("="+p.getInteger(column));
							sql.append(" ");
						}
						else {
							sql.append("='");
							sql.append(escape(p.getString(column)));
							sql.append("' ");
						}
						count++;
					}
				}
			}

			if(count>0) {
				sql.append(" WHERE ");
				sql.append(key);
				String typeInfo = (String)tableDescription.get(key);
				if(typeInfo!=null) {
					if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
						specialKeys.add(key);
						sql.append("=? ");
					}
					else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
						//						specialKeys.add(key);
						//						sql.append("=? ");
						// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
						if(DB_DATE_FORMAT==null) {
							DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						}
						sql.append("='");
						if(value instanceof Date) {
							sql.append(DB_DATE_FORMAT.format(value));
						}
						else {
							sql.append(value.toString());
						}
						sql.append("'");
					}
					else if (typeInfo.equals("bit"))
					{
						sql.append("="+value);
						sql.append(" ");
					}
					else if (typeInfo.indexOf("int")!=-1)
					{
						sql.append("="+value);
						sql.append(" ");
					}
					else {
						sql.append("='");
						sql.append(escape(value.toString()));
						sql.append("' ");
					}

					PreparedStatement statement = null;
					Connection connection = null;
					try {
						connection = jdbcAgent.getConnection();
						statement = connection.prepareStatement(sql.toString());
						debug("Creating PreparedStatement updateProps(): "+sql);
						for(int x=0;x<specialKeys.size();x++) {
							String column = (String)specialKeys.get(x);
							typeInfo = (String)tableDescription.get(column);
							try {
								if(typeInfo.indexOf("binary")!=-1 || typeInfo.indexOf("image")!=-1) {
									Object o = p.getProperty(column);
									if(column.equalsIgnoreCase(key)) {
										o = value;
									}
									if(o instanceof byte[]) {
										statement.setBlob(x+1,new SerialBlob((byte[])o));
									}
									else {
										try {
											statement.setBlob(x+1,new SerialBlob(ObjectSerializer.serialize(o)));
										}
										catch(Exception e) {
											error("Fatal error, cannot serialize property '"+column+"' into SerialBlob",e);
											throw(new JDBCPropsListException("Key '"+column+"' could not be serialized for the binary column type in table: "+table,e));
										}
									}
								}
								// This is to fix the epoch issue because java.sql.Timestamp requires a long to instantiate
								//								else if(typeInfo.indexOf("date")!=-1 || typeInfo.indexOf("time")!=-1) {
								//									statement.setTimestamp(x+1,new Timestamp(((Date)p.getProperty(column)).getTime()));
								//								}
							}
							catch(ClassCastException e) {
								error("Key type ("+column+") incompatible with database column for table: "+table,e);
								throw(new JDBCPropsListException("Key type ("+column+") incompatible with database column for table: "+table,e));
							}
						}

						//						if(!statement.execute()) {
						//							error("Unable to insert into database table: "+table);
						//							throw(new JDBCPropsListException("Unable to insert into database table: "+table));
						//						}
						//						else {

						// This statement always returns false
						statement.execute();
						rval = true;
						//						}
					}
					catch(SQLException e) {
						error("Unable to update database table: "+table,e);
						error("Statement: "+sql);
						throw(new JDBCPropsListException("Unable to update database table: "+table,e));
					}
					finally {
						try {
							statement.close();
							jdbcAgent.closeConnection(connection);
						}
						catch(Exception e) {;}
					}
				}
			}
		}
		return(rval);
	}
	





	/**
	 * Not implemented
	 */
	public void setFlushHandler(FlushInterface flushHandler)
	{
		;
	}


	/**
	 * Not implemented
	 */
	protected void setPropsChangeListener(IPropsChangeListener propsChangeListener)
	{
		;
	}



	/**
	 * Returns the database type of a column
	 */
	public String getColumnType(String columnName)
	{
		return((String)tableDescription.get(columnName));
	}


	/**
	 * Returns a list of the column names
	 */
	public List getColumnNames()
	{
		List rval = new Vector();
		for(int x=0;x<columnNames.size();x++) {
			rval.add(columnNames.get(x));
		}
		return(rval);
	}


	/**
	 * Types with "binary" or "image" in them are stored as byte[]
	 * Types with "date" or "time" in them are stored as a java.util.Date
	 */
	protected List createPropsList(ResultSet set) throws SQLException
	{
		List rval = new Vector();
		while(set.next()) {
			Props p = new Props();
			boolean notEmpty = false;
			for(int x=0;x<columnNames.size();x++) {
				String column = columnNames.get(x).toString();

				if(((String)tableDescription.get(column)).indexOf("binary")!=-1 || ((String)tableDescription.get(column)).indexOf("image")!=-1) {
					Blob value = set.getBlob(column);
					if(value!=null) {
						// Possible loss in value on this (int) cast here
						byte[] data = value.getBytes(1,(int)value.length());
						Object o = null;
						try {
							o = ObjectSerializer.unserialize(data);
						}
						catch(Exception e) {
							o = data;
						}
						p.setProperty(column,o);
						notEmpty = true;
					}
				}
				else if(((String)tableDescription.get(column)).indexOf("date")!=-1 || ((String)tableDescription.get(column)).indexOf("time")!=-1) {
					Date value = set.getDate(column);
					if(value!=null) {
						p.setProperty(column,value);
						notEmpty = true;
					}
				}
				else {
					String value = set.getString(column);
					if(value!=null && !value.equalsIgnoreCase("null")) {
						p.setProperty(column,value);
						notEmpty = true;
					}
				}
			}
			if(notEmpty) {
				p.setProperty(TABLENAME,table);
				rval.add(p);
			}
		}
		return(rval);
	}



	/**
	 * Used to escape apostrophes
	 */
	protected String escape(String value)
	{
		int index = value.indexOf('\'');

		while (index != -1)
		{
			value = value.substring(0, index) + "''" + value.substring(index + 1);

			index = value.indexOf('\'',index+2);
		}

		return (value);
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
					reporter.error(this, "   Error Code = " + x.getErrorCode());
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
	 * This method will go through all the properties in a Props and make sure any instances
	 * of an SQL Blob Object is converted to a SerialBlob. An Exception is thrown if an Object
	 * in the Props is not Serializable.
	 */
	public static void serializeBlobs(Props p) throws IOException
	{
		Enumeration names = p.enumerateKeys();
		while(names.hasMoreElements()) {
			String name = (String)names.nextElement();
			Object o = p.getProperty(name);
			if(o instanceof Blob) {
				try {
					Blob b = (Blob)o;
					byte[] data = b.getBytes(1,(int)b.length());
					Object o2 = null;
					try {
						o2 = ObjectSerializer.unserialize(data);
					}
					catch(Exception e) {
						o2 = data;
					}

					p.setProperty(name,o2);
				}
				catch(SQLException e) {
					//					error("Bad SQL Blob from property '"+name+"'",e);
				}
			}
		}
	}
}
