
package com.cometway.jdbc;


import com.cometway.ak.ServiceAgent;
import com.cometway.util.ReporterInterface;
import java.sql.Driver;
import java.sql.SQLException;


/**
* This agent creates and registers a PropsList to the service manager.
* It can be extended to provide alternate storage layers,
* or used externally with associated Import and Export agents.
*/

public class JDBCPropsListAgent extends ServiceAgent
{
	protected JDBCPropsList propsList;
	protected JDBCConnectionPool pool;
	protected Driver driver;


	/**
	* <PRE>Default properties:
	* service_name = database (name this PropsList registers with the Service Manager).</PRE>
	*/

	public void initProps()
	{
		setDefault("service_name", "database");
		setDefault("table_name", "SQLTABLE");
		setDefault("jdbc_agent_name","jdbc");
	}


	/**
	* Creates a PropsChangeListener for this agent and
	* registers with the Service Manager using service_name.
	*/

	public void start()
	{
		try
		{
			propsList = new JDBCPropsList((JDBCAgentInterface)getServiceImpl(getString("jdbc_agent_name")), getString("table_name"));

			propsList.setReporter(new JDBCPropsListReporter());
			
			registerService(getString("service_name"), propsList);
		}
		catch (SQLException e)
		{
			error("Could not start JDBCPropsList",e);
		}
	}


	/**
	* Unregister this agent with the Service Manager
	* and sets internal state variables to null.
	* All Props references are permanently lost.
	*/

	public void stop()
	{
		try
		{
			String service_name = getTrimmedString("service_name");

			debug("Closing connections...");

			if (pool != null) pool.closeConnections();

			unregisterService(service_name, propsList);
		}
		catch (Exception e)
		{
			error("Could not stop JDBCPropsList.", e);
		}
		finally
		{
			propsList = null;
			pool = null;
		}
	}


	// This allows us to be a little more selected about which JDBCPropsList
	// messages we want to have reported. Setting hide_debug for this agent
	// will significantly reduce the amount of reporter output.

	class JDBCPropsListReporter implements ReporterInterface
	{
		public void debug(Object objectRef, String message)
		{
			if (debugReporter != null) debugReporter.debug(objectRef, message);
		}


		public void warning(Object objectRef, String message)
		{
			if (warningReporter != null) warningReporter.warning(objectRef, message);
		}


		public void warning(Object objectRef, String message, Exception e)
		{
			if (warningReporter != null) warningReporter.warning(objectRef, message, e);
		}


		public void error(Object objectRef, String message)
		{
			if (errorReporter != null) errorReporter.error(objectRef, message);
		}


		public void error(Object objectRef, String message, Exception e)
		{
			if (errorReporter != null) errorReporter.error(objectRef, message, e);
		}


		public void println(Object objectRef, String message)
		{
			if (printlnReporter != null) printlnReporter.println(objectRef, message);
		}
	}
}


