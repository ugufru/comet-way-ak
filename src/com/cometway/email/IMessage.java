
package com.cometway.email;

import java.util.Enumeration;


/**
* IMessage represents an abstract message containing a message body and an arbitrary number of headers.
*/

public interface IMessage
{
	/**
	* getHeaderInfo returns the value of the header associated with key, if any
	* @param key the header name to be extracted
	* @return the header text associated with key, if any
	*/

	public String getHeaderInfo(String key);


	/**
	* getHeaders yields an Enumeration of header keys associated with an IMessage
	* @return an Enumeration of header keys
	*/

	public Enumeration getHeaders();


	/**
	* getMessage returns the message body of an IMessage
	* @return the message body of an IMessage
	*/

	public String getMessage();


	/**
	* removeAllHeaders removes all headers from an IMessage
	*/

	public void removeAllHeaders();


	/**
	* setHeaderInfo sets the value of a specified header for an IMessage, adding the header if necessary
	* @param key the header name to be set
	* @param info the header value
	*/

	public void setHeaderInfo(String key, String info);


	/**
	* setMessage sets the value of the message body for an IMessage
	* @param s the new message body
	*/

	public void setMessage(String s);


	/**
	* toString yields the String representation of an IMessage.  Classes implementing
	* IMessage are required to supply a toString() method
	*/

	public String toString();
}

