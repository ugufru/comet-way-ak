
package com.cometway.props;


import com.cometway.ak.Agent;
import com.cometway.props.Props;
import com.cometway.props.PropsException;
import com.cometway.props.PropsList;
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


/**
* This agent exports the contents of a Props list
* to a C-Style text file containing the structured data.
*/

public class CStylePropsExportAgent extends Agent
{
	protected final static String EOL = System.getProperty("line.separator");


	public void initProps()
	{
		setDefault("database_name", "database/item");
		setDefault("from_fields", "item_number, name, category, price, created, modified, request_remote_addr, request_remote_host, opcode");
		setDefault("save_file", "item.txt");
		setDefault("to_fields", "1, 2, 3, 4, 5, 6, 7, 8, 9");
	}


	public void start()
	{
		try
		{
			saveToFile();
		}
		catch (Exception e)
		{
			error("Could not continue.", e);
		}
	}


	public void saveToFile() throws PropsException
	{
		String save_file = getString("save_file");

		if (save_file.length() == 0)
		{
			throw new PropsException("Zero length filename");
		}

		String tempfile = save_file + ".temp";

		File file = new File(save_file);
		File temp = new File(tempfile);
		FileWriter tempWriter = null;
		BufferedWriter out = null;

		try
		{
			tempWriter = new FileWriter(temp);
			out = new BufferedWriter(tempWriter);

			saveAllRecords(out);

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
			throw new PropsException("Could not save: " + save_file, e);
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
					System.err.println("Could not close BufferedWriter: " + save_file);
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
					System.err.println("Could not close FileWriter: " + save_file);
					e2.printStackTrace(System.err);
				}
			}
		}
	}


	protected void writeDataMap(List fromFields, List toFields, Writer out) throws Exception
	{
		boolean whitespace = getBoolean("whitespace");
		boolean sorted = getBoolean("sorted");
		Props p = new Props();

		int toCount = toFields.size();

		for (int i = 0; i < toCount; i++)
		{
			String toField = (String) toFields.get(i);
			String fromField = (String) fromFields.get(i);

			p.setProperty(toField, fromField);
		}

		p.setProperty("key", "map");

		// Write the property-name mapping to be used in the following data records.

		CStylePropsWriter w = new CStylePropsWriter(out);
		w.setWhiteSpace(whitespace);
		w.setSorted(sorted);
		w.writeProps(p);
	}


	protected void saveAllRecords(Writer out) throws Exception
	{
		List from_fields = getTokens("from_fields");
		List to_fields = getTokens("to_fields");

		writeDataMap(from_fields, to_fields, out);

		writeData(from_fields, to_fields, out);

		out.flush();
	}


	protected void writeData(List fromFields, List toFields, Writer out) throws Exception
	{
		String database_name = getString("database_name");
		boolean whitespace = getBoolean("whitespace");
		boolean sorted = getBoolean("sorted");

		CStylePropsWriter w = new CStylePropsWriter(out);
		w.setWhiteSpace(whitespace);
		w.setSorted(sorted);

		PropsList database = (PropsList) getServiceImpl(database_name);
		List v = database.listProps();
		int count = v.size();

		Props tp = new Props();

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) v.get(i);

			int fromCount = fromFields.size();
	
			for (int x = 0; x < fromCount; x++)
			{
				String fromKey = (String) fromFields.get(x);
				String toKey = (String) toFields.get(x);
				Object value = p.getProperty(fromKey);

				if (tp.hasProperty(toKey) == false)
				{
					//if (value instanceof String) tp.setProperty(toKey, "String");
					if (value instanceof Date) tp.setProperty(toKey, "Date");
					else if (value instanceof Number) tp.setProperty(toKey, "Number");
					else if (value instanceof Boolean) tp.setProperty(toKey, "Boolean");
				}
			}
		}

		tp.setProperty("key", "type");
		w.writeProps(tp);


		for (int i = 0; i < count; i++)
		{
			Props p = (Props) v.get(i);
			Props pp = new Props();

			println("Saving " + database_name + " record (" + (i + 1) + " of " + count + ")");

//			debug("\n" + p);

			int toCount = toFields.size();

			for (int x = 0; x < toCount; x++)
			{
				String fromField = (String) fromFields.get(x);
				String toField = (String) toFields.get(x);
				Object value = p.getProperty(fromField);

//debug("fromField = " + fromField + ", toField = "+ toField + ", value = " + value);

				pp.setProperty(toField, value);
			}
	
			pp.setProperty("key", "data");

//debug("writeProps()\n" + pp);

			w.writeProps(pp);
		}
	}
}

