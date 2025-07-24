
package com.cometway.props;


import com.cometway.ak.Agent;
import com.cometway.util.Base64Encoding;
import com.cometway.util.DateTools;
import com.cometway.util.ObjectSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;



/**
* Imports Props records from a CStyle props file into the specified PropsList.
*/

public class CStylePropsImportAgent extends Agent
{
	public void initProps()
	{
		setDefault("load_file", "database.txt");
		setDefault("database_name", "database");
	}


	public void start()
	{
		String filename = getTrimmedString("load_file");
		FileInputStream in = null;

		try
		{
			println("Importing Props from " + filename);

			in = new FileInputStream(new File(filename));

			int count = parseInputStream(in);

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


	public int parseInputStream(InputStream in) throws PropsException
	{
		int count = 0;
		PropsList database = (PropsList) getServiceImpl(getTrimmedString("database_name"));
		CStylePropsReader reader = new CStylePropsReader(in);

		try
		{
			List v = reader.listProps();
			count = v.size();

			for (int i = 0; i < count; i++)
			{
				Props p = (Props) v.get(i);

				database.addProps(p);
			}

			database.flush();
		}
		catch (Exception e)
		{
			error("Could not read next Props", e);
		}

		return (count);
	}
}

