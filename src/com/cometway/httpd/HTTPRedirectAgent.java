package com.cometway.httpd;

import com.cometway.ak.*;

import java.util.*;
import java.io.*;

import com.cometway.util.*;



/**
 * This Request Agent is used by the WebServer. The WebServer uses this agent
 * to determine whether a web request should be redirected and if so, the
 * request is given to this agent for it to handle. This agent requires a file
 * which lists the requests which to redirect. Requests with more than one
 * redirect (listed in the file) will be cycled. The file format is name/value
 * pairs delimited by newlines and lines starting with '#' are ignored.
 *
 * There are 2 kinds of name/value pairs used in the redirect config file. The
 * first is a direct mapping in the format NAME=VALUE. This mapping tells the 
 * redirect agent what PATH should be redirected to what PATH. The PATH can 
 * consist of a full URL or the full URI path. The following are examples of
 * valid direct mapped name/value pairs:
 * /page.html=/redirected_page.html
 * /directory/=http://www.myotherdomain.com/directory2/
 * http://www.mydomain.com/mydir/=http://web1.mydomain.com/mydir2/page.html
 * 
 * This, however, would not be a valid name/value pair:
 * http://www.domain.com/page.html=/page2.html
 *
 * A direct mapped name which contains a host must have a host in its value.
 * A direct mapping will not recurse subdirectories in a path and each path 
 * is regarded as literal. Direct mapped names can by mapped to multiple values
 * if the values are separated by commas. The redirect agent will cycle through
 * the different paths given as the value.
 *
 * The second kind of mapping maps only hosts to hosts, and passes the path along
 * for the mapped host. This is an example of a host mapping:
 * http://myhost.com>http://www.myhost.com
 *
 * No path information is included in the syntax of this mapping, only the host
 * names. Any path intended for myhost.com will be mapped to the host 
 * www.myhost.com. This mapping is especially useful when the webserver is 
 * multihomed. Multiple hosts can all by mapped to a single host. 
 *
 * Cycles in the redirect mappings are not checked, if the redirect mapping has cycles
 * there will be an infinite loop of redirects.
 */
public class HTTPRedirectAgent extends WebServerExtension implements Runnable
{
	private RedirectHash redirects;
	protected boolean stopRunning;
	protected Thread thread;
	protected Object syncObject = new Object();


	public void initProps()
	{
		setDefault("service_name","extension://.*");
		setDefault("redirect_file", "./http.redirect");
		setDefault("check_interval","300000");
		setDefault("initial_wait_time","60000");
		setDefault("check_hosts","false");
		setDefault("default_port","80");

		setDefault("webserver_service_name","none");
		setDefault("domains","none");
	}

	public void start()
	{
		readRedirectFile();

		try {
			thread = new Thread(this);
			thread.start();
		}
		catch(Exception e) {
			//			e.printStackTrace();
			error("Exception while starting thread",e);
		}

		super.start();
	}

	public void stop()
	{
		stopRunning = true;
		synchronized(syncObject) {
			syncObject.notifyAll();
		}

		redirects = null;
		thread = null;
	}

	public void readRedirectFile()
	{
		redirects = new RedirectHash();

		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(new File(getString("redirect_file"))));
			String line = in.readLine();
			boolean hasDefaultHost = false;
			while(line!=null) {
				if(!line.startsWith("#")) {
					int index = line.indexOf(">");
					if(index!=-1) {
						String lval = line.substring(0,index).trim();
						if(lval.length()>0) {
							if(lval.charAt(lval.length()-1)=='/') {
								lval = lval.substring(0,lval.length()-1);
							}
							redirects.put("*"+lval,line.substring(index+1));
						}
					}
					else {
						index = line.indexOf("=");
						if(index!=-1) {
							String lval = line.substring(0,index).trim();
							if(lval.length()>0) {
								redirects.put(lval,line.substring(index+1));
							}
						}
					}
				}
				line = in.readLine();
			}

			debug("Redirect Hash:\n"+redirects.toString());
		}
		catch(Exception e) {
			error("Cannot read HTTP redirect file: "+getString("redirect_file"),e);
			//			e.printStackTrace();
			//			error("",e);
		}

		try {
			in.close();
		}
		catch(Exception e) {;}
	}

	public boolean handleRequest(HTTPAgentRequest request)
	{
		OutputStream socketOut = request.getOutputStream();
		String path = request.getProps().getString("request");
		String host = request.getProps().getString("host");
		boolean rval = false;

		if(path.indexOf(" ")!=-1) {
			path = path.substring(path.indexOf(" ")+1);
			if(path.indexOf(" ")!=-1) {
				path = path.substring(0,path.indexOf(" "));
			}
			else {
				if(path.indexOf("\n")!=-1) {
					path = path.substring(0,path.indexOf("\n"));
				}
			}
			path = path.trim();
		}

		//		debug("PATH = "+path);

		try {
			String location = null;

			if(redirects.containsKey("*http://"+host)) {
				location = redirects.get("*http://"+host)+path;
			}
			else if(redirects.containsKey("*https://"+host)) {
				location = redirects.get("*https://"+host)+path;
			}
			else if(redirects.containsKey("http://"+host+path)) {
				location = redirects.get("http://"+host+path);
			}
			else if(redirects.containsKey("https://"+host+path)) {
				location = redirects.get("https://"+host+path);
			}
			else if(host.indexOf(":")==-1) {
				host = host+":"+getString("default_port");

				if(redirects.containsKey("*http://"+host)) {
					location = redirects.get("*http://"+host)+path;
				}
				else if(redirects.containsKey("*https://"+host)) {
					location = redirects.get("*https://"+host)+path;
				}
				else if(redirects.containsKey("http://"+host+path)) {
					location = redirects.get("http://"+host+path);
				}
				else if(redirects.containsKey("https://"+host+path)) {
					location = redirects.get("https://"+host+path);
				}
			}
			else if(redirects.containsKey(path)) {
				location = redirects.get(path);
			}

			if(location!=null) {
				debug("Redirecting to location: "+location);
			
				socketOut.write(WebServer.getHTMLByCode(WebServer.MOVED,null,"Connection: Close\nLocation: "+location+"\n").getBytes());
				((HTTPAgentRequest)request).returnVal = "302";
				socketOut.flush();
				rval = true;
			}
			else {
				debug("Found no redirect location for host/path: "+host+" "+path);
			}
		}
		catch(Exception e) {
			error("Unable to handle request",e);
		}

		return(rval);
	}


	public boolean isRedirected(String path, String host)
	{
		boolean rval = false;
		String redirect = null;

		if(redirects.containsKey("*http://"+host)) {
			rval = true;
		}
		else if(redirects.containsKey("*https://"+host)) {
			rval = true;
		}
		else if(redirects.containsKey("http://"+host+path)) {
			rval = true;
		}
		else if(redirects.containsKey("https://"+host+path)) {
			rval = true;
		}
		else if(host.indexOf(":")==-1) {
			host = host+":"+getString("default_port");
			if(redirects.containsKey("*http://"+host)) {
				rval = true;
			}
			else if(redirects.containsKey("*https://"+host)) {
				rval = true;
			}
			else if(redirects.containsKey("http://"+host+path)) {
				rval = true;
			}
			else if(redirects.containsKey("https://"+host+path)) {
				rval = true;
			}
		}
		else if(redirects.containsKey(path)) {
			rval = true;
		}

		return(rval);
	}


	public void run()
	{
		try {
			synchronized(syncObject) {
				syncObject.wait(getInteger("initial_wait_time"));
			}
			while(!stopRunning) {
				if(getBoolean("check_hosts")) {
					redirects.checkAll();
				}
				try {
					synchronized(syncObject) {
						syncObject.wait(getInteger("check_interval"));
					}
				}
				catch(Exception e) {;}
			}
		}
		catch(Exception e) {
			//			e.printStackTrace();
			error("Exception in run()",e);
		}

	}







	class RedirectHash
	{
		public Hashtable hash;
		public Hashtable status;
		
		public RedirectHash()
		{
			hash = new Hashtable();
			status = new Hashtable();
		}

 
		public void checkAll()
		{
			if(getBoolean("check_hosts")) {
				try {
					Enumeration keys = hash.keys();
					while(keys.hasMoreElements()) {
						Object o = hash.get(keys.nextElement());
						if(o instanceof Vector) {
							Vector v = (Vector)o;
							for(int x=0;x<v.size();x++) {
								check((String)v.elementAt(x));
							}
						}
						else {
							check((String)o);
						}
					}
				}
				catch(Exception e) {
					//				e.printStackTrace();
					error("Exception while pinging redirect host",e);
				}
			}
		}

		public void check(String redirect)
		{
			if(getBoolean("check_hosts")) {
				try {
					int index = redirect.indexOf("://");
					if(index!=-1) {
						redirect = redirect.substring(index+3);
						index = redirect.indexOf("/");
						if(index!=-1) {
							redirect = redirect.substring(0,index);
						}
						index = redirect.indexOf(":");
						int port = 80;
						if(index!=-1) {
							port = Integer.parseInt(redirect.substring(index+1));
							redirect = redirect.substring(0,index);
						}

						java.net.Socket s = null;
						try {
							s = new java.net.Socket(redirect,port);
							status.put(redirect+":"+port, "UP");
						}
						catch(Exception e) {
							status.put(redirect+":"+port, "DOWN");
						}
					
						try {
							s.close();
						}
						catch(Exception e) {;}
					}
				}
				catch(Exception e) {
					//				e.printStackTrace();
					error("Exception while pinging redirect host",e);
				}
			}
		}

		public boolean isUp(String redirect)
		{
			boolean rval = false;
  			try {
				if(!redirect.startsWith("*")) {
					int index = redirect.indexOf("://");
					if(index!=-1) {
						redirect = redirect.substring(index+3);
						index = redirect.indexOf("/");
						if(index!=-1) {
							redirect = redirect.substring(0,index);
						}
						index = redirect.indexOf(":");
						int port = 80;
						if(index!=-1) {
							port = Integer.parseInt(redirect.substring(index+1));
							redirect = redirect.substring(0,index);
						}

						if(status.get(redirect+":"+port).equals("UP")) {
							rval = true;
						}
					}
					else {
						rval = true;
					}
				}
				else {
					rval = true;
				}
			}
			catch(Exception e) {
				//				e.printStackTrace();
				error("Exception setting redirect host isUp()",e);
			}

			return(rval);
		}


		public synchronized void put(Object key, Object object)
		{
			if(hash.containsKey(key)) {
				Object o = hash.get(key);
				if(o instanceof String) {
					Vector list = new Vector();
					list.addElement(o);
					list.addElement(object);
					check((String)object);
					hash.put(key,list);
				}
				else {
					check((String)object);
					((Vector)o).addElement(object);
				}
			}
			else {
				check((String)object);
				hash.put(key,object);
			}
		}

		public synchronized String get(Object key)
		{
			String rval = null;
			Object o = hash.get(key);
			if(o!=null) {
				if(o instanceof Vector) {
					synchronized(o) {
						for(int x=0;x<((Vector)o).size();x++) {
							rval = (String)((Vector)o).elementAt(0);
							((Vector)o).removeElementAt(0);
							((Vector)o).addElement(rval);
							if(getBoolean("check_hosts") && !isUp(rval)) {
								rval = null;
							}
							else {
								break;
							}
						}
					}
				}
				else {
					rval = (String)o;
					if(getBoolean("check_hosts") && !isUp(rval)) {
						rval = null;
					}
				}
			}
			return(rval);
		}

		public int size()
		{
			return(hash.size());
		}

		public boolean containsKey(Object key)
		{
			return(hash.containsKey(key));
		}


		public String toString()
		{
			StringBuffer rval = new StringBuffer();
			
			Enumeration keys = hash.keys();
			while(keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				Object o = hash.get(key);
				rval.append("["+key+"]\n");
				if(o instanceof Vector) {
					for(int x=0;x<((Vector)o).size();x++) {
						rval.append("     => "+((Vector)o).elementAt(x)+"\n");
					}
				}
				else {
					rval.append("     => "+o+"\n");
				}
			}
			
			return(rval.toString());
		}
	}
					


}
