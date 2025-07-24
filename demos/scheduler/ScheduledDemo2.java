
import com.cometway.ak.Agent;

/**
* This agent demonstrates writing an agent that is instantiated and started
* by a ScheduledAgent.
*/

public class ScheduledDemo2 extends Agent
{
	public void start()
	{
		// Let's keep track of how many times this instance is called.

		incrementInteger("count");

		println("count = " + getString("count"));

		// Because ScheduledAgent will create a new instance, count will always be 1.
	}
}

