
package com.cometway.jdbc;


import com.cometway.ak.Agent;
import com.cometway.props.Props;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.List;
import java.util.Vector;


/**
* The JDBCAgent manages a JDBC connection and performs simple database
* tasks such as creating and dropping tables, and executing queries,
* inserts and updates.
*/

public class JDBCAgent extends Agent implements JDBCAgentInterface
{
	/**
	* Initializes the Props for this agent:
	* "jdbc_driver" is the classname of the JDBC driver to use (default: sun.jdbc.odbc.JdbcOdbcDriver),
	* "jdbc_url" is the JDBC URL of the database (ie: jdbc:odbc:mydb),
	* "jdbc_username" is the database login username (default: none),
	* "jdbc_password" is the database login password (default: none),
	* "service_name" is the service_name used to register this agent
	* if it is to be used as a service (default: none).
	*/

	public void initProps()
	{
		setDefault("jdbc_driver", "sun.jdbc.odbc.JdbcOdbcDriver");
		setDefault("jdbc_url", "jdbc:odbc:mydb");
		setDefault("jdbc_username", "none");
		setDefault("jdbc_password", "none");
		setDefault("service_name", "none");
	}


	/**
	* Starts this agent by 
	*/

	public void start()
	{
		String jdbc_driver = getTrimmedString("jdbc_driver");
		String jdbc_url = getTrimmedString("jdbc_url");
		String jdbc_username = getTrimmedString("jdbc_username");
		String jdbc_password = getTrimmedString("jdbc_password");
		String service_name = getTrimmedString("service_name");

		if (jdbc_username.equals("none")) removeProperty("jdbc_username");
		if (jdbc_password.equals("none")) removeProperty("jdbc_password");

		if ((service_name.length() > 0) && (service_name.equals("none") == false))
		{
			register();
		}

		debug("jdbc_driver   = " + jdbc_driver);
		debug("jdbc_url      = " + jdbc_url);
		debug("jdbc_username = " + jdbc_username);
//		debug("jdbc_password = " + jdbc_password);
	}


	public void stop()
	{
		String service_name = getTrimmedString("service_name");

		if ((service_name.length() > 0) && (service_name.equals("none") == false))
		{
			unregister();
		}
	}


	//--------------------------------------------------------------------------
	

	/**
	* Properly closes the specified Connection. All Connections issued by getConnection should be
	* closed using this method when they are no longer needed.
	*/
	
	public void closeConnection(Connection connection)
	{
		if (connection != null)
		{
			try
			{
				connection.close();

				debug("Connection closed.");
			}
			catch (Exception e)
			{
				error("closeConnection", e);
			}
		}
	}


	/**
	* Creates the specified table using the specified parameters.
	* Elements in fieldList and typeList are Strings.
	* The type is a string similar to "varchar (5) NOT NULL" part
	* of a sql CREATE statement. Remember to quote table and field
	* names appropriately for your SQL server.
	*/
	
	public boolean createTable(String tableName, List fieldList, List typeList)
	{
		StringBuffer sql = new StringBuffer();
		sql.append("CREATE TABLE ");
		sql.append(tableName);
		sql.append("\n(\n");

		int count = fieldList.size();

		for (int i = 0; i < count; i++)
		{
			String field = (String) fieldList.get(i);
			String type = (String) typeList.get(i);

			sql.append("   ");
			sql.append(field);
			sql.append(' ');
			sql.append(type);

			if (i < (count - 1))
			{
				sql.append(',');
			}

			sql.append('\n');
		}

		sql.append(')');

		return (executeUpdate(sql.toString()));
	}


	/**
	* Drops the specified table from the databsae.
	*/
	
	public boolean dropTable(String tableName)
	{
		String sql = "DROP TABLE " + tableName;

		return (executeUpdate(sql));
	}


	/**
	* Overrided to fully report SQLExceptions.
	*/

	public void error(String message, Exception e)
	{
		if (e instanceof SQLException)
		{
			SQLException ee = (SQLException) e;

			error(message);

			while (ee != null)
			{
				StringBuffer b = new StringBuffer(ee.getMessage());
				b.append(" (SQLState = ");
				b.append(ee.getSQLState());
				b.append(", Error Code = ");
				b.append(ee.getErrorCode());
				b.append(")");

				super.error(b.toString(), e);

				ee = ee.getNextException();
			}
		}
		else
		{
			super.error(message, e);
		}
	}


	/**
	* Properly escapes apostrophes in the String literal for use in an SQL statement.
	*/
	
	public static String escapeApostrophes(String value)
	{
		int index = value.indexOf('\'');

		while (index != -1)
		{
			value = value.substring(0, index) + "''" + value.substring(index + 1, value.length());

			index = value.indexOf('\'',index+2);
		}

		return (value);
	}


	/**
	* Inserts the specified Props values into a table.
	* Returns true if successful; false otherwise.
	*/
	
	public boolean executeInsert(String tableName, Props p)
	{
		String sql = getInsertPropsSQL(tableName, p);

		return (executeUpdate(sql));
	}


	/**
	* Executes the specified SQL query, returning the result as a Vector of Props.
	*/

	public Vector executeQuery(String sql)
	{
		Vector results = new Vector();

		Connection connection = null;
		Statement statement = null;

		try
		{
			if (sql.indexOf("\n") >= 0)
			{
				debug("executeQuery:\n" + sql);
			}
			else
			{
				debug("executeQuery: " + sql);
			}

			connection = getConnection();

			statement = connection.createStatement();

			ResultSet rs = statement.executeQuery(sql);
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			String[] columnName = new String[columnCount];

			for (int n = 0; n < columnCount; n++)
			{
				columnName[n] = rsmd.getColumnName(n + 1);
			}

			while (rs.next())
			{
				Props p = new Props();

				for (int i = 0; i < columnCount; i++)
				{
					p.setProperty(columnName[i], rs.getObject(i + 1));
				}

				results.addElement(p);
			}
		}
		catch (Exception e)
		{
			error("executeQuery: " + sql, e);
		}
		finally
		{
			closeConnection(connection);
		}

		return (results);
	}


	/**
	* Executes a search query based on a unique key and value, returning the result as a Vector of Props.
	*/

	public Vector executeQuery(String tableName, String uniqueKey, String uniqueValue)
	{
		String sql = "SELECT * FROM " + tableName + " WHERE " + uniqueKey + "='" + uniqueValue + "'";

		return (executeQuery(sql));
	}


	/**
	* Executes the specified update SQL, returning true if the update succeeded; false otherwise.
	*/
	
	public boolean executeUpdate(String sql)
	{
		boolean success = false;
		Connection connection = null;
		Statement statement = null;

		try
		{
			if (sql.indexOf("\n") >= 0)
			{
				debug("executeUpdate:\n" + sql);
			}
			else
			{
				debug("executeUpdate: " + sql);
			}

//			for (int i = 0; i < 3; i++)
//			{
//				try
//				{
					connection = getConnection();
					statement = connection.createStatement();
					statement.executeUpdate(sql);
					success = true;
//					break;
//				}
//				catch (SQLException x)
//				{
//					if (x.getMessage().contains("Broken pipe") == false)
//					{
//						throw x;
//					}
//				}
//			}
				
		}
		catch (Exception e)
		{
			error("executeUpdate: " + sql, e);
		}
		finally
		{
			closeConnection(connection);
		}

		return (success);
	}


	/**
	* Updates the specified Props into the record specified by uniqueKey and uniqueValue.
	* Returns true if successful; false otherwise.
	*/
	
	public boolean executeUpdate(String tableName, Props p, String uniqueKey, String uniqueValue)
	{
		Vector keys = p.getKeys();
		StringBuffer sql = new StringBuffer();

		sql.append("UPDATE ");
		sql.append(tableName);
		sql.append(" SET ");

		for (int i = 0; i < keys.size(); i++)
		{
			String key = (String) keys.get(i);

			// no need to update the same key we use to find our record.

			if (key.equals(uniqueKey))
			{
				continue;
			}

			if (i > 0)
			{
				sql.append(", ");
			}

			sql.append(key);
			sql.append('=');

			String value = escapeApostrophes(p.getString(key));

			sql.append('\'');
			sql.append(value);
			sql.append('\'');
		}

		sql.append(" WHERE ");
		sql.append(uniqueKey);
		sql.append('=');
		sql.append(uniqueValue);

		return (executeUpdate(sql.toString()));
	}


	/**
	* Generates appropriate SQL for inserting the specified Props into a table.
	*/
	
	public String getInsertPropsSQL(String tableName, Props p)
	{
		Vector keys = p.getKeys();
		StringBuffer sql = new StringBuffer();

		sql.append("INSERT INTO ");
		sql.append(tableName);
		sql.append(" (");

		for (int i = 0; i < keys.size(); i++)
		{
			if (i > 0)
			{
				sql.append(", ");
			}

			sql.append((String) keys.get(i));
		}

		sql.append(" ) VALUES (");

		for (int n = 0; n < keys.size(); n++)
		{
			String value = p.getString((String) keys.get(n));

			value = escapeApostrophes(value);

			if (n > 0)
			{
				sql.append(", ");
			}

			sql.append('\'');
			sql.append(value);
			sql.append('\'');
		}

		sql.append(" )");

		return (sql.toString());
	}


	/**
	* Creates a new connection to the database using the current JDBC related settings.
	*/
	
	public Connection getConnection() throws SQLException
	{
		Connection connection = null;

		String jdbc_driver = getString("jdbc_driver");
		String jdbc_url = getString("jdbc_url");
		String jdbc_username = getString("jdbc_username");
		String jdbc_password = getString("jdbc_password");

		Properties p = new Properties();
		p.put("user", jdbc_username);
		p.put("password", jdbc_password);

		try
		{
			Driver driver = (Driver) Class.forName(jdbc_driver).newInstance();

			println("Connecting to " + jdbc_url + ", " + jdbc_username + "/" + jdbc_password);

			connection = driver.connect(jdbc_url, p);

			if (connection == null)
			{
				error("Could not establish connection.");
			}
			else
			{
				debug("Connection established.");
			}
		}
		catch (Exception e)
		{
			error("getConnection (" + jdbc_driver + ", " + jdbc_url + ", " + jdbc_username + "/" + jdbc_password + ")", e);

			connection = null;
		}

		return (connection);
	}
}


