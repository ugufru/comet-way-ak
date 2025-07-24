
package com.cometway.swing;

import com.cometway.ak.*;
import com.cometway.props.*;
import com.cometway.xml.XMLPropsList;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;



/**
* This is a Startup Agent which can be used to edit Agent Kernel
* .startup files. To use this agent, you must invoke the com.cometway.ak.AK
* class using the -startup_agent com.cometway.swing.Startup editor option.
* This will load the current startup configuration into the Startup Editor
* application, where changes to the agent Props files can be made using
* a Swing based GUI. Saving the configuration will delete the old .startup
* files, and write out the new configuration.
*/

public class StartupEditor extends AbstractJFrameAgent
{
	private static final String[] FILE_MENU = { "File", "Revert", "Save", "-", "Export Startup Files", "Export Startup Agent", "-", "Quit" };
	private static final String[] EDIT_MENU = { "Edit", "Cut", "Copy", "Paste", "Clear", "-", "Select All" };
	private static final String[] AGENT_MENU = { "Agents", "Create Agent", "Edit Agent", "Duplicate Agent", "Delete Agent", "-", "Renumber Agents", "-", "Start Agent Kernel"};
	private static final String[][] MENUBAR = { FILE_MENU, EDIT_MENU, AGENT_MENU };


	private Vector agentList;
	private Vector clipboard;
	private JTable agentTable;


	/**
	* Initializes the Props for this agent.
	*/
	
	public void initProps()
	{
		setDefault("lnf_classname", "java");
		setDefault("frame_title", "Comet Way Agent Browser");
		setDefault("frame_width", "1024");
		setDefault("frame_height", "800");
		setDefault("frame_visible", "false");
		setDefault("next_agent_id", "1001");
		setDefault("startup_dir", ".");
		setDefault("startup_file", "ak.xstartup");
		setDefault("output_classname", "StaticStartupAgent");
	}


	/**
	* Sets the look and feel, initializes, and opens the JFrame for this agent.
	*/
	
	public void start()
	{
		setLookAndFeel();
		openFrame();
	}


	/**
	* Immediately exits the current Java VM without saving changes.
	*/

	public void stop()
	{
		println("Exiting on " + getDateTimeStr());

		System.exit(0);
	}


	/**
	* Creates the ActionListener for interpreting menu events used
	* by this agent.
	*/

	protected ActionListener createMenuActionListener()
	{
		ActionListener l = new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				String command = event.getActionCommand();
				debug("Command: " + command);

				if (command.equals("Revert"))
				{
					revert();
				}
				else if (command.equals("Save"))
				{
					agentTable.clearSelection();
					saveAgents();
				}
				else if (command.equals("Export Startup Files"))
				{
					agentTable.clearSelection();
					exportStartupFiles(getStartupDir());
				}
				else if (command.equals("Export Startup Agent"))
				{
					agentTable.clearSelection();
					exportStartupAgent(getStartupDir());
				}
				else if (command.equals("Quit"))
				{
					getAgentController().stop();
				}
				else if (command.equals("Cut"))
				{
					copyAgent();
					deleteAgent();
				}
				else if (command.equals("Copy"))
				{
					copyAgent();
				}
				else if (command.equals("Paste"))
				{
					pasteAgent();
				}
				else if (command.equals("Select All"))
				{
					agentTable.selectAll();
				}
				else if (command.equals("Create Agent"))
				{
					createAgent();
				}
				else if (command.equals("Duplicate Agent"))
				{
					duplicateAgent();
				}
				else if (command.equals("Delete Agent") || command.equals("Clear"))
				{
					deleteAgent();
				}
				else if (command.equals("Renumber Agents"))
				{
					renumberAgents();
				}
				else if (command.equals("Edit Agent"))
				{
					editAgent();
				}
				else if (command.equals("Start Agent Kernel"))
				{
					startAgentKernel(agentList);
				}
			}
		};

		return (l);
	}


	/**
	* Saves agents to the specified startup_file, deleting
	* any .startup or .agent files in the startup_dir.
	*/

	private void saveAgents()
	{
		deleteAgentFiles(getStartupDir());

		String startupFile = getString("startup_file");

		println("Saving agents to " + startupFile);

		try
		{
			XMLPropsList.saveToFile(startupFile, agentList);
		}
		catch (Exception e)
		{
			error("Could not save agents", e);
		}
	}


	/**
	* Attempts to load the agents from the file specified by the
	* startup_file property. If this fails, an attempt is made to
	* load individual .startup files from the directory specified
	* by the startup_dir property.
	*/

	private void loadAgents()
	{
		String startup_file = getString("startup_file");

//		debug("startup_file = " + startup_file);

		try
		{
			agentList = XMLPropsList.loadFromFile(startup_file);
		}
		catch (Exception e)
		{
			agentList = loadStartupFiles(getStartupDir());
		}
	}


	/**
	* Creates and initializes the Props Editor JFrame.
	*/

	protected void openFrame()
	{
		super.openFrame();
		menuActionListener = createMenuActionListener();
		setMenuBar(MENUBAR);

//		MouseAdapter mouseListener = new MouseAdapter()
//		{
//			public void mouseClicked(MouseEvent e)
//			{
//				TableColumnModel columnModel = agentTable.getColumnModel();
//				int viewColumn = columnModel.getColumnIndexAtX(e.getX()); 
//				int column = agentTable.convertColumnIndexToModel(viewColumn); 
//
//				if ((e.getClickCount() == 1) && (column == 1))
//				{
//					debug("Sorting by Agent ID");
//					AgentTableModel model = (AgentTableModel) agentTable.getModel();
//					model.sortByAgentID();
//				}
//			}
//		};

		loadAgents();

		agentTable = new JTable(new AgentTableModel(agentList));

		setColumnWidth(agentTable, 0, 50);
		setColumnWidth(agentTable, 1, 40);
		setColumnWidth(agentTable, 5, 30);
		setColumnWidth(agentTable, 6, 30);
		setColumnWidth(agentTable, 7, 30);

//		JTableHeader h = agentTable.getTableHeader(); 
//		h.addMouseListener(mouseListener); 

		JScrollPane scrollPane = new JScrollPane(agentTable);
		scrollPane.getViewport().setBackground(Color.white);

		Container c = frame.getContentPane();
		c.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		c.add(scrollPane);

		frame.setVisible(true);
	}


	/**
	* Copies the selected agents to an internal clipboard.
	*/
	
	private void copyAgent()
	{
		clipboard = new Vector();

		int rows[] = agentTable.getSelectedRows();

		for (int i = 0; i < rows.length; i++)
		{
			int row = rows[i];
			Props p = (Props) agentList.elementAt(row);
			debug("Copying " + p.getString("agent_id") + "_" + p.getString("name"));

			Props clone = new Props();
			clone.copyFrom(p);
			clipboard.addElement(clone);
		}
	}


	/**
	* Creates a new agent description and adds it to the editor table.
	*/
	
	private void createAgent()
	{
		Props p = new Props();
		p.setProperty("agent_id", getNextAgentID());
		p.setProperty("name", "Untitled");
		p.setProperty("classname", "com.cometway.ak.Agent");
		agentList.addElement(p);


		AgentTableModel model = (AgentTableModel) agentTable.getModel();
		model.notifyAgentsChanged();

		int lastRow = agentTable.getRowCount() - 1;
		agentTable.changeSelection(lastRow, 1, false, false);
	}


	/**
	* Deletes all .agent and .startup files from the startup directory.
	* This method is called prior to saving the current agent configuration.
	*/

	private void deleteAgentFiles(String startupDir)
	{
		FilenameFilter filter = new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return (name.endsWith(".startup") | name.endsWith(".agent"));
			}
		};

		File f = new File(startupDir);
		String s[] = f.list(filter);

		for (int i = 0; i < s.length; i++)
		{
			File ff = new File(s[i]);

			ff.delete();

			debug("Deleted " + startupDir + ff);
		}
	}


	/**
	* Deletes the selected agent descriptions and removes them from the editor table.
	*/
	
	private void deleteAgent()
	{
		int rows[] = agentTable.getSelectedRows();

		agentTable.clearSelection();

		for (int i = rows.length - 1; i >= 0; i--)
		{
			int row = rows[i];
			Props p = (Props) agentList.elementAt(row);
			debug("Deleting " + p.getString("agent_id") + "_" + p.getString("name"));
			agentList.removeElementAt(row);
		}

//		agentTable.repaint();
		AgentTableModel model = (AgentTableModel) agentTable.getModel();
		model.notifyAgentsChanged();
	}


	/**
	* Deletes the selected agent descriptions and removes them from the editor table.
	*/
	
	private void renumberAgents()
	{
		int base_id = 100;
		int rows[] = agentTable.getSelectedRows();

		for (int i = 0; i < rows.length; i++)
		{
			int row = rows[i];
			Props p = (Props) agentList.elementAt(row);
			debug("Renumbering " + p.getString("agent_id") + "_" + p.getString("name"));

			if (i == 0)
			{
				base_id = p.getInteger("agent_id");
			}

			p.setInteger("agent_id", (i * 10) + base_id);
		}

		AgentTableModel model = (AgentTableModel) agentTable.getModel();
		model.notifyAgentsChanged();
	}


	/**
	* Duplicates the selected agent descriptions and adds them to the editor table.
	*/
	
	private void duplicateAgent()
	{
		int rows[] = agentTable.getSelectedRows();

		for (int i = 0; i < rows.length; i++)
		{
			int row = rows[i];
			Props p = (Props) agentList.elementAt(row);
			debug("Duplicating " + p.getString("agent_id") + "_" + p.getString("name"));

			Props clone = new Props();
			clone.copyFrom(p);
			clone.setProperty("agent_id", getNextAgentID());

			agentList.addElement(clone);
		}

		AgentTableModel model = (AgentTableModel) agentTable.getModel();
		model.notifyAgentsChanged();

		int rowCount = agentTable.getRowCount();
		agentTable.setRowSelectionInterval(rowCount - rows.length, rowCount - 1);
	}


	/**
	* Creates and starts a PropsEditor agent for each selected agent description.
	*/
	
	private void editAgent()
	{
		int rows[] = agentTable.getSelectedRows();

		for (int i = rows.length - 1; i >= 0; i--)
		{
			int row = rows[i];
			Props p = (Props) agentList.elementAt(row);
			debug("Editing " + p.getString("agent_id") + "_" + p.getString("name"));

			Props agentProps = new Props();
			agentProps.setProperty("classname", "com.cometway.swing.PropsEditor");
			agentProps.setProperty("edit_props", (Object) p);

			AgentControllerInterface agent = AK.getAgentKernel().createAgent(agentProps);
			agent.start();
		}
	}


	/**
	* Returns the next agent ID.
	*/
	
	private String getNextAgentID()
	{
		incrementInteger("next_agent_id");

		return (getString("next_agent_id"));
	}


	/**
	* Returns the current startup directory.
	*/
	
	private String getStartupDir()
	{
		String startupDir = getString("startup_dir");
		String fileSeparator = System.getProperty("file.separator");

		if (startupDir.endsWith(fileSeparator) == false)
		{
			startupDir += fileSeparator;
		}

		return (startupDir);
	}


	/**
	* Returns a Vector of agent Props loaded from the startup directory.
	*/

	private Vector loadStartupFiles(String startupDir)
	{
		Vector agents = new Vector();

		File f = new File(startupDir);

		println("Loading agents from " + f);


		/* Get a list of .startup files from the startup directory */

		FilenameFilter filter = new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return (name.endsWith(".startup") | name.endsWith(".agent"));
			}
		};

		String  s[] = f.list(filter);


		/* Create a Vector containing the loaded agent Props. */
	
		for (int i = 0; i < s.length; i++)
		{
			String  str = startupDir + s[i];

			Props agentProps = Props.loadProps(str);

			if (agentProps == null)
			{
				error("Could not load agent: " + str);
			}
			else
			{
				if (str.endsWith(".startup"))
				{
					agentProps.setBoolean("startup", true);
				}

				if (agentProps.hasProperty("name") == false)
				{
					String  name = agentProps.getString("classname");
					int     index = name.lastIndexOf('.');

					if (index > 0)
					{
						name = name.substring(index + 1);
					}

					agentProps.setProperty("name", name);
				}

				agentProps.setDefault("agent_id", getNextAgentID());

				agents.addElement(agentProps);
			}
		}

		return (agents);
	}


	/**
	* Adds agent descriptions from the internal clipboard to the editor table.
	*/

	private void pasteAgent()
	{
		if (clipboard != null)
		{
			for (int i = 0; i < clipboard.size(); i++)
			{
				Props p = (Props) clipboard.elementAt(i);
				p.setProperty("agent_id", getNextAgentID());
				debug("Pasting " + p.getString("agent_id") + "_" + p.getString("name"));
				agentList.addElement(p);
			}

			AgentTableModel model = (AgentTableModel) agentTable.getModel();
			model.notifyAgentsChanged();

			int rowCount = agentTable.getRowCount();
			agentTable.setRowSelectionInterval(rowCount - clipboard.size(), rowCount - 1);
		}
	}


	/**
	* Discards any changes made to the agent descriptions, and reloads them
	* from the startup directory.
	*/
	
	private void revert()
	{
		agentTable.clearSelection();

		loadAgents();

		AgentTableModel model = (AgentTableModel) agentTable.getModel();
		model.setAgentList(agentList);
	}


	/**
	* Write out the Java source for an agent that loads this configuration.
	*/

	private void exportStartupAgent(String startupDir)
	{
		println("Saving agents to " + startupDir);

		try
		{
			String output_classname = getString("output_classname");
			String output_file = output_classname + ".java";
	
			println("Writing static startup class to " + output_file);
	
			File outFile = new File(output_file);
			FileWriter w = new FileWriter(outFile);
	
			w.write("\n");
			w.write("import com.cometway.ak.AK;\n");
			w.write("import com.cometway.ak.Agent;\n");
			w.write("import com.cometway.ak.AgentControllerInterface;\n");
			w.write("import com.cometway.props.Props;\n");
			w.write("\n");
			w.write("public class " + output_classname + " extends Agent\n");
			w.write("{\n");
			w.write("	public void start()\n");
			w.write("	{\n");
			w.write("		Props p;\n");
	
			for (int i = 0; i < agentList.size(); i++)
			{
				Props agentProps = (Props) agentList.elementAt(i);

				if (agentProps.getBoolean("startup"))
				{
					String str = agentProps.getString("agent_id") + "_" + agentProps.getString("name") + " (" + agentProps.getString("classname") + ")";
		
					w.write("\n");
					w.write("		// " + str + "\n\n");
					w.write("		p = new Props();\n");

					writeProperty(w, agentProps, "agent_id");
					writeProperty(w, agentProps, "name");
					writeProperty(w, agentProps, "classname");
	
					Vector v = agentProps.getKeys();
					v.remove("agent_id");
					v.remove("name");
					v.remove("classname");
					v.remove("created");
					v.remove("modified");
					v.remove("next_state");
					v.remove("request_remote_addr");
					v.remove("request_remote_host");
					v.remove("startup");
					v.remove("started");

					Collections.sort(v);
	
					for (int x = 0; x < v.size(); x++)
					{
						String key = (String) v.elementAt(x);
	
						if (key.startsWith("#") == false)
						{
							writeProperty(w, agentProps, key);
						}
					}
	
					w.write("		startAgent(p);\n");
				}
			}

			w.write("	}\n\n\n");
			w.write("	protected void startAgent(Props p)\n");
			w.write("	{\n");
			w.write("		AgentControllerInterface a = AK.getAgentKernel().createAgent(p);\n\n");
			w.write("		if (a != null) a.start();\n");
			w.write("	}\n");
			w.write("}\n\n");
			w.flush();
			w.close();
		}
		catch (Exception e)
		{
			error("Could not write the startup class", e);
		}
	}


	private void writeProperty(Writer w, Props p, String key) throws Exception
	{
		String value = p.getString(key);

		w.write("		p.setProperty(\"");
		w.write(key);
		w.write("\", \"");
		w.write(value);
		w.write("\");\n");
	}



	/**
	* Saves the specified list of agent descriptions to the startup directory.
	*/
	
	private void exportStartupFiles(String startupDir)
	{
		println("Saving agents to " + startupDir);

		deleteAgentFiles(startupDir);
		
		for (int i = 0; i < agentList.size(); i++)
		{
			try
			{
				Props p = (Props) agentList.elementAt(i);
				String filename = startupDir + p.getString("agent_id") + "_" + p.getString("name");
				boolean startup = p.getBoolean("startup");

				if (startup)
				{
					filename += ".startup";
				}
				else
				{
					filename += ".agent";
				}

				p.removeProperty("startup");

				Props.saveProps(filename, p);

				p.setBoolean("startup", startup);

				println("Written " + filename);
			}
			catch (Exception e)
			{
				error("exportStartupFiles", e);
			}
		}
	}


	/**
	* Sets the column width of the specified JTable column.
	*/
	
	private void setColumnWidth(JTable table, int column, int width)
	{
		TableColumnModel tcm = table.getColumnModel();
		TableColumn tc = tcm.getColumn(column);
		tc.setMaxWidth(width);
		tc.setMinWidth(width);
	}


	protected void startAgentKernel(Vector agentList)
	{
		closeFrame();

		int count = agentList.size();

		for (int i = 0; i < count; i++)
		{
			Props agentProps = (Props) agentList.elementAt(i);
			String agentName = agentProps.getString("agent_id") + "_" + agentProps.getString("name");

			if (agentProps.getBoolean("startup"))
			{
				AgentControllerInterface agent = AK.getAgentKernel().createAgent(agentProps);
		
				if (agent == null)
				{
					error("Could not create agent: " + agentName);
				}
				else
				{
					agent.start();
				}
			}
			else
			{
				debug("Skipping: " + agentName);
			}
		}
	}
}


