
package com.cometway.ak;


import com.cometway.props.Props;
import com.cometway.states.CommandException;
import com.cometway.states.CommandInterface;
import com.cometway.states.StateMachineModelInterface;
import com.cometway.states.StateModelInterface;
import com.cometway.states.TransitionException;
import com.cometway.states.TransitionInterface;


/**
* An implementation of AgentControllerInterface that is created by
* AgentKernel to control the agents it creates.
*/

public class AgentController extends Props implements AgentControllerInterface
{
	private final static String CREATING_STATE = "creating";
	private final static String STOPPED_STATE = "stopped";
	private final static String STARTING_STATE = "starting";
	private final static String RUNNING_STATE = "running";
	private final static String STOPPING_STATE = "stopping";
	private final static String FAILED_STATE = "failed";
	private final static String DESTROYING_STATE = "destroying";
	private final static String DESTROYED_STATE = "destroyed";


	/** The Agent instance this controller is attached to. */

	private Agent agent;
	private StateMachineModelInterface stateMachine;


	/**
	* Constructor for this class.
	* @param agent an AgentInterface instance to be controlled
	*/

	public AgentController(AgentInterface agent)
	{
		this.agent = (Agent) agent;

		this.stateMachine = agent.getStateMachineModel();

		agent.setAgentController(this);

		setPropsContainer(agent.getProps().getPropsContainer());

		setProperty("next_state", CREATING_STATE);

		waitForState(STOPPED_STATE);
	}


	/**
	* Returns the controlled agent's toString output (ie: 100_ServiceManager).
	*/

	public String toString()
	{
		return (agent.toString());
	}


	/**
	* Returns the Props for the controlled agent.
	*/

	public Props getProps()
	{
		return (agent.getProps());
	}


	/**
	* Starts the controlled Agent and puts it into the
	* RUNNING_STATE. This method will throw an AgentStateException
	* if the agent's current state is not the STOPPED_STATE.
	*/

	public void start()
	{
		String current_state = getString("current_state");

		if (current_state.equals(STOPPED_STATE))
		{
			agent.println("Starting on " + Agent.getDateTimeStr());

			setProperty("next_state", STARTING_STATE);

			waitForState(RUNNING_STATE);

			setBoolean("started", true); // for backwards compatibility
		}
		else
		{
			throw new AgentStateException("Illegal attempt to start while " + current_state);
		}
	}


	/**
	* Starts the controlled Agent and puts it into the
	* STOPPED_STATE. This method will throw an AgentStateException
	* of the agent's current state is not RUNNING_STATE.
	*/

	public void stop()
	{
		String current_state = getString("current_state");

		if (current_state.equals(RUNNING_STATE))
		{
			setBoolean("started", false); // for backwards compatibility

			agent.println("Stop requested on " + Agent.getDateTimeStr());

			setProperty("next_state", STOPPING_STATE);

			waitForState(STOPPED_STATE);
		}
		else
		{
			throw new AgentStateException("Illegal attempt to stop while " + current_state);
		}
	}


	/**
	* Starts the controlled Agent and puts it into the
	* DESTROYED_STATE. This method will throw an AgentStateException
	* of the agent's current state is not STOPPED_STATE.
	*/

	public void destroy()
	{
		String current_state = getString("current_state");

		if (current_state.equals(STOPPED_STATE))
		{
			setProperty("next_state", DESTROYING_STATE);

			waitForState(DESTROYED_STATE);
		}
		else
		{
			throw new AgentStateException("Illegal attempt to destroy while " + current_state);
		}
	}


	/* These methods are used internally. */


	/**
	* Execute the agent's state machine model until the specified state is reached.
	* If the state returned by the agent's state machine model reaches a null state
	* or the DESTROYED_STATE this method will also return.
	* @param waitState returns from this method if this state is reached
	*/

	private String waitForState(String waitState)
	{
		String current_state = getString("next_state");

		if (current_state.length() == 0)
		{
			current_state = FAILED_STATE;
		}

		String next_state = null;

		while (true)
		{
			next_state = executeState(current_state);

			if (next_state == null)
			{
//				agent.debug("exiting waitForState: next state is null");

				break;
			}
			else if (current_state.equals(waitState))
			{
				break;
			}
			else if (next_state.equals(DESTROYED_STATE))
			{
//				agent.debug("exiting waitForState: final state reached");

				break;
			}
			else
			{
				current_state = next_state;
			}
		}

		return (next_state);
	}


	/**
	* Executes the specified agent state model.
	*/

	private String executeState(String stateName)
	{
		String next_state = null;

		StateModelInterface state = stateMachine.getStateModel(stateName);
		CommandInterface[] commands = state.getCommands();
		TransitionInterface[] transitions = state.getTransitions();

//		agent.debug(stateName);

		setProperty("current_state", stateName);

		/* Execute the commands */

		if (commands != null)
		{
			for (int i = 0; i < commands.length; i++)
			{
//				agent.debug("-> Executing command " + (i + 1) + ": " + commands[i].getName());
				
				try
				{
					commands[i].execute();
				}
				catch (CommandException e)
				{
					next_state = FAILED_STATE;

					agent.error("CommandException", e.getOriginalException());
				}
			}
		}


		/* Loop on the transitions */

		if (transitions != null)
		{
			while (next_state == null)
			{
				for (int i = 0; i < transitions.length; i++)
				{
//					agent.debug("-> Executing transition " + (i + 1) + ": " + transitions[i].getName());
		
					try
					{
						next_state = transitions[i].execute();
					}
					catch (TransitionException e)
					{
						next_state = FAILED_STATE;

						agent.error("Exception", e);
					}
		
					if (next_state != null)
					{
						break;
					}
				}
			}
		}

		setProperty("next_state", next_state);

//		agent.debug("> Exiting state: " + stateName + " (next state: " + next_state + ')');
	
		return (next_state);
	}
}


