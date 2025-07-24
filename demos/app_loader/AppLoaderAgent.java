

import java.lang.reflect.*;
import com.cometway.ak.*;
import com.cometway.util.*;

/**
* This agent instantiates the class specified by the <TT>app_classname</TT> property
* and calls its <TT>public static void main()</TT> method. Useful as an example of 
* bootstrapping a java application that isn't an agent, such as the included AppLoaderDemo.
*/

public class AppLoaderAgent extends Agent
{
	public void initProps()
	{
		setDefault("app_classname", "AppLoaderDemo");
	}


	public void start()
	{
		String  classname = getString("app_classname");

		if (classname.length() > 0)
		{
			println("Loading application" + classname);

			try
			{
				Class   pc[] = {};
				Class   clas = Class.forName(classname);
				Method  method = clas.getMethod("main", pc);

				println("Invoking " + method.toString());
				method.invoke(null, null);
			}
			catch (Exception e)
			{
				error("Could not launch " + classname);
			}
		}
	}


}

