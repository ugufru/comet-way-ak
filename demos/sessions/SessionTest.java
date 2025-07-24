
import com.cometway.ak.*;
import com.cometway.om.*;
import com.cometway.props.Props;
import com.cometway.util.*;

/**
* This agent creates a new session using the resident session manager every 5 seconds.
* It requires a service manager, object manager, scheduler, session manager, and session reaper
* to see everything in action.
*/

public class SessionTest extends ScheduledAgent
{
	public void initProps()
	{
                setDefault("schedule", "between 0:0:0-23:59:59 every 5s");
	}

	public void wakeup()
	{
                SessionManagerInterface  manager = SessionManager.getSessionManager();

                String  sid = manager.createSession();
                Props   p = manager.getSessionProps(sid);
	}
}

