
package com.cometway.biolife;


import com.cometway.ak.*;
import com.cometway.props.*;


public class PlantAgent extends GeneticAgent
{
	private int CO2;
	private int H2O;
	private int O2;


	public void initProps()
	{
		props.setDefault("life_span", "15");
		props.setDefault("cell_index", "-1");
		props.setDefault("divide_energy", "5");
		props.setDefault("spawn_agents", "true");
		props.setDefault("colors_show_state", "false");
		props.setDefault("default_color", "3");
	}


	public void chooseNextState()
	{
		if (getHeartbeat() >= props.getInteger("life_span"))
		{
			setNextState(DIE);
		}
		else if (getEnergy() >= props.getInteger("divide_energy"))
		{
			setNextState(DIVIDE);
		}
		else if ((CO2 > 0) && (H2O > 0))
		{
			setNextState(PHOTOSYNTHESIZE);
		}
		else
		{
			setNextState(PLANT_RESPIRATE);
		}
	}


	public void executeCurrentState()
	{
		switch (getCurrentState())
		{
			case LIVE:
				live();
			break;

			case PLANT_RESPIRATE:
				respirate();
			break;

			case PHOTOSYNTHESIZE:
				photosynthesize();
			break;

			case DIVIDE:
				divide();
			break;

			case DIE:
				die();
			break;
		}

		writeStats();
	}


	public int getVisibleState(int state)
	{
		int level = getEnergy();

		if (level > 4)
		{
			level = 4;
		}
		else if (level < 0)
		{
			level = 0;
		}

		return (20 + level);
	}


	public void respirate()
	{
		println("Respirating");

		CO2 += 3;
		H2O += 2;

		if (O2 > 0)
		{
			O2 -= 1;
		}
	}


	public void photosynthesize()
	{
		if ((CO2 > 0) && (H2O > 0))
		{
			println("Photosynthesizing");

			CO2 -= 1;
			H2O -= 1;
			O2++;

			addEnergy(1);
		}
	}



	public void writeStats()
	{
		println("CO2=" + CO2 + " H2O=" + H2O + " O2=" + O2 + " E=" + getEnergy());
	}
}


