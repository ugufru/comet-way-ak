
import com.cometway.ak.*;
import com.cometway.util.*;

/**
* This agent is a demonstration of accessing another agent that is registered
* with the service manager. It looks up the service as specified by the
* <TT>demo_service</TT> property (demo_service by default), taking care
* to cast the returned object as <TT>DemoService</TT>, and calling its
* run method.
*/

public class DemoServiceClient extends Agent
{
	public void initProps()
	{
		setDefault("service_name", "demo_service");
	}


	public void start()
	{
		String		serviceName = getString("service_name");
		DemoService	service = (DemoService) getServiceImpl(serviceName);

		println("Accessing DemoService...");
		service.run();
		println("Completed.");
	}


}

