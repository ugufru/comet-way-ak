
package com.cometway.biolife;


public interface GeneticAgentInterface extends com.cometway.ak.AgentInterface
{
	public int getCurrentState();

	public int eatAgent();

	public int getEnergy();

	public int getHeartbeat();

	public void heartbeat();
}



