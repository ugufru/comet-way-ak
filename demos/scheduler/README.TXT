ScheduledDemo
=============

This agent demonstrates how to use com.cometway.ak.ScheduledAgent

Before ScheduledAgent can be used, a ServiceManager and Scheduler must be started.

The ScheduledDemo is a direct subclass of ScheduledAgent and overrides the wakeup method to perform its own action.
This inheritence pattern should be used when a single agent instance needs to be called periodically.

The ScheduledDemo2 is a generic agent that is instantiated and started everytime a ScheduledAgent's wakeup is called.
This usesage pattern should be used to schedule a specific operation provided by an existing agent, or one that does
not already inherit ScheduledAgent.

Note: Providing a schedule property of "none" will inhibit a scheduled agent from being scheduled.


Updated: 2002-02-06 PC