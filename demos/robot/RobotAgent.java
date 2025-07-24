
import com.cometway.ak.*;
import com.cometway.swing.*;
import java.awt.Robot;


/**
* This agent moves the mouse cursor in a circle every 15 seconds
* to keep the screen saver from kicking in.
*/

public class RobotAgent extends ScheduledAgent
{
	Robot robot;


	public void initProps()
	{
		setDefault("schedule", "between 0:0:0-23:59:59 every 15s");
		setDefault("sleep_time_ms", "2");
	}


	public void start()
	{
		System.setProperty("com.apple.macos.useRobot", "true");

		try
		{
			robot = new Robot();
	
			schedule();
		}
		catch (java.awt.AWTException e)
		{
			error("Could not start", e);
		}
	}


	public void wakeup()
	{
		int sleep_time_ms = getInteger("sleep_time_ms");

		for (double a = 0.0; a <= (StrictMath.PI * 2); a += 0.01)
		{
			double x = StrictMath.cos(a);
			double y = StrictMath.sin(a);
			
			debug("a = " + a + " x = " + x + " y = " + y);

			Long xx = new Long(StrictMath.round(x * 250.0 + 400));
			Long yy = new Long(StrictMath.round(y * -250.0 + 300));
			
			robot.mouseMove(xx.intValue(), yy.intValue());

			try
			{
				Thread.sleep(sleep_time_ms);
			}
			catch (Exception e) {}
		}
	}
}


