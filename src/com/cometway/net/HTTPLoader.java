package com.cometway.net;

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;
//import com.cometway.net.*;
import com.cometway.text.*;
import com.cometway.util.*;
import com.cometway.props.*;

import java.security.*;
import java.security.spec.*;
import java.security.cert.*;

import javax.net.*;
import javax.net.ssl.*;



/**
 * This class provides a access to the HTTP protocol at a lower level than the
 * URLConnection class provided by java. This class allows the caller to configure
 * every aspect of an HTTP transaction. <p>
 *
 * There is also a fully functional static main method provided for using the HTTPLoader
 * on the command line. The list of available options can be listed by giving no
 * arguments or using the --help option.
 *
 */

public class HTTPLoader
{
	// Class constants
	// public final static String DEFAULT_USER_AGENT_STR = "Mozilla/4.06 [en] (X11; I; Linux 2.0.34 i686; Nav)\n"
	public final static String      DEFAULT_USER_AGENT_STR = "HTTPLoader/1.0\n";
	public final static String      META_HTTP_REGEXP = "<meta http-equiv=\"refresh\"[\\\"\\'\\#A-Za-z0-9\\s-_\\%\\=]*content\\s*=\\s*\"\\d;url=";
	public final static String      DEFAULT_ERROR_STR = "!HTTPLoader! ";
	public final static String      DEFAULT_PRINT_STR = "[HTTPLoader] ";
	public final static String      DEFAULT_DEBUG_STR = "%HTTPLoader% ";
	public final static int		DEFAULT_TIMEOUT = 60000;
	public final static String      ALL_FRAMES = "*";
	public final static int		DEFAULT_RECURSION_LIMIT = 5;



	// Runtime flags and settings
	protected boolean		autoRedirect = true;
	protected boolean		allowForeignCookies = true;
	protected boolean		debug = false;
	protected boolean		verbose = false;
	protected int			recursionLimit = DEFAULT_RECURSION_LIMIT;
	protected int			requestTimeout = DEFAULT_TIMEOUT;

	protected String		loadFrames = ALL_FRAMES;

	protected String		debugStr = DEFAULT_DEBUG_STR;
	protected String		errorStr = DEFAULT_ERROR_STR;
	protected String		printStr = DEFAULT_PRINT_STR;
	protected String		userAgent = DEFAULT_USER_AGENT_STR;

	protected IExceptionHandler     exceptionHandler = null;

	public String                 proxyServer = null;
	public int                    proxyServerPort = 0;

	public String                 httpsProxyServer = null;
	public int                    httpsProxyServerPort = 0;

	public String charset = "iso-8859-1";

	// Class variables
	protected Vector		cookieList;
	protected String		requestProtocol;
	protected String		requestHost;
	protected int			requestPort;
	protected String		requestPath;
	public String		requestURL;
	protected Socket		requestSocket;
	protected HTTPRequest		request;
	protected long			requestMillis;
	protected OutputStream	requestWriter;
	protected InputStream requestReader;
	protected HTTPResponse		response;
	protected int requestDataLength = -1;
	protected int			recursionCount;

	protected Vector extraHeaders;

	// publicly accessable runtime flags
	public boolean useReferer = true;
	public boolean useUserAgent = true;
	public boolean useHost = true;
	public boolean useAccept = true;
	public boolean useContentType = true;
	public boolean useCarraigeReturnsInRequest = true;

	public String acceptString = "text/html, image/gif, image/jpeg, */*; q=.2, */*; q=.2";
	public String contentTypeString = "application/x-www-form-urlencoded";


	public String referer;

	public String key_file = "server.key";
	public String cert1_file = "newcert.pem";

	public HTTPLoader()
	{
		request = new HTTPRequest("/");
	}



	/**
	 * * Closes the connection if it is open.
	 */

	public boolean closeConnection()
	{
		if ((requestWriter != null) || (requestReader != null))
		{
			try
			{
				requestWriter.close();
				requestReader.close();

				requestWriter = null;
				requestReader = null;
			}
			catch (Exception t)
			{
				;
			}
		}

		if (requestSocket != null)
		{
			try
			{
				requestSocket.close();
				println("Connection closed to " + requestHost + ':' + requestPort);
			}
			catch (Exception e)
			{		// A NullPointerException is typically thrown here when using SSL sockets.
				if (requestProtocol.equals("https") == false)
				{
					exception("closeConnection", e);
				}
			}

			requestSocket = null;
		}

		return (requestSocket != null);
	}

	/**
	 * * Returns the default port number of the specified protocol; -1 if protocol is unknown.
	 */

	public int getDefaultPort(String protocol)
	{
		int     port;

		if (protocol.equals("http"))
		{
			port = 80;
		}
		else if (protocol.equals("https"))
		{
			port = 443;
		}
		else
		{
			port = -1;
		}

		return (port);
	}

	/**
	 * This method returns the HTTP result code from the last HTTP request.
	 */
	public String getResultCode()
	{
		if(response!=null) {
			return(response.resultCode);
		}
		else {
			return(null);
		}
	}

	/**
	 * Returns the response headers
	 */ 
	public Hashtable getResponseHeader()
	{
		return(response.headers);
	}

	/**
	 * Returns the redirect location if the result code starts with 3
	 */
	public String getRedirectLocation()
	{
		return(response.redirectLocation);
	}


	/**
	 * Sets the key and cert file to use with HTTPS requests. 
	 */
	public void setHTTPKey(String keyFile, String certFile)
	{
		key_file = keyFile;
		cert1_file = certFile;
	}

	/**
	 * This method sets the HTTPLoader to use a proxy server.
	 */
	public void setProxyServer(String proxyHostName, int port) 
	{
		proxyServer = proxyHostName;
		proxyServerPort = port;
	}


	/**
	 * This method sets the HTTPLoader to use a proxy server for HTTPS requests.
	 */
	public void setHTTPSProxyServer(String proxyHostName, int port) 
	{
		httpsProxyServer = proxyHostName;
		httpsProxyServerPort = port;
	}


	/**
	 * This method forces the HTTPLoader to use BASIC authentication
	 */
	public void useBasicAuthentication(String username, String password)
	{
		request.authString = Base64Encoding.encode(username+":"+password);
	}

	/**
	 * This method turns removes the username and password authentication, subsequent requests will not use any authentication
	 */
	public void removeAuthentication()
	{
		request.authString = null;
	}

	/**
	 * * Sets the state of the auto-redirect feature.
	 */

	public void setAutoRedirect(boolean state)
	{
		autoRedirect = state;
	}

	/**
	 * * Prints additional debugging output to System.out when turned on.
	 */

	public void setDebug(boolean state)
	{
		debug = state;
	}


	/**
	 * * Sets the exception handler for this object.
	 */

	public void setExceptionHandler(IExceptionHandler e)
	{
		exceptionHandler = e;
	}


	/**
	 * * Sets the names of the frames that will be automatically loaded;
	 * set to ALL_NAMES (load all frames) by default. Names are separated by
	 * whitespace. Setting to null disables this feature.
	 * Note: Only ALL_NAMES and null are currently implemented.
	 * any non-null value will be treated like ALL_NAMES.
	 */

	public void setLoadFrames(String frames)
	{
		if (frames == null)
		{
			loadFrames = null;
		}
		else
		{
			loadFrames = frames.toLowerCase();
		}
	}


	/**
	 * Sets the recursion limit. The default value is 5. 
	 */

	public void setRecursionLimit(int limit)
	{
		recursionLimit = limit;
	}


	/**
	 * * Sets the request timeout used when opening a connection.
	 */

	public void setRequestTimeout(int timeout)
	{
		requestTimeout = timeout;
	}


	/**
	 * * Sets the string passed to web servers to identify a client.
	 */

	public void setUserAgent(String s)
	{
		userAgent = s;
	}


	public void setVerbose(boolean enabled)
	{
		verbose = enabled;
	}


	//
	// Cookie methods
	//


	/**
	 * * Cookies are accepted anyway when their domain and path do not match the request
	 * when this option is turned on; otherwise they are ignored.
	 */

	public void setAllowForeignCookies(boolean allow)
	{
		allowForeignCookies = allow;
	}


	/**
	 * * Sets a vector for storing a list of CookiePropsContainers.
	 */

	public void setCookieList(Vector cookieList)
	{
		this.cookieList = cookieList;
	}


	/**
	 * * Enables cookie support by creating a new empty vector for storing cookies;
	 * disabling removes any existing cookie list.
	 */

	public void setCookiesSupported(boolean supported)
	{
		if (supported)
		{
			if (cookieList == null)
			{
				cookieList = new Vector();
			}
		}
		else
		{
			cookieList = null;
		}
	}


	/**
	 * * Returns a cookie string to be added to the list of headers.
	 */

	protected String getCookieString()
	{
		StringBuffer    cookieString = new StringBuffer();

		if (cookieList != null)
		{
			Props   p = new Props();
			int     size = cookieList.size();
			
			if(size>0) {
				cookieString.append("Cookie: ");
			}

			for (int i = 0; i < size; i++)
			{
				p.setPropsContainer((IPropsContainer) cookieList.elementAt(i));

				String  domain = p.getString("domain");
				String  path = p.getString("path");

				if (allowForeignCookies || (requestHost.endsWith(domain) && requestPath.startsWith(path)))
				{
					if (i > 0)
					{
						cookieString.append("; ");
					}

					String tmpCookie = p.getString("cookie");
					if(tmpCookie.indexOf(";")!=-1) {
						tmpCookie = tmpCookie.substring(0,tmpCookie.indexOf(";"));
					}
					cookieString.append((tmpCookie));
				}
			}
		}

		return (cookieString.toString());
	}


	/**
	 * * Returns the vector used to store cookies as CookiePropsContainers.
	 */

	public Vector getCookieList()
	{
		return (cookieList);
	}


	/**
	 * * Resets the cookie list by creating a new empty Vector;
	 * this replaces the previous cookie list if present.
	 */

	public void resetCookieList()
	{
		cookieList = new Vector();
	}


	/**
	 * * Adds a cookie to the cookie list.
	 */

	public void addCookie(String cookieString)
	{
		addCookie(new CookiePropsContainer(cookieString));
	}


	/**
	 * * Adds a cookie to the cookie list.
	 */

	public void addCookie(CookiePropsContainer c)
	{		// get the name of the cookie; bail out if none provided.
		String  cookieName = (String) c.getProperty("name");

		if (cookieName == null)
		{
			error("unnamed cookie: " + c.getProperty("cookie"));

			return;
		}		// get the domain for the cookie; use request host if none provided.

		String  cookieDomain = (String) c.getProperty("domain");

		if (cookieDomain == null)
		{
			cookieDomain = requestHost;

			c.setProperty("domain", requestHost);
		}		// get the path for the cookie; use reqeust path if none provided.

		String  cookiePath = (String) c.getProperty("path");

		if (cookiePath == null)
		{
			cookiePath = requestPath;

			c.setProperty("path", requestPath);
		}		// get the cookie string

		String  cookieString = (String) c.getProperty("cookie");

		debug("addCookie: " + cookieString);

		if (cookieList == null)
		{
			cookieList = new Vector();
		}		// check the expiration date on this cookie; bail if it's gone bad.

		String  expires = (String) c.getProperty("expires");

		if (expires != null)
		{
			debug("expires = " + expires);

			try
			{
				// Wdy, DD-Mon-YYYY HH:MM:SS GMT
				// Sun, 14 Sep 2008 07:10:59 GMT
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
				Date d = new Date();
				try {
					d = dateFormat.parse(expires);
				}
				catch(ParseException pe) {
					dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
					d = dateFormat.parse(expires);
				}
					
				if (d.before(new Date()))
				{
					error("cookie expired: " + cookieString);

					//					return;		// i don't usually do this.
				}
			}
			catch (Exception e)
			{
				exception("addCookie", e);
			}
		}		// don't accept the cookie if it does not match the request host and path

		if (allowForeignCookies || (requestHost.endsWith(cookieDomain) && requestPath.startsWith(cookiePath)))
		{
			Props   p = new Props();
			int     size = cookieList.size();		// go through our cookie list to see if any of them match.

			for (int i = 0; i < size; i++)
			{
				p.setPropsContainer((IPropsContainer) cookieList.elementAt(i));

				String  name = p.getString("name");
				String  domain = p.getString("domain");
				String  path = p.getString("path");		// replace the cookie if we had one previously.

				if (cookieName.equals(name) && cookieDomain.endsWith(domain) && cookiePath.startsWith(path))
				{
					println("replacing cookie: " + cookieString);
					cookieList.setElementAt(c, i);

					return;		// I usually don't do this, but this time it works well.
				}
			}		// if we made it here, we do not already have this cookie. add it.

			println("adding cookie: " + cookieString);
			cookieList.addElement(c);
		}
		else
		{
			debug("cookie does not match request host and path");
		}
	}



	/**
	 * Allows you to set the extra headers in a request.
	 */
	public void setExtraHeaders(String headers)
	{
		if(extraHeaders==null) {
			extraHeaders = new Vector();
		}
		extraHeaders.addElement(headers);
	}

	/**
	 * Allows you to set the extra headers in a request.
	 */
	public void setExtraHeaders(String name, String value) 
	{
		if(extraHeaders==null) {
			extraHeaders = new Vector();
		}
		extraHeaders.addElement(name+": "+value);
	}

	/**
	 * Removes any extra headers set in the request
	 */
	public void removeExtraHeaders()
	{
		extraHeaders = null;
	}










	//
	//  GET requests
	//
	/**
	 * Sends a GET request; HTTP parameters contained in a Props.
	 */

	public String getURL(String url, Props params)
	{
		return(getURL(url,params,null));
	}

	public String getURL(String url, Props params, OutputStream dataOut)
	{
		url = url + '?' + getHTTPParamString(params);

		return (requestURL(url, HTTPRequest.GET_REQUEST_TYPE, null, null, dataOut,null));
	}



	/**
	 * Sends a GET request; HTTP parameters contained in Vectors.
	 */
	public String getURL(String url, Vector fields, Vector values)
	{
		return(getURL(url,fields,values,null));
	}

	public String getURL(String url, Vector fields, Vector values, OutputStream dataOut)
	{
		url = url + '?' + getHTTPParamString(fields, values);

		return (requestURL(url, HTTPRequest.GET_REQUEST_TYPE, null, null, dataOut,null));
	}


	/**
	 * Sends a GET request; no HTTP parameters.
	 */

	public String getURL(String url, String referer)
	{
		return (requestURL(url, HTTPRequest.GET_REQUEST_TYPE, null,null, null,referer));
	}

	public String getURL(String url)
	{
		return (requestURL(url, HTTPRequest.GET_REQUEST_TYPE, null,null, null,null));
	}

	public String getURL(String url, OutputStream dataOut)
	{
		return (requestURL(url, HTTPRequest.GET_REQUEST_TYPE, null, null, dataOut,null));
	}


	/**
	 * Uses shorter GET header for simple requests.
	 */

	public String shortGetURL(String url)
	{
		return (requestURL(url, HTTPRequest.SHORT_GET_REQUEST_TYPE, null, null, null,null));
	}		
	public String shortGetURL(String url, OutputStream dataOut)
	{
		return (requestURL(url, HTTPRequest.SHORT_GET_REQUEST_TYPE, null, null, dataOut,null));
	}		








	//
	// POST requests
	//
	/**
	 * Sends a POST request; HTTP parameters contained in a Props.
	 */

	public String postURL(String url, Props params)
	{
		return(postURL(url,params,null));
	}

	public String postURL(String url, Props params, OutputStream dataOut)
	{
		return (requestURL(url, HTTPRequest.POST_REQUEST_TYPE, getHTTPParamString(params), null, dataOut,null));
	}


	/**
	 * Sends a POST request; HTTP parameters contained in Vectors.
	 */

	public String postURL(String url, Vector fields, Vector values)
	{
		return(postURL(url,fields,values,null));
	}

	public String postURL(String url, Vector fields, Vector values, OutputStream dataOut)
	{
		String  httpParamStr = getHTTPParamString(fields, values);

		return (requestURL(url, HTTPRequest.POST_REQUEST_TYPE, httpParamStr, null, dataOut,null));
	}


	/**
	 * Sends a POST request; HTTP parameters already encoded as String.
	 */

	public String postURL(String url, String httpParamString)
	{
		return (requestURL(url, HTTPRequest.POST_REQUEST_TYPE, httpParamString, null, null,null));
	}

	public String postURL(String url, String httpParamString, OutputStream dataOut)
	{
		return (requestURL(url, HTTPRequest.POST_REQUEST_TYPE, httpParamString, null, dataOut,null));
	}


	/**
	 * Sends a POST request; no HTTP parameters.
	 */

	public String postURL(String url)
	{
		return (requestURL(url, HTTPRequest.POST_REQUEST_TYPE, "", null, null,null));
	}
	public String postURL(String url, OutputStream dataOut)
	{
		return (requestURL(url, HTTPRequest.POST_REQUEST_TYPE, "", null, dataOut,null));
	}

	/**
	 * Sends a POST request that is multipart-mime body
	 */
	public String postURL(String url, Hashtable mimeTypes, Props params)
	{
		return(postURL(url,mimeTypes,params,null));
	}

	public String postURL(String url, Hashtable mimeTypes, Props params, OutputStream dataOut)
	{
		String rval = "";
		if(mimeTypes==null) {
			mimeTypes = new Hashtable();
		}
		try {
			String boundary = "--------------------------------__"+UUID.randomUUID().toString();
			while(true) {
				boolean boundaryOK = true;
				Enumeration e = params.getKeys().elements();
				while(e.hasMoreElements()) {
					if(params.getString((String)e.nextElement()).indexOf(boundary)!=-1) {
						boundaryOK = false;
						break;
					}
				}
			
				if(boundaryOK) {
					break;
				}
				else {
					boundary = "--------------------------------__"+UUID.randomUUID().toString();
				}
			}

			boolean tmp = useContentType;
			String oldContentTypeString = contentTypeString;
			useContentType = true;
			contentTypeString = "multipart/form-data; boundary="+boundary;

			ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
			Enumeration e = params.getKeys().elements();
			while(e.hasMoreElements()) {
				String paramName = (String)e.nextElement();
				tmpOut.write(("--"+boundary+"\n").getBytes());
				tmpOut.write(("Content-Disposition: form-data; name=\""+paramName+"\"").getBytes());
				if(mimeTypes.containsKey(paramName)) {
					if(mimeTypes.containsKey(paramName+"_filename")) {
						tmpOut.write(("; filename=\""+mimeTypes.get(paramName+"_filename")+"\"").getBytes());
					}
					tmpOut.write(("\nContent-Type: "+mimeTypes.get(paramName)+"\n\n").getBytes());
					Object o = params.getProperty(paramName);
					if(o instanceof byte[]) {
						tmpOut.write((byte[])o);
					}
					else {
						String s = o.toString();
						tmpOut.write(s.getBytes());
						if(s.charAt(s.length()-1)!='\n') {
							tmpOut.write(("\n").getBytes());
						}
					}
				}
				else {
					tmpOut.write(("\n\n"+params.getString(paramName)+"\n").getBytes());
				}
			}
			tmpOut.write(("--"+boundary+"--\n").getBytes());
			
			//			System.out.println(new String(tmpOut.toByteArray()));
			requestDataLength = tmpOut.toByteArray().length;
			rval = requestURL(url,HTTPRequest.POST_REQUEST_TYPE,null,new ByteArrayInputStream(tmpOut.toByteArray()),dataOut,null);

			contentTypeString = oldContentTypeString;
			useContentType = tmp;
		}
		catch(IOException e) {
			exception("Error creating multipart-mime request",e);
		}

		return(rval);
	}


	public String postURL(String url, File postFile, String mimeType)
	{
		String rval = "";
		InputStream in = null;
		StringBuffer data = new StringBuffer();
		try {
			in = new FileInputStream(postFile);
			boolean tmp = useContentType;
			useContentType = true;
			contentTypeString = mimeType;
			requestDataLength = (int)postFile.length();
			
			rval = requestURL(url, HTTPRequest.POST_REQUEST_TYPE, null, in, null,null);
			
			useContentType = tmp;
		}
		catch(IOException e) {
			exception("Error reading file",e);
		}
		try {
			in.close();
		}
		catch(Exception e) {;}

		return(rval);
	}

	public String postURL(String url, File postFile, String mimeType, OutputStream dataOut)
	{
		String rval = "";
		InputStream in = null;
		StringBuffer data = new StringBuffer();
		try {
			in = new FileInputStream(postFile);
			boolean tmp = useContentType;
			useContentType = true;
			contentTypeString = mimeType;
			requestDataLength = (int)postFile.length();
			
			rval = requestURL(url, HTTPRequest.POST_REQUEST_TYPE, null, in, dataOut,null);
			
			useContentType = tmp;
		}
		catch(IOException e) {
			exception("Error reading file",e);
		}
		try {
			in.close();
		}
		catch(Exception e) {;}

		return(rval);
	}








	//
	// HEAD requests
	//
	/**
	 * Sends a HEAD request
	 */
	public Props getHeaders(String url)
	{
		Props rval = new Props();

		requestURL(url, HTTPRequest.HEAD_REQUEST_TYPE, null, null, null,null);

		if(response!=null) {		
			Enumeration keys = response.headers.keys();
			while(keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				rval.setProperty(key,response.headers.get(key));
			}
		}

		return(rval);
	}









	//
	// PUT requests
	//
	public boolean putURL(String url, File file, String mimeType)
	{
		boolean rval = false;

		InputStream in = null;

		try {
			in = new FileInputStream(file);
			boolean tmp = useContentType;
			useContentType = true;
			contentTypeString = mimeType;
			requestDataLength = (int)file.length();

			requestURL(url,HTTPRequest.PUT_REQUEST_TYPE,null,in,null,null);

			useContentType = tmp;
		}
		catch(Exception e) {
			exception("Could not read file "+file,e);
		}
			
		if(request!=null) {
			if(response.resultCode == HTTPResponse.CODE_201) {
				rval = true;
			}
		}

		try {
			in.close();
		}
		catch(Exception e) {;}

		return(rval);
	}


	public boolean putURL(String url, byte[] data, String mimeType)
	{
		boolean rval = false;

		boolean tmp = useContentType;
		useContentType = true;
		contentTypeString = mimeType;
		requestDataLength = data.length;

		requestURL(url,HTTPRequest.PUT_REQUEST_TYPE,new String(data),null,null,null);

		useContentType = tmp;

		if(response.resultCode == HTTPResponse.CODE_201) {
			rval = true;
		}

		return(rval);
	}

	public boolean putURL(String url, String data)
	{
		boolean rval = false;

		boolean tmp = useContentType;
		useContentType = true;
		contentTypeString = "text/html";
		requestDataLength = data.length();

		requestURL(url,HTTPRequest.PUT_REQUEST_TYPE,data,null,null,null);

		useContentType = tmp;

		if(response.resultCode == HTTPResponse.CODE_201) {
			rval = true;
		}

		return(rval);
	}

































	//
	// 
	//
	//


	/**
	 * Creates a parameter list suitable for GET and POST style requests.
	 */

	public static String getHTTPParamString(Vector fields, Vector values)
	{


		// System.out.println("[getHTTPParamString] fields = " + fields);
		// System.out.println("[getHTTPParamString] values = " + values);

		StringBuffer    b = new StringBuffer();
		int		size = fields.size();

		for (int i = 0; i < size; i++)
		{
			if (i > 0)
			{
				b.append('&');
			}

			b.append(HTTPClient.convert((String)fields.elementAt(i)));
			b.append('=');
			b.append(HTTPClient.convert((String) values.elementAt(i)));
		}


		// System.out.println("[getHTTPParamString] " + b.toString());

		return (b.toString());
	}


	/**
	 * Creates a parameter list suitable for GET and POST style requests.
	 */

	public static String getHTTPParamString(Props p)
	{


		// System.out.println("[getHTTPParamString] p = \n" + p.toString());

		StringBuffer    b = new StringBuffer();
		Enumeration     e = p.getKeys().elements();

		while (e.hasMoreElements())
		{
			String  key = (String) e.nextElement();
			String  value = p.getString(key);

			if (b.length() > 0)
			{
				b.append('&');
			}

			b.append(key);
			b.append('=');
			b.append(HTTPClient.convert(value));
		}


		// System.out.println("[getHTTPParamString] " + b.toString());

		return (b.toString());
	}







	//
	//
	//  Internal utility methods
	//
	//
	//
	/**
	 * * Loads pages from framesets embedded in the results and returns them inline.
	 */

	protected String loadFrames(String page)
	{


		// debug("loadFrames: " + page);

		StringBuffer    b = new StringBuffer();
		Vector		matches = jGrep.grepText("<[i]?frame[\\\"\\'\\#A-Za-z0-9\\s-_\\%\\=]*src\\s*=\\s*", page, true);	// , perl);
		int		i = 0;

		if (matches != null)
		{
			for (int x = 0; x < matches.size(); x++)
			{
				try
				{
					IntegerPair     pair = (IntegerPair) matches.elementAt(x);
					String		url;
					int		start = pair.firstInt();
					int		end = pair.secondInt();

					if (page.charAt(end) == '"')
					{
						url = page.substring(end + 1, page.indexOf("\"", end + 1));
					}
					else if (page.charAt(end) == '\'')
					{
						url = page.substring(end + 1, page.indexOf("'", end + 1));
					}
					else
					{
						int     y = page.indexOf(">", end + 1);

						if (y > page.indexOf(" ", end + 1))
						{
							y = page.indexOf(" ", end + 1);
						}

						url = page.substring(end, y);
					}		// Complete the frame URL if necessary.

					if (url.indexOf("://") == -1)
					{
						debug("Completing frame URL: " + url);

						if (url.startsWith("/"))
						{
							url = requestURL.substring(0, requestURL.indexOf('/', requestURL.indexOf("://") + 3)) + url;
						}
						else
						{
							url = requestURL.substring(0, requestURL.lastIndexOf('/') + 1) + url;
						}
					}

					println("Loading frame: " + url);
					b.append(page.substring(i, start));
					b.append("\n<!-- begin frame ");
					b.append(url);
					b.append(" -->\n");
					b.append(getURL(url));
					b.append("\n<!-- end frame ");
					b.append(url);
					b.append(" -->\n");

					i = page.indexOf('>', end) + 1;
				}
				catch (Exception e)
				{
					//					e.printStackTrace();
					exception("Could not load frames",e);
				}
			}

			b.append(page.substring(i));

			page = b.toString();


			// debug("page:\n" + page);

		}

		return (page);
	}


	/**
	 * * Opens a request connection.
	 */

	protected boolean openConnection()
	{
		boolean success = false;

		try
		{
			openSocket();
			println("Connected to " + requestHost + ':' + requestPort);
			requestSocket.setSoTimeout(requestTimeout);
			debug("Connection timeout set to " + requestTimeout);		// Open a reader and writer to communicate with the server.
			debug("Opening reader and writer");

			//			requestWriter = new BufferedWriter(new OutputStreamWriter(requestSocket.getOutputStream()));
			requestWriter = requestSocket.getOutputStream();
			requestReader = requestSocket.getInputStream();

			debug("Connection to " + requestHost + ':' + requestPort + " established");

			success = true;
		}
		catch (ConnectException ex1)
		{
			exception("Connection refused to host: " + requestHost + ':' + requestSocket, ex1);
		}
		catch (NoRouteToHostException ex2)
		{
			exception("No route to host: " + requestHost, ex2);
		}
		catch (ProtocolException ex3)
		{
			exception("Protocol error", ex3);
		}
		catch (UnknownHostException ex4)
		{
			exception("Unknown host: " + requestHost, ex4);
		}
		catch (SocketException ex5)
		{
			exception("Socket error", ex5);
		}
		catch (Exception ex6)
		{
			exception("openConnection", ex6);
		}

		if(!success) {
			try {
				requestReader.close();
			}
			catch(Exception e) {;}
			try {
				requestWriter.close();
			}
			catch(Exception e) {;}
			try {
				requestSocket.close();
			}
			catch(Exception e) {;}
		}
					
		return (success);
	}


	/**
	 * * Opens a socket connection.
	 */

	protected void openSocket() throws Exception
	{
		if(requestProtocol.equals("http")) {
			if(proxyServer==null) {
				debug("Opening socket to " + requestHost + ':' + requestPort);
				
				requestSocket = new Socket(requestHost, requestPort);
			}
			else {
				debug("Opening socket to proxy server "+proxyServer+":"+proxyServerPort+" for "+requestHost+":"+requestPort);
				
				requestSocket = new Socket(proxyServer, proxyServerPort);
			}
		}
		else if(requestProtocol.equals("https")) {
			if(httpsProxyServer==null) {
				//				debug("Opening socket to " + requestHost + ':' + requestPort);
				
				//				requestSocket = new Socket(requestHost, requestPort);
				File cert = new File(cert1_file);
				File key = new File(key_file);

				FileInputStream fis = new FileInputStream(cert);

				CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
				X509Certificate certificate = (X509Certificate)certFactory.generateCertificate(fis);
				X509Certificate[] certs = new X509Certificate[1];
				certs[0] = certificate;
		    
				PKCS8EncodedKeySpec encodedKey = new PKCS8EncodedKeySpec(readBinaryFile(key));
				//	    X509EncodedKeySpec encodedKey = new X509EncodedKeySpec(buffer);
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				PrivateKey privatekey = keyFactory.generatePrivate(encodedKey);
		    
				KeyStore store = KeyStore.getInstance("JKS","SUN");
				store.load(null,null);
				store.setKeyEntry("client",privatekey,new char[0],certs);

				SSLContext sc = SSLContext.getInstance("SSL");
				KeyManagerFactory keyManager = KeyManagerFactory.getInstance("SunX509");
				keyManager.init(store,new char[0]);
				TrustManager[] trusts = new TrustManager[1];
				trusts[0] = new TrustEverything();
				sc.init(keyManager.getKeyManagers(),trusts,null);

				SocketFactory sf = sc.getSocketFactory();

				if(proxyServer!=null) {
					requestSocket = new Socket(proxyServer,proxyServerPort);
					try {
						OutputStream out = requestSocket.getOutputStream();
						out.write(("open "+requestHost+":"+requestPort+"\n").getBytes());
						out.flush();
					}
					catch(Exception e) {
						e.printStackTrace();
						try {
							requestSocket.close();
						}
						catch(Exception e2) {;}
						requestSocket = null;
						return;
					}
					requestSocket = ((SSLSocketFactory)sf).createSocket(requestSocket, requestHost, requestPort, true);
				}
				else {
					requestSocket = sf.createSocket(requestHost,requestPort);
					//		    requestSocket = new SSLSocket(requestHost,requestPort,sc);
				}
				((SSLSocket)requestSocket).setNeedClientAuth(false);
				((SSLSocket)requestSocket).setWantClientAuth(false);
				((SSLSocket)requestSocket).setUseClientMode(true);
				((SSLSocket)requestSocket).startHandshake();

			}
			else {
				debug("Opening socket to HTTPS proxy server "+httpsProxyServer+":"+httpsProxyServerPort+" for "+requestHost+":"+requestPort);
				
				requestSocket = new Socket(httpsProxyServer, httpsProxyServerPort);
			}
		}
	}


	/**
	 * * Prepares this object for a connection by parsing the target URL.
	 */

	protected void prepareRequest(String url)
	{
		debug("Preparing request for " + url);		// find the protocol; assume http if none is provided.

		url = jGrep.grepAndReplaceText("\\s", "", url, false);

		int     i = url.indexOf("://");

		// fix ampersands in GET urls
		while(url.indexOf("&amp;")!=-1) {
			int index = url.indexOf("&amp;");
			url = url.substring(0,index+1)+url.substring(index+5);
		}

		if ((i == -1) && (i < 8))
		{
			requestProtocol = "http";
		}
		else
		{
			requestProtocol = url.substring(0, i).trim().toLowerCase();
			url = url.substring(i + 3);
		}		// find the hostname, port, and path; construct a new url.

		i = url.indexOf(':');

		int     j = url.indexOf('/');

		if (i == -1)
		{
			requestPort = getDefaultPort(requestProtocol);

			if (j == -1)
			{
				requestHost = url.trim();
				requestPath = "/";
			}
			else
			{
				requestHost = url.substring(0, j);
				requestPath = url.substring(j).trim();
			}

			requestURL = requestProtocol + "://" + requestHost + requestPath;
		}
		else
		{
			if (j == -1)
			{
				requestHost = url.substring(0, i).trim();
				requestPort = Integer.parseInt(url.substring(i + 1).trim());
				requestPath = "/";
			}
			else
			{
				if (j > i)
				{
					requestPort = Integer.parseInt(url.substring(i + 1, j));
					requestHost = url.substring(0, i).trim();
					requestPath = url.substring(j).trim();
				}
				else
				{
					requestPort = getDefaultPort(requestProtocol);
					requestHost = url.substring(0, j).trim();
					requestPath = url.substring(j).trim();
				}
			}

			requestURL = requestProtocol + "://" + requestHost + ':' + requestPort + requestPath;
		}

		debug("requestProtocol = " + requestProtocol);
		debug("requestHost     = " + requestHost);
		debug("requestPort     = " + requestPort);
		debug("requestPath     = " + requestPath);
		debug("requestURL      = " + requestURL);
		debug("requestTimeout  = " + requestTimeout);
	}



	/**
	 * Used to make the request. requestType is defined in HTTPRequest. httpParamString can be null.
	 */

	protected String requestURL(String url, int requestType, String httpParamString, InputStream dataIn, OutputStream dataOut, String referer)
	{
		if (recursionCount >= recursionLimit)
		{
			error("Exceeded a level " + recursionLimit + " recursion for URL="+url+", refered by "+referer+". Returning empty string.");
			return ("");
		}

		String  result = "";
                
		recursionCount++;

		prepareRequest(url);

		requestMillis = System.currentTimeMillis();

		println("Requesting " + requestURL);

		if (openConnection())
		{
			request.requestType = requestType;
			if(proxyServer==null) {
				request.requestString = requestPath;
			}
			else {
				request.requestString = requestURL;
			}
			if(useReferer) {
				if(referer==null) {
					if(this.referer!=null) {
						request.referer = this.referer;
					}
					else {
						request.referer = requestURL;
					}
				}
				else {
					request.referer = referer;
				}
			}
			if(useUserAgent) {
				request.userAgent = userAgent;
			}
			if(useHost) {
				request.host = requestHost;
			}
			if(useAccept) {
				request.acceptString = acceptString;
			}
			if(useContentType) {
				request.contentType = contentTypeString;
			}
			request.connection = HTTPRequest.CLOSE_CONNECTION;
			request.otherHeaders = null;
			if(extraHeaders!=null) {
				for(int z=0;z<extraHeaders.size();z++) {
					request.addHeader(extraHeaders.elementAt(z).toString());
				}
			}
			request.requestData = httpParamString;		// Support for sending cookies
			if(dataIn!=null) {
				request.dataSource = dataIn;
			}
			if(requestDataLength!=-1) {
				request.requestDataLength = requestDataLength;
			}


			if (cookieList != null)
			{
				debug("cookieList = " + cookieList);

				String  cookieString = getCookieString();

				if (cookieString.length() > 0)
				{
					debug("cookieString = \n" + cookieString);
					request.addHeader(cookieString);
				}
			}

			result = sendRequest(dataOut);		// Support for receiving cookies

			debug("cookieList = " + cookieList);

			if (cookieList != null)
			{
				String[]	headers = response.getHeader("set-cookie");

				debug("headers = " + headers);

				if (headers != null)
				{
					for (int i = 0; i < headers.length; i++)
					{
						addCookie(headers[i]);
					}
				}
			}

			closeConnection();
		}

		requestMillis = System.currentTimeMillis() - requestMillis;

		println(requestURL + " loaded in " + requestMillis + "ms");		// Handle redirects.


		/* looking for <META HTTP-EQUIV> tags for redirects embedded in HTML. */

		if ((result != null) && (response.redirectLocation == null) && autoRedirect)
		{
			// Check if result is HTML/XML
			int i = 0;
			boolean isXML = true;
			try {
				while(Character.isWhitespace(result.charAt(i))) {i++;}
				for(int x=0;x<50;x++) {
					if(!(Character.isWhitespace(result.charAt(i)) ||
						  Character.isLetterOrDigit(result.charAt(i)) ||
						  result.charAt(i)=='/' ||
						  result.charAt(i)=='\'' ||
						  result.charAt(i)=='"' ||
						  result.charAt(i)=='=' ||
						  result.charAt(i)=='<' ||
						  result.charAt(i)=='>' ||
						  result.charAt(i)=='-' ||
						  result.charAt(i)=='!' ||
						  result.charAt(i)==':' ||
						  result.charAt(i)=='.')) {
						isXML = false;
						break;
					}
				}
			}
			catch(Exception e) {;}
			
			if(isXML && result.length()<100000) {
				StringTextBuffer	b = new StringTextBuffer(result);
				RegExpTextFinder	finder = new RegExpTextFinder(META_HTTP_REGEXP, true);
				TextRange		r = b.findText(0, finder);
				
				if (r != null)
				{
					debug("Found " + r.toString());
					r.getStartPointer().setPosition(r.getEndPointer());
					
					if (r.getEndPointer().findNext('"'))
					{
						debug("URL = " + r.toString());
						
						response.redirectLocation = r.toString();
					}
				}
			}
		}

		if ((response.redirectLocation != null) && autoRedirect && (response.redirectLocation.equals(requestURL) == false))
		{
			url = response.redirectLocation;

			int     i = url.indexOf("://");

			if (i == -1)
			{
				if (url.startsWith("/"))
				{
					url = requestURL.substring(0, requestURL.indexOf('/', requestURL.indexOf("://") + 3)) + url;
				}
				else
				{
					int lastSlash = requestURL.lastIndexOf('/');
					if(requestURL.indexOf("?")!=-1) {
						if(lastSlash > requestURL.indexOf("?")) {
							lastSlash = requestURL.substring(0,requestURL.indexOf("?")).lastIndexOf('/');
						}
					}
					url = requestURL.substring(0, lastSlash + 1) + url;
				}
			}

			println("Redirecting to " + url);

			result = getURL(url,requestURL);
		}		// Handle framesets.

		if (loadFrames != null)
		{
			result = loadFrames(result);
		}

		recursionCount--;

		return (result);
	}


	/**
	 * * Uses the open connection to make a request.
	 */

	protected String sendRequest(OutputStream dataOut)
	{
		String  result = "";

		try
		{
			if(useCarraigeReturnsInRequest) {
				request.useCarraigeReturns = true;
			}
			else {
				request.useCarraigeReturns = false;
			}
			response = HTTPClient.sendRequest(request, requestReader, requestWriter, dataOut,this);

			if (response.data != null)
			{
				result = response.data;
			}

			debug("response.resultCode       = " + response.resultCode);
			debug("response.redirectLocation = " + response.redirectLocation);
		}
		catch (IOException ex1)
		{
			exception("I/O Error: " + requestHost + ':' + requestPort, ex1);
		}
		catch (Exception ex2)
		{
			exception("sendRequest", ex2);
		}

		requestDataLength = -1;

		return (result);
	}




	public String getRequestURL()
	{
		return(requestURL);
	}


	protected byte[] readBinaryFile(File file)
	{
		byte[] rval = null;
		if(file.exists()) {
			rval = new byte[(int)file.length()];
			BufferedInputStream in = null;
			try {
				in = new BufferedInputStream(new FileInputStream(file));
				in.read(rval);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			try {
				in.close();
			}
			catch(Exception e) {;}
		}
		return(rval);
	}


	class TrustEverything implements X509TrustManager
	{
		public TrustEverything()
		{
			;
		}


		public void checkClientTrusted(X509Certificate[] chain, String authType)
		{
			return;
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
		{
			return;
		}

		public X509Certificate[] getAcceptedIssuers()
		{
			return(new X509Certificate[0]);
		}
	}















	/**
	 * * Prints a message to System.out
	 */

	protected void println(String s)
	{
		if (verbose)
		{
			System.out.println(printStr + s);
		}
	}


	/**
	 * * Prints a debugging message to System.out when debugging is turned on.
	 */

	protected void debug(String s)
	{
		if (debug)
		{
			System.out.println(debugStr + s);
		}
	}


	/**
	 * * Prints an error message to System.err
	 */

	protected void error(String s)
	{
		System.err.println(errorStr + s);
	}

	/**
	 * * Passes the exception to the IExceptionHandler set for this object,
	 * or prints the exception to System.err if no IExceptionHandler is set.
	 */

	protected void exception(String s, Exception e)
	{
		boolean handled = false;

		if (exceptionHandler != null)
		{
			handled = exceptionHandler.handleException(e, this, s);
		}

		if (handled == false)
		{
			System.err.println(errorStr + s);
			e.printStackTrace();
		}
	}
























	private static String printHelp()
	{
		StringBuffer out = new StringBuffer();

		out.append("HTTPLoader usage:\n");
		out.append("java com.cometway.net.HTTPLoader <REQUEST> [args]\n");
		out.append("   where <REQUEST> is one of the 4 supported HTTP requests (GET, POST, HEAD, PUT)\n");
		out.append("   and [args] are the requests arguments and global options.\n\n");

		out.append("GET request: GET or get\n");
		out.append(" -url <URL>\n");
		out.append("    This option tells the HTTPLoader what URL to send as a GET request.\n\n");

		out.append("POST request: POST or post\n");
		out.append(" -url <URL>\n");
		out.append("    This option tells the HTTPLoader what URL to POST to.\n");
		out.append(" -contentTypeString <STRING>\n");
		out.append("    The mime type of the data to be sent with the request.\n");
		out.append(" -parameters <param string>\n");
		out.append("    The parameter name/value pairs to send with the request.\n");
		out.append(" -file <FILE>\n");
		out.append("    The file <FILE> will be sent as the parameters with the request.\n\n");

		out.append("HEAD request: HEAD or head\n");
		out.append(" -url <URL>\n");
		out.append("    This option tells the HTTPLoader what URL to send as a HEAD request.\n\n");

		out.append("PUT requests: PUT or put\n");
		out.append(" -url <URL>\n");
		out.append("    This option tells the HTTPLoader what URL to request a PUT to.\n");
		out.append(" -contentTypeString <STRING>\n");
		out.append("    The mime type of the data to be sent with the request.\n");
		out.append(" -file <FILE>\n");
		out.append("    The file <FILE> will be sent as the data for the PUT request.\n\n");

		out.append("Global options:\n");
		out.append(" -outfile <FILE>\n");
		out.append("    This option causes the HTTPLoader to output the returned HTML document in the file <FILE>.\n");
		out.append(" -autoRedirect\n");
		out.append("    Using this option will cause the HTTPLoader to automatically redirect. This option is set by default.\n");
		out.append(" -noAutoRedirect\n");
		out.append("    Using this option will cause the HTTPLoader not to redirect, instead it will return the response of the webserver's redirect.\n");
		out.append(" -allowCookies\n");
		out.append("    This option will enable to storage of cookies (cached in memory). This option is set by default.\n");
		out.append(" -noAllowCookies\n");
		out.append("    This option will cause the HTTPLoader to reject all the cookies it is sent.\n");
		out.append(" -debug\n");
		out.append("    This option turns on debugging output.\n");
		out.append(" -noDebug\n");
		out.append("    This option turns off debugging output. This option is set by default.\n");
		out.append(" -verbose\n");
		out.append("    This option turns on verbose output.\n");
		out.append(" -noVerbose\n");
		out.append("    This option turns off verbose output. This option is set by default.\n");
		out.append(" -recursionLimit <INTEGER>\n");
		out.append("    This option sets the number of times the HTTPLoader will redirect (in case the redirects are cyclic). The default is "+DEFAULT_RECURSION_LIMIT+".\n");
		out.append(" -requestTimeout <INTEGER>\n");
		out.append("    This option sets the number of milleseconds the HTTPLoader will wait for the webserver until it gives up. The default is "+DEFAULT_TIMEOUT+".\n");
		out.append(" -userAgent <STRING>\n");
		out.append("    This option sets the USER-AGENT-STRING which will be sent as a header in the HTTP request. The default user agent is: "+DEFAULT_USER_AGENT_STR);
		out.append(" -noReferer\n");
		out.append("    Using this option will cause the HTTPLoader not to include a referer field in the header of the request.\n");
		out.append(" -noUserAgent\n");
		out.append("    Using this option will cause the HTTPLoader not to include a user agent field in the header of the request.\n");
		out.append(" -noHost\n");
		out.append("    Using this option will cause the HTTPLoader not to include a host field in the header of the request.\n");
		out.append(" -noAccept\n");
		out.append("    Using this option will cause the HTTPLoader not to include a list of accepted mime types in the header of the request.\n");
		out.append(" -noContentType\n");
		out.append("    Using this option will cause the HTTPLoader not to include a content type field in the header of the request.\n");
		out.append(" -noLF\n");
		out.append("    Using this option will cause the HTTPLoader not to use line feeds (ASCII 13) in the request.\n");
		out.append(" -acceptString <STRING>\n");
		out.append("    This option sets the list of accepted mime types in the header of the HTTP request.\n\n");
		out.append(" -useProxyServer <hostname> <port>\n");
		out.append("    This option uses the given hostname and port as the HTTP proxy server.\n\n");
		out.append(" -useAuthentication <username> <password>\n");
		out.append("    This option uses the BASIC authentication using the given username and password.\n\n");

		return(out.toString());
	}

	private static void setGlobalOptions(HTTPLoader loader, String[] args)
	{
		for(int x=0;x<args.length;x++) {
			if(args[x].equalsIgnoreCase("-autoredirect")) {
				loader.autoRedirect = true;
			}
			else if(args[x].equalsIgnoreCase("-noautoredirect")) {
				loader.autoRedirect = false;
			}
			else if(args[x].equalsIgnoreCase("-allowcookies")) {
				loader.allowForeignCookies = true;
			}
			else if(args[x].equalsIgnoreCase("-noallowcookies")) {
				loader.allowForeignCookies = false;
			}
			else if(args[x].equalsIgnoreCase("-debug")) {
				loader.debug = true;
			}
			else if(args[x].equalsIgnoreCase("-nodebug")) {
				loader.debug = false;
			}
			else if(args[x].equalsIgnoreCase("-verbose")) {
				loader.verbose = true;
			}
			else if(args[x].equalsIgnoreCase("-noverbose")) {
				loader.verbose = false;
			}
			else if(args[x].equalsIgnoreCase("-recursionlimit")) {
				loader.recursionLimit = Integer.parseInt(args[++x]);
			}
			else if(args[x].equalsIgnoreCase("-requesttimeout")) {
				loader.requestTimeout = Integer.parseInt(args[++x]);
			}
			else if(args[x].equalsIgnoreCase("-useragent")) {
				loader.userAgent = args[++x];
			}
			else if(args[x].equalsIgnoreCase("-useProxyServer")) {
				loader.proxyServer = args[++x];
				loader.proxyServerPort = Integer.parseInt(args[++x].trim());
			}
			else if(args[x].equalsIgnoreCase("-noreferer")) {
				loader.useReferer = false;
			}
			else if(args[x].equalsIgnoreCase("-nouseragent")) {
				loader.useUserAgent = false;
			}
			else if(args[x].equalsIgnoreCase("-nohost")) {
				loader.useHost = false;
			}
			else if(args[x].equalsIgnoreCase("-noaccept")) {
				loader.useAccept = false;
			}
			else if(args[x].equalsIgnoreCase("-nocontenttype")) {
				loader.useContentType = false;
			}
			else if(args[x].equalsIgnoreCase("-nolf")) {
				loader.useCarraigeReturnsInRequest = false;
			}
			else if(args[x].equalsIgnoreCase("-acceptstring")) {
				loader.acceptString = args[++x];
			}
			else if(args[x].equalsIgnoreCase("-contenttypestring")) {
				loader.contentTypeString = args[++x];
			}
			else if(args[x].equalsIgnoreCase("-url") ||
					  args[x].equalsIgnoreCase("-parameters") ||
					  args[x].equalsIgnoreCase("-file") ||
					  args[x].equalsIgnoreCase("-outfile")) {
				x++;
			}
			else if(args[x].equalsIgnoreCase("get") ||
					  args[x].equalsIgnoreCase("post") ||
					  args[x].equalsIgnoreCase("-time") ||
					  args[x].equalsIgnoreCase("head") ||
					  args[x].equalsIgnoreCase("put")) {
				;
			}
			else if(args[x].equalsIgnoreCase("-useAuthentication")) {
				loader.useBasicAuthentication(args[++x],args[++x]);
			}
			else {
				System.err.println("WARNING: Unknown option: "+args[x]);
			}
		}
	}

	/*
	public static void main(String[] args)
	{
		HTTPLoader loader = new HTTPLoader();
		Hashtable mimeTypes = new Hashtable();
		mimeTypes.put("param1","text/html");
		mimeTypes.put("param4","application/octet-stream");
		mimeTypes.put("param4_filename","smallfile.bin");
		Props params = new Props();
		params.setProperty("param1","<HTML>\n\nyo\n\n</HTML>\n");
		params.setProperty("param2","value2");
		params.setProperty("param3","value3");
		byte[] data = new byte[5];
		data[0]=0x22;
		data[1]=0x23;
		data[2]=0x24;
		data[3]=0x25;
		data[4]=0x26;
		params.setProperty("param4",data);
		System.out.println(loader.postURL("http://www.tesuji.org:8000/blah",mimeTypes,params));
	}

*/
	public static void main(String[] args)
	{
		if(args.length==0 ||
			args[0].equals("--help")) {
			System.out.println(printHelp());
		}
		else {
			HTTPLoader loader = new HTTPLoader();
			setGlobalOptions(loader, args);

			int index = 0;
			String url = null;
			String file = null;
			boolean showHeaders = false;

			long timer = -1;

			try {
				while(!args[++index].equalsIgnoreCase("-url")) {;}
				url = args[++index];
			}
			catch(Exception e) {
				System.err.println("You must supply a URL. Use the -url option or use the --help option for a list of all the available options.");
				return;
			}

			index = 0;
			try {
				while(!args[++index].equalsIgnoreCase("-outfile")) {;}
				file = args[++index];
			}
			catch(Exception e) {;}

			try {
				while(!args[++index].equalsIgnoreCase("-time")) {;}
				timer = System.currentTimeMillis();
			}
			catch(Exception e) {
				timer = -1;
			}

			try {
				while(!args[++index].equalsIgnoreCase("-headers")) {;}
				showHeaders = true;
			}
			catch(Exception e) {;}

			if(args[0].equalsIgnoreCase("get")) {
				if(file!=null) {
					try {
						File f = new File(file);
						loader.getURL(url,new FileOutputStream(f));
					}
					catch(Exception e) {
						System.err.println("Exception calling getURL()");
						e.printStackTrace(System.err);
					}
				}
				else {
					loader.getURL(url,System.out);
				}				
			}
			else if(args[0].equalsIgnoreCase("post")) {
				String paramfile = null;
				index = 0;
				try {
					while(!args[++index].equalsIgnoreCase("-file")) {;}
					paramfile = args[++index];
				}
				catch(Exception e) {;}
				
				String params = "";
				index = 0;
				try {
					while(!args[++index].equalsIgnoreCase("-parameters")) {;}
					params = args[++index];
				}
				catch(Exception e) {;}
				
				if(file!=null) {
					if(paramfile!=null) {
						try {
							File f = new File(file);
							loader.postURL(url,new File(paramfile),loader.contentTypeString,new FileOutputStream(f));
						}
						catch(Exception e) {
							System.err.println("Exception calling postURL()");
							e.printStackTrace(System.err);
						}
					}
					else {
						try {
							File f = new File(file);
							loader.postURL(url,params,new FileOutputStream(f));
						}
						catch(Exception e) {
							System.err.println("Exception calling postURL()");
							e.printStackTrace(System.err);
						}
					}
				}
				else {
					if(paramfile!=null) {
						try {
							loader.postURL(url,new File(paramfile),loader.contentTypeString,System.out);
						}
						catch(Exception e) {
							System.err.println("Exception calling postURL()");
							e.printStackTrace(System.err);
						}
					}
					else {
						try {
							loader.postURL(url,params,System.out);
						}
						catch(Exception e) {
							System.err.println("Exception calling postURL()");
							e.printStackTrace(System.err);
						}
					}
				}				
			}
			else if(args[0].equalsIgnoreCase("head")) {
				try {
					Props p = loader.getHeaders(url);
					p.dump();
				}
				catch(Exception e) {
					System.err.println("Exception calling getHeaders()");
					e.printStackTrace(System.err);
				}
			}
			else if(args[0].equalsIgnoreCase("put")) {
				String dataFile = null;
				index = 0;
				try {
					while(!args[++index].equalsIgnoreCase("-file")) {;}
					dataFile = args[++index];
				}
				catch(Exception e) {;}
				
				if(dataFile == null) {
					System.err.println("You must supply a file to send to the server. User the -file option or use the --help option for a list of all the available options.");
				}
				else {
					try {
						System.out.println(loader.putURL(url,new File(dataFile),loader.contentTypeString));
					}
					catch(Exception e) {
						System.err.println("Exception calling putURL()");
						e.printStackTrace(System.err);
					}
				}
			}
			else {
				System.out.println(printHelp());
			}

			if(showHeaders) {
				if(loader.response!=null) {
					System.out.println(loader.response.headers);
				}
			}

			//			if(timer!=-1) {
			//				timer = System.currentTimeMillis()-timer;
			//				System.out.println("\n\nRequest time: "+timer+" ms");
			//			}
		}
	}


}
