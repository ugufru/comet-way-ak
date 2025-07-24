
package com.cometway.jdbc;


import com.cometway.jdbc.PooledJDBCAgent;
import com.cometway.jdbc.JDBCConnection;
import com.cometway.props.Props;
import com.cometway.props.PropsException;
import com.cometway.props.PropsList;
import com.cometway.util.ObjectSerializer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import javax.sql.rowset.serial.SerialBlob;



/**
* This agent exports the specified fields from a PropsList
* to the specified field and field types in a SQL table
* through a JDBC connection to the database.
*
* Note: This has only been tested with MS-SQL using the JTDS driver so far.
*/

public class JDBCExportAgent extends PooledJDBCAgent
{
	final static protected String VARCHAR = "varchar";
	final static protected String INT = "int";
	final static protected String DOUBLE = "double";
	final static protected String DATETIME = "datetime";
	final static protected String BINARY = "binary";

	final static protected SimpleDateFormat SQL_SDF = new SimpleDateFormat("MM/dd/yyyy");


	protected JDBCConnection jdbcConnection;
	protected PreparedStatement statement;
	protected Vector from_fields;
	protected Vector to_types;

	
	public void initProps()
	{
		setDefault("database_name", "database");
		setDefault("from_fields", "First Name, Last Name, Address, City, State, Zip Code, Birth Date, Age");
		setDefault("jdbc_driver", "net.sourceforge.jtds.jdbc.Driver");
		setDefault("jdbc_url", "jdbc:jtds:sqlserver://hostname/database");
		setDefault("jdbc_username", "<username>");
		setDefault("jdbc_password", "<password>");
		setDefault("to_fields", "[First Name], [Last Name], Address, City, State, [Zip Code], Birthdate, Age");
		setDefault("to_types", "varchar, varchar, varchar, varchar, varchar, varchar, datetime, int");
		setDefault("to_table", "Contacts");
	}


	public void start()
	{
		try
		{
			from_fields = getTokens("from_fields");
			to_types = getTokens("to_types");

			openConnectionDriver();
			jdbcConnection = (JDBCConnection) getConnection();
			createStatement();
			saveToDatabase();
		}
		catch (Exception e)
		{
			error("Could not continue", e);
		}
		finally
		{
			try
			{
				closeConnection(jdbcConnection);
				closeConnectionDriver();

				jdbcConnection = null;
			}
			catch (Exception e)
			{
				error("Could not close database", e);
			}
		}
	}


	/**
	* Creates a PreparedStatement for inserting values into the table
	* based on the to_table and to_fields properties.
	*/

	protected void createStatement() throws Exception
	{
		StringBuffer sql = new StringBuffer();
		sql.append("INSERT INTO ");
		sql.append(getTrimmedString("to_table"));
		sql.append(" (");
		sql.append(getTrimmedString("to_fields"));
		sql.append(") VALUES (");

		int count = from_fields.size();

		for (int i = 0; i < count; i++)
		{
			if ((i + 1) == count)
			{
				sql.append('?');
			}
			else
			{
				sql.append("?, ");
			}
		}

		sql.append(')');

		debug("PreparedStatement: " + sql);

		statement = jdbcConnection.prepareStatement(sql.toString());
	}


	/**
	* Inserts the records from the PropsList specified by database_name
	* into the table specified by to_table.
	*/

	protected void saveToDatabase() throws Exception
	{
		String database_name = getString("database_name");

		int fieldCount = from_fields.size();
		PropsList database = (PropsList) getServiceImpl(database_name);
		List v = database.listProps();
		int count = v.size();

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) v.get(i);

			println("Exporting " + database_name + " record (" + (i + 1) + " of " + count + ")");

			for (int x = 0; x < fieldCount; x++)
			{
				String type = (String) to_types.get(x);
				String key = (String) from_fields.get(x);

				debug(key + " = " + '(' + type + ") " + p.getString(key));

				if (type.equals(VARCHAR))
				{
					statement.setString(x + 1, p.getString(key));
				}
				else if (type.equals(INT))
				{
					statement.setInt(x + 1, p.getInteger(key));
				}
				else if (type.equals(DOUBLE))
				{
					statement.setDouble(x + 1, p.getDouble(key));
				}
				else if (type.equals(DATETIME))
				{
					Object o = p.getProperty(key);

					if (o instanceof Date)
					{
						Date d = (Date) o;
						java.sql.Date sd = new java.sql.Date(d.getTime());
						statement.setDate(x + 1, sd);
					}
					else
					{
						statement.setString(x + 1, p.getString(key));
					}
				}
				else if (type.equals(BINARY))
				{
					Object o = p.getProperty(key);

					if (o instanceof byte[])
					{
						statement.setBlob(x + 1, new SerialBlob((byte[]) o));
					}
					else
					{
						try
						{
							statement.setBlob(x + 1, new SerialBlob(ObjectSerializer.serialize(o)));
						}
						catch (Exception e)
						{
							error("Cannot serialize property '" + key + "' into SerialBlob", e);
						}
					}

				}
				else
				{
					throw new Exception("Unknown type: " + type + " (key = " + key + ')');
				}
			}

			statement.executeUpdate();
		}
	}
}

