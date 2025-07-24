
import com.cometway.ak.*;
import java.util.*;

/**
* This is a simple agent that dumps the System properties to the console.
*/

public class SystemPropertiesAgent extends Agent
{
	public void start()
	{
		Properties p = System.getProperties();

		p.list(System.out);
	}
}

