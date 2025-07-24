
import com.cometway.ak.*;
import java.util.*;
import com.cometway.util.*;

/**
* This scheduled agent reports free and total memory sizes every 15s.
* It's a good one to throw into your kernel when monitoring memory leaks.
*/

public class MemoryMonitor extends ScheduledAgent
{
	protected Date		next;

        public void initProps()
        {
                setDefault("schedule", "between 0:0:0-23:59:59 every 15s");
        }


	public void wakeup()
	{
		Date    now = new Date();
		long    nowTime = now.getTime();

		if (next == null)
		{
			println("schedule = " + getString("schedule"));
		}
		else
		{
			Runtime rt = Runtime.getRuntime();
			long free = rt.freeMemory() / 1000;
			long total = rt.totalMemory() / 1000;
			int percentage = (int) ((double) free / (double) total * 100.0);

			debug("Memory: " + free + "K / " + total + "K (" + percentage + "%)");
		}


		now = new Date();
		next = getSchedule().getNextDate(now);
	}
}

