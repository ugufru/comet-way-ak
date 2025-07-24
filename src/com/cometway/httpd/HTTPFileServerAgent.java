


package com.cometway.httpd;

import com.cometway.ak.*;
import java.util.*;
import java.io.*;
import com.cometway.util.*;
import com.cometway.net.*;
import com.cometway.io.RegExpFilenameFilter;

import java.net.SocketException;

/**
* This agent is used by the WebServer to handle requests for HTML files and
* other static documents from the file system.
*/

public class HTTPFileServerAgent extends WebServerExtension
{
	protected String html_directory;

	/** 
	*Initializes this agent's properties by providing default
	* values for each of the following missing properties:
	* "service_name" is used to register this agent with the Service Manager (default: HTTPFileServerAgent),
	* "html_directory" points to the root directory where HTML files are served (default: ./),
	* "default_index" is the default file served when no filename is specified, can be more than one index (default: index.html),
	* "allow_directory_lists" when enabled, allows this agent to display links to files in a directory (default: yes)
	* "domains" used for registering extension, can be more than one domain (default: default)
	*/

	public void initProps()
	{
		setDefault("service_name", "extension://.*");
		setDefault("webserver_service_name","none");
		setDefault("domains","none");

		setDefault("html_directory","./");
		setDefault("default_index","index.html");

		setDefault("allow_directory_lists","yes");
		// This is the list of files that shouldn't appear in directory listings, can use '*' to denote a wildcard
		setDefault("dont_index_list","^\\.$,~\\.\\.$,^#[A-Za-z0-9\\.]*#$,^[A-Za-z0-9\\.]*~$");
		// This alpha sorts the directory listings if set to true
		setDefault("alpha_sort_index","true");

		setDefault("mime_typer","mime_typer");
		setDefault("socket_exception_warnings","true");
	}
	
	/**
	* Registers Server and sets up directories, url's, etc.
	*/
	
	public void start()
	{
		html_directory = getString("html_directory");

		try {
			if(!hasProperty("service_url")) {
				setProperty("service_url","http://"+java.net.InetAddress.getLocalHost().getHostName()+"/");
			}
			else {
				String url = getString("service_url");
				if(url.charAt(url.length()-1) != '/') {
					url = url+"/";
					setProperty("service_url",url);
				}
			}
		}
		catch(Exception e) {
			//			e.printStackTrace();
			error("Exception determining local hostname",e);
		}

		super.start();
	}

	/**
	* Prints redirect information for a site to HTML output.  Takes
	* AgentRequest and String path as input.
	*/
	protected void printRedirect(HTTPAgentRequest request, String path, boolean keepAlive)
	{
		String server = getString("service_url");
		String keepAliveString = null;
		if(server.charAt(server.length()-1)=='/') {
			server = server.substring(0,server.length()-1);
		}

		if(keepAlive) {
			if(request.hasProperty(ConnectionKMethod.KEEP_ALIVE_FIELD)) {
				keepAliveString = request.getString(ConnectionKMethod.KEEP_ALIVE_FIELD);
			}
			request.setProperty(ConnectionKMethod.KEEP_ALIVE,"true");
		}
		request.print(WebServer.getHTMLByCode(WebServer.MOVED_PERMANENTLY,keepAliveString,"Location: "+server+path+"\n"));
	}



	/**
	 * Checks if the request given asks for a keep-alive
	 */
	public boolean isKeepAlive(AgentRequest request)
	{
		//		if(request.hasProperty(ConnectionKMethod.KEEP_ALIVE_FIELD)) {
		//			String httpRequest = request.getString("request").toLowerCase();
		//			int index = httpRequest.indexOf("connection");
		//			if(index!=-1) {
		//				index = httpRequest.indexOf(":",index);
		//				if(index!=-1) {
		//					httpRequest = httpRequest.substring(index+1);
		//					index = httpRequest.indexOf("\n");
		//					if(index!=-1) {
		//						httpRequest = httpRequest.substring(0,index).trim();
		//						return(httpRequest.equals("keep-alive"));
		//					}
		//				}
		//			}
		//		}
		//		return(false);
		return(request.hasProperty(ConnectionKMethod.KEEP_ALIVE_FIELD));
	}


	/**
	* Writes the entire http response header.
	*/ 
	 
	public boolean handleRequest(HTTPAgentRequest request)
	{
		boolean responded = false;
		OutputStream socketOut = request.getOutputStream();
		String path = HTMLStringTools.decode(request.getProps().getString("path"));
		boolean keepAlive = isKeepAlive(request);

		try {
			// Only accept GET or HEAD requests
			if(request.getString("request").startsWith("GET ") || request.getString("request").startsWith("HEAD ")) {
				// This is a file. fetch the file from the "html_directory"
				if(!hasProperty("html_directory")) {
					request.returnVal = ""+WebServer.FORBIDDEN;
					socketOut.write(WebServer.getHTMLByCode(WebServer.FORBIDDEN).getBytes());
					socketOut.flush();
					responded = true;
				}
				else {
					String fullpath = html_directory;
					
					if(path.startsWith("/")) {
						path = path.substring(1);
					}
					if(fullpath.charAt(fullpath.length()-1)!='/') {
						fullpath = fullpath+"/";
					}
					File file = new File(fullpath+path);

					if(file.isDirectory()) {
						if(path.length()==0 || (path.charAt(path.length()-1) == '/')) {
							String defaultIndex = null;
							String[] indeces = StringTools.commaToArray(getString("default_index"));
							File dir = file;
							for(int x=0;x<indeces.length;x++) {
								file = new File(dir,indeces[x]);
								if(file.exists()) {
									defaultIndex = indeces[x];
									break;
								}
							}
							if(defaultIndex==null) {
								if(getString("allow_directory_lists").equals("yes")) {
									//								socketOut.write(("HTTP/1.1 200 Ok.\nConnection: close\n\n").getBytes());
									socketOut.write(("HTTP/1.1 200 Ok.\r\n").getBytes());
									String directoryList = generateDirectoryList(new File(fullpath+path),path);
									socketOut.write(("Content-Length: "+directoryList.length()+"\r\n").getBytes());
									socketOut.write(("Date: "+WebServer.dateFormat_RFC822.format(new Date())+"\r\n").getBytes());
									if(keepAlive) {
										if(request.hasProperty(ConnectionKMethod.KEEP_ALIVE_FIELD)) {
											socketOut.write((request.getString(ConnectionKMethod.KEEP_ALIVE_FIELD)+"\r\n").getBytes());
											request.setProperty(ConnectionKMethod.KEEP_ALIVE,"true");
										}
										socketOut.write(("Connection: Keep-Alive\r\n\r\n").getBytes());
									}
									else {
										socketOut.write(("Connection: Close\r\n\r\n").getBytes());
									}
								
									if(!request.isHeadRequest()) {
										socketOut.write(directoryList.getBytes());
									}
									socketOut.flush();
									request.returnVal = "200";
								}
								else {
									request.returnVal = ""+WebServer.FORBIDDEN;
									if(keepAlive) {
										String response = WebServer.getHTMLByCode(WebServer.FORBIDDEN,
																								request.getString(ConnectionKMethod.KEEP_ALIVE_FIELD));
										request.setProperty(ConnectionKMethod.KEEP_ALIVE,"true");
										if(!request.isHeadRequest()) {
											socketOut.write(response.getBytes());
										}
										else {
											int tmpIndex = response.indexOf("\r\n\r\n");
											if(tmpIndex!=-1) {
												response = response.substring(0,tmpIndex+2);
											}
											socketOut.write(response.getBytes());
										}
									}
									else {
										String response = WebServer.getHTMLByCode(WebServer.FORBIDDEN);
										if(!request.isHeadRequest()) {
											socketOut.write(response.getBytes());
										}
										else {
											int tmpIndex = response.indexOf("\r\n\r\n");
											if(tmpIndex!=-1) {
												response = response.substring(0,tmpIndex+2);
											}
											socketOut.write(response.getBytes());
										}
									}
									socketOut.flush();
								}
								responded=true;
							}
							else {
								path = path+defaultIndex;
							}
						}
						else {
							if(path.startsWith("/")) {
								printRedirect(request,path+"/",keepAlive);
							}
							else {
								printRedirect(request,"/"+path+"/",keepAlive);
							}
							responded = true;
						}
					}
					if(!responded) {
						if(file.exists() && !file.canRead()) {
							request.returnVal = ""+WebServer.UNAUTHORIZED;
							if(keepAlive) {
								String response = WebServer.getHTMLByCode(WebServer.UNAUTHORIZED,
																						request.getString(ConnectionKMethod.KEEP_ALIVE_FIELD));
								request.setProperty(ConnectionKMethod.KEEP_ALIVE,"true");
								if(!request.isHeadRequest()) {
									socketOut.write(response.getBytes());
								}
								else {
									int tmpIndex = response.indexOf("\r\n\r\n");
									if(tmpIndex!=-1) {
										response = response.substring(0,tmpIndex+2);
									}
									socketOut.write(response.getBytes());
								}												 
							}
							else {
								String response = WebServer.getHTMLByCode(WebServer.UNAUTHORIZED);
								if(!request.isHeadRequest()) {
									socketOut.write(response.getBytes());
								}
								else {
									int tmpIndex = response.indexOf("\r\n\r\n");
									if(tmpIndex!=-1) {
										response = response.substring(0,tmpIndex+2);
									}
									socketOut.write(response.getBytes());
								}												 
							}
							socketOut.flush();
							responded = true;
						}
						else if(file.exists()) {
							Date modDate = request.getIfModifiedSince();
							if(modDate!=null && file.lastModified() <= modDate.getTime()) {
								socketOut.write(WebServer.getHTMLByCode(WebServer.NOT_MODIFIED,request.getString(ConnectionKMethod.KEEP_ALIVE_FIELD)).getBytes());
								request.setProperty(ConnectionKMethod.KEEP_ALIVE,"true");
								socketOut.flush();
								responded = true;
								request.returnVal = ""+WebServer.NOT_MODIFIED;
							}
							else {
								BufferedInputStream fis = null;
								try {
									fis = new BufferedInputStream(new FileInputStream(file));

									socketOut.write(("HTTP/1.1 200 Ok.\r\n").getBytes());						
									socketOut.write(("Date: "+WebServer.dateFormat_RFC822.format(new Date())+"\r\n").getBytes());
									socketOut.write(("Content-Length: "+file.length()+"\r\n").getBytes());
									socketOut.write(("Last-Modified: "+WebServer.dateFormat_RFC822.format(new Date(file.lastModified()))+"\r\n").getBytes());
									if(keepAlive) {
										if(request.hasProperty(ConnectionKMethod.KEEP_ALIVE_FIELD)) {
											socketOut.write((request.getString(ConnectionKMethod.KEEP_ALIVE_FIELD)+"\r\n").getBytes());
											request.setProperty(ConnectionKMethod.KEEP_ALIVE,"true");
										}
										socketOut.write(("Connection: Keep-Alive\r\n").getBytes());
									}
									else {
										socketOut.write(("Connection: Close\r\n").getBytes());
									}
									socketOut.write((getFileMimeType(fis,path)+"\r\n").getBytes());
									socketOut.flush();
									responded = true;
									request.returnVal = "200";
								
									if(!request.isHeadRequest()) {
										sendFile(fis,request);
									}
								}
								catch(SocketException se) {
									if(getBoolean("socket_exception_warnings")) {
										warning("Could not read file or send file data to client, URI= "+path,se);
									}
									else {
										error("Could not read file or send file data to client, URI= "+path,se);
									}
								}
								catch(Exception e) {
									error("Could not read file or send file data to client, URI= "+path,e);
								}
								finally {
									try {fis.close();} catch(Exception e) {;}
								}
							}
						}
					}
				}
			}
			else {
				responded = false;
			}
		}
		catch(Exception e) {
			error("An error occured while handling event. URI= "+path,e);
			if(!responded) {
				try {
					request.returnVal = "500";
					if(keepAlive) {
						String response = WebServer.getHTMLByCode(WebServer.SERVER_ERROR,
																				request.getString(ConnectionKMethod.KEEP_ALIVE_FIELD));
						if(!request.isHeadRequest()) {
							socketOut.write(response.getBytes());
						}
						else {
							int tmpIndex = response.indexOf("\r\n\r\n");
							if(tmpIndex!=-1) {
								response = response.substring(0,tmpIndex+2);
							}
							socketOut.write(response.getBytes());
						}												 
					}
					else {
						String response = WebServer.getHTMLByCode(WebServer.SERVER_ERROR);

						if(!request.isHeadRequest()) {
							socketOut.write(response.getBytes());
						}
						else {
							int tmpIndex = response.indexOf("\r\n\r\n");
							if(tmpIndex!=-1) {
								response = response.substring(0,tmpIndex+2);
							}
							socketOut.write(response.getBytes());
						}												 
					}
					//					socketOut.write(WebServer.getHTMLByCode(WebServer.SERVER_ERROR).getBytes());
					socketOut.flush();
					responded = true;
				}
				catch(Exception e1) {;}
			}
		}

		return(responded);
	}


	protected String getFileMimeType(InputStream fis, String path)
	{
		String rval = WebServer.getMimeType(path);

		// The WebServer's mime mappings overwrite the global ones, 
		if(!WebServer.hasMimeType(path)) {
			try {
				BinaryMimeTyperInterface typer = (BinaryMimeTyperInterface)ServiceManager.getService(getString("mime_typer"));
				rval = typer.getMimeType(fis);
				//				System.out.println("Got Type: "+rval);
				rval = "Content-Type: " + rval + "\r\n";
			}
			catch(Exception e) {;}
		}
		//		else {
			//			System.out.println("Default Type: "+rval);
		//		}

		return(rval);
	}


	protected void sendFile(InputStream fis, HTTPAgentRequest request) throws IOException
	{
		OutputStream socketOut = request.getOutputStream();
		byte[] fileData = new byte[1024];
		int bytesread = fis.read(fileData);
		while(bytesread>0) {
			socketOut.write(fileData,0,bytesread);
			socketOut.flush();
			bytesread = fis.read(fileData);
		}
	}
		


	/**
	* Generates a directory Listing.  Takes File and String path as input.
	*/
	
	public String generateDirectoryList(File file, String path)
	{
		StringBuffer rval = new StringBuffer();
		
		path = path.trim();
		if(path.length()==0) {
			path = "/";
		}
		else if(path.length()>1) {
			if(path.charAt(path.length()-1)=='/') {
				path = path.substring(0,path.length()-1);
			}
			if(path.charAt(0)!='/') {
				path = "/"+path;
			}
		}
		
		rval.append("<HTML><HEAD><TITLE>Index of: "+path+"</TITLE></HEAD><BODY>\n");
		rval.append("<H1>Index of: "+path+"</H1>\n");
		if(path.lastIndexOf("/")!=-1) {
			String parent = path.substring(0,path.lastIndexOf("/")+1);
			rval.append("<A HREF=\""+parent+"\"><I><B>Parent Directory</B></I></A><P>\n");
		}
		path = path.substring(1)+"/";
		rval.append("<TABLE><TR><TD WIDTH=\"50%\"><B>Name</B></TD><TD ALIGN=RIGHT WIDTH=\"25%\"><B>Last Modified</B></TD><TD ALIGN=RIGHT WIDTH=\"25%\"><B>Size</B></TD></TR>\n");

		if(file.isDirectory()) {
			String[] files = file.list();
			String[] doNotIndex = StringTools.commaToArray(getString("dont_index_list"));

			Vector doNotIndexVector = new Vector();
			for(int x=0;x<doNotIndex.length;x++) {
				String[] tmpList = file.list(new RegExpFilenameFilter(doNotIndex[x], false));
				for(int z=0;z<tmpList.length;z++) {
					if(!doNotIndexVector.contains(tmpList[z])) {
						doNotIndexVector.addElement(tmpList[z]);
					}
				}
			}

			Vector sortedFiles = new Vector();
			Vector unsortedFiles = new Vector();
			for(int x=0;x<files.length;x++) {
				boolean ignore = false;
				for(int z=0;z<doNotIndexVector.size();z++) {
					if(doNotIndexVector.elementAt(z).toString().equals(files[x])) {
						ignore = true;
						break;
					}
				}
				if(!ignore) {
					unsortedFiles.addElement(new Pair(files[x],files[x]));
				}
			}
			if(getBoolean("alpha_sort_index")) {
				sortedFiles = StringTools.pairSort(unsortedFiles);
			}
			for(int x=0;x<sortedFiles.size();x++) {
				String thisFileString = (String)((Pair)sortedFiles.elementAt(x)).second();
				File thisFile = new File(file,thisFileString);
				if(!path.startsWith("/")) {
					path = "/"+path;
				}
				if(thisFile.isFile()) {
					rval.append("<TR><TD WIDTH=\"50%\"><A HREF=\""+path+HTMLStringTools.encode(thisFileString)+"\">"+thisFileString+"</A></TD>\n");
					rval.append("<TD WIDTH=\"25%\" ALIGN=RIGHT>"+(new Date(thisFile.lastModified())).toString()+"</TD>\n");
					rval.append("<TD WIDTH=\"25%\" ALIGN=RIGHT>"+thisFile.length()+" bytes</TD></TR>\n");
				}
				else {
					rval.append("<TR><TD WIDTH=\"50%\"><A HREF=\""+path+HTMLStringTools.encode(thisFileString)+"\">"+thisFileString+"</A></TD>\n");
					rval.append("<TD WIDTH=\"25%\" ALIGN=RIGHT>"+(new Date(thisFile.lastModified())).toString()+"</TD>\n");
					rval.append("<TD WIDTH=\"25%\" ALIGN=CENTER>-</TD></TR>\n");
				}
			}
		}		

		rval.append("</TABLE></BODY></HTML>\n");


		return(rval.toString());
	}

}
