
package com.cometway.biolife;


import com.cometway.ak.*;
import com.cometway.props.*;


public class AnimalAgent extends GeneticAgent
{
	private int O2;
	private int H2O;
	private int CO2;


	public void initProps()
	{
		props.setDefault("life_span", "25");
		props.setDefault("cell_index", "-1");
		props.setDefault("divide_energy", "5");
		props.setDefault("move_energy", "2");
		props.setDefault("spawn_agents", "true");
		props.setDefault("max_offspring", "3");
		props.setDefault("colors_show_state", "false");
		props.setDefault("default_color", "4");
	}


	public void chooseNextState()
	{
		if (props.getBoolean("has_divided"))
		{
			props.incrementInteger("offspring");
			props.removeProperty("has_divided");
		}

		int offspring = props.getInteger("offspring");
		int max_offspring = props.getInteger("max_offspring");

		if (getHeartbeat() >= props.getInteger("life_span"))
		{
			setNextState(DIE);
		}
		else if ((offspring < max_offspring) && (getEnergy() >= props.getInteger("divide_energy")))
		{
			setNextState(DIVIDE);
		}
		else if ((findPlantNeighbor() != -1) && (getEnergy() >= props.getInteger("move_energy")))
		{
			setNextState(EAT);
		}
		else if ((offspring >= max_offspring) && (getEnergy() >= props.getInteger("move_energy")))
		{
			setNextState(MOVE);
		}
		else if ((O2 > 0) && (H2O > 0))
		{
			setNextState(METABOLIZE);
		}
		else
		{
			setNextState(ANIMAL_RESPIRATE);
		}
	}


	public void executeCurrentState()
	{
		switch (getCurrentState())
		{
			case LIVE:
				live();
			break;

			case ANIMAL_RESPIRATE:
				respirate();
			break;

			case METABOLIZE:
				metabolize();
			break;

			case EAT:
				eat();
			break;

			case MOVE:
				move();
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

		return (10 + level);
	}


	public int findPlantNeighbor()
	{
		int direction = -1;
		int startDir = Math.abs(random.nextInt()) % 8;

		for (int dir = startDir; dir < startDir + 8; dir++)
		{
			GeneticAgentInterface neighbor = slide.getNeighbor(cellIndex, dir % 8);

			if (neighbor instanceof PlantAgent)
			{
				direction = dir % 8;
			}
		}

		return (direction);
	}


	public void respirate()
	{
		println("Respirating");

		O2++;
		H2O++;

		if (CO2 > 0)
		{
			CO2--;
		}
	}


	public void metabolize()
	{
		if ((O2 > 0) && (H2O > 0))
		{
			println("Metabolizing");

			O2 -= 1;
			H2O -= 1;
			CO2++;

			addEnergy(1);
		}
	}


	public void eat()
	{
		if (getEnergy() >= 2)
		{
			int plantDir = findPlantNeighbor();

			if (plantDir != -1)
			{
				int plantIndex = slide.getNeighborIndex(cellIndex, plantDir);
				GeneticAgentInterface plant = slide.getAgent(plantIndex);

				if (plant != null)
				{
					println("Eating " + plant); 

					if (microscope != null)
					{
						microscope.setCellState(cellIndex, 0);
					}

					addEnergy(plant.eatAgent());					

					slide.removeAgent(cellIndex, this);

					cellIndex = plantIndex;
					slide.setAgent(cellIndex, this);

					useEnergy(1);
				}
			}
		}
	}


	public void move()
	{
		if (getEnergy() >= 2)
		{
			int startDir = Math.abs(random.nextInt()) % 8;

			for (int dir = startDir; dir < startDir + 8; dir++)
			{
				GeneticAgentInterface neighbor = slide.getNeighbor(cellIndex, dir % 8);

				if (neighbor == null)
				{
					println("Moving direction " + (dir % 8));

					if (microscope != null)
					{
						microscope.setCellState(cellIndex, 0);
					}

					slide.removeAgent(cellIndex, this);
					cellIndex = slide.getNeighborIndex(cellIndex, dir % 8);
					slide.setAgent(cellIndex, this);

					useEnergy(1);
					break;
				}
			}
		}
	}


	public void writeStats()
	{
		println("CO2=" + CO2 + " H2O=" + H2O + " O2=" + O2 + " E=" + getEnergy());
	}
}


