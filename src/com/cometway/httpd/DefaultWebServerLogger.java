

package com.cometway.httpd;


import com.cometway.ak.AgentRequest;
import com.cometway.ak.RequestAgent;
import com.cometway.props.PropsContainer;
import com.cometway.util.StringTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
* This agent logs Strings to a time-stamped file.
*/

public class DefaultWebServerLogger extends RequestAgent
{
	protected final static String EOL = System.getProperty("line.separator");

	
	protected int logCount;
	protected BufferedWriter out;
	protected Object logSync;

	protected String[] dontLogCGIParams;

	/** 
	 * <pre>
	 * Initializes this agent's properties by providing default
	 * values for each of the following missing properties:
	 * (default: log_), "logs_per_file" specifies the maximum
	 * log entries per file (default: 10000).
	 *
	 * The log_format is a String that determines the format of the lines in the log file.
	 * Formatting strings are represented with a '%' followed by a keyword. Anything else
	 * will be considered literal. The supported % codes are as follows:
	 *   %a          Remote IP Address.
	 *   %D          The time taken to serve the request, in microseconds.
	 *   %h          Remote hostname.
	 *   %{field}i   The value of the request header field named 'field'. If that field doesn't
	 *               exist, a '-' will be used. For example: %{Referer}i
	 *   %m          The request method.
	 *   %q          The query string (prepended with a ? if a query string exists, otherwise an empty string).
	 *   %r          First line of request
	 *   %{name}R    The value of the property named 'name' in the AgentRequest. If that field
	 *               doesn't exist, a '-' will be used. For example: %{page_name}R
	 *   %s          The HTTP response code for the request (or status).
	 *   %S          A readable string of the status of the HTTP handler that handled the request.
	 *   %t          Time the request was received (standard english format).
	 *   %U          The URL path requested, not including any query string.
	 *   %v          The name of the server the request was for (equivalent to %{Host}i).
	 *   %%          A literal for '%'.
	 * </pre>
	 */

	public void initProps()
	{
		setDefault("service_name","logger_agent");
		setDefault("log_file_dir", "./");
		setDefault("log_file_date_format", "yyyyMMdd-HHmmss");
		setDefault("log_file_suffix", ".log");
		setDefault("logs_per_file","10000");

		// This is a list of CGI params that will NOT get logged
		setDefault("dont_log_cgi_params_list","password");

		setDefault("log_format","%a - %v - [%t] \"%r\" %s %D \"%{Referer}i\" \"%{User-Agent}i\" (%S)");
	}


	/**
	* Creates new logSync object, creates the log file, and registers with the service manager.
	*/
	
	public void start()
	{
		logSync = new Object();

		dontLogCGIParams = StringTools.commaToArray(getString("dont_log_cgi_params_list"));

		createLogFile();

		register();
	}


	/**
	* Writes information given as a String input to log file.
	*/

	public void handleRequest(AgentRequest request)
	{
		synchronized(logSync)
		{
			try
			{
				String logString = createLogString(request);

				if(logString!=null && logString.trim().length()>0) {
					out.write(logString);
					out.write(EOL);
					out.flush();
					logCount++;

					int logs_per_file = getInteger("logs_per_file");
					
					if (logCount > logs_per_file)
					{
						closeLogFile();
						createLogFile();
					}
				}
			}
			catch(Exception e)
			{
				error("log", e);
			}
		}
	}

	/**
	 * Subclasses can overwrite this method for generating the String that gets logged.
	 */
	public String createLogString(AgentRequest request)
	{
		StringBuffer logString = new StringBuffer();

		SimpleDateFormat standardEnglishFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss ZZZZZ");
		Date requestDate = request.getDate("request_date");
		// If the request date isn't given, we'll just use now
		if(requestDate==null) {
			requestDate = new Date();
		}
		String dateString = standardEnglishFormat.format(requestDate);

		// These are properties set by the ConnectionKMethod
		//
		// True if the connection to the client timed out after a keep-alive. If this is true, we typically 
		// don't log anything as it's just normal client/server operation.
		boolean keepAliveTimedOut = request.getBoolean("logger:keepAliveTimedOut");

		// True if the connection was refused for some reason, we may or may not want to log something special
		boolean refused = request.getBoolean("logger:refused");

		// True if the connection timed out and the server was expecting input
		boolean timedOut = request.getBoolean("logger:timedOut");

		// True if there was an overflow in the input, either the request URI was too long or request fields were too long.
		boolean overflowed = request.getBoolean("logger:overflowed");

		// This is the cgi parameters from the request, if there are any. We may want to include them in the logs.
		String cgiParams = request.getString("logger:cgiParams");
		if(cgiParams!=null && cgiParams.length()>0) {
			cgiParams = removeCGIParams(cgiParams);
		}

		// This is the URI path of the request.
		String path = request.getString("logger:path");

		// True if the server is running in multihome mode. 
		boolean multihome = request.getBoolean("logger:multihome");

		// This is the return value of the server's response, if there was one.
		String returnVal = request.getString("logger:returnVal");

		// This is the amount of time in milliseconds it took to handle the request.
		int latency = request.getInteger("logger:latency");

		// This is the InetAddress Object associated with the client. 
		InetAddress cachedInetAddress = (InetAddress)request.getProperty("logger:inetAddress");

		// This is the log snippet from the ConnectionKMethod, it is usually the first line read from the HTTP request
		// which includes the request method (GET/POST/HEAD/etc), the URI, and the HTTP protocol version.
		String handlerLogString = request.getString("logger:logString");

		// This is the PropsContainer created by the ConnectionKMethod which maps all the request header name/value pairs.
		// This is null if no request was sent or if the request was refused outright.
		PropsContainer headerProps = (PropsContainer)request.getProperty("logger:http_headers");

		// This is the entire HTTP request sent by the client.
		String httpReqStr = request.getString("logger:http_request");

		// This is the socket timeout that the webserver is set to use
		int socketTimeout = request.getInteger("logger:socket_timeout");
		
		if(!request.getBoolean("logger:success")) {
			if(httpReqStr.trim().length()>0 && !keepAliveTimedOut) {
				returnVal = "404";
				
				if(timedOut) {
					returnVal = "408";
				}
			}
		}

		// Don't log anything during keep-alive timeout
		if(!keepAliveTimedOut) {
			if(returnVal!=null && returnVal.trim().length()>0) {
				if(latency < socketTimeout) {
					String format = getString("log_format");
					for(int x=0;x<format.length();x++) {
						if(format.charAt(x)=='%') {
							x++;
							if(x>=format.length()) {
								break;
							}
							if(format.charAt(x)=='a') {
								// Remote IP-address
								logString.append(cachedInetAddress.getHostAddress());
							}
							else if(format.charAt(x)=='D') {
								// The time taken to serve the request, in microseconds.
								logString.append(""+latency);
							}
							else if(format.charAt(x)=='h') {
								// Remote hostname
								logString.append(cachedInetAddress.getCanonicalHostName());
							}
							else if(format.charAt(x)=='m') {
								// request method
								if(handlerLogString!=null && handlerLogString.trim().length()>0) {
									int tmp = handlerLogString.indexOf(" ");
									if(tmp!=-1) {
										logString.append(handlerLogString.substring(0,tmp));
									}
									else {
										logString.append("-");
									}
								}
								else {
									logString.append("-");
								}
							}
							else if(format.charAt(x)=='q') {
								// The query string
								if(cgiParams!=null && cgiParams.trim().length()>0) {
									logString.append("?");
									logString.append(cgiParams);
								}
							}
							else if(format.charAt(x)=='r') {
								// First line of request
								if(handlerLogString!=null && handlerLogString.trim().length()>0) {
									logString.append(handlerLogString);
								}
								else {
									logString.append("-");
								}
							}
							else if(format.charAt(x)=='s') {
								// The response code
								if(returnVal!=null && returnVal.trim().length()>0) {
									logString.append(returnVal);
								}
								else {
									logString.append("-");
								}
							}
							else if(format.charAt(x)=='S') {
								// Readable status string
								if(refused) {
									logString.append("request refused");
								}
								else if(timedOut) {
									logString.append("request timed out");
								}
								else if(overflowed) {
									logString.append("request was too long");
								}
								else {
									logString.append("request successfully handled");
								}
							}
							else if(format.charAt(x)=='t') {
								// Time the request was received
								logString.append(dateString);
							}
							else if(format.charAt(x)=='U') {
								// The URL path requested, not including any query string.
								if(path!=null && path.trim().length()>0) {
									String tmpPath = path;
									if(path.indexOf("?")!=-1) {
										tmpPath = path.substring(0,path.indexOf("?"));
									}
									logString.append(path);
								}
								else {
									logString.append("-");
								}
							}
							else if(format.charAt(x)=='v') {
								// The name of the server the request was for
								if(headerProps!=null) {
									if(headerProps.getProperty("host") != null) {
										logString.append(headerProps.getProperty("host").toString());
									}
									else {
										logString.append("-");
									}
								}
								else {
									logString.append("-");
								}
							}
							else if(format.charAt(x)=='%') {
								// literal %
								logString.append("%");
							}
							else if(format.charAt(x)=='{') {
								// { }  name
								int end = format.indexOf("}",x);
								if(end!=-1) {
									String fieldName = format.substring(x+1,end);
									x = end+1;
									if(x>=format.length()) {
										// end of format string, we need to know what the code is, we'll do nothing
										break;
									}
									if(format.charAt(x)=='i') {
										// header value
										if(headerProps!=null) {
											if(headerProps.getProperty(fieldName.toLowerCase()) != null) {
												logString.append(headerProps.getProperty(fieldName.toLowerCase()).toString());
											}
											else {
												logString.append("-");
											}
										}
										else {
											logString.append("-");
										}
									}
									else if(format.charAt(x)=='R') {
										// AgentRequest value
										if(request.hasProperty(fieldName)) {
											logString.append(request.getString(fieldName));
										}
										else {
											logString.append("-");
										}
									}
									else {
										// this is an unrecognized code, we'll do nothing.
									}
								}
								else {
									// If there isn't a closing }, we'll just consider it a literal
									logString.append("%{");
								}									
							}
							else {
								// Unknown code, we'll just consider it a literal
								logString.append("%");
								logString.append(format.charAt(x));
							}
						}
						else {
							logString.append(format.charAt(x));
						}
					}
				}
			}
		}

		return(logString.toString());
	}

	/**
	 * Returns a String with the certain CGI parameters removed based on the dontLogCGIParams array
	 */
	protected String removeCGIParams(String cgiParams)
	{
		StringBuffer rval = new StringBuffer();

		int index = cgiParams.indexOf("&");
		while(index!=-1) {
			String paramPair = cgiParams.substring(0,index);
			String paramName = paramPair;
			cgiParams = cgiParams.substring(index+1);

			int equals = paramPair.indexOf("=");
			if(equals!=-1) {
				paramName = paramPair.substring(0,equals);
			}

			boolean log = true;
			for(int x=0;x<dontLogCGIParams.length;x++) {
				if(paramName.equalsIgnoreCase(dontLogCGIParams[x])) {
					log = false;
					break;
				}
			}

			if(log) {
				if(rval.length()>0) {
					rval.append("&");
				}
				rval.append(paramPair);
			}

			index = cgiParams.indexOf("&");
		}
		if(cgiParams.length()>0) {
			int equals = cgiParams.indexOf("=");
			String paramName = cgiParams;
			if(equals!=-1) {
				paramName = cgiParams.substring(0,equals);
			}

			boolean log = true;
			for(int x=0;x<dontLogCGIParams.length;x++) {
				if(paramName.equalsIgnoreCase(dontLogCGIParams[x])) {
					log = false;
					break;
				}
			}

			if(log) {
				if(rval.length()>0) {
					rval.append("&");
				}
				rval.append(cgiParams);
			}
		}

		return(rval.toString());
	}

	
	/**
	* Initializes log File / checks to see if matching file already exists.
	*/
	protected void createLogFile()
	{
		String filename = null;

		try
		{
			Date now = new Date();
			String log_file_dir = getTrimmedString("log_file_dir");
			String log_file_suffix = getTrimmedString("log_file_suffix");
			SimpleDateFormat sdf = new SimpleDateFormat(getTrimmedString("log_file_date_format"));
			filename = log_file_dir + sdf.format(now) + log_file_suffix;

			File file = new File(log_file_dir, sdf.format(now) + log_file_suffix);

			closeLogFile();
			
			out = new BufferedWriter(new FileWriter(file));
			
			logCount = 0;
		}
		catch (Exception e)
		{
			error("Could not create log file: " + filename, e);
		}
	}


	/**
	* Properly closes the currently open log file.
	*/
	protected void closeLogFile()
	{
		if (out == null) return;

		try
		{
			out.close();
		}
		catch(Exception ex)
		{
			error("Could not close BufferedWriter", ex);
		}
		finally
		{
			out = null;
		}
	}
}

