package com.cometway.httpd;


import com.cometway.ak.*;

import java.util.*;
import java.io.*;
import java.net.*;


import com.cometway.util.*;
import com.cometway.net.*;
import com.cometway.props.Props;


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
	int socket_linger_time;
	Socket socket;
	WebServer server;
	
	public static final String KEEP_ALIVE = "__CONNECTION_KEEP_ALIVE";
	public static final String KEEP_ALIVE_FIELD = "__KEEP_ALIVE_FIELD";

	/**
	 * This connection handler requires a reference to the WebServer which created
	 * it, the Socket connected to the HTTP client, and the amount of time to linger
	 * after the request has been handled before it closes the connection to the client
	 */
	public ConnectionKMethod(WebServer server, Socket sock, int linger_time)
	{
		this.server = server;
		socket = sock;
		socket_linger_time = linger_time;

		server.counter++;
		server.debug("++ Kmethods: "+server.counter);
	}

	@Deprecated
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

	public String getKeepAliveField()
	{
		return("Keep-Alive: timeout="+((int)(server.getInteger("socket_timeout")/1000))+", max="+(keepAliveLimit-keepAliveCount));
	}

	protected String readLine(PushbackInputStream in, int maxLength) throws IOException
	{
		if(maxLength>0) {
			StringBuffer rval = new StringBuffer();
			boolean overflow = true;
			boolean eof = false;
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
			if(!overflow) {
				return(rval.toString());
			}
			else if(eof) {
				return(null);
			}
			else {
				return("@@@overflow@@"+rval.toString());
			}
		}
		else {
			return(socketReadLine(in));
		}
	}

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
					rval.append(new String(buffer,0,x-1,"ISO-8859-1"));
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
	 * Everything happens here
	 */ 
	public void execute()
	{
		PushbackInputStream socketIn = null;
		BufferedOutputStream socketOut = null;

		// Handler State flags and variables
		String response = "HTTP/1.1 500 Server Error.\r\nDate: "+WebServer.dateFormat_RFC822.format(new Date())+"\r\nConnection: close\r\n\r\n";
		String returnVal = null;
		InetAddress cachedInetAddress = null;
		boolean responded = false;
		boolean refused = false;
		boolean dropped = false;
		boolean timedOut = false;
		boolean keepAliveTimedOut = false;   // if this is true, we don't report errors because the keep alive connection timed out normally
		boolean overflowed = false;
		StringBuffer httpRequest = new StringBuffer();
		String logString = "";
		String request = null;
		String path = null;
		String hostField = "";
		String fullpath = "/";
		String cgiParams = null;
		String tmpParams = null;
		Props p = null;
		long latency = System.currentTimeMillis();
		String requestContentType = "application/x-www-form-urlencoded";

		server.debug("Socket: "+socket);

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

				/////////////////////////////////////////////////////////////////////////
				// This first block deals with reading the first line of the request (whether keep-alive or not)
				try {
					// FIRST read in the request line. Is it POST or GET?
					//					String line = socketIn.readLine();
					String line = readLine(socketIn, server.getInteger("max_uri_length"));
					socket.setSoTimeout(server.getInteger("socket_timeout"));
					if(line==null) {
						try {
							socketOut.close();
						}
						catch(Exception e) {;}
						try {
							socketIn.close();
						}
						catch(Exception e) {;}
						return;
					}
					else if(line.startsWith("@@@overflow@@")) {
						try {
							socketOut.write(WebServer.getHTMLByCode(WebServer.REQUEST_URI_TOO_LONG).getBytes());
							socketOut.flush();
						}
						catch(Exception e) {;}
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
						returnVal = "414";
						responded = true;
						overflowed = true;
						server.debug("Request too long: "+line.substring(13));
					}

					if(keepAliveCount>0) {
						while(line!=null && line.trim().length()==0) {
							//							line = socketIn.readLine();
							line = socketReadLine(socketIn);
						}
						if(line==null) {
							throw(new InterruptedIOException());
						}
					}

					if(line.startsWith("@@@overflow@@")) {
						try {
							socketOut.write(WebServer.getHTMLByCode(WebServer.REQUEST_URI_TOO_LONG).getBytes());
							socketOut.flush();
						}
						catch(Exception e) {;}
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
						returnVal = "414";
						responded = true;
						overflowed = true;
						server.debug("Request too long: "+line.substring(13));
					}

					if(!responded && server.getBoolean("strict_http_version")) {
						try {
							if(line.trim().lastIndexOf(" HTTP/1.1")!=(line.trim().length()-9)) {
								responded = true;
								refused = true;
							}
						}
						catch(Exception e) {
							responded = true;
							refused = true;
						}
						if(refused) {
							int index = line.indexOf(" ");
							request = line.substring(0,index);
							path = "/";
							if(line.indexOf(" ",index+1) > 0) {
								path = line.substring(index+1,line.indexOf(" ",index+1));
							}
							else {
								path = line.substring(index+1);
							}
							
							fullpath = path;

							try {
								socketOut.write(WebServer.getHTMLByCode(WebServer.VERSION_NOT_SUPPORTED).getBytes());
								socketOut.flush();
							}
							catch(Exception e) {;}
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
							returnVal = "505";
						}
					}

					logString = "\""+line+"\"";
			
					if(!overflowed) {
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
						
						fullpath = path;
					}

					keepAlive = false; // This flag will be set to true at the bottom, after the request has been handled
				}
				catch(IndexOutOfBoundsException ioob) {
					if(keepAlive) {
						keepAlive = false;
						keepAliveTimedOut = true;
					}
					timedOut = true;
				}
				catch(SocketException se) {
					if(keepAlive) {
						keepAlive = false;
						keepAliveTimedOut = true;
					}
					timedOut = true;
				}
				catch(InterruptedIOException iioe) {
					if(keepAlive) {
						keepAlive = false;
						keepAliveTimedOut = true;
					}
					timedOut = true;
				}
				catch(Exception e) {
					keepAlive = false;
					server.error("Error handling the request URI",e);
				}
			
				//////////////////////////////////////////////////////////////////
				// The second block deals with reading in the rest of the request, the fields, and request body
				try {
					// If there are CGI parameters, parse them out
					if(!responded && !timedOut) {
						if(path.indexOf("?",path.lastIndexOf("/"))!=-1) {
							// This is a GET request, parse out the params from the path
							tmpParams = path.substring(path.indexOf("?",path.lastIndexOf("/"))+1);
					
							path = path.substring(0,path.indexOf("?",path.lastIndexOf("/")));

							if(path.startsWith("/")) {
								path = path.substring(1);
							}
						}
						if(request.equalsIgnoreCase("POST")) {
							if(path.startsWith("/")) {
								path = path.substring(1);
							}

							// This is a POST request, read until the post stuff
							try {
								String tmp = readLine(socketIn,server.getInteger("max_field_length"));
								if(!tmp.startsWith("@@@overflow@@")) {
									httpRequest.append(tmp);
									httpRequest.append("\n");
									int contentLength = -1;
									String multipartBoundary = null;
									
									while(tmp.length()!=0) {
										if(tmp.toLowerCase().startsWith("content-length:")) {
											contentLength = Integer.parseInt(tmp.substring(15).trim());
										}
										else if(tmp.toLowerCase().startsWith("content-type:")) {
											tmp = tmp.substring(13).trim();
											requestContentType = tmp;
											if(tmp.toLowerCase().startsWith("multipart/form-data; boundary=")) {
												multipartBoundary = tmp.substring(30).trim();
											}
										}
										httpRequest.append(tmp);
										httpRequest.append("\n");
										
										tmp=readLine(socketIn, server.getInteger("max_field_length"));
										if(tmp.startsWith("@@@overflow@@")) {
											overflowed = true;
											break;
										}
									}

									if(!overflowed) {
										if(contentLength==-1) {
											//											tmpParams = socketIn.readLine();
											tmpParams = socketReadLine(socketIn);
											httpRequest.append(tmpParams);
											httpRequest.append("\n");
										}
										else {
											if(multipartBoundary==null) {
												byte[] buffer = new byte[contentLength];
												int bytesRead = socketIn.read(buffer);
												if(bytesRead < contentLength) {
													for(int x=bytesRead;x<contentLength;x++) {
														buffer[x] = (byte)socketIn.read();
													}
												}
												tmpParams = new String(buffer,"ISO-8859-1");

												//												char inChar = '0';
												//												for(int x=0;x<contentLength;x++) {
												//													inChar = (char)socketIn.read();
												//													if(((int)inChar)!=-1) {								
												//														tmpParams = tmpParams+inChar;
												//													}
												//													else {
												//														break;
												//													}
												//												}
											
												httpRequest.append("\n");							
												httpRequest.append(tmpParams);
												httpRequest.append("\n");	
											}
											else {
												// I have NO idea why I need to do this....
												multipartBoundary = "--"+multipartBoundary;
											
											
												// This is to limit a possible DOS attack by uploading a VERY large file
												// we may want to handle this differently
												if(contentLength<=server.getInteger("max_multipart_length")) {
													// Multipart parsing code goes here
													Vector parts = new Vector();
													
													// This might be slow, but I can't think of any other way to read the
													// same data from a string in both char form and byte form 
													StringBuffer contentStr = new StringBuffer();
													byte[] content = new byte[contentLength];
													//													for(int x=0;x<content.length;x++) {
														//														content[x] = socketIn.read();
													//														content[x] = socket.getInputStream().read();
													//														contentStr.append((char)(content[x]&0x00FF));
													//													}
													int bytesRead = socketIn.read(content);
													if(bytesRead < contentLength) {
														for(int x=bytesRead;x<contentLength;x++) {
															content[x] = (byte)socketIn.read();
														}
													}
													contentStr.append(new String(content,"ISO-8859-1"));

													try {
														tmp = contentStr.toString();
														contentStr = null;    // this is to free up the buffer in case it happens to be VERY large
														
														int boundaryIndex1 = tmp.indexOf(multipartBoundary);
														int boundaryIndex2 = -1;
														if(boundaryIndex1!=-1) {
															boundaryIndex2 = tmp.indexOf(multipartBoundary,boundaryIndex1+1);
														}
														while(boundaryIndex2!=-1) {
															String multipartBlock = tmp.substring(boundaryIndex1+multipartBoundary.length(),boundaryIndex2);
															if(multipartBlock.substring(multipartBlock.length()-2).equals("\r\n")) {
																multipartBlock = multipartBlock.substring(0,multipartBlock.length()-2);
														}
															else if(multipartBlock.substring(multipartBlock.length()-1).equals("\n")) {
																multipartBlock = multipartBlock.substring(0,multipartBlock.length()-1);
															}
															
															if(multipartBlock.indexOf("Content-Disposition: form-data;")!=-1) {
																int tmpIndex = multipartBlock.indexOf("; name=\"");
																int tmpIndex2 = multipartBlock.indexOf("\"",tmpIndex+8);
																String formDataName = multipartBlock.substring(tmpIndex+8,tmpIndex2);
																String filename = null;
																if(multipartBlock.substring(tmpIndex2+1).startsWith("; filename=\"")) {
																	tmpIndex = multipartBlock.indexOf("\"",tmpIndex2+13);
																	filename = multipartBlock.substring(tmpIndex2+13,tmpIndex);
																	tmpIndex2 = multipartBlock.indexOf("Content-Type",tmpIndex);
																	boolean binary = false;
																	if(tmpIndex2!=-1) {
																		if(multipartBlock.substring(tmpIndex2).startsWith("Content-Type: text")) {
																			tmpIndex = multipartBlock.indexOf("\n",tmpIndex2);
																		}
																		else {
																			tmpIndex = multipartBlock.indexOf("\n",tmpIndex2);
																			binary = true;
																		}
																	}
																	if(tmpIndex2<multipartBlock.length()) {
																		tmpIndex++;
																		while(Character.isWhitespace(multipartBlock.charAt(tmpIndex))) {
																			tmpIndex++;
																			if(tmpIndex2>=multipartBlock.length()) {
																				break;
																			}
																		}
																	}
																	
																	if(!binary) {
																		Hashtable values = new Hashtable();
																		values.put("filename",filename);
																		values.put("filedata",multipartBlock.substring(tmpIndex));
																		p.setProperty(formDataName,values);
																	}
																	else {
																		Hashtable values = new Hashtable();
																		values.put("filename",filename);
																		byte[] filedata = new byte[multipartBlock.length()-tmpIndex];
																		System.arraycopy(content,boundaryIndex1+multipartBoundary.length()+tmpIndex,filedata,0,filedata.length);
																		//																		for(int x=0;x<filedata.length;x++) {
																		//																			filedata[x]=content[x+boundaryIndex1+multipartBoundary.length()+tmpIndex];
																		//																		}
																		values.put("filedata",filedata);
																		p.setProperty(formDataName,values);
																	}
																}
																else {
																	tmpIndex2++;
																	if(tmpIndex2<multipartBlock.length()) {
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
															boundaryIndex1 = boundaryIndex2;
															boundaryIndex2 = tmp.indexOf(multipartBoundary,boundaryIndex1+1);
														}
													}
													catch(Exception e) {
														server.error("Could not read multipart MIME POST data: "+cachedInetAddress+" "+request+" "+path,e);
													}
												}
												else {
													overflowed = true;
												}
											}
										}
									}
								}
								else {
									overflowed = true;
								}

								if(overflowed) {
									try {
										socketOut.write(WebServer.getHTMLByCode(WebServer.REQUEST_ENTITY_TOO_LARGE).getBytes());
										socketOut.flush();
									}
									catch(Exception e) {;}
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
									returnVal = "413";
									responded = true;
									if(tmp.length()>13) {
										server.debug("Request too long: "+httpRequest+tmp.substring(13));
									}
									else {
										server.debug("Request too long: "+httpRequest);
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
							try {
								String tmp = readLine(socketIn, server.getInteger("max_field_length"));
								while(tmp!=null && tmp.length()!=0 && !tmp.startsWith("@@@overflow@@")) {
									httpRequest.append(tmp);
									httpRequest.append("\n");
									tmp = readLine(socketIn, server.getInteger("max_field_length"));
								}
								if(tmp.startsWith("@@@overflow@@")) {
									overflowed = true;
								}
							}
							catch(SocketException iioe) {
								timedOut = true;
							}
							catch(InterruptedIOException iioe) {
								timedOut = true;
							}
							catch(Exception e) {
								server.error("Error reading request: "+cachedInetAddress+" "+request+" "+path,e);
							}

							if(overflowed) {
								try {
									socketOut.write(WebServer.getHTMLByCode(WebServer.REQUEST_ENTITY_TOO_LARGE).getBytes());
									socketOut.flush();
								}
								catch(Exception e) {;}
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
								returnVal = "413";
								responded = true;
							}
						}
					}
				}
				catch(Exception e) {
					server.error("Error while reading request fields and/or request body.",e);
				}

				/////////////////////////////////////////////////////////////////////////
				// This third section parses any CGI parameters and handles the request, giving it to the String of possible request handlers
				try {
					if(!responded && !timedOut) {
						cgiParams = tmpParams;

						// if the request body is form encoded, otherwise we ignore
						if(requestContentType.startsWith("application/x-www-form-urlencoded")) {
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
						//						else {
						//							System.out.println("read: "+cgiParams);
						//						}

						if(!path.startsWith("/")) {
							path = "/"+path;
						}
						if(!fullpath.startsWith("/")) {
							fullpath = "/"+fullpath;
						}

						String host = "";
						{
							String requestString = httpRequest.toString();
							if(requestString.indexOf("Host:")!=-1) {
								requestString = requestString.substring(requestString.indexOf("Host:")+5);
								int index1 = requestString.indexOf("\n");
								if(index1==-1) {
									index1 = requestString.length();
								}
								requestString = requestString.substring(0,index1);
								hostField = requestString.trim();
								index1 = requestString.indexOf(":");
								if(index1!=-1) {
									requestString = requestString.substring(0,index1);
								}
								host = requestString.toLowerCase().trim();

								if(server.getInteger("bind_port")==443) {
									try {
										host = host.substring(0,host.indexOf(":"));
									}
									catch(Exception e) {;}
								}
								else if(server.getInteger("bind_port")==80) {
									try {
										host = host.substring(0,host.indexOf(":"));
									}
									catch(Exception e) {;}
								}					    
							}
						}							

						path = HTMLStringTools.decode(path);
						fullpath = HTMLStringTools.decode(path);
					

						// Setup http stuff in the HTTPAgentRequest Props
						p.setProperty("request",httpRequest.toString());
						p.setProperty("request_type","HTTP");
						p.setProperty("path",fullpath);
						p.setProperty("request_server_name",socket.getLocalAddress().getHostAddress());
						p.setProperty("request_server_port",server.getProps().getString("bind_port"));
						p.setProperty("host",hostField);
						if(server.getProps().getBoolean("use_inet_address_methods")) {
							p.setProperty("request_remote_host",cachedInetAddress.getHostName());
							p.setProperty("request_remote_addr",cachedInetAddress.getHostAddress());
							p.setProperty("request_id","HTTP:"+cachedInetAddress.getHostName());
						}
				
						if(httpRequest.toString().toLowerCase().indexOf("connection: keep-alive")!=-1) {
							if(keepAliveLimit-keepAliveCount>0) {
								p.setProperty(KEEP_ALIVE_FIELD,getKeepAliveField());
							}
						}

						HTTPAgentRequest agentRequest = null;

						try {
							agentRequest = new HTTPAgentRequest(p,socketOut,System.out,socketIn);
							agentRequest.headRequest = request.equalsIgnoreCase("HEAD");

							// look through all relevant WebServerExtensions
							Vector extensions = server.getExtensions(host);
							for(int x=0;x<extensions.size();x++) {
								WebServerExtension extension = (WebServerExtension)extensions.elementAt(x);
								if(extension.handleRequest(agentRequest)) {
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


				/////////////////////////////////////////////////////////////////////////////////
				// This last section deals with server output and logging
				try {
					//		server.println("Completed request for: "+cachedInetAddress+" "+request+" "+path);
					if(!keepAliveTimedOut) {
						if(dropped) {
							server.warning(cachedInetAddress + " > [request was dropped] ("+(System.currentTimeMillis()-latency) + "ms)");
						}
						else if(refused) {
							server.warning(cachedInetAddress + " > " + request + ' ' + fullpath + " [request was refused] ("+(System.currentTimeMillis()-latency) + "ms)");
						}
						else if(timedOut) {
							server.warning(cachedInetAddress + " > " + request + ' ' + fullpath + " [timed out] ("+(System.currentTimeMillis()-latency) + "ms)");
						}
						else if(overflowed) {
							server.warning(cachedInetAddress + " > " + "[dropped, request too long] ("+(System.currentTimeMillis()-latency) + "ms)");
						}
						else if (cgiParams != null)
						{
							server.println(cachedInetAddress + " > " + request + ' ' + hostField + fullpath + '?' + cgiParams + " (" + (System.currentTimeMillis()-latency) + "ms)");
						}
						else
						{
							server.println(cachedInetAddress + " > " + request + ' ' + hostField + fullpath + " (" + (System.currentTimeMillis()-latency) + "ms)");
						}
	
						//					System.out.println("returnVal="+returnVal);
						if(server.logger!=null && returnVal!=null) {
							if(System.currentTimeMillis()-latency < server.getInteger("socket_timeout")) {
								String httpReqStr = httpRequest.toString();
								int tmpIndex = httpReqStr.indexOf("Host:");
								if(dropped) {
									logString = cachedInetAddress.getHostAddress()+" - (request dropped) - ["+getLogDate()+"] "+logString+" "+returnVal+" "+(System.currentTimeMillis()-latency);
								}
								else if(refused) {
									logString = cachedInetAddress.getHostAddress()+" - (request refused) - ["+getLogDate()+"] "+logString+" "+returnVal+" "+(System.currentTimeMillis()-latency);
								}
								else if(timedOut) {
									logString = cachedInetAddress.getHostAddress()+" - (request timed out) - ["+getLogDate()+"] "+logString+" "+returnVal+" "+(System.currentTimeMillis()-latency);
								}
								else if(overflowed) {
									logString = cachedInetAddress.getHostAddress()+" - (request was too long) - ["+getLogDate()+"] "+logString+" "+returnVal+" "+(System.currentTimeMillis()-latency);
								}
								else {
									if(server.multihome && tmpIndex!=-1) {
										int tmpIndex2 = httpReqStr.indexOf("\n",tmpIndex);
										logString = cachedInetAddress.getHostAddress()+" - "+httpReqStr.substring(tmpIndex+5,tmpIndex2).trim()+" - ["+getLogDate()+"] "+logString+" "+returnVal+" "+(System.currentTimeMillis()-latency);
									}
									else {
										logString = cachedInetAddress.getHostAddress()+" - - ["+getLogDate()+"] "+logString+" "+returnVal+" "+(System.currentTimeMillis()-latency);
									}
									tmpIndex = httpReqStr.indexOf("Referer:");
									if(tmpIndex!=-1) {
										int tmpIndex2 = httpReqStr.indexOf("\n",tmpIndex);
										logString = logString + " \""+httpReqStr.substring(tmpIndex+8,tmpIndex2).trim()+"\" ";
									}
									else {
										logString = logString + " \"\" ";
									}
									tmpIndex = httpReqStr.indexOf("User-Agent:");
									if(tmpIndex!=-1) {
										int tmpIndex2 = httpReqStr.indexOf("\n",tmpIndex);
										logString = logString + "\""+httpReqStr.substring(tmpIndex+11,tmpIndex2).trim()+"\"";
									}
									else {
										logString = logString + "\"\"";
									}
								}
							
								server.logger.log(logString);
							}
						}
					}
				}
				catch(Exception e) {;}


				if(keepAlive) {
					// RESET all the variables
					responded = false;
					httpRequest = new StringBuffer();
					logString = "";
					request = null;
					path = null;
					hostField = "";
					fullpath = "/";
					cgiParams = null;
					latency = System.currentTimeMillis();
				}
			}
		}


		try {
			//			Thread.sleep(socket_linger_time);
		}
		catch(Exception e) {;}

		if(!responded) {
			try {
				if(timedOut) {
					if(!keepAliveTimedOut) {
						socketOut.write(WebServer.getHTMLByCode(WebServer.REQUEST_TIMED_OUT).getBytes());
						socketOut.flush();
					}
				}
				else if(httpRequest.toString().trim().length()>0) {
					socketOut.write(WebServer.getHTMLByCode(WebServer.URL_NOT_FOUND).getBytes());
					socketOut.flush();
				}
			}
			catch(Exception e) {;}

			try {
				if(server.logger!=null) {
					if(httpRequest.toString().trim().length()>0 && !keepAliveTimedOut) {
						//						logString = cachedInetAddress.getHostAddress()+" "+logString;
						//						logString = "400 RESPONSE: "+logString+"\nREQUEST----------------------------------------------\n"+httpRequest+"-----------------------------------------------------\n";
						String httpReqStr = httpRequest.toString();
						int tmpIndex = httpReqStr.indexOf("Host:");
						if(timedOut) {
							returnVal = "408";
						}
						else {
							returnVal = "404";
						}
						if(server.multihome && tmpIndex!=-1) {
							int tmpIndex2 = httpReqStr.indexOf("\n",tmpIndex);
							logString = cachedInetAddress.getHostAddress()+" - "+httpReqStr.substring(tmpIndex+5,tmpIndex2).trim()+" - ["+getLogDate()+"] "+logString+" "+returnVal+" "+(System.currentTimeMillis()-latency);
						}
						else {
							logString = cachedInetAddress.getHostAddress()+" - - ["+getLogDate()+"] "+logString+" "+returnVal+" "+(System.currentTimeMillis()-latency);
						}
						tmpIndex = httpReqStr.indexOf("Referer:");
						if(tmpIndex!=-1) {
							int tmpIndex2 = httpReqStr.indexOf("\n",tmpIndex);
							logString = logString + " \""+httpReqStr.substring(tmpIndex+8,tmpIndex2).trim()+"\" ";
						}
						else {
							logString = logString + " \"\" ";
						}
						tmpIndex = httpReqStr.indexOf("User-Agent:");
						if(tmpIndex!=-1) {
							int tmpIndex2 = httpReqStr.indexOf("\n",tmpIndex);
							logString = logString + "\""+httpReqStr.substring(tmpIndex+11,tmpIndex2).trim()+"\"";
						}
						else {
							logString = logString + "\"\"";
						}
						server.logger.log(logString);
					}
				}
			}
			catch(Exception e) {;}
		}

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

		//		System.out.println("Exiting...");
		//		System.gc();
	}



	protected String getLogDate()
	{
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		String s = c.get(c.DAY_OF_MONTH)+"/";
		int tmp = c.get(c.MONTH);
		if(tmp==0) {
			s = s+"Jan/";
		}
		else if(tmp==1) {
			s = s+"Feb/";
		}
		else if(tmp==2) {
			s = s+"Mar/";
		}
		else if(tmp==3) {
			s = s+"Apr/";
		}
		else if(tmp==4) {
			s = s+"May/";
		}
		else if(tmp==5) {
			s = s+"Jun/";
		}
		else if(tmp==6) {
			s = s+"Jul/";
		}
		else if(tmp==7) {
			s = s+"Aug/";
		}
		else if(tmp==8) {
			s = s+"Sep/";
		}
		else if(tmp==9) {
			s = s+"Oct/";
		}
		else if(tmp==10) {
			s = s+"Nov/";
		}
		else if(tmp==11) {
			s = s+"Dec/";
		}
		s = s+c.get(c.YEAR)+":";

		if((c.get(c.HOUR_OF_DAY)+"").length()==1) {
			s = s+"0"+c.get(c.HOUR_OF_DAY)+":";
		}
		else {
			s = s+c.get(c.HOUR_OF_DAY)+":";
		}

		if((c.get(c.MINUTE)+"").length()==1) {
			s = s+"0"+c.get(c.MINUTE)+":";
		}
		else {
			s = s+c.get(c.MINUTE)+":";
		}

		if((c.get(c.SECOND)+"").length()==1) {
			s = s+"0"+c.get(c.SECOND)+" ";
		}
		else {
			s = s+c.get(c.SECOND)+" ";
		}

		String zoneOffset = "";
		int zoffset = c.get(c.ZONE_OFFSET);
		if(zoffset<0) {
			zoneOffset = "-";
			zoffset = zoffset*-1;
		}

		zoffset = zoffset/(60*600);
		if((""+zoffset).length()<3) {
			zoneOffset = zoneOffset+"00"+zoffset;
		}
		else if((""+zoffset).length()<4) {
			zoneOffset = zoneOffset+"0"+zoffset;
		}

		s = s+zoneOffset;

		return(s);
	}
}
