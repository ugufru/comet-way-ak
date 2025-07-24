
package com.cometway.email;

import com.cometway.props.Props;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;


/**
* Message is an IMessage that represents an RFC822 compliant message.  A Message is
* required to have at least the 'To' and 'From' headers (though these headers may be empty) 
*/

public class Message extends Props implements IMessage, Serializable
{
	public final static String HEADER_NOT_FOUND = "";

	private String message;


	/**
	* A constructor for creating an empty Message with empty 'To' and 'From' fields
	*/

	public Message()
	{
		this("", "", "");
	}


	/**
	* A constructor for creating a Message from a String
	* @param entireMessage A String representing the message body and headers for this Message
	*/

	public Message(String entireMessage)
	{
		MessageTools mt = new MessageTools(this);

		mt.setEntireMessage(entireMessage);
	}


	/**
	* A constructor for creating a Message with the specified 'To' and 'From' fields and message body
	* @param from A String that will be the value of the 'From' header for this Message
	* @param to A String that will be the value of the 'To' header for this Message
	* @param message A String that will be the value of the message body for this Message
	*/

	public Message(String from, String to, String message)
	{
		setHeaderInfo("To", to);
		setHeaderInfo("From", from);

		setMessage(message);
	}


	/**
	* A constructor for creating a Message with the specified headers and message body
	* @param headerInfo A String representing the headers for this Message
	* @param message A String that will be the value of the message body for this Message
	*/


	public Message(String headerInfo, String message)
	{
		MessageTools mt = new MessageTools(this);

		mt.setHeaderInfo(headerInfo);

		setMessage(message);
	}


	/**
	* Given a key, method returns the value of the header referenced by key if any
	* @param key the header name to be extracted
	* @return the header text associated with key, if any
	*/

	public String getHeaderInfo(String key)
	{
		String s = getString(key.toLowerCase());

		if (s == null)
		{
			s = HEADER_NOT_FOUND;
		}

		return (s);
	}


	/**
	* Method yields an Enumeration of header keys for this Message
	* @return an Enumeration of header keys
	*/

	public Enumeration getHeaders()
	{
		Vector v = new Vector();
		HeaderInfo info;

		Vector keys = getKeys();
		int count = keys.size();

		for (int i = 0; i < count; i++)
		{
			String key = (String) keys.get(i);

			info = (HeaderInfo) getProperty(key);

			v.addElement(info.name);
		}

		return (v.elements());
	}


	/**
	* getMessage returns the message body of this Message
	* @return the message body of this Message
	*/


	public String getMessage()
	{
		return (message);
	}


	/**
	* removeAllHeaders removes all headers from this Message
	*/


	public void removeAllHeaders()
	{
		Enumeration e = enumerateKeys();

		while (e.hasMoreElements())
		{
			removeProperty((String) e.nextElement());
		}
	}


	/**
	* setHeaderInfo sets the value of a specified header for this Message, adding the header if necessary
	* @param key the header name to be set
	* @param info the header value
	*/


	public void setHeaderInfo(String key, String info)
	{
		if (key != null)
		{
			if (info != null)
			{
				setProperty(key.toLowerCase(), new HeaderInfo(key, info));
			}
			else
			{
				removeProperty(key.toLowerCase());
			}
		}
		else
		{
			System.out.println("setHeaderInfo: key is null");
		}
	}


	/**
	* setMessage sets the value of the message body for this Message
	* @param s the new message body
	*/


	public void setMessage(String s)
	{
		message = s;
	}


	/**
	* Method yields the String representation of this Message
	*/


	public String toString()
	{
		MessageTools mt = new MessageTools(this);

		return (mt.getHeadersString() + '\n' + message);
	}


	class HeaderInfo
	{
		String name;
		Object value;


		HeaderInfo(String name, Object value)
		{
			this.name = name;
			this.value = value;
		}


		/**
		* This method will return the value as a String. If the value is an
		* instance of a Vector, it will return a comma seperated list of the
		* elements after converting them to Strings.
		*/

		public String toString()
		{
			return (value.toString());
		}
	}

}

