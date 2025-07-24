
package com.cometway.httpd;


import com.cometway.ak.*;
import com.cometway.util.*;
import java.util.*;
import java.io.*;


/**
* This agent can be loaded to support traditional HTTP Put functionality,
* saving files received to the specified "html_directory". You must specify
* this agent's service name for the "put_agent" property in the webserver.
* This agent can be easily replaced by a different RequestAgent for custom
* handling of HTTP Put requests.
*/

public class HTTPPutAgent extends WebServerExtension
{

	public void initProps()
	{
		setDefault("html_directory","./");
		setDefault("make_directories","false");
		setDefault("service_name","extension://.*");
		setDefault("max_put_size","-1");
		setDefault("allow_overwriting","true");

		setDefault("webserver_service_name","none");
		setDefault("domains","none");
	}

	public void start()
	{
		super.start();
	}


	public File getFile(String uri)
	{
		File rval = null;

		rval = new File(getString("html_directory"),uri);

		// This is to prevent people from using ../.. to get past the root directory node
		File rootFile = new File(getString("html_directory"));
		try {
		    String putPath = rval.getCanonicalPath();
		    String rootPath = rootFile.getCanonicalPath();
		    if(!putPath.startsWith(rootPath)) {
			rval = null;
		    }
		    else {
			if(getBoolean("make_directories")) {
			    File tmp = new File(rval.getParent());
			    if(!tmp.exists()) {
				tmp.mkdirs();
			    }
			}
		    }
		}
		catch(Exception e) {
		    error("Cannot create canonical paths",e);
		}
		return(rval);
	}

	public boolean handleRequest(HTTPAgentRequest req)
	{
		boolean rval = false;
		String requestString = req.getProps().getString("request");
		//		System.out.println(requestString);
		if(requestString.startsWith("PUT ")) {
			File f = getFile(req.getProps().getString("path"));
			
			if(f!=null) {
				boolean success = false;
				int contentLength = -1;
				boolean expectContinue = false;
				if(requestString.toLowerCase().indexOf("expect: 100-")!=-1) {
					expectContinue = true;
				}					

				int index = requestString.toLowerCase().indexOf("content-length:");
				if(index!=-1) {
					index = index + 15;
					requestString = requestString.substring(index);
					index = requestString.indexOf("\n");
					if(index!=-1) {
						requestString = requestString.substring(0,index);
					}
					requestString = requestString.trim();
					
					contentLength = Integer.parseInt(requestString);
				}

				if(getInteger("max_put_size")!=-1 && contentLength!=-1 && getInteger("max_put_size")<contentLength) {
					error("Attempting to PUT a file larger than 'max_put_size' ("+getInteger("max_put_size"));
					if(expectContinue) {
						req.print(WebServer.getHTMLByCode(WebServer.EXPECTATION_FAILED));
					}
					else {
						req.println("HTTP/1.1 204 Error\r\n\r\n");
					}
					return(true);
				}

				if(f.exists() && !getBoolean("allow_overwriting")) {
					error("Cannot overwrite file: "+f);
					if(expectContinue) {
						req.print(WebServer.getHTMLByCode(WebServer.EXPECTATION_FAILED));
					}
					else {
						req.println("HTTP/1.1 204 Error\r\n\r\n");
					}
					return(true);
				}

				if(f.exists() && !f.canWrite()) {
					error("Writing to file: "+f+" is not allowed");
					if(expectContinue) {
						req.print(WebServer.getHTMLByCode(WebServer.EXPECTATION_FAILED));
					}
					else {
						req.println("HTTP/1.1 204 Error\r\n\r\n");
					}
					return(true);
				}

				if(expectContinue) {
					req.print(WebServer.getHTMLByCode(WebServer.CONTINUE));
				}

				if(contentLength==-1) {
					FileOutputStream out = null;
					try {
						out = new FileOutputStream(f);
						int i = req.getClientInputStream().read();
						while(i!=-1) {
							out.write(i);
							i = req.getClientInputStream().read();
						}
						out.flush();
						success = true;
					}
					catch(Exception e) {
						error("Error putting file on local system",e);
					}
					
					try {
						out.close();
					}
					catch(Exception e) {;}
				}
				else {
					FileOutputStream out = null;
					try {
						out = new FileOutputStream(f);
						for(int x=0;x<contentLength;x++) {
							out.write(req.getClientInputStream().read());
						}
						out.flush();
						success = true;
					}
					catch(Exception e) {
						error("Error putting file on local system",e);
					}
					
					try {
						out.close();
					}
					catch(Exception e) {;}
					
				}
				
				if(success) {
					req.println("HTTP/1.1 201 File Created\r\n\r\n");
				}
				else {
					req.println("HTTP/1.1 204 Error\r\n\r\n");
				}
			}
			else {
				req.println("HTTP/1.1 204 Error\r\n\r\n");
			}
			rval = true;
		}

		return(rval);
	}


}
