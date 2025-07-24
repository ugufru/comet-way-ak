
package com.cometway.biolife;

import com.cometway.ak.*;
import com.cometway.props.*;
import com.cometway.swing.*;
import java.util.*;


public class GeneticAgent extends Agent implements GeneticAgentInterface
{
	public final static int DIE = 0;
	public final static int LIVE = 1;
	public final static int DIVIDE = 2;
	public final static int PHOTOSYNTHESIZE = 3;
	public final static int PLANT_RESPIRATE = 7;
	public final static int ANIMAL_RESPIRATE = 8;
	public final static int METABOLIZE = 4;
	public final static int MOVE = 5;
	public final static int EAT = 6;
	
	protected static Random random = new Random();
	private static int agentCount;

	private int heartbeat;
	private int energy;
	private int currentState;
	private int nextState;
	protected int cellIndex;
	protected LightBox microscope;
	protected SlideAgent slide;


	public void initProps()
	{
		props.setDefault("life_span", "25");
		props.setDefault("cell_index", "-1");
		props.setDefault("colors_show_state", "false");
		props.setDefault("default_color", "1");
	}


	public void start()
	{
		microscope = (LightBox) ServiceManager.getService("microscope");
		slide = (SlideAgent) ServiceManager.getService("slide");
		cellIndex = props.getInteger("cell_index");
		heartbeat = 0;
		energy = 1;
	
		if (slide != null)
		{
			if (cellIndex == -1)
			{
				cellIndex = slide.getRandomEmptyIndex();
			}

			if (cellIndex != -1)
			{
				slide.setAgent(cellIndex, this);
			}
		}

		setNextState(LIVE);
	}


	public void addEnergy(int amount)
	{
		energy += amount;
	}


	public void chooseNextState()
	{
		// Decide which state will be executed for the next turn.
	}


	public int eatAgent()
	{
		int eatEnergy = energy;

		die();

		println("Help! I've been eaten for " + eatEnergy + " points of energy!");

		return (eatEnergy);
	}


	public void executeCurrentState()
	{
		// Use currentState to select an action for this turn.
	}


	public int getCurrentState()
	{
		return (currentState);
	}


	public int getEnergy()
	{
		return (energy);
	}


	public int getHeartbeat()
	{
		return (heartbeat);
	}


	public int getVisibleState(int state)
	{
		return ((state > 0) ? props.getInteger("default_color") : 0);
	}


	public void heartbeat()
	{
		currentState = nextState;

		if (props.getBoolean("started"))
		{
			heartbeat++;
	
			executeCurrentState();
			chooseNextState();
		}
	}


	public void live()
	{
		println("I'm alive!");

		agentCount++;
	}


	public void die()
	{
		println("I'm dead!");

		agentCount--;

		energy = 0;

		if (cellIndex != -1)
		{
			slide.removeAgent(cellIndex, this);
		}

		props.setBoolean("started", false);
	}


	public void divide()
	{
		if (getEnergy() >= 2)
		{
			println("Dividing");

			if (props.getBoolean("spawn_agents"))
			{
				int startDir = Math.abs(random.nextInt()) % 8;
				int newIndex;

				for (int dir = startDir; dir < startDir + 8; dir++)
				{
					GeneticAgentInterface neighbor = slide.getNeighbor(cellIndex, dir % 8);

					if (neighbor == null)
					{
						spawn(slide.getNeighborIndex(cellIndex, dir % 8));
						useEnergy(2);
						props.setBoolean("has_divided", true);
						break;
					}
				}
			}
		}
	}


	protected void spawn(int index)
	{
		Props p = new Props();

		p.copyFrom(props);
		p.removeProperty("agent_id");
		p.removeProperty("started");
		p.setInteger("cell_index", index);

		AgentControllerInterface agent = AK.instance.createAgent(p);

		if (agent != null)
		{
			agent.start();
		}
	}


	public void setNextState(int nextState)
	{
		this.nextState = (energy <= 0) ? DIE : nextState;

		if (microscope != null)
		{
			if (props.getBoolean("colors_show_state"))
			{
				microscope.setCellState(cellIndex, nextState);
			}
			else
			{
				microscope.setCellState(cellIndex, getVisibleState(nextState));
			}
		}
	}


	public void useEnergy(int amount)
	{
		energy -= amount;
	}
}


