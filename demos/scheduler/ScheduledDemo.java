
import com.cometway.ak.*;
import com.cometway.util.*;
import java.util.*;

/**
* This agent demonstrates writing an agent that uses the Scheduler by subclassing
* the ScheduledAgent. When taking this approach, only the <TT>schedule</TT> property
* needs to be specified (see <TT>com.cometway.util.Schedule</TT> for syntax) and a
* wakeup method defined. Upon startup, the agent will automatically schedule itself.
* Upon receiving a stop request, the agent will automatically unschedule itself.
*/

public class ScheduledDemo extends ScheduledAgent
{
	public void initProps()
	{
		setDefault("schedule", "between 0:0:0-23:59:59 every 2s");
	}


	public void wakeup()
	{
		// Let's keep track of how many times this instance is called.
		// count will increase by 1 each time this agent wakes up.

		incrementInteger("count");

		println("count = " + getString("count"));
	}
}

