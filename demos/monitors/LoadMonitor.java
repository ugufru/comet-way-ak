
import com.cometway.ak.*;
import java.util.*;
import com.cometway.util.*;

/**
* This agent implements a simple load monitor by monitoring latency of the scheduler.
* Theoretically, the scheduler will wake up this agent on time under light load conditions.
*/

public class LoadMonitor extends ScheduledAgent
{
	protected Date		next;
	protected long[]	latency;
	protected long		average;

	public void wakeup()
	{
		Date    now = new Date();
		long    nowTime = now.getTime();


		// println("now = " + nowTime);

		if (next == null)
		{
			println("schedule = " + getString("schedule"));
		}
		else
		{
			System.arraycopy(latency, 0, latency, 1, latency.length - 1);

			long    nextTime = next.getTime();


			// println("nextTime = " + nextTime);

			long    lat = nowTime - nextTime;


			// println("lat = " + lat);

			latency[0] = (new Long(lat)).longValue();

			println("Latency = " + latency[0] + ", Last5 = " + getAverage(5) + ", Last10 = " + getAverage(10));
		}

		next = getSchedule().getNextDate(new Date());


		// println("next = " + (next.getTime()));

		long    waitTime = next.getTime() - nowTime;


		// println("wait = " + waitTime);

	}


	public long getAverage(int range)
	{
		if (range > latency.length)
		{
			range = latency.length;
		}

		long    average = 0;

		for (int i = 0; i < range; i++)
		{


			// println("latency[" + i + "] = " + latency[i]);

			average += latency[i];
		}

		average /= range;

		return (average);
	}


	public void initProps()
	{
		setDefault("schedule", "between 0:0:0-23:59:59 every 5s");
		setDefault("series_length", "10");
	}


	public void start()
	{
		latency = new long[getInteger("series_length")];

		schedule();
	}
}

