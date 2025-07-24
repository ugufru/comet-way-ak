package com.cometway.httpd;

import com.cometway.ak.AgentRequest;
import com.cometway.ak.RequestAgent;

import com.cometway.util.KMethod;

import com.cometway.props.IPropsContainer;
import com.cometway.props.PropsContainer;
import com.cometway.props.Props;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Calendar;
import java.util.Date;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PushbackInputStream;
import java.io.BufferedOutputStream;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ssl.SSLHandshakeException;

/**
 * This class contains all the code which is needed to deal with an incoming
 * HTTP connection. The WebServer creates this KMethod for each incoming request
 * it receives.
 *
 * NOTES CONCERNING Keep-Alive connections:
 *   There are 2 fields in the AgentRequest that pertains to keep-alive connections, KEEP_ALIVE and KEEP_ALIVE_FIELD
 *   The KEEP_ALIVE_FIELD property is set by the ConnectionKMethod, if this property is NOT set, a Keep-Alive connection cannot be used
 *   The KEEP_ALIVE property is set by the agent that handles the request, if this property is set, the ConnectionKMethod will keep the connection alive
 * 
 *   The agent that handles the request is responsible for the following:
 *     It must return a content-length header in the response, this usually means the agent must return its own HTTP response
 *     If a keep alive is allowed, and the client wants a keep-alive, the agent must return the <b>Connection: Keep-Alive</b> field as well as the contents of the KEEP_ALIVE_FIELD property
 *     If a keep alive is allowed, and the client wants a keep-alive, the agent must set the KEEP_ALIVE property in the AgentRequest
 *     If a keep alive is allowed, and the client wants a keep-alive, the agent must NOT close the Socket or the Socket streams
 */
public class ConnectionKMethod extends KMethod
{
	//	public static int counter;

	// This designates how many requests this one thread will serve
	int keepAliveLimit = 50;
	int keepAliveCount = 0;

	boolean keepAlive = true;
	//	int socket_linger_time;
	Socket socket;
	WebServer server;
	
	public static final String KEEP_ALIVE = "__CONNECTION_KEEP_ALIVE";
	public static final String KEEP_ALIVE_FIELD = "__KEEP_ALIVE_FIELD";

	public static final String OVERFLOW = "\t@\n\t@\n\t@\n\tOVERFLOW\n\t@\n\t@\n\t@\r\r\r";

	private String response = null;                // used to store the canned response for this request (if needed)
	private String returnVal = null;               // used to store the return value of this request
	private InetAddress cachedInetAddress = null;  // used to store the request's InetAddress object
	private boolean responded = false;             // used to store whether a response has been given for this request
	private boolean refused = false;               // used to store whether this request has been refused
	private boolean timedOut = false;              // used to store whether this request has timed out
	private boolean keepAliveTimedOut = false;     // if this is true, we don't report errors because the keep alive connection timed out normally
	private boolean overflowed = false;            // used to store whether this request has an overflow
	private StringBuffer httpRequest = null;       // used to store the complete unaltered request
	private String logString = null;               // used to store the string used for logging this request
	private String request = null;                 // used to store various chunks of the request
	private String requestVersion = null;          // used to store the request's HTTP version
	private String path = null;                    // used to store the logical path of the request
	private String hostField = null;               // used to store the host header in the request
	private String cgiParams = null;               // used for parsing various types of CGI parameters
	private String tmpParams = null;               // used for parsing various types of CGI parameters (to support both GET and POST params at the same time)
	private String requestContentType = null;      // content type of the incoming request
	private Props p = null;                        // This is the Props that is used to create the HTTPAgentRequest
	private Date requestDate = null;               // This is the date the request was received
	private HTTPAgentRequest agentRequest = null;



	/**
	 * This connection handler requires a reference to the WebServer which created
	 * it, the Socket connected to the HTTP client, and the amount of time to linger
	 * after the request has been handled before it closes the connection to the client
	 */
	public ConnectionKMethod(WebServer server, Socket sock)
	{
		this.server = server;
		socket = sock;
		//		socket_linger_time = linger_time;

		server.counter++;
		server.debug("++ Kmethods: "+server.counter);
	}

	/**
	 * Accounting stuff goes here
	 */
	protected void finalize() throws Throwable
	{
		if(socket!=null) {
			try {
				socket.close();
			}
			catch(Exception e) {;}
		}
		server.counter--;
		server.debug("-- KMethods: "+server.counter);
		server = null;
	}

	/**
	 * Returns the Keep-Alive HTTP response field as a String for the connection that this KMethod is handling
	 */
	public String getKeepAliveField()
	{
		return("Keep-Alive: timeout="+((int)(server.getInteger("socket_timeout")/1000))+", max="+(keepAliveLimit-keepAliveCount));
	}

	/**
	 * Attempts to read a single line of input up to a max length. If the max length is exceeded before a newline 
	 * or EOF is encountered, then the returned String will be prepended with the OVERFLOW String. This method is used
	 * when the input read is expected to be US-ASCII. This is used when reading the HTTP request and headers.
	 */
	protected String readLine(PushbackInputStream in, int maxLength) throws IOException
	{
		if(maxLength>0) {
			StringBuffer rval = new StringBuffer();
			boolean overflow = true;
			boolean eof = false;

			// We need to make a special case here in the event that there is an SSLHandshakeException because it 
			// doesn't get properly caught.
			try {
				for(int x=0;x<maxLength;x++) {
					int i = in.read();
					if(i==-1) {
						eof = true;
						break;
					}
					char c = (char)i;
					if(c=='\n') {
						overflow = false;
						break;
					}
					else {
						if(c!='\r') {
							rval.append(c);
						}
					}
				}
			}
			catch(SSLHandshakeException handshake) {
				return(null);
			}

			if(!overflow) {
				return(rval.toString());
			}
			else if(eof) {
				return(null);
			}
			else {
				return(OVERFLOW+rval.toString());
			}
		}
		else {
			return(socketReadLine(in));
		}
	}

	/**
	 * Attempts to read a single line of input, until either a newline or EOF is encountered. This method is used
	 * to read either binary or ISO-8859-1 encoded data. The String returned is guaranteed to be ISO-8859-1 encoded
	 * and is used when reading the HTTP POST request body.
	 */
	protected String socketReadLine(PushbackInputStream in) throws IOException
	{
		StringBuffer rval = new StringBuffer();
		byte[] buffer = new byte[1024];
		int bytesRead = in.read(buffer);
		boolean bailout = false;
		boolean bailoutLF = false;
		while(bytesRead>0) {
			for(int x=0;x<bytesRead;x++) {
				if(((char)buffer[x]) == '\r') {
					rval.append(new String(buffer,0,x,"ISO-8859-1"));
					bailoutLF = true;
				}
				else if(((char)buffer[x]) == '\n') {
					if(!bailoutLF) {
						rval.append(new String(buffer,0,x-1,"ISO-8859-1"));
					}
					bailout = true;
				}
				else if(bailout || bailoutLF) {
					in.unread(buffer,x,bytesRead-x);
					bailout = true;
					break;
				}
			}
			if(bailout) {
				break;
			}
			else {
				bytesRead = in.read(buffer);
			}
		}

		return(rval.toString());
	}


	/**
	 * Everything happens here. The HTTP request is read and parsed, and the KMethod creates an HTTPAgentRequest and
	 * attempts to hand it off to a RequestAgent. Otherwise an appropriate error is returned.
	 */ 
	public void execute()
	{
		PushbackInputStream socketIn = null;
		BufferedOutputStream socketOut = null;

		// used to measure the time it took to handle a request
		long latency = System.currentTimeMillis();


		// Handler State flags and variables
		response = "HTTP/1.1 500 Server Error.\r\nDate: "+WebServer.dateFormat_RFC822.format(new Date())+"\r\nConnection: close\r\n\r\n";
		logString = "";
		hostField = "";
		requestContentType = "application/x-www-form-urlencoded";
		httpRequest = new StringBuffer();

		server.debug("Socket: "+socket);

		// We'll set the request date here
		requestDate = new Date();

		// Set the initial timeout and init the streams
		try {
			socket.setSoTimeout(server.getInteger("socket_initial_timeout"));
			
			socketIn = new PushbackInputStream(socket.getInputStream());
			socketOut = new BufferedOutputStream(socket.getOutputStream());
		}
		catch(Exception e) {
			socketIn = null;
			socketOut = null;
			//			e.printStackTrace();
		}

		try {
			// attempt to cache the InetAddress to prevent continuously looking it up
			cachedInetAddress = socket.getInetAddress();
		}
		catch(Exception e) {;}
		
		// The main keep-alive loop
		while(keepAlive) {
			if(socketIn!=null && socketOut!=null) {
				// this needs to be reset each time around the keep-alive
				p = new Props();

				// read the first line of the request to determine the type, URI, and the version
				if(!readFirstLine(socketIn, socketOut)) {
					return;
				}

				// read the rest of the request headers and the body
				readHTTPRequestHeadersAndBody(socketIn, socketOut);

				try {
					// If this request hasn't timed out or already been responded to, then
					// we'll try to handle it. From here, we'll try to find a way to handle the request
					if(!responded && !timedOut) {

						// Compatibility check, we need the path to start with a "/"
						if(!path.startsWith("/")) {
							path = "/"+path;
						}

						// Determine the host the request is meant for. The "hostField" includes any port information
						// and the "host" should ONLY be the hostname and not port information.
						hostField = p.getString("http_headers:host");
						String host = hostField;
						if(hostField.length()>0) {
							int index1 = hostField.indexOf(":");
							if(index1!=-1) {
								host = hostField.substring(0,index1);
							}
						}							

						// Decode any URL encoding
						path = HTMLStringTools.decode(path);

						// Setup http stuff in the HTTPAgentRequest Props
						p.setProperty("request",httpRequest.toString());
						p.setProperty("request_type","HTTP");
						p.setProperty("request_date",requestDate);
						p.setProperty("path",path);
						p.setProperty("request_server_name",socket.getLocalAddress().getHostAddress());
						p.setProperty("request_server_port",server.getProps().getString("bind_port"));
						p.setProperty("host",hostField);
						if(server.getProps().getBoolean("use_inet_address_methods")) {
							p.setProperty("request_remote_host",cachedInetAddress.getHostName());
							p.setProperty("request_remote_addr",cachedInetAddress.getHostAddress());
							p.setProperty("request_id","HTTP:"+cachedInetAddress.getHostName());
						}
				
						// Check if the request wants to be keep-alive
						if(p.getString("http_headers:connection").toLowerCase().startsWith("keep-alive")) {
							// We have a limit to the number of requests a single connection will handle, check it here
							if(keepAliveLimit-keepAliveCount>0) {
								// This property is used by the HTTPAgentRequest for generating the keep-alive header.
								p.setProperty(KEEP_ALIVE_FIELD,getKeepAliveField());
							}
						}

						try {
							// Create the HTTPAgentRequest
							agentRequest = new HTTPAgentRequest(p,socketOut,System.out,socketIn);
							agentRequest.headRequest = request.equalsIgnoreCase("HEAD");

							// look through all relevant WebServerExtensions
							Vector extensions = server.getExtensions(host);
							for(int x=0;x<extensions.size();x++) {
								// Go through each of the extensions and hand it the HTTPAgentRequest
								WebServerExtension extension = (WebServerExtension)extensions.elementAt(x);
								if(extension.handleRequest(agentRequest)) {
									// WebServerExtensions will only return true if the request has been responded to
									// and there is no need to further handle the request.
									responded = true;
									try {
										if(agentRequest.bufferOutput) {
											agentRequest.close();
										}
										
										if(!p.hasProperty(KEEP_ALIVE)) {
											agentRequest.getOutputStream().close();
										}
									}
									catch(Exception e) {;}
									break;
								}
							}

							// None of the extensions were able to handle the request, let's hope an agent in the
							// ServiceManager can.
							if(!responded) {
								// In order for the ConnectionKMethod to automatically fetch a RequestAgent from the
								// ServiceManager, it MUST end with ".agent", otherwise a special Extension needs to
								// be used.
								if((path.length()>6) && (path.indexOf(".agent")==(path.length()-6))) {
									RequestAgent agent = server.getRequestAgent(host,path);
									
									if(agent!=null) {
										agent.handleRequest(agentRequest);
										responded = true;
										try {
											if(agentRequest.bufferOutput) {
												agentRequest.close();
											}
											
											if(!p.hasProperty(KEEP_ALIVE)) {
												agentRequest.getOutputStream().close();
											}
										}
										catch(Exception e) {;}								
									}
								}
							}

							// OK, so everything we tried failed, let's do a quick check to make sure the client isn't
							// doing something that we don't support, since we need to return the proper code if this is the case
							if(!responded) {
								// These are the 3 request types that should always be handled
								if(!request.equalsIgnoreCase("GET") && !request.equalsIgnoreCase("HEAD") && !request.equalsIgnoreCase("POST")) {
									socketOut.write(WebServer.getHTMLByCode(WebServer.METHOD_NOT_ALLOWED).getBytes());
									socketOut.flush();
									responded = true;
									socketOut.close();
									keepAlive = false;
									keepAliveTimedOut = false;
									agentRequest.returnVal = "405";
								}
								else {
									// If there is something else that made it so we couldn't handle the request, just return a 404.
									socketOut.write(WebServer.getHTMLByCode(WebServer.URL_NOT_FOUND).getBytes());
									socketOut.flush();
									responded = true;
									agentRequest.returnVal = "404";
								}									
							}

							if(responded) {
								returnVal = agentRequest.returnVal;
							}
						}
						catch(SocketException iioe) {
							timedOut = true;
						}
						catch(InterruptedIOException iioe) {
							timedOut = true;
						}
						catch(Exception e) {
							server.error("Error processing request:\n"+p.getString("request"),e);
						}
						if(agentRequest!=null) {
							if(agentRequest.bufferOutput) {
								agentRequest.close();
							}
						}

						// Check keepalive property here, if not there, then exit
						if(httpRequest.toString().toLowerCase().indexOf("connection: keep-alive")!=-1) {
							if(!p.hasProperty(KEEP_ALIVE)) {
								keepAlive = false;
							}
							else {
								// remove the property in case the next session is not keepalive
								p.removeProperty(KEEP_ALIVE);
								keepAlive = true;
								if(keepAliveLimit-keepAliveCount==0) {
									keepAlive = false;
								}
								keepAliveCount++;
							}
						}
					}
				}
				catch(ClassCastException cce) {
					try {
						server.error("Error handling connection, requested Agent is not a RequestAgent: "+cachedInetAddress+" "+request+" "+path);
						if(!responded) {
							try {
								socketOut.write(WebServer.getHTMLByCode(WebServer.SERVER_ERROR).getBytes());
								socketOut.flush();
								responded = true;
								returnVal = "500";
							}
							catch(Exception e1) {;}
						}
					}
					catch(Exception e2) {;}
				}
				catch(Exception e) {
					try {
						server.error("Error handling connection: "+cachedInetAddress+" "+request+" "+path,e);
						if(!responded) {
							try {
								socketOut.write(WebServer.getHTMLByCode(WebServer.SERVER_ERROR).getBytes());
								socketOut.flush();
								responded = true;
								returnVal = "500";
							}
							catch(Exception e1) {;}
						}
					}
					catch(Exception e2) {;}
				}

				// Log to the server logs
				if(agentRequest==null) {
					// We'll just put an empty request here
					if(p==null) {
						agentRequest = new HTTPAgentRequest(new Props(),null,null);
					}
					else {
						agentRequest = new HTTPAgentRequest(p,null,null);
					}
				}
				handleLog(agentRequest,latency,true);

				if(keepAlive) {
					// RESET all the variables and state
					responded = false;
					timedOut = false;
					refused = false;
					overflowed = false;
					httpRequest = new StringBuffer();
					logString = "";
					request = null;
					path = null;
					hostField = "";
					cgiParams = null;
					latency = System.currentTimeMillis();
				}
			}
		}


		// If we've gotten this far (outside of the keep-alive loop), if we haven't responded yet that means
		// we've either timed out or ultimately, for whatever reason, the request couldn't be handled.		
		if(!responded) {
			try {
				if(timedOut) {
					// If it's a timeout, we need to check if it's a keep-alive timeout, we only respond if
					// the timeout ISN'T keep-alive. The keep-alive timing out is one of the natural results 
					// of finishing a set of successful keep-alive requests.
					if(!keepAliveTimedOut) {
						socketOut.write(WebServer.getHTMLByCode(WebServer.REQUEST_TIMED_OUT).getBytes());
						socketOut.flush();
					}
				}
				else if(httpRequest.toString().trim().length()>0) {
					// If it's not a timeout, then let's see if a request was even sent. If something was
					// sent but there wasn't anything we can do to handle the request, we return a 404.
					socketOut.write(WebServer.getHTMLByCode(WebServer.URL_NOT_FOUND).getBytes());
					socketOut.flush();
				}
				else {
					// If neither of those cases, then the client hasn't sent a request, so we do nothing.
				}
			}
			catch(Exception e) {;}

			if(agentRequest==null) {
				// We'll just put an empty request here
				if(p==null) {
					agentRequest = new HTTPAgentRequest(new Props(),null,null);
				}
				else {
					agentRequest = new HTTPAgentRequest(p,null,null);
				}
			}
			handleLog(agentRequest,latency,false);
		}


		// Do final cleanup
		if(socketIn!=null) {
			try {
				socketIn.close();
			}
			catch(Exception e) {;}
			socketIn = null;
		}
		if(socketOut!=null) {
			try {
				socketOut.close();
			}
			catch(Exception e) {;}
			socketOut = null;
		}

		if(socket!=null) {
			try {
				socket.close();
			}
			catch(Exception e) {;}
			socket = null;
		}
	}


	/**
	 * This method parses a String of mime-type www-form-urlencoded parameters provided by the String parameter.
	 * The parameter names and values are stored in the Props Object. If more than one name=value pair exists for
	 * the same name, the stored Property is a Vector containing the multiple parameter values.
	 */
	public void parseFormEncodedParameters(String tmpParams)
	{
		if(tmpParams!=null) {
			// parse out the individual cgi params
			
			int i = tmpParams.indexOf("&");
			while(i!=-1) {
				String tmp = tmpParams.substring(0,i);
				tmpParams = tmpParams.substring(i+1);
				
				i = tmp.indexOf("=");
				if(i!=-1) {
					String property = HTMLStringTools.decode(tmp.substring(0,i));
					if(p.hasProperty(property)) {
						Object o = p.getProperty(property);
						if(o instanceof Vector) {
							((Vector)o).addElement(HTMLStringTools.decode(tmp.substring(i+1)));
						}
						else {
							Vector v = new Vector();
							v.addElement(o);
							v.addElement(HTMLStringTools.decode(tmp.substring(i+1)));
							p.setProperty(property,v);
						}
					}
					else {
						p.setProperty(property,HTMLStringTools.decode(tmp.substring(i+1)));
					}
				}
				else {
					p.setProperty(HTMLStringTools.decode(tmp)," ");
				}
				
				i = tmpParams.indexOf("&");
			}
			if(tmpParams.length()>0) {
				i = tmpParams.indexOf("=");
				
				if(i!=-1) {
					String property = HTMLStringTools.decode(tmpParams.substring(0,i));
					if(p.hasProperty(property)) {
						Object o = p.getProperty(property);
						if(o instanceof Vector) {
							((Vector)o).addElement(HTMLStringTools.decode(tmpParams.substring(i+1)));
						}
						else {
							Vector v = new Vector();
							v.addElement(o);
							v.addElement(HTMLStringTools.decode(tmpParams.substring(i+1)));
							p.setProperty(property,v);
						}
					}
					else {
						p.setProperty(HTMLStringTools.decode(tmpParams.substring(0,i)),HTMLStringTools.decode(tmpParams.substring(i+1)));
					}
				}
				else {
					p.setProperty(HTMLStringTools.decode(tmpParams)," ");
				}
			}
		}
	}


	/**
	 * This method reads multipart/form-data from the InputStream and parses it into the form-data parameters. The parsed parameters are stored in
	 * the Props "p". Properties that are not text are stored as byte[]. In the event that extra properties are in the form-data, 
	 * specifically, "filename", property is stored as a Hashtable with keys "filename", "filedata", and "content-type".
	 */
	public void parseMultipartMimeEncodedParameters(PushbackInputStream socketIn, int contentLength, String multipartBoundary) throws IOException
	{
		// Pad the boundary String
		multipartBoundary = "--"+multipartBoundary;
											
		// Multipart parsing code goes here
		Vector parts = new Vector();
			
		// This might be slow, but I can't think of any other way to read the
		// same data from a string in both char form and byte form 
		StringBuffer contentStr = new StringBuffer();
		byte[] content = new byte[contentLength];
		int bytesRead = socketIn.read(content);
		if(bytesRead==-1) {
			throw(new IOException("Client connection closed"));
		}
		if(bytesRead < contentLength) {
			byte[] tmpBuffer = new byte[2048];
			int tmpread = socketIn.read(tmpBuffer);
			System.arraycopy(tmpBuffer,0,content,bytesRead,tmpread);
			bytesRead = bytesRead + tmpread;
			while(bytesRead< contentLength) {
				tmpread = socketIn.read(tmpBuffer);
				System.arraycopy(tmpBuffer,0,content,bytesRead,tmpread);
				bytesRead = bytesRead + tmpread;
			}
		}
		contentStr.append(new String(content,"ISO-8859-1"));
		
		// After the entire content has been read, parse out the different parts iteratively.	
		try {
			String tmp = contentStr.toString();
			contentStr = null;    // this is to free up the buffer in case it happens to be VERY large
				
			// The multipart parts are separated by the boundaries
			int boundaryIndex1 = tmp.indexOf(multipartBoundary);
			int boundaryIndex2 = -1;
			if(boundaryIndex1!=-1) {
				boundaryIndex2 = tmp.indexOf(multipartBoundary,boundaryIndex1+1);
			}
			while(boundaryIndex2!=-1) {
				// Iterate, extract this multipart block, and parse it
				String multipartBlock = tmp.substring(boundaryIndex1+multipartBoundary.length(),boundaryIndex2);

				// Trim off the end of the block. This is where the form-data ends and depending on the content's
				// type, it may end with a \r\n or \n. Note that we can't use trim because we need to preserve the
				// actual data if it were to end with whitespace.
				if(multipartBlock.substring(multipartBlock.length()-2).equals("\r\n")) {
					multipartBlock = multipartBlock.substring(0,multipartBlock.length()-2);
				}
				else if(multipartBlock.substring(multipartBlock.length()-1).equals("\n")) {
					multipartBlock = multipartBlock.substring(0,multipartBlock.length()-1);
				}

				//	We only support content-disposition's that are "form-data", anything else we'll ignore.
				if(multipartBlock.indexOf("Content-Disposition: form-data;")!=-1) {
					// Parse out the parameter name
					int tmpIndex = multipartBlock.indexOf("; name=\"");
					int tmpIndex2 = multipartBlock.indexOf("\"",tmpIndex+8);
					String formDataName = multipartBlock.substring(tmpIndex+8,tmpIndex2);
					String filename = null;
					String blockContentType = null;

					// Check if there is a filename component to this parameter
					if(multipartBlock.substring(tmpIndex2+1).startsWith("; filename=\"")) {
						tmpIndex = multipartBlock.indexOf("\"",tmpIndex2+13);
						filename = multipartBlock.substring(tmpIndex2+13,tmpIndex);
						tmpIndex2 = multipartBlock.indexOf("Content-Type",tmpIndex);
						boolean binary = false;
						// Check the content type to see if it's some type of text
						if(tmpIndex2!=-1) {
							if(multipartBlock.substring(tmpIndex2).startsWith("Content-Type: text")) {
								tmpIndex = multipartBlock.indexOf("\n",tmpIndex2);
							}
							else {
								tmpIndex = multipartBlock.indexOf("\n",tmpIndex2);
								binary = true;
							}
							blockContentType = multipartBlock.substring(tmpIndex2,tmpIndex).trim();
						}

						// Advance to the beginning of the data
						if(tmpIndex<multipartBlock.length()) {
							tmpIndex++;
							while(Character.isWhitespace(multipartBlock.charAt(tmpIndex))) {
								tmpIndex++;
								if(tmpIndex>=multipartBlock.length()) {
									break;
								}
							}
						}
							
						// If the filename is blank, then there was nothing set in this field, so let's not store anything.
						// The assumption here is that the filename field is inserted by browsers using a file form input,
						// but if no file is included the filename field is blank. So if there's no filename, there shouldn't
						// be any data. This is to prevent blank file-data from getting inserted into the HTTPAgentRequest Props.
						if(filename.trim().length()>0) {
							// Whether text or binary, we create the Hashtable and fill it with the same 3 keys.
							if(!binary) {
								Hashtable values = new Hashtable();
								values.put("filename",filename);
								values.put("filedata",multipartBlock.substring(tmpIndex));
								if(blockContentType!=null) {
									values.put("content-type",blockContentType);
								}
								p.setProperty(formDataName,values);
							}
							else {
								Hashtable values = new Hashtable();
								values.put("filename",filename);
								byte[] filedata = new byte[multipartBlock.length()-tmpIndex];
								System.arraycopy(content,boundaryIndex1+multipartBoundary.length()+tmpIndex,filedata,0,filedata.length);
								System.out.println("************************* LENGTH="+filedata.length);
								values.put("filedata",filedata);
								if(blockContentType!=null) {
									values.put("content-type",blockContentType);
								}
								p.setProperty(formDataName,values);
							}
						}
					}
					else {
						// Without a filename component, we parse this like normal
						tmpIndex2 = multipartBlock.indexOf("\n",tmpIndex2) + 1;

						// The assumption is the next form-data header is either "Content-Type" or the end of the header
						if(multipartBlock.substring(tmpIndex2).trim().startsWith("Content-Type:")) {
							// If the content type is some kind of text, then we just parse it and store the property
							if(multipartBlock.substring(tmpIndex2).trim().startsWith("Content-Type: text")) {
								tmpIndex2 = multipartBlock.indexOf("\n",tmpIndex2+13);
								if(tmpIndex2<multipartBlock.length()) {
									// Advance to the beginning of the data
									while(Character.isWhitespace(multipartBlock.charAt(tmpIndex2))) {
										tmpIndex2++;
										if(tmpIndex2>=multipartBlock.length()) {
											break;
										}
									}
								}
								p.setProperty(formDataName,multipartBlock.substring(tmpIndex2));
							}
							else {
								// Not a text block, parse it and store the property as a byte[]
								tmpIndex2 = multipartBlock.indexOf("\n",tmpIndex2);
								if(tmpIndex2<multipartBlock.length()) {
									// Advance to the beginning of the data
									while(Character.isWhitespace(multipartBlock.charAt(tmpIndex2))) {
										tmpIndex2++;
										if(tmpIndex2>=multipartBlock.length()) {
											break;
										}
									}
								}
								byte[] tmpData = new byte[multipartBlock.length()-tmpIndex2];
								System.arraycopy(content,boundaryIndex1+multipartBoundary.length()+tmpIndex,tmpData,0,tmpData.length);
								p.setProperty(formDataName,tmpData);
							}
						}
						else {
							// No content type so we assume text/plain.
							if(tmpIndex2<multipartBlock.length()) {
								// Advance until the beginning of the data
								while(Character.isWhitespace(multipartBlock.charAt(tmpIndex2))) {
									tmpIndex2++;
									if(tmpIndex2>=multipartBlock.length()) {
										break;
									}
								}
							}
							p.setProperty(formDataName,multipartBlock.substring(tmpIndex2));
						}
					}
				}

				// seek to the next boundary
				boundaryIndex1 = boundaryIndex2;
				boundaryIndex2 = tmp.indexOf(multipartBoundary,boundaryIndex1+1);
			}
		}
		catch(Exception e) {
			server.error("Could not read multipart MIME POST data: "+cachedInetAddress+" "+request+" "+path,e);
		}
	}


	/**
	 * This method is used to read and partially process the first line of the HTTP request. This is a special case because of the number of things
	 * that can be handled simply by the first line, which tells us the request type (GET/POST/HEAD) the request's URI and the version.
	 * The URI itself contains the majority of all information inside most HTTP requests so this needs to be handled differently than
	 * the rest of the request. Overflows (specific parts of the request being longer than WebServer settings), timeouts, and specific
	 * request types and/or URI's can also be refused; which are all handled in this method. If any of these events occur, the sockets
	 * will automatically be closed and the proper state properties set. A return value of true indicates that processing of this request
	 * should continue, otherwise the ConnectionKMethod can instantly return and exit.
	 */
	protected boolean readFirstLine(PushbackInputStream socketIn, BufferedOutputStream socketOut)
	{
		try {
			String line = readLine(socketIn, server.getInteger("max_uri_length"));
			
			if(line==null) {
				// client's not there anymore, we simply exit, there's no data to log
				try {
					socketOut.close();
				}
				catch(Exception e) {;}
				try {
					socketIn.close();
				}
				catch(Exception e) {;}
				return(false);
			}
			else if(line.startsWith(OVERFLOW)) {
				// there has been a URI length overflow, return a REQUEST_URI_TOO_LONG response
				try {
					socketOut.write(WebServer.getHTMLByCode(WebServer.REQUEST_URI_TOO_LONG).getBytes());
					socketOut.flush();
				}
				catch(Exception e) {;}
				returnVal = "414";
				responded = true;
				overflowed = true;
				server.debug("Request too long: "+line.substring(13));
			}
			
			// Check if this connection is keep-alive and a previous request has already been responded to
			// thus we need to block until the next request (and possibly some whitespace) is sent to us.
			// Anything read indicates that we've read the first line.
			if(keepAliveCount>0) {
				while(line!=null && line.trim().length()==0) {
					line = readLine(socketIn, server.getInteger("max_uri_length"));
				}
				if(line==null) {
					// The client left us at some point while we were blocking, waiting for the next request
					throw(new InterruptedIOException());
				}
			
				// Likewise, we perform an additional overflow check here, but only if we didn't already read an overflow
				if(!overflowed && line.startsWith(OVERFLOW)) {
					try {
						socketOut.write(WebServer.getHTMLByCode(WebServer.REQUEST_URI_TOO_LONG).getBytes());
						socketOut.flush();
					}
					catch(Exception e) {;}
					returnVal = "414";
					responded = true;
					overflowed = true;
					server.debug("Request too long: "+line.substring(13));
				}
			}

			// Parse the request version
			if(line.lastIndexOf(" ")!=-1) {
				String version = line.substring(line.lastIndexOf(" ")).trim();
				if(version.startsWith("HTTP/")) {
					requestVersion = version;
				}
			}
			
			// If the WebServer is set to strict HTTP version, we must only accept requests that are HTTP 1.1,
			// we do that check here. If the request's version isn't 1.1, we refuse the request.
			if(!responded && server.getBoolean("strict_http_version")) {
				try {
					if(!requestVersion.equals("HTTP/1.1")) {
						refused = true;
					}
				}
				catch(IndexOutOfBoundsException e) {
					// Malformed request, refuse it by default
					refused = true;
				}
				if(refused) {
					// respond with a VERSION NOT SUPPORTED response
					try {
						socketOut.write(WebServer.getHTMLByCode(WebServer.VERSION_NOT_SUPPORTED).getBytes());
						socketOut.flush();
					}
					catch(Exception e) {;}
					responded = true;
					returnVal = "505";
				}
			}
			
			if(!overflowed) {
				// Start the log_string with the first line of the request
				logString = line;
			
				httpRequest.append(line);
				httpRequest.append("\n");
				
				// Process the first line of the request
				int index = line.indexOf(" ");
				request = line.substring(0,index);
				path = "/";
				
				if(line.indexOf(" ",index+1) > 0) {
					path = line.substring(index+1,line.indexOf(" ",index+1));
				}
				else {
					path = line.substring(index+1);
				}
				
				// Now that we've read the first line of input, we need to change the initial socket timeout to the real socket timeout
				socket.setSoTimeout(server.getInteger("socket_timeout"));
			}
			else {
				// If there was an overflow, let's show at least some of the request in the logs. For now, just 100 chars of the request.
				logString = line.substring(OVERFLOW.length(),OVERFLOW.length()+100);
			}
		}
		catch(IndexOutOfBoundsException ioob) {
			if(keepAlive) {
				keepAliveTimedOut = true;
			}
			timedOut = true;
		}
		catch(SocketException se) {
			if(keepAlive) {
				keepAliveTimedOut = true;
			}
			timedOut = true;
		}
		catch(InterruptedIOException iioe) {
			if(keepAlive) {
				keepAliveTimedOut = true;
			}
			timedOut = true;
		}
		catch(Exception e) {
			server.error("Error handling the request URI",e);
		}

		// Clean up the sockets. We don't want to close sockets for a time out because technically, we need to respond to them
		if(overflowed || refused) {
			try {
				socketOut.close();
			}
			catch(Exception e) {;}
			try {
				socketIn.close();
			}
			catch(Exception e) {;}
			try {
				socket.close();
			}
			catch(Exception e) {;}
		}
		
		// This flag will be set to true at the end, after the request has been handled. Keep-alive is always off if an error occurs.
		keepAlive = false; 

		// If we've gotten this far, we keep going
		return(true);
	}


	/**
	 * This method is responsible for reading and processing the remainder of the HTTP request headers and the body (if there is one).
	 * The request headers are parsed into a Props that is stored as "http_headers" and the property names are all lowercase.
	 * Supported content-types for the request body are "application/x-www-form-urlencoded" and "multipart/form-data", an appropriate 
	 * error response will be sent otherwise. If any timeouts or overflows occur when reading the request headers or body, an error
	 * response will be sent. After an error response, the streams and socket will be closed and the proper state properties set.
	 */
	protected void readHTTPRequestHeadersAndBody(PushbackInputStream socketIn, BufferedOutputStream socketOut)
	{
		// Props where headers are stored
		Props httpHeaders = new Props();

		try {
			// If there are CGI parameters, parse them out
			if(!responded && !timedOut) {
				if(path.indexOf("?")!=-1) {
					// We must parse GET style CGI parameters regardless of the type of request.
					tmpParams = path.substring(path.indexOf("?")+1);
					path = path.substring(0,path.indexOf("?"));
					
					parseFormEncodedParameters(tmpParams);

					cgiParams = tmpParams;
				}
				else {
					cgiParams = "";
				}

				// Now let's parse the rest of the headers.
				overflowed = parseHTTPRequestHeaders(socketIn, httpHeaders, httpRequest);
				p.setProperty("http_headers",httpHeaders.getPropsContainer());

				if(request.equalsIgnoreCase("POST")) {
					try {
						int contentLength = -1;
						String multipartBoundary = null;
						boolean expectingContinue = false;

						// Grab the relevant headers needed for parsing the POST request body
						expectingContinue = httpHeaders.getString("expect").startsWith("100");

						if(httpHeaders.hasProperty("content-length")) {
							contentLength = httpHeaders.getInteger("content-length");
						}

						if(httpHeaders.hasProperty("content-type")) {
							requestContentType = httpHeaders.getString("content-type");
							if(requestContentType.toLowerCase().startsWith("multipart/form-data")) {
								int index = requestContentType.indexOf("boundary=");
								if(index!=-1) {
									multipartBoundary = requestContentType.substring(index+9).trim();
								}
							}
						}

						if(expectingContinue) {
							// According to RFC 2616, we cannot accept an Expect: 100 unless the protocol version is 1.1
							// so we need to check the version.
							if(!requestVersion.equals("HTTP/1.1")) {
								try {
									socketOut.write(WebServer.getHTMLByCode(WebServer.EXPECTATION_FAILED).getBytes());
									socketOut.flush();
								}
								catch(Exception e) {;}
								returnVal = "417";
								responded = true;
								server.debug("Partial Request not supported: "+httpRequest);
							}
							else {
								// Check if the content length is longer than we're willing to deal with
								if(contentLength>server.getInteger("max_multipart_length")) {
									try {
										socketOut.write(WebServer.getHTMLByCode(WebServer.EXPECTATION_FAILED).getBytes());
										socketOut.flush();
									}
									catch(Exception e) {;}
									returnVal = "417";
									responded = true;
									server.debug("POST Body too large (exceeds "+server.getInteger("max_multipart_length")+"): "+httpRequest);
								}
								else {
									// Issue the continue
									socketOut.write(WebServer.getHTMLByCode(WebServer.CONTINUE).getBytes());
									socketOut.flush();
								}
							}
						}
						else {
							// According to RFC 2616, if the protocol version is 1.1, we *may* need to return a 100 Continue
							// in a POST request if the data isn't immediately available.
							if(requestVersion.equals("HTTP/1.1")) {
								// We'll only do this if a content-length is specified.
								if(contentLength>0 && socketIn.available()==0) {
									// Issue the continue
									socketOut.write(WebServer.getHTMLByCode(WebServer.CONTINUE).getBytes());
									socketOut.flush();
								}
							}
						}

						if(!overflowed && !responded) {
							String tmpPostParams = "";

							// We handle POST requests differently depending on if there is a content length
							if(contentLength==-1) {
								if(requestContentType.startsWith("application/x-www-form-urlencoded")) {
									// There's no content length, so we assume POST style parameters that's only 1 line then parse the parameters
									tmpPostParams = socketReadLine(socketIn);
									httpRequest.append(tmpPostParams);
									httpRequest.append("\n");
									parseFormEncodedParameters(tmpPostParams);

									// Combine any URI params with POST body params
									if(cgiParams.trim().length()>0) {
										cgiParams = cgiParams + "&" + tmpPostParams;
									}
									else {
										cgiParams = tmpPostParams;
									}
								}
								else {
									// We won't deal with non-form encoded content types if there is no content length
									// so we respond with a 411 LENGTH REQUIRED response.
									try {
										socketOut.write(WebServer.getHTMLByCode(WebServer.LENGTH_REQUIRED).getBytes());
										socketOut.flush();
									}
									catch(Exception e) {;}
									returnVal = "411";
									responded = true;
									server.debug("Non-form encoded request content type missing a Content-Length: "+httpRequest);
								}
							}
							else {
								if(multipartBoundary==null) {
									// This is not a multipart-mime content type
									if(requestContentType.startsWith("application/x-www-form-urlencoded")) {
										// This is form encoded, so read all the content then parse it.
										byte[] buffer = new byte[contentLength];
										int bytesRead = socketIn.read(buffer);
										if(bytesRead < contentLength) {
											for(int x=bytesRead;x<contentLength;x++) {
												buffer[x] = (byte)socketIn.read();
											}
										}
										tmpPostParams = new String(buffer,"ISO-8859-1");
										parseFormEncodedParameters(tmpPostParams);
											
										httpRequest.append("\n");							
										httpRequest.append(tmpPostParams);
										httpRequest.append("\n");	

										// Combine any URI params with POST body params
										if(cgiParams.trim().length()>0) {
											cgiParams = cgiParams + "&" + tmpPostParams;
										}
										else {
											cgiParams = tmpPostParams;
										}
									}
									else if(requestContentType.startsWith("application/octet-stream")) {
										byte[] buffer = new byte[contentLength];
										int bytesRead = socketIn.read(buffer);
										if(bytesRead < contentLength) {
											for(int x=bytesRead;x<contentLength;x++) {
												buffer[x] = (byte)socketIn.read();
											}
										}

										httpRequest.append("\n");							
										httpRequest.append(new String(buffer,"ISO-8859-1"));
										httpRequest.append("\n");	
										
										p.setProperty("octet-stream_request_body",buffer);
									}
									else {
										// We won't support any other content types (like gzip). In the future we may.
										try {
											socketOut.write(WebServer.getHTMLByCode(WebServer.UNSUPPORTED_MEDIA_TYPE).getBytes());
											socketOut.flush();
										}
										catch(Exception e) {;}
										returnVal = "415";
										responded = true;
										server.debug("Unsupported request content-type: "+httpRequest);
									}
								}
								else {
									// With a multipart boundary and content length, we can parse the parameters, however,
									// these parameters won't be merged with any GET parameters
									parseMultipartMimeEncodedParameters(socketIn,contentLength,multipartBoundary);
								}
							}
					

							// Combine any CGI parameters read in the POST request body with the parameters parsed from the URI (if any).
							// This is used in the logging process so text parameters can get logged.
							if(requestContentType.startsWith("application/x-www-form-urlencoded")) {
								if(tmpPostParams.length()>0) {
									if(tmpParams!=null && tmpParams.length()>0) {
										tmpParams = tmpParams+"&";
										byte[] b1 = tmpParams.getBytes("ISO-8859-1");
										byte[] b2 = tmpPostParams.getBytes("ISO-8859-1");
										byte[] tmpBuffer = new byte[b1.length+b2.length];
										System.arraycopy(b1,0,tmpBuffer,0,b1.length);
										System.arraycopy(b2,0,tmpBuffer,b1.length,b2.length);
										tmpParams = new String(tmpBuffer,"ISO-8859-1");
									}
									else {
										tmpParams = tmpPostParams;
									}
								}
							}
						}
					}
					catch(SocketException iioe) {
						timedOut = true;
					}
					catch(InterruptedIOException iioe) {
						timedOut = true;
					}
					catch(Exception e) {
						server.error("Could not read POST data: "+cachedInetAddress+" "+request+" "+path,e);
					}
				}
				else {
					// Not a POST request so we have no body to parse, don't need to do anything
				}
			}

			if(!responded && overflowed) {
				// Write the overflow response.
				try {
					socketOut.write(WebServer.getHTMLByCode(WebServer.REQUEST_ENTITY_TOO_LARGE).getBytes());
					socketOut.flush();
				}
				catch(Exception e) {;}
				returnVal = "413";
				responded = true;
				server.debug("Request too long: "+httpRequest);
			}
		}
		catch(SocketException iioe) {
			timedOut = true;
		}
		catch(InterruptedIOException iioe) {
			timedOut = true;
		}
		catch(Exception e) {
			server.error("Error while reading request fields and/or request body.",e);
		}

		// Clean up after ourselves
		if(responded) {
			try {
				socketOut.close();
			}
			catch(Exception e) {;}
			try {
				socketIn.close();
			}
			catch(Exception e) {;}
			try {
				socket.close();
			}
			catch(Exception e) {;}
		}
	}

	/**
	 * Removes all leading whitespace from the String. If the String has only whitespace, returns an empty String
	 */
	protected String removeLeadingWhitespace(String in)
	{
		int index = 0;
		while(Character.isWhitespace(in.charAt(index))) {
			index++;
			if(index>=in.length()) {
				index = in.length();
				break;
			}
		}
		return(in.substring(index));
	}

	/**
	 * This method reads and parses the actual HTTP request headers. Each header is read from the socket and 
	 * parsed and stored in the Props Object as well as appended to the StringBuffer which stores the entirety
	 * of the HTTP request. If a header is repeated, all values are appended together and separated by commas
	 * in accordance with RFC 2616. The headers are stored via 'Header-Name: Header-value', where 'Header-Name' 
	 * is the property name and 'Header-value' is the property value. The property names are always stored in lowercase.
	 * After this method, the Socket's input stream should be at the point where the Request Body starts.
	 */
	protected boolean parseHTTPRequestHeaders(PushbackInputStream socketIn, Props httpHeaders, StringBuffer httpRequest) throws IOException
	{
		boolean overflow = false;
		String tmp = readLine(socketIn,server.getInteger("max_field_length"));
		StringBuffer tmpHeaderLine = new StringBuffer();
		while(tmp.length()>0) {
			// Check for overflow
			if(!tmp.startsWith(OVERFLOW)) {
				httpRequest.append(tmp);
				httpRequest.append("\n");
			}
			else {
				overflow = true;
				break;
			}

			// We have to check the first character of each line for linear white space
			// which indicates that this line is part of the previous header
			if(Character.isWhitespace(tmp.charAt(0))) {
				// Remove the leading whitespace and append to the StringBuffer
				tmp = removeLeadingWhitespace(tmp);
				tmpHeaderLine.append(" ");
				tmpHeaderLine.append(tmp);
				// Do an overflow check, we don't want people getting around overflow by breaking headers into multiple lines
				if(tmpHeaderLine.length()>server.getInteger("max_field_length")) {
					overflow = true;
					break;
				}
			}
			else {
				// No leading whitespace so parse this header.
				String tmpHeader = tmpHeaderLine.toString();
				int index = tmpHeader.indexOf(":");
				if(index!=-1 || (tmpHeader.length()>0 && Character.isWhitespace(tmpHeader.charAt(0)))) {
					// for now, we'll just ignore broken headers.
					String tmpHeaderName = tmpHeader.substring(0,index).toLowerCase();
					String tmpHeaderValue = removeLeadingWhitespace(tmpHeader.substring(index+1));
					if(httpHeaders.hasProperty(tmpHeaderName)) {
						httpHeaders.setProperty(tmpHeaderName,httpHeaders.getString(tmpHeaderName)+" "+tmpHeaderValue);
					}
					else {
						httpHeaders.setProperty(tmpHeaderName,tmpHeaderValue);
					}
				}

				tmpHeaderLine = new StringBuffer();
				tmpHeaderLine.append(tmp);
			}
			tmp = readLine(socketIn,server.getInteger("max_field_length"));
		}

		// There should be something left in the header queue, parse that header.
		if(tmpHeaderLine.length()>0) {
			String tmpHeader = tmpHeaderLine.toString();
			int index = tmpHeader.indexOf(":");
			if(index!=-1) {
				// for now, we'll just ignore broken headers.
				String tmpHeaderName = tmpHeader.substring(0,index).toLowerCase();
				String tmpHeaderValue = removeLeadingWhitespace(tmpHeader.substring(index+1));
				if(httpHeaders.hasProperty(tmpHeaderName)) {
					httpHeaders.setProperty(tmpHeaderName,httpHeaders.getString(tmpHeaderName)+" "+tmpHeaderValue);
				}
				else {
					httpHeaders.setProperty(tmpHeaderName,tmpHeaderValue);
				}
			}
		}

		return(overflow);
	}



	/**
	 * This method handles all "success" type server logging. Although this includes failures to handle requests, these are "successful" failures.
	 * I know that doesn't make much sense.
	 */
	protected void handleLog(AgentRequest agentRequest, long latency, boolean success)
	{
		try {
			if(!keepAliveTimedOut && success) {
				if(refused) {
					server.warning(cachedInetAddress + " > " + request + ' ' + path + " [request was refused] ("+(System.currentTimeMillis()-latency) + "ms)");
				}
				else if(timedOut) {
					server.warning(cachedInetAddress + " > " + request + ' ' + path + " [timed out] ("+(System.currentTimeMillis()-latency) + "ms)");
				}
				else if(overflowed) {
					server.warning(cachedInetAddress + " > " + "[dropped, request too long] ("+(System.currentTimeMillis()-latency) + "ms)");
				}
				else if (cgiParams != null) {
					server.println(cachedInetAddress + " > " + request + ' ' + p.getString("http_headers:host") + path + '?' + cgiParams + " (" + (System.currentTimeMillis()-latency) + "ms)");
				}
				else {
					server.println(cachedInetAddress + " > " + request + ' ' + p.getString("http_headers:host") + path + " (" + (System.currentTimeMillis()-latency) + "ms)");
				}
			}

			if(server.logger!=null) {
				PropsContainer logProps = new PropsContainer();
				logProps.setProperty("keepAliveTimedOut",""+keepAliveTimedOut);
				logProps.setProperty("refused",""+refused);
				logProps.setProperty("timedOut",""+timedOut);
				logProps.setProperty("overflowed",""+overflowed);
				logProps.setProperty("cgiParams",cgiParams);
				logProps.setProperty("path",path);
				logProps.setProperty("multihome",""+server.multihome);
				logProps.setProperty("returnVal",returnVal);
				logProps.setProperty("latency",""+(System.currentTimeMillis()-latency));
				logProps.setProperty("inetAddress",cachedInetAddress);
				logProps.setProperty("logString",logString);
				logProps.setProperty("http_headers",p.getProperty("http_headers"));
				logProps.setProperty("http_request",httpRequest.toString());
				logProps.setProperty("success",""+success);
				logProps.setProperty("socket_timeout",server.getString("socket_timeout"));
				
				agentRequest.setProperty("logger",logProps);

				server.logger.handleRequest(agentRequest);
			}
		}
		catch(Exception e) {;}
	}
}
