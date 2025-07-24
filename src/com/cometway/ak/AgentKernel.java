
package com.cometway.ak;


import com.cometway.props.Props;
import com.cometway.util.ClassFactoryInterface;
import java.util.Vector;


/**
* Reference implementation of the AgentKernelInterface.
*/

public class AgentKernel extends Agent implements AgentKernelInterface
{
	/**
	* Creates an agent instance using the specified
	* agent information; a String containing the classname
	* of an AgentInterface implementation, or a Props with the
	* classname property. The agent's initProps method is called
	* before this method returns.
	*/

	public AgentControllerInterface createAgent(Object agentInfo)
	{
		if (agentInfo instanceof Props)
		{
			return (createAgent((Props) agentInfo));
		}
		else
		{
			Props agentProps = new Props();
	
			agentProps.setProperty("classname", agentInfo.toString());
	
			return (createAgent(agentProps));
		}
	}


	/**
	* Creates an agent instance using the classname property of
	* the specified Props and automatically adopting any other
	* properties which exist. If the name property isn't provided,
	* the name will be set to the agent's classname. If the agent_id
	* property isn't provided, one will be assigned by the agent kernel.
	* Returns null if unsuccessful.
	*/

	protected AgentControllerInterface createAgent(Props agentProps)
	{
		AgentController controller = null;

		String classname = agentProps.getString("classname");

		if (classname.length() > 0)
		{
			println("Creating agent " + classname);

			try
			{
				ClassFactoryInterface classFactory = (ClassFactoryInterface) agentProps.getProperty("class_factory");

				if (classFactory == null)
				{
					classFactory = AK.getDefaultClassFactory();
				}

				AgentInterface agent = (AgentInterface) classFactory.createInstance(classname);
				Class clas = agent.getClass();

				/* Use the agent classname as the default agent name. */

				if (agentProps.hasProperty("name") == false)
				{
					String name = clas.getName();
					int i = name.lastIndexOf('.');

					if (i > 0)
					{
						name = name.substring(i + 1);
					}

					agentProps.setProperty("name", name);
				}

				agentProps.setDefault("agent_id", getNextAgentID());
				agent.setProps(agentProps);
				controller = new AgentController(agent);
			}
			catch (Exception e)
			{
				error("Could not create Agent: " + classname + "\n" + agentProps, e);
			}
		}

		return (controller);
	}


	/**
	* Increments and returns the next agent ID.
	*/

	protected String getNextAgentID()
	{
		int i = getInteger("next_agent_id");

		setInteger("next_agent_id", i + 1);

		return (getString("next_agent_id"));
	}


	/**
	* Provides default values for the following properties if missing:
	* <UL>
	* <LI>next_agent_id - seed value for agent_id (default: 1001)
	* </UL>
	*/

	public void initProps()
	{
		setDefault("next_agent_id", "1001");
	}


	/**
	* Shuts down the agent kernel and exits the VM by calling System.exit(0).
	*/

	public void stop()
	{
		println("Shutting down...");
		System.exit(0);
	}
}

