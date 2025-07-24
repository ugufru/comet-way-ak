
import com.cometway.ak.*;
import java.io.*;
import java.util.*;
import com.cometway.util.*;

/**
* This agent scans a directory for new and updated files based on their file date.
* It uses the scheduler to wakeup every 15 seconds.
*/

public class DirectoryScanner extends ScheduledAgent implements FilenameFilter
{
	public void initProps()
	{
		setDefault("schedule", "between 0:0:0-23:59:59 every 15s");
		setDefault("scan_dir", "./");
	}


	public boolean accept(File dir, String name)
	{
		boolean accept;
		File f = new File(dir, name);

		long last_modified = f.lastModified();
		long last_check_ms = getLong("last_check_ms");

//		debug(dir + "/" + name + " (last modified " + new Date(last_modified) + ")");

		if (last_check_ms > 0)
		{
			accept = (last_modified > last_check_ms);	
		}
		else
		{
			accept = true;
		}

//		debug("accept = " + accept);

		return (last_modified > last_check_ms);
	}


	public void wakeup()
	{
		String  scan_dir = getString("scan_dir");
		File    f = new File(scan_dir);

		println("Scanning Directory: " + f + "/");

		scan_dir = f.toString();

		if (scan_dir.endsWith("/") == false)
		{
			scan_dir += "/";
		}

		String  s[] = f.list(this);

		for (int i = 0; i < s.length; i++)
		{
			println("File updated: " + scan_dir +  s[i]);
		}

		setLong("last_check_ms", System.currentTimeMillis());
	}
}

