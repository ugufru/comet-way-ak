

package com.cometway.httpd;

import com.cometway.om.SessionManagerInterface;
import com.cometway.ak.AgentRequest;
import com.cometway.props.Props;
import com.cometway.util.jGrep;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.OutputStream;
import java.util.Vector;
import java.util.Date;

/**
 * This file is an extension of the com.cometway.ak.AgentRequest class. This class prints an HTTP 
 * response in the case that an agent which was requested by a browser does not return a proper
 * HTTP/1.1 response header.public class HTTPAgentRequest extends AgentRequest.
 */

public class HTTPAgentRequest extends AgentRequest
{
	/**
	 * For some slower internet connections or under heavy traffic, this flag set to true will help ensure the TCP
	 * connection doesn't get cut off. For faster blocks for writing data to the browser, set this to false.
	 */

	public boolean byteWiseWriting = false;

	/**
	 * This flag tells the HTTPAgentRequest whether or not to buffer all the output that is to be sent to the browser.
	 * When the HTTPAgentRequest.close() method is called, the entire buffer will be sent to the browser. If this flag
	 * is set to false, the output will be sent to the browser immediately when a print() method is called.
	 */

	public boolean bufferOutput = false;

	private static final int OUTPUT_BLOCK_SIZE = 1024;

	protected StringBuffer dataBuffer = new StringBuffer();

	public String defaultResponse = "HTTP/1.1 200 Ok\r\n";
	public boolean headerSending;
	public boolean headerSent;
	private boolean newline;

	protected InputStream browserIn;

	protected Vector cookies;
	protected String sessionID;
	protected boolean headRequest;

	protected String returnVal = null;

	public HTTPAgentRequest(Props p, OutputStream out, OutputStream err)
	{
		super(p, out, err);
	}

	public HTTPAgentRequest(Props p, OutputStream out, OutputStream err, Reader in)
	{
		super(p, out, err);
		browserIn = new com.cometway.io.ReaderInputStream(in);
	}

	public HTTPAgentRequest(Props p, OutputStream out, OutputStream err, InputStream in)
	{
		super(p, out, err);
		browserIn = in;
	}

	
	/**
	 * Returns true if the request is a headers only request
	 */
	public boolean isHeadRequest()
	{
		return(headRequest);
	}


	/**
	 * Returns the value of the "User-Agent" header parameter.
	 */

	public String getUserAgent()
	{
		return (getUserAgent(this));
	}


	/**
	 * Returns the User-Agent header parameter value from the specified AgentRequest.
	 */

	public static String getUserAgent(AgentRequest request)
	{
		if(request instanceof HTTPAgentRequest) {
			return (((HTTPAgentRequest)request).getRequestHeader("user-agent"));
		}
		else {
			return("");
		}
	}


	/**
	 * Returns this requests header parameter
	 */

	public String getRequestHeader(String header)
	{
		return(getString("http_headers:"+header));
	}


	/**
	 * If an input stream to the client was provided, this method will return that stream.
	 */
	public InputStream getClientInputStream()
	{
		return(browserIn);
	}

	/**
	 * This method returns the body of the client's request
	 *
	 */
	public String getRequestBody()
	{
		String rval = "";
		String request = props.getString("request");
		int x = request.indexOf("\n\n");
		if(x!=-1) {
			if(request.indexOf("\r\n\r\n")!=-1 && request.indexOf("\r\n\r\n")<x) {
				x = request.indexOf("\r\n\r\n");
				if(x!=-1) {
					rval = request.substring(x+4);
				}
			}
			else {
				rval = request.substring(x+2);
			}
		}
		else {
			x = request.indexOf("\r\n\r\n");
			if(x!=-1) {
				rval = request.substring(x+4);
			}
		}
		return(rval);
	}

	/**
	 * This method returns the headers of the client's request
	 */
	public String getRequestHeaders()
	{
		String rval = "";
		String request = props.getString("request");
			
		int x = request.indexOf("\n\n");
		if(x!=-1) {
			if(request.indexOf("\r\n\r\n")!=-1 && request.indexOf("\r\n\r\n")<x) {
				x = request.indexOf("\r\n\r\n");
			}
			if(x!=-1) {
				rval = request.substring(0,x);
			}
			else {
				rval = request;
			}
		}
		else {
			x = request.indexOf("\r\n\r\n");
			if(x!=-1) {
				rval = request.substring(0,x);
			}
			else {
				rval = request;
			}
		}

		return(rval);
	}


	/**
	 * Thie method returns the Octet-Stream request body as a byte array. If the request
	 * had no Octet-Stream body, then an empty array is returned.
	 */
	public byte[] getOctetStreamRequestBody()
	{
		if(hasProperty("octet-stream_request_body")) {
			return((byte[])getProperty("octet-stream_request_body"));
		}
		else {
			return(new byte[0]);
		}
	}

	/**
	 * Returns a content length of the request if one exists. If the content length was not
	 * specified, it returns a -1
	 */

	public int getContentLength()
	{
		int rval = -1;

		if(hasProperty("http_headers:content-length")) {
			rval = getInteger("http_headers:content-length");
		}

		return(rval);
	}


	/**
	 * Tests the User-Agent value and stores the results as properties of this request.
	 */

	public void detectBrowsers()
	{
		String s = getUserAgent();

		setProperty("user_agent", s);

		if (s.indexOf("compatible") >= 0)
		{
			setBoolean("compatible_browser", true);
		}

		if ((s.indexOf("Windows") >= 0) || (s.indexOf("Win") >= 0))
		{
			setBoolean("windows_browser", true);
		}

		if ((s.indexOf("Windows 95") >= 0) || (s.indexOf("Windows95") >= 0) || (s.indexOf("Win95") >= 0) || (s.indexOf("Win 95") >= 0))
		{
			setBoolean("windows_95_browser", true);
		}

		if ((s.indexOf("Windows 98") >= 0) || (s.indexOf("Windows98") >= 0) || (s.indexOf("Win98") >= 0) || (s.indexOf("Win 98") >= 0))
		{
			setBoolean("windows_98_browser", true);
		}

		if ((s.indexOf("Windows NT") >= 0) || (s.indexOf("WindowsNT") >= 0) || (s.indexOf("WinNT") >= 0) || (s.indexOf("Win NT") >= 0))
		{
			setBoolean("windows_nt_browser", true);
		}

		if ((s.indexOf("Windows 2000") >= 0) || (s.indexOf("Windows NT 5.0") >= 0))
		{
			setBoolean("windows_2000_browser", true);
		}

		if ((s.indexOf("Windows XP") >= 0) || (s.indexOf("Windows NT 5.1") >= 0))
		{
			setBoolean("windows_xp_browser", true);
		}

		if ((s.indexOf("Macintosh") >= 0) || (s.indexOf("Mac OS") >= 0) || (s.indexOf("Mac_PowerPC") >= 0))
		{
			setBoolean("macintosh_browser", true);
		}

		if (s.indexOf("Mac OS X") >= 0)
		{
			setBoolean("macosx_browser", true);
		}

		if (s.indexOf("Linux") >= 0)
		{
			setBoolean("linux_browser", true);
		}

		if (s.indexOf("X11") >= 0)
		{
			setBoolean("x11_browser", true);
		}

		if ((s.indexOf("PPC") >= 0) || (s.indexOf("PowerPC") >= 0) || (s.indexOf("Power Macintosh") >= 0))
		{
			setBoolean("ppc_browser", true);
		}

		if (s.indexOf("MSIE") >= 0)
		{
			int start = s.indexOf("MSIE") + 5;
			int end = s.indexOf(';', start);

			setProperty("msie_version", s.substring(start, end));
			setBoolean("msie_browser", true);
		}

		if (s.startsWith("Mozilla") && (s.indexOf("compatible") == -1))
		{
			int start = s.indexOf('/') + 1;
			int end = s.indexOf(' ', start);

			setProperty("mozilla_version", s.substring(start, end));
			setBoolean("mozilla_browser", true);
		}
	}


	/** deals with checking for header and sending it if needed */
	protected void handleResponseHeader(String s)
	{
		// Only need to check if header hasn't already been sent and if the header isn't being sent
		if(!headerSent && !headerSending) {
			if(s.startsWith("HTTP")) {
				headerSending = true;
				try {
					int index = s.indexOf(" ");
					int index2 = s.indexOf(" ",index+1);
					if(index2==-1) {
						index2 = s.length();
						returnVal = s.substring(index+1,index2);
					}
				}
				catch(Exception e) {;}
			}
			else {
				String response = createResponseHeaders();
				returnVal = "200";
				if(bufferOutput) {
					dataBuffer.append(response);
				}
				else {
					if(byteWiseWriting) {
						writeData(response.getBytes());
					}
					else {
						super.print(response);
					}
				}
				
				headerSent = true;
			}
		}
	}		

	/**
	 * Updates the state flags after a print() or println()
	 */
	protected void updateFlags(String s)
	{
		// only applies when header is being sent (headerSending == true)
		// - headerSending and headerSent should NEVER both be true
		// - there are 3 cases to check for newlines in the String: END, START, MIDDLE

		if(headerSending) {
			int length = s.length();

			// CASE 1: newlines at the END of String
			// no previous newline and String ends with at least 1 newline
			if(!newline && s.indexOf("\r\n")==length-2) { 
				// if String ends with 2 newlines, end headerSending
				if(s.indexOf("\r\n\r\n")==length-4) { 
					headerSending = false;
					headerSent = true;
				}
				// else, String only ends with 1 newline
				else { 
					newline = true;
				}
			}

			// CASE 2: newlines at the BEGINING of String
			// if there's a previous newline and String starts with a newline
			else if(newline && s.startsWith("\r\n")) {
				headerSending = false;
				headerSent = true;
			}

			// CASE 3: newlines in the MIDDLE of String (Default Case)
			// if header hasn't been sent, check for 2 newlines in middle
			if(!headerSent && s.indexOf("\r\n\r\n")!=-1) {
				headerSending = false;
				headerSent = true;
			}
		}
	}


	/**
	 * Prints String input and header information (content-type, etc.).
	 */
	public void print(String s)
	{
		handleResponseHeader(s);

		if(!headRequest || headerSending) {
			if(headRequest) {
				if(s.indexOf("\r\n\r\n")!=-1) {
					s = s.substring(0,s.indexOf("\r\n\r\n")+4);
					headerSent = true;
					headerSending = false;
					s = jGrep.grepAndReplaceText("([^\\r])\\n","$1\r\n",s,false);
				}
				else if(s.indexOf("\n\n")!=-1) {
					s = s.substring(0,s.indexOf("\n\n")+2);
					headerSent = true;
					headerSending = false;
					s = jGrep.grepAndReplaceText("([^\\r])\\n","$1\r\n",s,false);
				}
				else if(newline && headerSending && s.startsWith("\n")) {
					s = "\r\n";
					headerSent = true;
					headerSending = false;
				}
			}
			
			if(headerSending) {
				s = jGrep.grepAndReplaceText("([^\\r])\\n","$1\r\n",s,false);
			}
			if(bufferOutput) {
				dataBuffer.append(s);
			}
			else if(byteWiseWriting) {
				writeData(s.getBytes());
			}
			else {
				super.print(s);
			}
		}

		updateFlags(s);
	}      


	/**
	 * Prints String and header information to HTML output.
	 */
	
	public void println(String s)
	{
		handleResponseHeader(s+"\n");

		if(!headRequest || headerSending) {
			if(headRequest) {
				if(s.indexOf("\r\n\r\n")!=-1) {
					s = s.substring(0,s.indexOf("\r\n\r\n")+4);
					headerSent = true;
					headerSending = false;
					s = jGrep.grepAndReplaceText("([^\\r])\\n","$1\r\n",s,false);
				}
				else if(s.indexOf("\n\n")!=-1) {
					s = s.substring(0,s.indexOf("\n\n")+1);
					headerSent = true;
					headerSending = false;
					s = jGrep.grepAndReplaceText("([^\\r])\\n","$1\r\n",s,false);
				}
				else if(newline && headerSending && (s.startsWith("\n")||s.startsWith("\r\n"))) {
					headerSent = true;
					headerSending = false;
					s = "";
				}
			}
			if(headerSending) {
				s = s+"\r\n";
			}
			else {
				s = s+"\n";
			}
			
			if(bufferOutput) {
				dataBuffer.append(s);
			}
			else {
				if(byteWiseWriting) {
					writeData(s.getBytes());
				}
				else {
					super.print(s);
				}
			}
		}

		updateFlags(s);
	}      

	public void println()
	{
		String s = "\n";
		if(headerSending) {
			s = "\r\n";
		}

		handleResponseHeader(s);

		if(bufferOutput) {
			dataBuffer.append(s);
		}
		else {
			writeData(s.getBytes());
		}

		updateFlags(s);
	}


	/**
	 * This method will return a Props of the cookie name=value pairs from the client's request. If there
	 * are no cookies, the props is empty. Duplicate names are ignored. Note that although all the cookies 
	 * in the returned Props are directed toward a single handler, those cookie values may not have originated 
	 * from the same cookie; the 'expires', 'path', etc are not included in the client's response header and
	 * are not part of the returned value.
	 */
	public Props getCookies()
	{
		Props rval = new Props();
		
		String headers = getRequestHeaders();
		int index = headers.indexOf("Cookie:");
		if(index!=-1) {
			headers = headers.substring(index+7).trim();
			index = headers.indexOf("\n");
			if(index!=-1) {
				headers = headers.substring(0,index).trim();
			}

			index = headers.indexOf("=");
			while(headers.trim().length()>0) {
				if(index!=-1) {
					if((headers.indexOf(";")!=-1 && headers.indexOf(";")>index) || headers.indexOf(";")==-1) {
						int tmpIndex = headers.indexOf(";",index);
						if(tmpIndex!=-1) {
							rval.setProperty(headers.substring(0,index).trim(),headers.substring(index+1,tmpIndex).trim());
							headers = headers.substring(tmpIndex+1).trim();
							index = headers.indexOf("=");
						}
						else {
							rval.setProperty(headers.substring(0,index).trim(),headers.substring(index+1).trim());
							headers = "";
						}
					}
					else {
						index = headers.indexOf(";");
						rval.setProperty(headers.substring(0,index).trim(),"NO_VALUE");
						headers = headers.substring(index+1);
						index = headers.indexOf("=");
					}
				}
				else {
					if(headers.indexOf(";")!=-1) {
						index = headers.indexOf(";");
						rval.setProperty(headers.substring(0,index).trim(),"NO_VALUE");
						headers = headers.substring(index+1);
						index = headers.indexOf("=");
					}
					else {
						rval.setProperty(headers.trim(),"NO_VALUE");
						headers = "";
					}
				}			    
			}
		}
		return(rval);
	}

	/**
	 * This method returns the Set-Cookie response header String. If no cookies have been
	 * set in this HTTPAgentRequest, then the String will be empty. This is primarily used
	 * by agents that write their own HTTP response.
	 */
	public String getSetCookies()
	{
		StringBuffer buffer = new StringBuffer();
		if(cookies!=null) {
			for(int x=0;x<cookies.size();x++) {
				buffer.append("Set-Cookie: "+cookies.elementAt(x)+"\r\n");
			}
		}
		return(buffer.toString());
	}

	/**
	 * This method adds a Cookie to the portion of the HTTP response header, when the header is sent.
	 * The cookie will not be added unless the header is sent automatically; if the header is printed
	 * by the caller using the println() methods, cookies added will not be sent.
	 *
	 * Note:
	 *   The server sets cookies via the Set-Cookie header, each cookie is set with its own Set-Cookie,
	 *   here is the syntax:
	 *       Set-Cookie: session_id=6a4ffea80df889bd7bfe6a592c0789d8; path=/
	 *       Set-Cookie: member_id=67; expires=Thu, 26-Aug-04 10:27:18 GMT; path=/
	 *       Set-Cookie: pass_hash=4b8e908578ba4bc4ef26740b39bb8831; expires=Thu, 26-Aug-04 10:27:18 GMT; path=/
	 *   
	 *   These cookies are sent from the client to the server via the Cookie: header, all the cookies 
	 *   are sent in one Cookie header, only the data prior to the ';' symbol is sent. Here is the syntax
	 *   of a client sending the server cookie data for the above example:
	 *       Cookie: session_id=6a4ffea80df889bd7bfe6a592c0789d8; member_id=67; pass_hash=4b8e908578ba4bc4ef26740b39bb8831
	 *
	 *   It is up to the client to determine what cookies need to be sent to what servers depending on the
	 *   URI path, and whether to send them or not depending on the expiration of the cookie.
	 *
	 *
	 *  Therefore, an example of the cookieData parameter is: member_id=67; expires=Thu, 26-Aug-04 10:27:18 GMT; path=/
	 *
	 */
	public void addCookie(String cookieData)
	{
		if(cookies==null) {
			cookies = new Vector();
		}
		cookies.addElement(cookieData);
	}


	/**
	 * This method will automatically fetch the session ID of the requesting client's cookies. 
	 * The name of the cookie is assumed to be 'session_id'
	 *
	 * If no session ID was found in the client cookies, null is returned.
	 */
	public String getSessionID()
	{
		if(sessionID==null) {
			Props cookies = getCookies();
			if(cookies.hasProperty("session_id")) {
				sessionID = cookies.getString("session_id");
			}
		}
		return(sessionID);	
	}

	/**
	 * This method will automatically fetch the session ID of the requesting client's cookies. 
	 * The name of the cookie is provided as a parameter
	 *
	 * If no session ID was found in the client cookies, null is returned.
	 */
	public String getSessionID(String cookieName)
	{
		if(sessionID==null) {
			Props cookies = getCookies();
			if(cookies.hasProperty(cookieName)) {
				sessionID = cookies.getString(cookieName);
			}
		}
		return(sessionID);	
	}


	/**
	 * This method will automatically set a cookie to be written to the client when the response
	 * header is to be written. The session ID cookie name will be assumed 'session_id', the
	 * path cookie name will be assumed 'path', and the expires cookie name will be assumed 'expires'.
	 * If a path and/or expires name is not found in the session props for this session ID, they will
	 * not be included in the cookie.
	 *
	 * If no session ID was found in the client cookies, no cookie will be sent.
	 */
	public boolean setSessionCookie(SessionManagerInterface sessionManager, String id)
	{
		boolean rval = false;

		if(id==null) {
			id = sessionManager.createSession();
		}
		Props sessionProps = sessionManager.getSessionProps(id);
		if(sessionProps!=null) {
			String cookieString = "session_id="+id;
			if(sessionProps.hasProperty("expires")) {
				cookieString = cookieString+"; expires="+sessionProps.getString("expires");
			}
			if(sessionProps.hasProperty("path")) {
				cookieString = cookieString+"; path="+sessionProps.getString("path");
			}
			addCookie(cookieString);
			rval = true;
		}
		else {
			//		System.out.println("..");
		}
		return(rval);
	}

	/**
	 * This method will automatically set a cookie to be written to the client when the response
	 * header is to be written. The session ID cookie name will provided via the parameter 'cookieName', the
	 * path cookie name will be assumed 'path', and the expires cookie name will be assumed 'expires'.
	 * If a path and/or expires name is not found in the session props for this session ID, they will
	 * not be included in the cookie.
	 *
	 * If no session ID was found in the client cookies, no cookie will be sent.
	 */
	public boolean setSessionCookie(SessionManagerInterface sessionManager, String id, String cookieName)
	{
		boolean rval = false;

		if(id==null) {
			id = sessionManager.createSession();
		}
		Props sessionProps = sessionManager.getSessionProps(id);
		if(sessionProps!=null) {
			String cookieString = cookieName+"="+id;
			if(sessionProps.hasProperty("expires")) {
				cookieString = cookieString+"; expires="+sessionProps.getString("expires");
			}
			if(sessionProps.hasProperty("path")) {
				cookieString = cookieString+"; path="+sessionProps.getString("path");
			}
			addCookie(cookieString);
			rval = true;
		}		
		return(rval);
	}



	/**
	 * This method returns a Date Object that corresponds to the request's If-Modifed-Since field.
	 * This date can be used to determine if the data requested has been changed and needs to be
	 * resent. If not, a 304 response should be returned. In the event that this field doesn't exist,
	 * null is returned.
	 */
	public Date getIfModifiedSince()
	{
		Date rval = null;

		if(hasProperty("http_headers:if-modified-since")) {
			String request = getString("http_headers:if-modified-since");
			try {
				rval = WebServer.dateFormat_RFC822.parse(request);
			}
			catch(Exception e) {;}
			if(rval==null) {
				try {
					rval = WebServer.dateFormat_RFC850.parse(request);
				}
				catch(Exception e) {;}
			}
			if(rval==null) {
				try {
					rval = WebServer.dateFormat_ANSI.parse(request);
				}
				catch(Exception e) {;}
			}
		}

		return(rval);
	}












	private String createResponseHeaders()
	{
		StringBuffer buffer = new StringBuffer();
		boolean keepAlive = false;

		buffer.append(defaultResponse);
		if(props.hasProperty("content_type")) {
			buffer.append("Content-Type: ");
			buffer.append(props.getString("content_type"));
			buffer.append("\r\n");
		}
		else {
			buffer.append("Content-Type: text/html\r\n");
		}
		buffer.append(getSetCookies());
		buffer.append("Date: "+WebServer.dateFormat_RFC822.format(new Date())+"\r\n");
		
		if(bufferOutput) {
			if(hasProperty(ConnectionKMethod.KEEP_ALIVE)) {
				if(hasProperty(ConnectionKMethod.KEEP_ALIVE_FIELD)) {
					if(getBoolean("http_headers:connection")) {
						buffer.append(getString(ConnectionKMethod.KEEP_ALIVE_FIELD));
						buffer.append("\nConnection: Keep-Alive\n");
					}
					else {
						// If client doesn't want keep-alive, we must remove this property
						removeProperty(ConnectionKMethod.KEEP_ALIVE);
					}
				}
			}
		}

		if(!keepAlive) {
			buffer.append("Connection: close\r\n");
		}

		buffer.append("\r\n");

		return(buffer.toString());
	}



	private void writeData(byte[] data)
	{
		try
		{
			for(int x=0;x<data.length;x++)
			{
				out.write(data[x]);
				out.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void close() throws IOException
	{
		synchronized(this)
		{
			if (bufferOutput)	{
				String data = dataBuffer.toString();

				if (data.startsWith("HTTP/"))	{
					int headerIndex = data.indexOf("\n\n");
					int offset = 2;
					if(headerIndex!=-1) {
						if(data.indexOf("\r\n\r\n")!=-1 && data.indexOf("\r\n\r\n")<headerIndex) {
							headerIndex = data.indexOf("\r\n\r\n");
							offset = 4;
						}
					}
					else {
						headerIndex = data.indexOf("\r\n\r\n");
						offset = 4;
					}

					if (headerIndex!=-1)	{
						headerIndex = headerIndex + offset;

						if (data.substring(0,headerIndex).toLowerCase().indexOf("content-length:")==-1) {
							int contentlength = data.length()-headerIndex;
							int newline = data.indexOf("\n");
							data = data.substring(0,newline)+"\nContent-Length: "+contentlength+"\n"+data.substring(newline+1);
						}
					}

					if(offset==2) {
						headerIndex = data.indexOf("\n\n");
						if(headerIndex!=-1) {
							if(headRequest) {
								data = jGrep.grepAndReplaceText("([^\\r])\\n","$1\r\n",data.substring(0,headerIndex+2),false);
							}
							else {
								data = jGrep.grepAndReplaceText("([^\\r])\\n","$1\r\n",data.substring(0,headerIndex+2),false)+data.substring(headerIndex+2);
							}
						}
					}
					else if(offset==4) {
						headerIndex = data.indexOf("\r\n\r\n");
						if(headerIndex!=-1) {
							if(headRequest) {
								data = jGrep.grepAndReplaceText("([^\\r])\\n","$1\r\n",data.substring(0,headerIndex+4),false);
							}
							else {
								data = jGrep.grepAndReplaceText("([^\\r])\\n","$1\r\n",data.substring(0,headerIndex+4),false)+data.substring(headerIndex+4);
							}
						}
					}
				}

				if (byteWiseWriting)	{
					writeData(data.getBytes());
				}
				else {
					while(data.length()>0) {
						if (data.length()>OUTPUT_BLOCK_SIZE) {
							String tmp = data.substring(0,OUTPUT_BLOCK_SIZE);
							out.write(tmp.getBytes());
							out.flush();
							data = data.substring(OUTPUT_BLOCK_SIZE);
						}
						else {
							out.write(data.getBytes());
							out.flush();
							data = "";
						}
					}
				}

				bufferOutput = false;
			}
		}
	}
}
