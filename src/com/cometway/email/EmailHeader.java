
package com.cometway.email;

import java.lang.*;
import java.util.*;


/**
 * This class provides a set of static methods for parsing and manipulating headers 
 * in an email message.
 *
 */
public class EmailHeader
{
	/**
	* This class method takes a String, a list of users separated by comments, and 
	* removes the special header characters, such as (Comment), <one-machine-usable-reference>,
	* [domain-literal]. It also does not checks to make sure after these have been removed, that it is
	* somewhat of a valid address. A valid address contains an \"@\" and at least one \".\"
	* @param toLine This is the To: line in the emailheader. This may or maynot include the "TO:"
	* @return an enumeration of users as strings
	*/

	public static Enumeration getSendToUsers(String toLine)
	{
		Vector  rval = new Vector();

		if (toLine.toLowerCase().startsWith("to:"))
		{
			toLine = toLine.substring(3);
		}

		toLine = toLine.trim();

		StringTokenizer st = new StringTokenizer(toLine, ",", false);
		String		user;

		while (st.hasMoreTokens())
		{
			user = st.nextToken();

			while ((user.indexOf("<") != -1) && (user.indexOf(">") != -1))
			{
				user = user.substring(user.indexOf("<") + 1, user.indexOf(">")).trim();
			}		// System.out.println("user ="+user+"  index of (="+user.indexOf("(")+" index of )="+user.indexOf(")"));

			while ((user.indexOf("(") != -1) && (user.indexOf(")") != -1))
			{
				user = user.substring(0, user.indexOf("(")).trim() + user.substring(user.indexOf(")") + 1).trim();
			}

			while ((user.indexOf("[") != -1) && (user.indexOf("]") != -1))
			{
				user = user.substring(0, user.indexOf("[")).trim() + user.substring(user.indexOf("]") + 1).trim();
			}		// if((user.indexOf("@")!=-1)&&(user.indexOf(".")!=-1)) {

			rval.addElement(removeWhiteSpace(user.trim()));		// }
		}

		return (rval.elements());
	}


	/**
	* This class method takes a String, a user, and 
	* removes the special header characters, such as (Comment), <one-machine-usable-reference>,
	* [domain-literal]. It also does not  checks to make sure after these have been removed, that it is
	* somewhat of a valid address. A valid address contains an \"@\" and at least one \".\"
	* This is the same as getSendToUsers(String) method except this only expects one user
	* @param toLine This is the To: line in the emailheader. This may or maynot include the "TO:"
	* @return a String with the username and host
	*/


	public static String getSendToUser(String toLine)
	{
		if (toLine.toLowerCase().startsWith("to:"))
		{
			toLine = toLine.substring(3);
		}

		toLine = toLine.trim();

		String  user;

		user = toLine;

		while ((user.indexOf("<") != -1) && (user.indexOf(">") != -1))
		{
			user = user.substring(user.indexOf("<") + 1, user.indexOf(">")).trim();
		}		// System.out.println("user ="+user+"  index of (="+user.indexOf("(")+" index of )="+user.indexOf(")"));

		while ((user.indexOf("(") != -1) && (user.indexOf(")") != -1))
		{
			user = user.substring(0, user.indexOf("(")).trim() + user.substring(user.indexOf(")") + 1).trim();
		}

		while ((user.indexOf("[") != -1) && (user.indexOf("]") != -1))
		{
			user = user.substring(0, user.indexOf("[")).trim() + user.substring(user.indexOf("]") + 1).trim();
		}

		return (removeWhiteSpace(user));
	}


	public static String removeWhiteSpace(String input)
	{
		StringBuffer    rval = new StringBuffer();
		int		ceiling = input.length();

		for (int x = 0; x < ceiling; x++)
		{
			if (((int) (input.charAt(x))) > 32)
			{
				rval.append("" + input.charAt(x));
			}
		}

		return (rval.toString());
	}


	public static void main(String[] args)
	{
		String  shit = "This should \n have all\r the whitespace   \t should be gone   \n..";

		System.out.println(shit + "\n\n\nNO WS:\n" + removeWhiteSpace(shit));
		System.out.println("Code for space = " + (int) (' ') + " Code for <CR> = " + (int) ('\n'));
		System.out.println(EmailHeader.getSendToUser("<r  eg u l  ar  @ a d d r e  s  s.(comment) (comment)shit>"));
	}


}

