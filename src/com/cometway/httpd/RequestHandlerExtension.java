package com.cometway.httpd;

import com.cometway.ak.*;
import com.cometway.util.StringTools;
import java.util.*;
import java.io.*;

/**
 * This class is used to pass requests to request agents that don't register with a ".agent"
 * in its service name. 
 */
public class RequestHandlerExtension extends WebServerExtension
{
	protected String[] agents;
	protected WebServer server;
	
	/**
	 * The 'services' property is a comma separated list of service names which should ba handed requests to handle
	 */
	public void initProps()
	{
		setDefault("service_name", "extension://.*");

		setDefault("services","");
	}

	public void start()
	{
		super.start();

		agents = StringTools.commaToArray(getString("services"));
	}

	public boolean handleRequest(HTTPAgentRequest request)
	{
		boolean responded = false;

		if(server==null) {
			server = (WebServer)getServiceImpl(getString("webserver_service_name"));
		}

		if(agents!=null && server!=null) {
			String path = HTMLStringTools.decode(request.getString("path"));
			for(int x=0;x<agents.length;x++) {
				if(path.equals(agents[x])) {
					RequestAgent agent = server.getRequestAgent(request.getString("host"),path);
					if(agent!=null) {
						agent.handleRequest(request);
						responded = true;
						break;
					}
					else {
						debug("Can't find agent for path="+path);
					}
				}
			}
		}

		return(responded);
	}

}
