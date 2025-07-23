
package com.cometway.swing;

import com.cometway.ak.*;
import com.cometway.props.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;


/**
* This is a swing TableModel implementation for editing startup agents.
*/

public class AgentTableModel extends AbstractTableModel
{
	Vector agentList;
	Vector columnNames;


	/**
	* Constructor the AgentTableModel.
	* @param agentList The list of agent descriptions to model.
	*/
	
	public AgentTableModel(Vector agentList)
	{
		this.agentList = agentList;

		columnNames = new Vector();
		columnNames.addElement("Startup");
		columnNames.addElement("ID");
		columnNames.addElement("Name");
		columnNames.addElement("Class");
		columnNames.addElement("Service Name");
		columnNames.addElement("HP");
		columnNames.addElement("HD");
		columnNames.addElement("HW");

		sortByAgentID();
	}


	/**
	* Returns the number of columns (7).
	*/
	
	public int getColumnCount()
	{
		return (columnNames.size());
	}


	/**
	* Returns the Class of data in the specified Column.
	*/

	public Class getColumnClass(int column)
	{
		Class c;

		switch (column)
		{
			case 0:
			case 5:
			case 6:
			case 7:
				c = Boolean.class;
			break;

			case 1:
				c = Integer.class;
			break;

			default:
				c = String.class;
			break;
		}

		return (c);
	}


	/**
	* Returns the name of the specified column.
	*/
	
	public String getColumnName(int column)
	{
		return ((String) columnNames.elementAt(column));
	}


	/**
	* Returns the number of rows in this TableModel.
	*/
	
	public int getRowCount()
	{
		return (agentList.size());
	}


	/**
	* Returns the value of the specified row and column.
	*/

	public Object getValueAt(int row, int col)
	{
		Object value = null;
		Props p = (Props) agentList.elementAt(row);

		switch (col)
		{
			case 0:
				value = Boolean.valueOf(p.getBoolean("startup"));
			break;

			case 1:
				value = Integer.valueOf(p.getInteger("agent_id"));
			break;

			case 2:
				value = p.getProperty("name");
			break;

			case 3:
				value = p.getProperty("classname");
			break;

			case 4:
				value = p.getProperty("service_name");
			break;

			case 5:
				value = Boolean.valueOf(p.getBoolean("hide_println"));
			break;

			case 6:
				value = Boolean.valueOf(p.getBoolean("hide_debug"));
			break;

			case 7:
				value = Boolean.valueOf(p.getBoolean("hide_warning"));
			break;
		}

		return (value);
	}


	/**
	* Returns true if the specified cell is editable; false otherwise.
	*/
	
	public boolean isCellEditable(int row, int column)
	{
		return (true);
	}


	/**
	* Call this method to indicate that the agent descriptions in the table have changed.
	*/

	public void notifyAgentsChanged()
	{
		fireTableDataChanged();
	}


	/**
	* Sets the list of agents this TableModel is rendering.
	*/
	
	public void setAgentList(Vector agentList)
	{
		this.agentList = agentList;

		sortByAgentID();
	}

	/**
	* Sets the value of the specified cell.
	*/
	
	public void setValueAt(Object value, int row, int col)
	{
		Props p = (Props) agentList.elementAt(row);

		switch (col)
		{
			case 0:
				p.setProperty("startup", value);
			break;

			case 1:
				p.setProperty("agent_id", value);
				sortByAgentID();
			break;

			case 2:
				p.setProperty("name", value);
			break;

			case 3:
				p.setProperty("classname", value);
			break;

			case 4:
				p.setProperty("service_name", value);
			break;

			case 5:
				p.setProperty("hide_println", value);
			break;

			case 6:
				p.setProperty("hide_debug", value);
			break;

			case 7:
				p.setProperty("hide_warning", value);
			break;
		}
	}


	/**
	* Sorts the agents by their agent_id property.
	*/

	public void sortByAgentID()
	{
		PropsList.sortPropsList(agentList, "agent_id");

		fireTableDataChanged();
	}
}


