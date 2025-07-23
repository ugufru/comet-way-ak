
package com.cometway.ak;

import com.cometway.props.Props;
import com.cometway.util.SystemReporter;
import com.cometway.util.ReporterInterface;
import com.cometway.util.ClassFactoryException;
import com.cometway.util.ClassFactoryInterface;
import com.cometway.util.DefaultClassFactory;


/**
* This class is contains the <TT>public static void main()</TT> method for
* starting the Comet Way Agent Kernel from a command line or other Java application
* launching utility. It expects a set of command line parameters as defined
* in the description of the <TT>main</TT> method.
*/

public class AK
{
	/** This is the proper name of the Agent Kernel */
	public static final String PRODUCT_NAME = "Comet Way Agent Kernel";

	/** This is the version of the Agent Kernel */
	public static final String VERSION_STR = "3.0 Final 04-24-2008 - Patience.";

	/** This is the proper name and version of the Agent Kernel. */
	public static final String VERSION_INFO = PRODUCT_NAME + " " + VERSION_STR;

	/** The ID this class uses for reporting. */
	private static final String AK_ID = "AK";

	/** The classname to use as the Default Agent Kernel. */
	private static final String DEFAULT_AGENT_KERNEL = "com.cometway.ak.AgentKernel";

	/** The classname to use as the Default Reporter. */
	private static final String DEFAULT_REPORTER = "com.cometway.util.TimeStampedReporter";
//	private static final String DEFAULT_REPORTER = "com.cometway.util.SystemReporter";	// pre 2.4 public release.

	/** This is the class factory used to instantiate new agents. */
	private static ClassFactoryInterface classFactory;

	/** This is the instance of the default Reporter that is assigned to new agents. */
	private static ReporterInterface reporter;

	/**
	* This is the instance of the active Agent Kernel.
	* @deprecated Use getAgentKernel and setAgentKernel methods instead.
	*/
	@Deprecated
	public static AgentKernelInterface instance;

	/**
	* This is the instance of the active Service Manager.
	*/
	private static ServiceManagerInterface service_manager;


	/** There is no public constructor for this class. */

	protected AK()
	{
	}


	/** Returns the default ClassFactoryInterface instance used to create new class instances. */

	public static ClassFactoryInterface getDefaultClassFactory()
	{
		return (classFactory);
	}


	/** Sets the default ClassFactoryInterface instance used to create new class instances. */

	public static void setDefaultClassFactory(ClassFactoryInterface newFactory)
	{
		classFactory = newFactory;
	}


	/** Returns the default ReporterInterface instance that is assigned to new agents. */

	public static ReporterInterface getDefaultReporter()
	{
		return (reporter);
	}


	/** Sets the default ReporterInterface instance that is assigned to new agents. */

	public static void setDefaultReporter(ReporterInterface newReporter)
	{
		reporter = newReporter;
	}


	/** Returns the Agent Kernel instance referenced by AK static methods. */

	public static AgentKernelInterface getAgentKernel()
	{
		return (instance);
	}


	/** Sets the Agent Kernel instance referenced by AK static methods. */

	public static void setAgentKernel(AgentKernelInterface newAgentKernel)
	{
		instance = newAgentKernel;
	}


	/** Returns the default ServiceManagerInterface instance that is assigned to new agents. */

	public static ServiceManagerInterface getDefaultServiceManager()
	{
		return (service_manager);
	}


	/** Sets the default ServiceManagerInterface instance that is assigned to new agents. */

	public static void setDefaultServiceManager(ServiceManagerInterface newServiceManager)
	{
		service_manager = newServiceManager;
	}


	/**
	* Attempt to instantiate a class named ClassFactory which can be replaced by
	* an AK implementation with special class loading requirements (ie: J2ME).
	*/

	private static void initDefaultClassFactory(String[] args)
	{
		classFactory = new DefaultClassFactory();
	}



	/**
	* Initializes the default reporter assigned to new agent instances.
	*/

	private static void initDefaultReporter(String[] args)
	{
		String classname = getParam(args, "-reporter");

		if (classname == null)
		{
			if (reporter != null) return;

			classname = DEFAULT_REPORTER;
		}

		if (classname.equals("null") == false)
		{
			try
			{
				reporter = (ReporterInterface) classFactory.createInstance(classname);
			}
			catch (Exception e)
			{
				reporter = new SystemReporter();
			}
		}
	}



	// Create and initialize the AgentKernel

	private static void startAgentKernel(String[] args) throws com.cometway.util.ClassFactoryException
	{
		Props p = new Props();
		String s = getParam(args, "-agent_kernel");
	
		if (s != null)
		{
			p.setProperty("classname", s);
		}

		p.setDefault("classname", DEFAULT_AGENT_KERNEL);
		p.setProperty("agent_id", "000");
		p.setProperty("name", "AgentKernel");
		p.setProperty("version", VERSION_STR);
		p.setProperty("version_info", VERSION_INFO);
		p.setBoolean("hide_println", hasParam(args, "-hide_println"));
		p.setBoolean("hide_debug", hasParam(args, "-hide_debug"));
		p.setBoolean("hide_warning", hasParam(args, "-hide_warning"));

		String classname = p.getString("classname");
		instance = (AgentKernelInterface) classFactory.createInstance(classname);
		AgentInterface akAgent = (AgentInterface) instance;
		akAgent.setProps(p);

		AgentController controller = new AgentController(akAgent);
		controller.start();
	}


	private static void startStartupAgent(String[] args)
	{
		Props p = new Props();
		p.setProperty("agent_id", "001");
		p.setBoolean("hide_println", hasParam(args, "-hide_println"));
		p.setBoolean("hide_debug", hasParam(args, "-hide_debug"));
		p.setBoolean("hide_warning", hasParam(args, "-hide_warning"));
	
		String s = getParam(args, "-startup_agent");
	
		if (s != null)
		{
			p.setProperty("classname", s);
		}
	
		s = getParam(args, "-startup_dir");
	
		if (s != null)
		{
			p.setProperty("startup_dir", s);
		}
	
		s = getParam(args, "-startup_file");
	
		if (s != null)
		{
			p.setProperty("startup_file", s);
		}
	
		p.setDefault("classname", "com.cometway.xml.XMLStartupAgent");

		AgentControllerInterface startupAgent = instance.createAgent(p);
		startupAgent.start();
	}


	/**
	* Called with command line parameters to bootstrap the agent kernel.<P>
	* <TT>ak [-reporter [&lt;classname&gt;|null]] [-hide_println] [-hide_debug] [-hide_warning]
	* [-startup_agent &lt;classname&gt;] [-startup_dir &lt;dir&gt;] [&lt;.agent file&gt;|&lt;classname&gt;] ...</TT>
	* <UL>
	* <LI>-hide_println when present disables verbose activity output
	* <LI>-hide_debug when present disables debugging output
	* <LI>-hide_warning when present disables internal warning messages
	* <LI>-reporter specifies the classname of an alternate reporter agent (or null to disable)
	* <LI>-startup_agent specifies the classname of an alternate startup agent
	* <LI>-startup_dir specifies the directory from which to load agents as defined by .startup properties files
	* </UL>
	*/

	public static void main(String[] args)
	{
		initDefaultClassFactory(args);
		initDefaultReporter(args);

		if (reporter != null)
		{	
			reporter.println(AK_ID, VERSION_INFO);
		}

		try
		{
			startAgentKernel(args);
			startStartupAgent(args);

			for (int i = 0; i < args.length; i++)
			{
				if (args[i].startsWith("-hide"))
				{
					continue;
				}
				else if (args[i].startsWith("-"))
				{
					i += 1;
				}
				else
				{
					Props p = null;
					String name = args[i];

					if (name.endsWith(".agent"))
					{
						p = Props.loadProps(args[i]);
					}
					else
					{
						p = new Props();
						p.setProperty("classname", name);
					}

					if (p != null)
					{
						AgentControllerInterface agent = instance.createAgent(p);

						if (agent != null)
						{
							agent.start();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			reporter.error(AK_ID, "Could not start the Agent Kernel. Exiting.", e);
			System.exit(-1);
		}
	}


	/**     
	* returns a String value of the parameger 'sw' given all the parameter 'args'
	*/     
	
	public static String getParam(String[] args, String sw)
	{
		String  value = null;
	
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals(sw) && (args.length > (i + 1)))
			{ 
				i++;
				value = args[i];
			}     
		}
						
		return (value);  
	}                               
                 
                    
	/**      
	* returns true if 'sw' is a param within 'args', the list of parameters
	*/
	
	
	public static boolean hasParam(String[] args, String sw)
	{
		boolean hasIt = false;
	
		for (int i = 0; i < args.length; i++)   
		{
			if (args[i].equals(sw))
			{       
				hasIt = true;
	
				break;
			}
		}
				
		return (hasIt);
	}       
}

