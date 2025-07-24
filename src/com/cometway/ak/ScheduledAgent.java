
package com.cometway.ak;


import com.cometway.props.Props;
import com.cometway.util.ISchedule;
import com.cometway.util.ISchedulable;
import com.cometway.util.IScheduleChangeListener;
import com.cometway.util.Schedule;


/**
* Implementation of a scheduled agent.
* Subclass this agent and override the wakeup method that is called as
* specified by the schedule property.
* Creates and uses a Schedule as specified by the schedule property.
*/

public class ScheduledAgent extends Agent implements ISchedulable
{
	protected ISchedule schedule;


	/**
	* Override this method to provide default properties for subclasses.
	* ScheduledAgent recognizes the following properties:
	* schedule (scheduled string or none)
	* agent_classname and agent_name (used by default wakeup implementation
	* to instantiate and start a new agent based on this agent's properties).
	*/

	public void initProps()
	{
		setDefault("schedule", "none");
	}


	/**
	* Creates a Schedule as specified by the <I>schedule</I> property
	* and schedules this agent.
	*/

	public void start()
	{
		schedule();
	}


	/**
	* Unschedules this agent.
	*/

	public void stop()
	{
		unschedule();
	}


	/**
	* Creates a Schedule as specified by the <I>schedule</I> property
	* and schedules this agent.
	* Call this method instead of super.start for clarification when
	* overriding start method.
	*/

	protected boolean schedule()
	{
		boolean result = false;

		if (schedule != null)
		{
			warning("This agent was already scheduled.");
		}
		else
		{
			String scheduleStr = getTrimmedString("schedule");

			if ((scheduleStr.length() > 0) && (scheduleStr.equals("none") == false))
			{
				schedule = new Schedule(scheduleStr);
				result = Scheduler.getScheduler().schedule(this);

				if (result == false)
				{
					error("This agent could not be scheduled.");
				}
			}
		}

		return (result);
	}


	/**
	* Unschedules this agent.
	* Call this method instead of super.stop for clarification when
	* overriding stop method.
	*/

	protected boolean unschedule()
	{
		boolean result = false;

		if (schedule == null)
		{
//			warning("This agent did not need to be unscheduled.");
		}
		else
		{
			result = Scheduler.getScheduler().unschedule(this);

			if (result == false)
			{
				error("This agent could not be unscheduled.");
			}

			schedule = null;
		}

		return (result);
	}


	/**
	* Listeners are not implemented. Always returns false.
	*/

	public boolean addScheduleChangeListener(IScheduleChangeListener l)
	{
		return (false);
	}


	/**
	* Returns a reference to the schedule instance.
	*/

	public ISchedule getSchedule()
	{
		return (schedule);
	}


	/**
	* Listeners are not implemented. Always returns false.
	*/

	public boolean removeScheduleChangeListener(IScheduleChangeListener l)
	{
		return (false);
	}


	/**
	* Override this method to handle schedule wakeup notifications.
	* By default this agent will create and start a new agent based on the
	* properties of this agent and:
	* 1) agent_classname, the java class name of the agent instance to be created,
	* 2) agent_name, the name property assigned to the new agent instance,
	* 3) agent_id, the unique id property assigned to the new agent instance.
	* The wakeup driven agents are only started (not stopped) and if they
	* have a problem during execution (exception thrown) this agent would
	* be unscheduled automatically.
	*/

	public void wakeup()
	{
		String classname = getString("agent_classname");
		String name = getString("agent_name");

		/* Clones this agent's properties, set classname, and remove extraneous other properties. */

		Props p = new Props();

		p.copyFrom(this);
		p.removeProperty("agent_classname");
		p.removeProperty("agent_name");
		p.removeProperty("agent_id");
		p.removeProperty("started");

		p.setProperty("classname", classname);
		p.setProperty("name", name);


		/* Create and start the agent. */

		AgentControllerInterface agent = AK.getAgentKernel().createAgent(p);

		if (agent != null)
		{
			agent.start();
		}
		else
		{
			error("Could not start agent " + name + " (" + classname + ')');

			unschedule();
		}
	}
}

