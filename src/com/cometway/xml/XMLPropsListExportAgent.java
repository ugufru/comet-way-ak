
package com.cometway.xml;


import com.cometway.ak.Agent;
import com.cometway.ak.AK;
import com.cometway.props.Props;
import com.cometway.props.PropsList;
import com.cometway.props.PropsException;
import com.cometway.props.PropsComparator;
import com.cometway.util.Base64Encoding;
import com.cometway.util.DateTools;
import com.cometway.util.ObjectSerializer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
* Exports an XMLPropsList formatted file from the specified PropsList.
*/

public class XMLPropsListExportAgent extends Agent
{
	protected final static DateFormat ISO8601_DATEFORMAT = DateTools.ISO8601_DATEFORMAT;
	protected final static DateFormat ISO8601_TIMEFORMAT = DateTools.ISO8601_TIMEFORMAT;
	protected final static String EOL = System.getProperty("line.separator");
	protected final static boolean ALPHABETIZED_PROPS = true;


	public void initProps()
	{
		setDefault("save_file", "database.xml");
		setDefault("database_name", "database");
		setDefault("container_tag", "props_list");
		setDefault("props_tag", "props");
		setDefault("sort_by", "name");
	}

	
	/**
	* Loads a Props List XML file into a List of Props.
	*/

	public void start()
	{
		try
		{
			String filename = getString("save_file");

			if (filename.length() > 0)
			{
				PropsList propsList = (PropsList) getServiceImpl(getString("database_name"));
				List v = propsList.listProps();
				List sorts = getTokens("sort_by");
				int count = sorts.size();

				for (int i = 0; i < count; i++)
				{
					String key = (String) sorts.get(i);

					debug("Sorting by " + key + "...");

					Collections.sort(v, new PropsComparator(key));
				}

				println("Saving " + v.size() + " records to " + filename + " at " + getDateTimeStr());

				saveToFile(filename, v);
			}
		}
		catch (PropsException e)
		{
			error("Could not save database", e.getOriginalException());
		}
		catch (Exception e)
		{
			error("Could not save database", e);
		}
	}


	/**
	* Saves a List of Props to a XML file.
	*/

	protected void saveToFile(String filename, List propsList) throws PropsException
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
				file.delete();
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
					error("Could not close BufferedWriter: " + filename, e2);
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
					error("Could not close FileWriter: " + filename, e2);
				}
			}
		}
	}


	/**
	* Outputs a List of Props as XML using the specificed Writer.
	*/

	protected void write(Writer out, List propsList) throws IOException
	{
		String container_tag = getString("container_tag");
		String props_tag = getString("props_tag");

		writePropsList(out, propsList, container_tag, props_tag);
	}


	/**
	* User this method to write a PropsList as XML to a Writer.
	*/

	public static void writePropsList(Writer out, List propsList, String containerTag, String propsTag) throws IOException
	{
		String containerStart = '<' + containerTag + '>';
		String containerEnd = "</" + containerTag + '>';
		String propsStart = '<' + propsTag + '>';
		String propsEnd = "</" + propsTag + '>';

		out.write(XML.XML_10_HEADER);
		out.write(EOL);
		out.write(containerStart);
		out.write(EOL);

		int count = propsList.size();

//		println("Saving " + count + " records.");

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) propsList.get(i);

			if (p != null)
			{
				out.write('\t');
				out.write(propsStart);
				out.write(EOL);

				List keys = p.getKeys();

				if (ALPHABETIZED_PROPS)
				{
					Collections.sort(keys);
				}

				int keyCount = keys.size();

				for (int x = 0; x < keyCount; x++)
				{
					String key = (String) keys.get(x);
					Object value = p.getProperty(key);

					if ((value instanceof String) || (value instanceof Number) || (value instanceof Boolean))
					{
						String s = p.getString(key);

						s = XML.encode(s);

						out.write("\t\t<");
						out.write(key);
						out.write('>');
						out.write(s);
						out.write("</");
						out.write(key);
						out.write('>');
						out.write(EOL);
					}

					else if (value instanceof Date)
					{
// Someday we might want to make these UTC formatted dates with decimal seconds.
// For the sake of human readability and relevance, this was not added at this time.

						String s = DateTools.toISO8601String(p.getDate(key));

						out.write("\t\t<");
						out.write(key);
						out.write('>');
						out.write("<date>");
						out.write(s);
						out.write("</date>");
						out.write("</");
						out.write(key);
						out.write('>');
						out.write(EOL);
					}

					else if (value instanceof Serializable)
					{
						byte[] data = ObjectSerializer.serialize(value);
						String s = Base64Encoding.encode(data);

						out.write("\t\t<");
						out.write(key);
						out.write('>');
						out.write("<base64>");
						out.write(s);
						out.write("</base64>");
						out.write("</");
						out.write(key);
						out.write('>');
						out.write(EOL);
					}

					else
					{
AK.getDefaultReporter().warning("XMLPropsListExportAgent.writePropsList", "Property \"" + key + "\" (" + value.getClass().getName() + ") is not serializable and was not written.");
					}
				}

				out.write('\t');
				out.write(propsEnd);
				out.write(EOL);
			}
		}

		out.write(containerEnd);
		out.write(EOL);
		out.flush();
	}
}

