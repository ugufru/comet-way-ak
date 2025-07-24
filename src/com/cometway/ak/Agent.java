
package com.cometway.ak;

import com.cometway.props.Props;
import com.cometway.states.*;
import com.cometway.util.*;
import java.io.*;
import java.text.*;
import java.util.*;


/**
* An abstract implementation of the AgentInterface.
* There is built-in support for routing agent output to
* the agent's output and error streams, and masking this output through the
* use of hide_println, hide_debug, and hide_warning props.
* This implementation of AgentInterface recognizes these properties:
* <UL>
* <LI>name - the agent's internal name to be displayed in agent output (default: same as class name)
* <LI>id - the agent's id (default: assigned by the agent kernel)
* <LI>hide_println - disables println output if set to true (default: false)
* <LI>hide_debug - disables debug output if set to true (default: false)
* <LI>hide_warning - disables warning output if set to true (default: false)
* </UL>
*/

public abstract class Agent extends Props implements AgentInterface
{
	/** The agent is being created; initProps method called. */
	public final static String CREATING_STATE = "creating";

	/** The agent is ready to be started or destroyed. */
	public final static String STOPPED_STATE = "stopped";

	/** The agent is starting; start method called. */
	public final static String STARTING_STATE = "starting";

	/** The agent is running and ready to be stoppped. */
	public final static String RUNNING_STATE = "running";

	/** The agent is stopping; stop method called. */
	public final static String STOPPING_STATE = "stopping";

	/** The agent has failed; DESTROYING_STATE automatically entered. */
	public final static String FAILED_STATE = "failed";

	/** The agent is being destroyed; destroy method called. */
	public final static String DESTROYING_STATE = "destroying";

	/** The agent has been destroyed and can no longer be used. */
	public final static String DESTROYED_STATE = "destroyed";

	private final static SimpleDateFormat SDF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z");

	/** This is a reference to the Agent's Props for backwards compatibility.
	* Under the current architecture the Agent is the Agent's Props making
	* the Props methods to be accessible directly from other agent methods.
	* @deprecated use the getProps method if you need a Props reference.
	*/
	
	protected Props props;
	
	/** This is a reference to the AgentController assigned to this agent. */
	
	protected AgentControllerInterface agentController;
	
	/**
	* This is where the agent_id is cached for use by the toString method.
	*/
	
	protected String agent_id;
	
	/**
	* This is a reference to the Reporter assigned to this agent for println output.
	* If this field is null, the println method will be ignored.
	*/
	
	protected ReporterInterface printlnReporter;
	
	/**
	* This is a reference to the Reporter assigned to this agent for debug output.
	* If this field is null, the debug method will be ignored.
	*/
	
	protected ReporterInterface debugReporter;
	
	/**
	* This is a reference to the Reporter assigned to this agent for warning output.
	* If this field is null, the warning methods will be ignored.
	*/
	
	protected ReporterInterface warningReporter;
	
	/**
	* This is a reference to the Reporter assigned to this agent for error output.
	* If this field is null, the error methods will be ignored.
	*/
	
	protected ReporterInterface errorReporter;


	/**
	* Called by the agent kernel to assign a Props to this agent.
	*/

	public void setProps(Props p)
	{
		props = this;

		setPropsContainer(p.getPropsContainer());

		errorReporter = AK.getDefaultReporter();

		if (p.getBoolean("hide_println") == false)
		{
			printlnReporter = errorReporter;
		}

		if (p.getBoolean("hide_debug") == false)
		{
			debugReporter = errorReporter;
		}

		if (p.getBoolean("hide_warning") == false)
		{
			warningReporter = errorReporter;
		}
	}


	/**
	* Returns the Props assigned to this agent.
	*/

	public final Props getProps()
	{
		return (props);
	}


	/**
	* Override this method to initialize this agent's properties before it is started.
	*/

	public void initProps() {}


	/* ----- state machine methods ----- */


	/**
	* Returns true if the specified state is the current state.
	*/

	public final boolean currentStateEquals(String stateName)
	{
		return (getString("current_state").equals(stateName));
	}


	/**
	* Assigns an AgentController to this agent.
	*/

	public final void setAgentController(AgentControllerInterface agentController)
	{
		this.agentController = agentController;
	}


	/**
	* Returns the AgentController for this agent.
	*/

	protected final AgentControllerInterface getAgentController()
	{
		return (agentController);
	}


	/**
	* Called by AgentController to retrieve a reference to this agent's state machine model.
	*/

	public StateMachineModelInterface getStateMachineModel()
	{
		StateMachineModel stateMachine = new StateMachineModel(toString());

		// CREATING_STATE

		CommandInterface command = new AgentCommand(this, "initProps");
		TransitionInterface transition = new AutoTransition(CREATING_STATE, STOPPED_STATE);
		StateModelInterface stateModel = new StateModel(CREATING_STATE, command, transition);
		stateMachine.addStateModel(stateModel);

		// STOPPED_STATE

		transition = new AutoTransition(STOPPED_STATE, STARTING_STATE);
		stateModel = new StateModel(STOPPED_STATE, null, transition);
		stateMachine.addStateModel(stateModel);

		// STARTING_STATE

		command = new AgentCommand(this, "start");
		transition = new AutoTransition(STARTING_STATE, RUNNING_STATE);
		stateModel = new StateModel(STARTING_STATE, command, transition);
		stateMachine.addStateModel(stateModel);

		// RUNNING_STATE

		transition = new AutoTransition(RUNNING_STATE, STOPPING_STATE);
		stateModel = new StateModel(RUNNING_STATE, null, transition);
		stateMachine.addStateModel(stateModel);

		// STOPPING_STATE

		command = new AgentCommand(this, "stop");
		transition = new AutoTransition(STOPPING_STATE, STOPPED_STATE);
		stateModel = new StateModel(STOPPING_STATE, command, transition);
		stateMachine.addStateModel(stateModel);

		// DESTROYING_STATE

		command = new AgentCommand(this, "destroy");
		transition = new AutoTransition(DESTROYING_STATE, DESTROYED_STATE);
		stateModel = new StateModel(DESTROYING_STATE, command, transition);
		stateMachine.addStateModel(stateModel);

		// DESTROYED_STATE

		stateModel = new StateModel(DESTROYED_STATE);
		stateMachine.addStateModel(stateModel);

		return (stateMachine);
	}


	/**
	* Override this method to initiate activities for this agent.
	*/

	public void start() {}


	/**
	* Override this method to handle stop requests from the agent kernel.
	*/

	public void stop() {}


	/**
	* Override this method to perform special cleanup before an agent is destroyed.
	*/

	public void destroy() {}


	/* Agent utility methods called by Agent subclasses */


	/**
	* Prints a debug message tagged with this agent's identity to the output stream.
	*/

	public void debug(String message)
	{
		if (debugReporter != null)
		{
			debugReporter.debug(this, message);
		}
	}


	/**
	* Prints a warning message tagged with this agent's identity to the error stream.
	* If the Exception is type java.lang.reflect.InvocationTargetException
	* that exception's target exception is also stack traced.
	*/

	public void warning(String message)
	{
		if (warningReporter != null)
		{
			warningReporter.warning(this, message);
		}
	}


	/**
	* Prints an warning message tagged with this agent's identity
	* followed by a stack trace of the passed Exception to error stream.
	* If the Exception is type java.lang.reflect.InvocationTargetException
	* that exception's target exception is also stack traced.
	*/

	public void warning(String message, Exception e)
	{
		if (warningReporter != null)
		{
			warningReporter.warning(this, message, e);
		}
	}


	/**
	* Prints an error message tagged with this agent's identity to the error stream.
	* If the Exception is type java.lang.reflect.InvocationTargetException
	* that exception's target exception is also stack traced.
	*/

	public void error(String message)
	{
		if (errorReporter != null)
		{
			errorReporter.error(this, message);
		}
	}


	/**
	* Prints an error message tagged with this agent's identity
	* followed by a stack trace of the passed Exception to the error stream.
	* If the Exception is type java.lang.reflect.InvocationTargetException
	* that exception's target exception is also stack traced.
	*/

	public void error(String message, Exception e)
	{
		if (errorReporter != null)
		{
			errorReporter.error(this, message, e);
		}
	}


	/**
	* Prints a message tagged with this agent's identity to the output stream.
	* Can be disabled by setting hide_println property to "true".
	*/

	public void println(String message)
	{
		if (printlnReporter != null)
		{
			printlnReporter.println(this, message);
		}
	}

	
	/* Service Manager methods */


	/**
	* Returns a reference to the requested Service from the ServiceManager;
	* null if the requested service does not exist, or the ServiceManager has not been loaded.
	*/

	public Object getServiceImpl(String service_name)
	{
		Object serviceImpl = null;
		ServiceManagerInterface serviceManager = AK.getDefaultServiceManager();

		if (serviceManager == null)
		{ 
			error("Service Manager is not loaded");
		}
		else
		{
			serviceImpl = serviceManager.getServiceImpl(service_name, null);
		}

		return (serviceImpl);
	}


	/**
	* Registers this instance with the service manager using the service_name property.
	*/
	 
	public void register()
	{
		registerService(getString("service_name"), this);
	}
	

	/**
	* Registers the specified object with the service manager using the specified property.
	*/

	public void registerService(String service_name, Object serviceImpl)
	{
		if (service_name.length() > 0)
		{
			ServiceManager.register(service_name, serviceImpl);
		}
		else
		{
			error("attempt to register zero length service_name");
		}
	}


	/**
	* Use this method to send agent requests to  RequestAgents.
	*/

	protected void sendAgentRequest(AgentRequest request) throws Exception
	{
		String service_name = request.getTrimmedString("service_name");

		RequestAgentInterface agent = (RequestAgentInterface) getServiceImpl(service_name);

		debug("Sending request to " + service_name + " (" + agent.getClass().getName() + ")\n" + request);

		agent.handleRequest(request);
	}


	/**
	 * Unregisters this service implementation instance with the service manager using the service_name property.
	 */

	public void unregister()
	{
		unregisterService(getString("service_name"), this);
	}


	/**
	 * Unregisters the service implementation instance with the service manager using the specified property.
	 */

	public void unregisterService(String service_name, Object serviceImpl)
	{
		if (service_name.length() > 0)
		{
			ServiceManager.unregister(service_name, serviceImpl);
		}
	}



	/* Other methods */



	/**
	* Returns the string representation of this agent using the format
	* <TT>&lt;agent_id&gt;_&lt;name&gt;</TT> taken from this agent's
	* Props the first time this method is called, and cached for future
	* calls. This format follows the standard naming conventions for
	* agent Props files and allows the agent's output to be formatted
	* in a manner consistent with these files.
	*/

	public String toString()
	{
		if (agent_id == null)
		{
			agent_id = getString("agent_id") + "_" + getString("name");
		}

		return (agent_id);
	}

	
	/**
	* Returns the formatted date-time as a String.
	*/

	public static String getDateTimeStr()
	{
		return (SDF.format(new Date()));
	}
}

