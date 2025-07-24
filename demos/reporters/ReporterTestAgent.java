
import com.cometway.ak.*;
import com.cometway.util.*;

/**
* This is a sample agent to demonstrate different types of Reporters.
*/

public class ReporterTestAgent extends Agent
{
	public void start()
	{
		println("Use the println method to log agent progress.");
		debug("Use the debug method to log debugging activity.");
		warning("Use the warning method to report non-critical errors.");
		error("Use the error method to report fatal errors.");

		try
		{
			throw new NullPointerException("Here is an Exception!");
		}
		catch (Exception e)
		{
			error("There was a problem", e);
		}
	}
}

