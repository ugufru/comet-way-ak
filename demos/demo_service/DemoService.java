
import com.cometway.ak.*;
import com.cometway.util.*;

/**
* This agent is a demonstration of creating a service by subclassing ServiceAgent.
* This approach makes things simpler since the ServiceAgent automatically registers
* with the service manager using the <TT>service_name</TT> property when the agent
* starts. It automatically unregisters when a stop is requested. Other agents using
* this service (such as DemoServiceClient) can use the service manager to get
* a reference to it.
*/

public class DemoService extends ServiceAgent
{
	public void initProps()
	{
		setDefault("service_name", "demo_service");
	}


	public void run()
	{
		println("DemoService has been accessed.");
	}
}

