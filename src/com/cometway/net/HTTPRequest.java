/*--- formatted by Jindent 2.1, (www.c-lab.de/~jindent) ---*/

package com.cometway.net;

import java.util.*;
import java.io.*;



/**
 * This class is used as a data object which stores the HTTP/1.0 request.
 * This class will provide the HTTP/1.0 request in String form. This class
 * is used by the HTTPLoader.
 */
public class HTTPRequest
{


/**
 * * This is the type for a full GET request
 */

	public static final int GET_REQUEST_TYPE = 0;


	/**
	 * * This is the type for a POST request
	 */

	public static final int POST_REQUEST_TYPE = 1;


	/**
	 * * This is the type for a short GET request
	 */

	public static final int SHORT_GET_REQUEST_TYPE = 2;

	public static final int HEAD_REQUEST_TYPE = 3;

	public static final int PUT_REQUEST_TYPE = 4;


	/**
	 * * This field is to indicate what request this is
	 */

	public int		requestType;


	/**
	 * * This goes after the request type
	 */

	public String		requestString;


	/**
	 * * This is the request data, which appears after the header.
	 */

	public String		requestData;
	public InputStream dataSource;
	public long requestDataLength;


	/**
	 * * This is the CLOSE connection type
	 */

	public static final int CLOSE_CONNECTION = 0;


	/**
	 * * This is the KEEP ALIVE connection type
	 */

	public static final int KEEP_ALIVE_CONNECTION = 1;


	/**
	 * * This field is to indicate the connection type
	 */

	public int		connection;


	/**
	 * * This is the referer field.
	 */

	public String		referer;


	/**
	 * * This is the user agent field.
	 */

	public String		userAgent;


	/**
	 * * This is the host field of the request.
	 */

	public String		host;


	/**
	 * * This is the String of what media to accept.
	 */

	public String		acceptString;


	/**
	 * * This is the content type of the request data.
	 */

	public String		contentType;


	/**
	 * * This is the content encoding of the request data.
	 */

	public String		contentEncoding;


	/**
	 * * This is a Vector of Strings for user specific headers
	 */

	public Vector		otherHeaders;


    /**
     * This is the BASIC authentication to use
     */
    public String authString;

	public boolean useCarraigeReturns = false;

	/**
	 * * This generates a simple HTTP request using the request string given and a SHORT GET type.
	 */

	public HTTPRequest(String request)
	{
		requestString = request;
		requestType = SHORT_GET_REQUEST_TYPE;
	}


	/**
	 * * This method adds a user specific header.
	 */


	public void addHeader(String name, String value)
	{
		if (otherHeaders == null)
		{
			otherHeaders = new Vector();
		}

		otherHeaders.addElement(name.trim() + ": " + value.trim());
	}


	/**
	 * * This method adds a user specific header.
	 */


	public void addHeader(String header)
	{
		if (otherHeaders == null)
		{
			otherHeaders = new Vector();
		}

		otherHeaders.addElement(header);
	}


	public boolean equals(Object o)
	{
		if (o instanceof HTTPRequest)
		{
			if (o.toString().equals(toString()))
			{
				return (true);
			}
		}

		return (false);
	}


	public boolean writeRequest(OutputStream out)
	{
		boolean rval = false;
		if(dataSource==null) {
			try {
				String requestString = toString();
				while(requestString.length()>512) {
					out.write(requestString.substring(0,512).getBytes());
					out.flush();
					requestString = requestString.substring(512);
				}
				if(requestString.length()>0) {
					out.write(requestString.getBytes());
					out.flush();
				}
				rval = true;
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				out.write(toString().getBytes());
				int count = 0;
				while(count<requestDataLength) {
					int data = dataSource.read();
					count++;
					out.write(data);
				}
				out.flush();
				rval = true;
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		return(rval);
	}



	/**
	 * * This method builds and prints the request to a String.
	 */


	public String toString()
	{
		StringBuffer    buffer = new StringBuffer();

		if (requestType == GET_REQUEST_TYPE)
		{
			buffer.append("GET ");
		}
		else if (requestType == POST_REQUEST_TYPE)
		{
			buffer.append("POST ");
		}
		else if (requestType == SHORT_GET_REQUEST_TYPE)
		{
			buffer.append("GET ");

			if (requestString == null)
			{
				if(useCarraigeReturns) {
					buffer.append("/\r\n\r\n");
				}
				else {
					buffer.append("/\n\n");
				}
			}
			else
			{
				buffer.append(requestString.trim());
				if(useCarraigeReturns) {
					buffer.append("\r\n\r\n");
				}
				else {
					buffer.append("\n\n");
				}
			}

			return (buffer.toString());
		}
		else if (requestType == HEAD_REQUEST_TYPE) 
		{
			buffer.append("HEAD ");
		}			
		else if(requestType == PUT_REQUEST_TYPE) 
		{
			buffer.append("PUT ");
		}

		if (requestString == null)
		{
			buffer.append("/");
		}
		else
		{
			buffer.append(requestString.trim());
		}

		if(useCarraigeReturns) {
			buffer.append(" HTTP/1.0\r\n");
		}
		else {
			buffer.append(" HTTP/1.0\n");
		}

		if (connection == CLOSE_CONNECTION)
		{
			if(useCarraigeReturns) {
				buffer.append("Connection: close\r\n");
			}
			else {
				buffer.append("Connection: close\n");
			}
		}
		else
		{
			if(useCarraigeReturns) {
				buffer.append("Connection: Keep-Alive\r\n");
			}
			else {
				buffer.append("Connection: Keep-Alive\n");
			}
		}

		if (referer != null && referer.trim().length()>0)
		{
			buffer.append("Referer: ");
			buffer.append(referer.trim());
			if(useCarraigeReturns) {
				buffer.append("\r");
			}
			buffer.append("\n");
		}

		if (userAgent != null)
		{
			buffer.append("User-Agent: ");
			buffer.append(userAgent.trim());
			if(useCarraigeReturns) {
				buffer.append("\r");
			}
			buffer.append("\n");
		}

		if (host != null)
		{
			buffer.append("Host: ");
			buffer.append(host.trim());
			if(useCarraigeReturns) {
				buffer.append("\r");
			}
			buffer.append("\n");
		}

		if (acceptString != null)
		{
			buffer.append("Accept: ");
			buffer.append(acceptString.trim());
			if(useCarraigeReturns) {
				buffer.append("\r");
			}
			buffer.append("\n");
		}

		if (contentType != null)
		{
			buffer.append("Content-type: ");
			buffer.append(contentType.trim());
			if(useCarraigeReturns) {
				buffer.append("\r");
			}
			buffer.append("\n");
		}

		if (contentEncoding != null)
		{
			buffer.append("Content-encoding: ");
			buffer.append(contentEncoding.trim());
			if(useCarraigeReturns) {
				buffer.append("\r");
			}
			buffer.append("\n");
		}

		if (authString != null) {
		    buffer.append("Authorization: Basic "+authString);
		    if(useCarraigeReturns) {
			buffer.append("\r");
		    }
		    buffer.append("\n");
		}

		if (otherHeaders != null)
		{
			for (int x = 0; x < otherHeaders.size(); x++)
			{
				buffer.append(((String) otherHeaders.elementAt(x)).trim());
			if(useCarraigeReturns) {
				buffer.append("\r");
			}
				buffer.append("\n");
			}
		}

		if(dataSource!=null) {
			buffer.append("Content-length: "+requestDataLength);
			if(useCarraigeReturns) {
				buffer.append("\r\n\r\n");
			}
			else {
				buffer.append("\n\n");
			}
		}
		else if (requestData != null)
		{
			buffer.append("Content-length: ");
			buffer.append(requestData.length());
			if(useCarraigeReturns) {
				buffer.append("\r\n\r\n");
			}
			else {
				buffer.append("\n\n");
			}
			buffer.append(requestData);
			if(useCarraigeReturns) {
				buffer.append("\r");
			}
			buffer.append("\n");
		}
		else
		{
			if(useCarraigeReturns) {
				buffer.append("\r");
			}
			buffer.append("\n");
		}

		return (buffer.toString());
	}


}

