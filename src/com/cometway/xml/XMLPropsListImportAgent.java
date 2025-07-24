
package com.cometway.xml;


import com.cometway.ak.Agent;
import com.cometway.props.Props;
import com.cometway.props.PropsListInterface;
import com.cometway.props.PropsException;
import com.cometway.util.Base64Encoding;
import com.cometway.util.DateTools;
import com.cometway.util.ObjectSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Vector;



/**
* Imports Props records from a XMLPropsList file into the specified PropsListInterface.
*/

public class XMLPropsListImportAgent extends Agent
{
	public void initProps()
	{
		setDefault("load_file", "database.xml");
		setDefault("database_name", "database");
		setDefault("container_tag", "props_list");
		setDefault("props_tag", "props");
	}


	/**
	* Loads a Props List XML file into a Vector of Props.
	*/

	public void start()
	{
		String filename = getTrimmedString("load_file");
		FileInputStream in = null;

		try
		{
			println("Importing Props from " + filename);

			in = new FileInputStream(new File(filename));

			int count = parse(in);

			println("" + count + " records loaded");
		}
		catch (Exception e)
		{
			error("Could not continue", e);
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
					error("Could not close InputStream for " + filename + ": " + in, e);
				}
			}
		}
	}


	protected int parse(InputStream in) throws Exception
	{
		PropsListInterface database = (PropsListInterface) getServiceImpl(getTrimmedString("database_name"));
		String container_tag = getString("container_tag");
		String props_tag = getString("props_tag");
		List results = parseInputStream(in, container_tag, props_tag);
		int count = results.size();

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) results.get(i);

debug("******************** RECEIVED ********************\n" + p);

			database.addProps(p);
		}

		return (count);
	}


	/**
	* Loads a Props List XML document into a Vector of Props.
	*/

	public static List parseInputStream(InputStream in, String containerTag, String propsTag) throws XMLParserException
	{
		List results = new Vector();
		String containerStart = '<' + containerTag + '>';
		String containerEnd = "</" + containerTag + '>';
		String propsStart = '<' + propsTag + '>';
		String propsEnd = "</" + propsTag + '>';
		int count = 0;

		XMLParser parser = new XMLParser(in);
		parser.nextToken(XML.XML_10_HEADER);
		parser.nextToken(containerStart);

		while (true)
		{
			XMLToken t = parser.nextToken();

			if (t.data.equals(propsStart))
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
					else if (t.data.equals(propsEnd))
					{
						break;
					}
					else
					{
						throw new XMLParserException("Invaid token: " + t.data);
					}
				}

				results.add(p);
			}
			else if (t.data.equals(containerEnd))
			{
				break;
			}
			else
			{
				throw new XMLParserException("Invaid token: " + t.data);
			}
		}

		parser.close();

		return (results);
	}
}

