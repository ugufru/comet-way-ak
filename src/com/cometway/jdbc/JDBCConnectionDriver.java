
package com.cometway.jdbc;


import com.cometway.util.ReporterInterface;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;


/**
* The JDBCConnectionDriver class provides access to pooled JDBCConnections.
* 
* Connections will be reaped after they are stale for a while,
* but it's still important to call JDBCConnection.close() or 
* the available connections will get used up. 
*/

public class JDBCConnectionDriver implements Driver
{
    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 0;
 
    public String URL_PREFIX;
    
	protected ReporterInterface reporter;

	private JDBCConnectionPool pool;
    private Driver driver;


	/**
	* @param jdbcDriver The classname of the JDBCDriver to load, for 
	* instance com.ms.jdbc.odbc.JdbcOdbcDriver
	* @param jdbcURL The JDBC URL to use when connecting to the DB, 
	* for instance JDBC:ODBC:viaDB
	* @param jdbcUsername The username to use when connecting to the DB.
	* @param jdbcPassword The password to use when connecting to the DB.
	* @exception java.lang.ClassNotFoundException
	* @exception java.lang.InstantiationException
	* @exception java.lang.IllegalAccessException
	* @exception java.sql.SQLException
	*/
    
    public JDBCConnectionDriver(String jdbcDriver, String jdbcURL, String jdbcUsername, String jdbcPassword, ReporterInterface reporter) 
		throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException
    {
		this.reporter = reporter;

        println("Starting Pooled JDBC Connection.");

        String URL_PREFIX = jdbcURL.substring(0, jdbcURL.lastIndexOf(":"));

        DriverManager.registerDriver(this);

        driver = (Driver) Class.forName(jdbcDriver).newInstance();
        
        try
		{
            pool = new JDBCConnectionPool(jdbcURL, jdbcUsername, jdbcPassword, reporter);
        }
		catch (InterruptedException e)
		{
            System.err.println("Exception while creating connection pool:"+e.toString());
        }
    }

    
	/**
	* This method is needed to implement the driver interface.
	* 
	* @param jdbcURL
	*/
    
    public boolean acceptsURL(String jdbcURL)
	{
        return (jdbcURL.startsWith(URL_PREFIX));
    }


	/**
	* Returns a connection from the JDBC Connection Pool. 
	* These connections should be closed manually.
	* 
	* @return com.cometway.jdbc.JDBCConnection
	* @exception java.sql.SQLException
	*/

    public JDBCConnection connect() throws SQLException
	{
        debug("Getting a connection from the pool...");

        JDBCConnection conn = null;

        try
		{
            conn = pool.getConnection();
        }
		catch (InterruptedException e)
		{
            System.err.println("Exception while getting a connection from the pool:" + e.toString());
        }

        return (conn);
    }


	/**
	* Called by the JDBCConnection threads to get a real 
	* connection to the Database via the JDBC Driver.
	* 
	* @param jdbcURL The JDBC URL to use.
	* @param props The Properties object containing 
	* the username and password to use.
	* @exception java.sql.SQLException
	*/

    public Connection connect(String jdbcURL, Properties props)  throws SQLException
	{
        return (driver.connect(jdbcURL, props));
    }


	/**
	* Called by the JDBCConnection threads to get a real 
	* connection to the Database via the JDBC Driver.
	* 
	* @param jdbcURL The JDBC URL to use
	* @param jdbcUsername The JDBC username
	* @param jdbcPassword The JDBC password
	* @return com.cometway.jdbc.JDBCConnection
	* @exception java.sql.SQLException Usually thrown when there is a security problem.
	*/

    protected Connection getConnection(String jdbcURL, String jdbcUsername, String jdbcPassword)  throws SQLException
	{
        Properties props = new Properties();

		props.put("user", jdbcUsername);
		props.put("password", jdbcPassword);

        return (connect(jdbcURL, props));
    }


	/**
	* Retrieves the driver's major version number.
	*/

    public int getMajorVersion()
	{
        return (MAJOR_VERSION);
    }


	/**
	* Gets the driver's minor version number.
	*/

    public int getMinorVersion()
	{
        return (MINOR_VERSION);
    }


	/**
	* Gets information about the possible properties for this driver.
	* There is no information about this driver.
	*/

    public DriverPropertyInfo[] getPropertyInfo(String str, Properties props)
	{
        return (new DriverPropertyInfo[0]);
    }


	/**
	* This driver is not JDBC Compliant.
	* @returns false;
	*/

    public boolean jdbcCompliant()
	{
        return (false);
    }

    public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException
    {
        throw new java.sql.SQLFeatureNotSupportedException("getParentLogger not supported");
    }


	//--------------------------------------------------------------------------

	/**
	* Prints a debug message tagged with this JDBCConnectionDriver's identity to the output stream.
	*/

	public void debug(String message)
	{
		if (reporter != null)
		{
			reporter.debug(this, message);
		}
	}


	/**
	* Prints a warning message tagged with this JDBCConnectionDriver's identity to the error stream.
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
	* Prints an warning message tagged with this JDBCConnectionDriver's identity
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
	* Prints an error message tagged with this JDBCConnectionDriver's identity to the error stream.
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
	* Prints an error message tagged with this JDBCConnectionDriver's identity
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
}

