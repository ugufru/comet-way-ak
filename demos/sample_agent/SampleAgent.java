
import com.cometway.ak.*;
import com.cometway.util.*;

/**
* This is a sample agent to demonstrate some of the more common agent methods
* including the use of println, debug, warning, and error methods, as well
* as some examples of using the agent Props to store and retrieve data.
*/

public class SampleAgent extends Agent
{
	public void initProps()
	{
		println("Initializing properties.");

		setDefault("test_message", "This is a test message.");
		setDefault("test_value", "42");
	}


	public void start()
	{
		println("Use the println method to log agent progress.");
		debug("Use the debug method to log debugging activity.");
		warning("Use the warning method to report non-critical errors.");
		error("Use the error method to report fatal errors.");

		println("Testing the agent props...");

		String test_message = getString("test_message");
		int test_value = getInteger("test_value");

		debug("test_message = " + test_message);
		debug("test_value   = " + test_value);

		println("This shows using the error() method to report an exception...");

		try
		{
			throw new NullPointerException("test exception");
		}
		catch (Exception e)
		{
			error("There was a problem", e);
		}

		println("Props has automatic type conversion and other conveniences...");

		long long_val = getLong("test_value");
		long_val *= 2;
		setLong("test_value", long_val);
		debug("test_value   = " + getString("test_value"));
		incrementInteger("test_value");
		debug("test_value   = " + getString("test_value") + " ($" + getHexString("test_value") + ")");

		append("test_message", " This was appended.");
		debug("test_message = " + getString("test_message"));
	}


	public void stop()
	{
		println("A stop request has been received.");
		println("It is the agent's responsibility to cease its own operation.");
	}


	public void destroy()
	{
		println("Cleaning up before this agent instance is destroyed.");
	}
}

