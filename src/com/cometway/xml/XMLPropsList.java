
package com.cometway.xml;


import com.cometway.ak.AK;
import com.cometway.ak.AgentControllerInterface;
import com.cometway.ak.Scheduler;
import com.cometway.ak.ServiceAgent;
import com.cometway.props.IPropsChangeListener;
import com.cometway.props.Props;
import com.cometway.props.PropsException;
import com.cometway.props.PropsList;
import com.cometway.props.PropsListAgent;
import com.cometway.util.Base64Encoding;
import com.cometway.util.DateTools;
import com.cometway.util.FlushInterface;
import com.cometway.util.ISchedulable;
import com.cometway.util.ISchedule;
import com.cometway.util.IScheduleChangeListener;
import com.cometway.util.ObjectSerializer;
import com.cometway.util.Schedule;
import com.cometway.xml.XML;
import com.cometway.xml.XMLParser;
import com.cometway.xml.XMLParserException;
import com.cometway.xml.XMLRequestAgent;
import com.cometway.xml.XMLToken;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;


public class XMLPropsList extends PropsListAgent
{
	public final static String MIME_TYPE = "application/text";

	protected final static boolean ALPHABETIZED_PROPS = true;
	protected final static String EOL = System.getProperty("line.separator");
	protected final static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd-HHmmss");

	protected ScheduledTask task;


	public void initProps()
	{
		setDefault("service_name", "database");
		setDefault("schedule", "between 0:0:0-23:59:59 every 5m");
		setDefault("load_file", "database.props");
		setDefault("save_file", "database.props");
	}


	public void start()
	{
		String service_name = getTrimmedString("service_name");
		String schedule = getString("schedule");

		propsList = new PropsList();
		propsList.setFlushHandler(new FlushHandler());

		registerService(service_name, propsList);

		loadPropsList();

		if ((schedule.length() > 0) && (schedule.equals("none") == false))
		{
			task = new ScheduledTask(schedule);

			Scheduler.getScheduler().schedule(task);
		}
	}


	public void stop()
	{
		savePropsList();

		if (task != null)
		{
			Scheduler.getScheduler().unschedule(task);

			task = null;
		}

		String service_name = getTrimmedString("service_name");

		unregisterService(service_name, propsList);

		propsList = null;
	}


	public void savePropsList()
	{
		if (propsList.needsToSave())
		{
			String filename = getString("save_file");
			String service_name = getString("service_name");

			if ((filename.length() > 0) && (filename.equals("none") == false))
			{
				try
				{
					List v = propsList.listProps();

					Props agentProps = new Props();
					agentProps.setProperty("hide_println", this);
					agentProps.setProperty("hide_debug", this);
					agentProps.setProperty("hide_warning", this);
					agentProps.setProperty("name", service_name + " XMLPropsListExportAgent");
					agentProps.setProperty("classname", "com.cometway.xml.XMLPropsListExportAgent");
					agentProps.setProperty("save_file", filename);
					agentProps.setProperty("container_tag", "props_list");
					agentProps.setProperty("props_tag", "props");
					agentProps.setProperty("database_name", service_name);

					AgentControllerInterface agent = AK.getAgentKernel().createAgent(agentProps);
					agent.start();
					agent.stop();
					agent.destroy();

					propsList.resetNeedsToSave();
				}
				catch (Exception e)
				{
					error("Could not save: " + filename, e);
				}
			}
		}
	}


	protected void loadPropsList()
	{
		String filename = getString("load_file");
		String service_name = getString("service_name");

		if ((filename.length() > 0) && (filename.equals("none") == false))
		{

			try
			{
				Props agentProps = new Props();
				agentProps.setProperty("hide_println", this);
				agentProps.setProperty("hide_debug", this);
				agentProps.setProperty("hide_warning", this);
				agentProps.setProperty("name", service_name + " XMLPropsListImportAgent");
				agentProps.setProperty("classname", "com.cometway.xml.XMLPropsListImportAgent");
				agentProps.setProperty("load_file", filename);
				agentProps.setProperty("database_name", service_name);
				agentProps.setProperty("container_tag", "props_list");
				agentProps.setProperty("props_tag", "props");

				AgentControllerInterface agent = AK.getAgentKernel().createAgent(agentProps);
				agent.start();
				agent.stop();
				agent.destroy();
			}
			catch (Exception e)
			{
				error("Could not load: " + filename, e);
			}
		}
	}


	class ScheduledTask implements ISchedulable
	{
		Schedule schedule;


		public ScheduledTask(String scheduleStr)
		{
			schedule = new Schedule(scheduleStr);
		}

		/**
		* Called by Scheduler to add a listener to our Schedule.
		* Returns false to indicate that we don't need this feature.
		*/

		public boolean addScheduleChangeListener(IScheduleChangeListener l)
		{
			return (false);
		}


		/**
		* Returns the instance to the schedule.
		*/

		public ISchedule getSchedule()
		{
			return (schedule);
		}


		/**
		* Called by Scheduler to remove a listener from our Schedule.
		* Returns false to indicate that we don't need this feature.
		*/

		public boolean removeScheduleChangeListener(IScheduleChangeListener l)
		{
			return (false);
		}


		/**
		* Called by Scheduler based on the Schedule returned by getSchedule method.
		*/

		public void wakeup()
		{
			savePropsList();
		}
	}


	class FlushHandler implements FlushInterface
	{
		public void flush()
		{
			savePropsList();
		}
	}


	/**
	* Loads a Props List XML document into a Vector of Props.
	*/

	public static Vector parseInputStream(InputStream in) throws XMLParserException
	{
		Vector v = new Vector();

		XMLParser parser = new XMLParser(in);
		parser.nextToken(XML.XML_10_HEADER);
		parser.nextToken("<props_list>");

		while (true)
		{
			XMLToken t = parser.nextToken();

			if (t.data.equals("<props>"))
			{
				Props p = new Props();

				while (true)
				{
					t = parser.nextToken();

					if (t.type == XML.START_TAG)
					{
						String key = t.data.substring(1, t.data.length() - 1);
						t = parser.nextToken();

						if (t.data.equals("<base64>"))
						{
							t = parser.nextElementContent();

							try
							{
								byte[] data = Base64Encoding.decode(t.data);
								Object o = ObjectSerializer.unserialize(data);
								p.setProperty(key, o);
							}
							catch (Exception e)
							{
								throw new XMLParserException("Invaid token: " + t.data);
							}

							parser.nextToken("</base64>");
							parser.nextToken("</" + key + '>');
						}
						else if (t.data.equals("<date>"))
						{
							t = parser.nextElementContent();

							try
							{
// At some point we should recognize UTC formatting (ends with a Z)
// as well as decimal seconds. It would be prudent to also
// remove intermediate dashes and slashes before parsing.

								Date theDate = DateTools.parseISO8601String(t.data);

								p.setProperty(key, theDate);
							}
							catch (Exception e)
							{
								throw new XMLParserException("Invaid token: " + t.data);
							}

							parser.nextToken("</date>");
							parser.nextToken("</" + key + '>');
						}
						else if (t.type == XML.ELEMENT_CONTENT)
						{
							p.setProperty(key, t.data);
							parser.nextToken("</" + key + '>');
						}
						else if (t.data.equals("</" + key + '>') == false)
						{
							throw new XMLParserException("Invaid token: " + t.data);
						}
					}
					else if (t.data.equals("</props>"))
					{
						break;
					}
					else
					{
						throw new XMLParserException("Invaid token: " + t.data);
					}
				}

				v.addElement(p);
			}
			else if (t.data.equals("</props_list>"))
			{
				break;
			}
			else
			{
				throw new XMLParserException("Invaid token: " + t.data);
			}
		}

		parser.close();

		return (v);
	}


	/**
	* Loads a Props List XML file into a Vector of Props.
	*/

	public static Vector loadFromFile(String filename) throws PropsException
	{
		Vector v = new Vector();
		FileInputStream in = null;

		try
		{
			in = new FileInputStream(new File(filename));
			v = parseInputStream(in);
		}
		catch (Exception e)
		{
			throw new PropsException("Could not parse: " + filename, e);
		}
		finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (Exception e)
				{
					throw new PropsException("Could not close InputStream for " + filename + ": " + in, e);
				}
			}
		}

		return (v);
	}

	/**
	* Saves a Vector of Props to a XML file.
	*/

	public static void saveToFile(String filename, List propsList) throws PropsException
	{
		if (filename.length() == 0)
		{
			throw new PropsException("Zero length filename");
		}

		String tempfile = filename + ".temp";

		File file = new File(filename);
		File temp = new File(tempfile);
		FileWriter tempWriter = null;
		BufferedWriter out = null;

		try
		{
			tempWriter = new FileWriter(temp);
			out = new BufferedWriter(tempWriter);

			write(out, propsList);

			// Make sure our stream is closed so others can change this file.
			// Windows 2K is very strict about file locking.

			out.close();
			out = null;

			tempWriter.close();
			tempWriter = null;

			if (file.exists())
			{
				boolean result = file.delete();
// This always returns false under Windows 2K...how useful is that?
			}

			temp.renameTo(file);
		}
		catch (Exception e)
		{
			throw new PropsException("Could not save: " + filename, e);
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (Exception e2)
				{
					System.err.println("Could not close BufferedWriter: " + filename);
					e2.printStackTrace(System.err);
				}
			}

			if (tempWriter != null)
			{
				try
				{
					tempWriter.close();
				}
				catch (Exception e2)
				{
					System.err.println("Could not close FileWriter: " + filename);
					e2.printStackTrace(System.err);
				}
			}
		}
	}


	/**
	* Outputs a Vector of Props as XML using the specificed Writer.
	*/

	public static void write(Writer out, List propsList) throws IOException
	{
		out.write(XML.XML_10_HEADER);
		out.write(EOL);
		out.write("<props_list>");
		out.write(EOL);

		int count = propsList.size();

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) propsList.get(i);

			if (p != null)
			{
				out.write("\t<props>");
				out.write(EOL);

				Vector keys = p.getKeys();
				int keyCount = keys.size();

				if (ALPHABETIZED_PROPS && (keyCount > 1))
				{
					for (int a = 0; a < keyCount - 1; a++)
					{
						String aStr = (String) keys.elementAt(a);

						for (int b = a + 1; b < keyCount; b++)
						{
							String bStr = (String) keys.elementAt(b);

							if (aStr.compareTo(bStr) > 0)
							{
								keys.setElementAt(aStr, b);
								keys.setElementAt(bStr, a);
								aStr = bStr;
							}
						}
					}
				}

				for (int x = 0; x < keyCount; x++)
				{
					String key = (String) keys.elementAt(x);
					Object value = p.getProperty(key);

					out.write("\t\t<");
					out.write(key);
					out.write('>');

					if ((value instanceof String) || (value instanceof Integer) || (value instanceof Boolean))
					{
						String s = p.getString(key);
						out.write(XML.encode(s));
					}
					else
					{
						byte[] data = ObjectSerializer.serialize(value);
						String s = Base64Encoding.encode(data);

						out.write("<base64>");
						out.write(s);
						out.write("</base64>");
					}

					out.write("</");
					out.write(key);
					out.write('>');
					out.write(EOL);
				}

				out.write("\t</props>");
				out.write(EOL);
			}
		}

		out.write("</props_list>");
		out.write(EOL);
		out.flush();
	}
}

