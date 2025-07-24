
package com.cometway.jdbc;


import com.cometway.ak.ServiceAgent;
import com.cometway.props.Props;
import com.cometway.props.PropsException;
import com.cometway.props.PropsList;
import java.text.MessageFormat;
import java.util.List;
import java.util.Vector;


/**
* This agent facilitates the execution of JDBC statements
* stored in the statements database.
*/

public class StatementAgent extends ServiceAgent implements StatementAgentInterface
{
	public void initProps()
	{
		setDefault("service_name", "statement_agent");
		setDefault("jdbc_agent", "jdbc_agent");
		setDefault("database_url", "database/statement");
	}


	public boolean executeInsert(String statementName, Props p)
	{
		boolean success = false;

		try
		{
			JDBCAgentInterface jdbc = getJDBCAgent();
			Props statementProps = getStatementProps(statementName);
			String statement = statementProps.getTrimmedString("statement");

			success = jdbc.executeInsert(statement, p);
		}
		catch (Exception e)
		{
			error("Could not execute insert: " + statementName, e);
		}

		return (success);
	}


	public boolean executeInsert(String statementName, Props params, Props p)
	{
		boolean success = false;

		try
		{
			JDBCAgentInterface jdbc = getJDBCAgent();
			Props statementProps = getStatementProps(statementName);
			String statement = formatParams(statementProps, params);

			success = jdbc.executeInsert(statement, p);
		}
		catch (Exception e)
		{
			error("Could not execute insert: " + statementName, e);
		}

		return (success);
	}


	public List executeQuery(String statementName)
	{
		List results = null;

		try
		{
			JDBCAgentInterface jdbc = getJDBCAgent();
			Props statementProps = getStatementProps(statementName);
			String statement = statementProps.getTrimmedString("statement");

			results = jdbc.executeQuery(statement);
		}
		catch (Exception e)
		{
			error("Could not execute query: " + statementName, e);
		}

		return (results);
	}


	public List executeQuery(String statementName, Props params)
	{
		List results = null;

		try
		{
			JDBCAgentInterface jdbc = getJDBCAgent();
			Props statementProps = getStatementProps(statementName);
			String statement = formatParams(statementProps, params);

			results = jdbc.executeQuery(statement);
		}
		catch (Exception e)
		{
			error("Could not execute query: " + statementName, e);
		}

		return (results);
	}


	public boolean executeUpdate(String statementName)
	{
		boolean success = false;

		try
		{
			JDBCAgentInterface jdbc = getJDBCAgent();
			Props statementProps = getStatementProps(statementName);
			String statement = statementProps.getTrimmedString("statement");

			success = jdbc.executeUpdate(statement);
		}
		catch (Exception e)
		{
			error("Could not execute query: " + statementName, e);
		}

		return (success);
	}


	public boolean executeUpdate(String statementName, Props params)
	{
		boolean success = false;

		try
		{
			JDBCAgentInterface jdbc = getJDBCAgent();
			Props statementProps = getStatementProps(statementName);
			String statement = formatParams(statementProps, params);

			success = jdbc.executeUpdate(statement);
		}
		catch (Exception e)
		{
			error("Could not execute query: " + statementName, e);
		}

		return (success);
	}


	/**
	* Returns the specified statement Props from the database.
	*/

	protected Props getStatementProps(String statementName)
	{
		String serviceName = getString("database_url");
		PropsList list = (PropsList) getServiceImpl(serviceName);
		Props p = null;

		if (list == null)
		{
			error("Could not locate statement database: " + serviceName);
		}
		else
		{
			p = list.getProps("name", statementName);
		}

		return (p);
	}


	/**
	* Returns a reference to the JDBCAgentInterface specified by the jdbc_agent property.
	*/

	protected JDBCAgentInterface getJDBCAgent()
	{
		String jdbc_agent = getTrimmedString("jdbc_agent");
		JDBCAgentInterface jdbc = (JDBCAgentInterface) getServiceImpl(jdbc_agent);

		if (jdbc == null)
		{
			error("jdbc_agent was not found: " + jdbc_agent);
		}

		return (jdbc);
	}


	/**
	* Uses the specified MessageFormat pattern this method generates a
	* string containing the inserted values from the cooresponding
	* properties.
	* The pattern string is passed to java.text.MessageFormat.format()
	* after substituting appropriate argument indexes for property names.
	* The pattern string contains number format patterns in the syntax
	* { PropsKey, FormatType, FormatStyle } where
	* formatType 'number' uses the NumberFormat class (none, integer, currency, percent, custom NumberFormat pattern),
	* formatType 'date' uses the DateFormat class (none, short, medium, long, full, custom DateFormat pattern),
	* formatType 'time' uses the DateFormat class (none, short, medium, long, full,  custom DateFormat pattern), and
	* formatType 'choice' uses the ChoiceFormat (custom ChoiceFormat pattern) class.
	* eg: "At {my_date,time} on {my_date,date}, there was {my_event_name} on planet {my_planet_name,number,integer}."
	* eg: "The disk \"{disk_name}\" contains {file_count} file(s)."
	*/

	protected String formatParams(Props statementProps, Props p) throws PropsException
	{
		String pattern = statementProps.getTrimmedString("statement");
		boolean dont_escape_apostrophes = statementProps.getBoolean("dont_escape_apostrophes");

		String s = null;
		Object[] arguments = null;

		try
		{
			Vector keyList = new Vector();
			int pos = pattern.indexOf('{');

			while (pos >= 0)
			{
				String key = getNextKey(pattern, pos + 1);

				if (key == null) break;

				if (keyList.contains(key) == false)
				{
					keyList.add(key);
				}

				pos = pattern.indexOf('{', pos + 1);
			}

			int keyCount = keyList.size();

			arguments = new Object[keyCount];

			for (int i = 0; i < keyCount; i++)
			{
				String keyName = (String) keyList.get(i);
				Object o = p.getProperty(keyName);

				if (o == null)
				{
					o = "";
				}
				else if ((o instanceof String) && (dont_escape_apostrophes == false))
				{
					o = JDBCAgent.escapeApostrophes((String) o);
				}

				arguments[i] = o;

				String key = "\\{" + keyName;
				String idx = "\\{" + Integer.toString(i);

				pattern = pattern.replaceAll(key, idx);
			}

			s = MessageFormat.format(pattern, arguments);
		}
		catch (Exception e)
		{
			String message = "Could not format pattern (" + e.toString() + ")\n" + pattern;
			throw new PropsException(message, e);
		}

		return (s);
	}
}


