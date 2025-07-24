
import com.cometway.ak.*;
import com.cometway.swing.*;
import java.util.*;
import com.cometway.util.*;


public class LightBoxDemo extends Agent implements Runnable
{
	static Random random = new Random();


	public void initProps()
	{
		setDefault("refresh_count", "50");
	}


	public void start()
	{
		Thread t = new Thread(this);
		t.start();
	}


	/**
	* This method performs an infinite self-test.
	*/

	public void run()
	{
		LightBox lightBox = (LightBox) getServiceImpl("light_box");
		int grid_height = lightBox.getInteger("grid_height");
		int grid_width = lightBox.getInteger("grid_width");
		int refresh_count = getInteger("refresh_count");

		while (currentStateEquals("stopping") == false)
		{
			try
			{
				for (int times = 0; times < refresh_count; times++)
				{
					int h = Math.abs(random.nextInt()) % grid_width;
					int v = Math.abs(random.nextInt()) % grid_height;
					int s = Math.abs(random.nextInt()) % 6;

					debug("Cell (" + h + ", " + v + ") = " + s);
	
					lightBox.setCellState(h, v, s);
				}

				lightBox.drawChangedCells();

//				Thread.sleep(50);
			}
			catch (Exception e)
			{
				error("run", e);

				getAgentController().stop();
			}
		}
	}
}


