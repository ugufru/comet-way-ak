
package com.cometway.httpd;

import java.io.*;
import java.util.*;
import com.cometway.ak.*;
import com.cometway.util.*;
import com.cometway.props.Props;

/**
* HTMLFormWriter is a tool for writing simple HTML-based forms
* to a StringBuffer or AgentRequest output stream. Using this class
* it is relatively simple to create generic forms without worrying
* about the HTML code.
*/

public class HTMLFormWriter
{
	protected StringBuffer buffer;
	protected AgentRequest agentRequest;


	/**
	* Use this constructor to write HTML forms directly to an AgentRequest.
	*/

	public HTMLFormWriter(AgentRequest agentRequest)
	{
		this.agentRequest = agentRequest;
	}


	/**
	* Use this constructor to write HTML forms to a StringBuffer.
	*/

	public HTMLFormWriter(StringBuffer b)
	{
		this.buffer = b;
	}


	/**
	* Returns the AgentRequest assigned to this FormWriter.
	*/

	public AgentRequest getAgentRequest()
	{
		return (agentRequest);
	}

		
	/**
	* Converts the integer to a String and writes it to the output.
	*/
	
	public void print(int i) throws IOException
	{
		print(Integer.toString(i));
	}


	/**
	* Prints the String to the output.
	*/

	public void print(String s) throws IOException
	{
		if (agentRequest != null)
		{
			agentRequest.print(s);
		}
		else
		{
			buffer.append(s);
		}
	}

	/**
	* Converts the integer to a String and writes it to the output
	* followed by a carriage return.
	*/

	public void println(int i) throws IOException
	{
		println(Integer.toString(i));
	}


	/**
	* Prints the String to the output followed by a carriage return.
	*/

	public void println(String s) throws IOException
	{
		if (agentRequest != null)
		{
			agentRequest.println(s);
		}
		else
		{
			buffer.append(s + "\n");
		}
	}

	/**
	* Writes three menus on the same line to represent month, date, and year for choosing a date.
	*/

	public void writeAddressFields(String name, Props p, int size) throws IOException
	{
		String city = p.getString(name + "_city");
		String state = p.getString(name + "_state");
		String zip = p.getString(name + "_zip");

		writeMultilineField("Address", name + "_street", p, size, 3);
		
		println(" <TR>");
		println("  <TD ALIGN=RIGHT>City:</TD>\n");
		println("  <TD NOWRAP><INPUT name=\"" + name + "_city" + "\" value=\"" + encode(city) + "\" size=" + size + ">");
		println(" State: <SELECT name=\"" + name + "_state\">");
		
		writeStateSelectItems(state);

		println("</SELECT>");
		println(" Zip:<INPUT name=\"" + name + "_zip" + "\" value=\"" + encode(zip) + "\" size=\"7\">");
		println("  </TD></TR>");
	}



	/**
	* Adds a centered caption to the form output.
	*/

	public void writeCaption(String caption) throws IOException
	{
		println("<TR><TD ALIGN=CENTER COLSPAN=2>" + caption + "</TD></TR>");
	}


	/**
	* Writes a checkbox to the form output.
	*/
	
	public void writeCheckbox(String label, String name, boolean checked) throws IOException
	{
		println("<TR><TD ALIGN=RIGHT VALIGN=BOTTOM>" + label + ":</TD>");

		if (checked)
		{
			println("<TD VALIGN=BOTTOM><INPUT type=CHECKBOX name=\"" + encode(name) + "\" CHECKED></TD>\n</TR>");
		}
		else
		{
			println("<TD VALIGN=BOTTOM><INPUT type=CHECKBOX name=\"" + encode(name) + "\"></TD>\n</TR>");
		}
	}


	/**
	* Writes a checkbox to the form output and includes a title attribute.
	*/
	
	public void writeCheckbox(String label, String name, boolean checked, String title) throws IOException
	{
		println("<TR><TD ALIGN=RIGHT VALIGN=BOTTOM>" + label + ":</TD>");

		if (checked)
		{
			println("<TD VALIGN=BOTTOM><INPUT type=CHECKBOX name=\"" + encode(name) + "\" CHECKED></TD>\n</TR>");
		}
		else
		{
			println("<TD VALIGN=BOTTOM><INPUT type=CHECKBOX name=\"" + encode(name) + "\"></TD>\n</TR>");
		}
	}


	/**
	* Writes three menus on the same line to represent month, date, and year for choosing a date.
	*/

	public void writeDateFields(String label, String name, Props p) throws IOException
	{
		println("<TR>\n<TD ALIGN=RIGHT>" + label + ":</TD>\n<TD NOWRAP>");


		// Figure out what year, month, and day to display in the OPTIONS.

		Calendar cal = Calendar.getInstance();
		int lastYear = cal.get(cal.YEAR) + 5;
		Date d = p.getDate(name);

		if (d != null) cal.setTime(d);

		int year = cal.get(cal.YEAR);
		int month = cal.get(cal.MONTH);
		int day = cal.get(cal.DATE);


		// Write the SELECT and OPTION tags for month.

		println("<SELECT name=\"" + name + "_month\">");

		writeSelectItem("Jan", 0, month);
		writeSelectItem("Feb", 1, month);
		writeSelectItem("Mar", 2, month);
		writeSelectItem("Apr", 3, month);
		writeSelectItem("May", 4, month);
		writeSelectItem("Jun", 5, month);
		writeSelectItem("Jul", 6, month);
		writeSelectItem("Aug", 7, month);
		writeSelectItem("Sep", 8, month);
		writeSelectItem("Oct", 9, month);
		writeSelectItem("Nov", 10, month);
		writeSelectItem("Dec", 11, month);

		println("</SELECT>");


		// Write the SELECT and OPTION tags for day.

		println("<SELECT name=\"" + name + "_day\">");

		for (int i = 1; i <= 31; i++)
		{
			writeSelectItem("" + i, i, day);
		}

		println("</SELECT>");


		// Write the SELECT and OPTION tags for year.

		println("<SELECT name=\"" + name + "_year\">");

		for (int i = 1895; i <= lastYear; i++)
		{
			writeSelectItem("" + i, i, year);
		}

		println("</SELECT>");


		println("</TD>");
	}


	/**
	* Writes the form footer to the output. Call this method last to properly terminate the form HTML.
	*/
	
	public void writeFooter() throws IOException
	{
		println("</TABLE>\n</FORM>");
	}


	/**
	* Writes the form header to the output. Call this method first to properly begin the form HTML.
	* This creates a POST type request to the specified submitURL.
	*/
	
	public void writeHeader(String submitURL) throws IOException
	{
		println("<FORM method=POST action=" + submitURL + ">");
		println("<TABLE WIDTH=100%>");
	}


	/**
	* Writes the form header to the output. Call this method first to properly begin the form HTML.
	* This creates a POST type request to the specified submitURL.
	* Hidden "login_name" and "login_hash" parameters are added to the form for the specified
	* userProps.
	*/
	
	public void writeHeader(String submitURL, Props userProps) throws IOException
	{
		writeHeader(submitURL);
		writeLoginFields(userProps);
	}


	/**
	* Adds a help description over the fields.
	*/

	public void writeHelp(String message) throws IOException
	{
		println("<TR><TD></TD><TD>" + message + "</TD></TR>");
	}


	/**
	* Writes appropriate HTML to the output to add vertical space between form items.  
	*/
	
	public void writeSpace() throws IOException
	{
		println("<TR><TD COLSPAN=2>&nbsp;</TD></TR>");
	}


	/**
	* Writes a standard input field to the form output.
	*/
	
	public void writeField(String label, String name, int size) throws IOException
	{
		writeField(label, name, "", size);
	}


	/**
	* Writes a standard input field to the form output displaying the specified value.
	*/
	
	public void writeField(String label, String name, String value, int size) throws IOException
	{
		println("<TR>\n<TD ALIGN=RIGHT NOWRAP>" + label + ":</TD>");
		println("<TD><INPUT name=\"" + name + "\" value=\"" + encode(value) + "\" size=" + size + "></TD>\n</TR>");
	}

	/**
	* Writes a standard input field to the form output displaying the value
	* as retrieved from the Props using the specified name as the Props key.
	*/
	
	public void writeField(String label, String name, Props p, int size) throws IOException
	{
		writeField(label, name, p.getString(name), size);
	}


	/**
	* Writes a FILE input field to the form output displaying the specified value.
	*/
	
	public void writeFileUpload(String label, String name) throws IOException
	{
		println("<TR>\n<TD ALIGN=RIGHT NOWRAP>" + label + ":</TD>");
		println("<TD><INPUT type=\"FILE\" name=\"" + name + "\"></TD>\n</TR>");
	}


	/**
	* Writes a hidden input parameter to the form output.
	*/
	
	public void writeHiddenField(String name, String value) throws IOException
	{
		println("<INPUT type=HIDDEN name=\"" + name + "\" value=\"" + encode(value) + "\">");
	}


	/**
	* Writes a hidden input parameter to the form output
	* as retrieved from the Props using the specified name as the Props key.
	*/
	
	public void writeHiddenField(String name, Props p) throws IOException
	{
		writeHiddenField(name, p.getString(name));
	}


	/**
	* Hidden "login_name" and "login_hash" parameters are added to the form output
	* using the specified name and password.
	*/

	public void writeLoginFields(String name, String password) throws IOException
	{
		writeHiddenField("login_user", name);
		writeHiddenField("login_hash", String.valueOf(password.hashCode()));

	}


	/**
	* Hidden "login_name" and "login_hash" parameters are added to the form output
	* using the "name" and "password" values of the specified Props.
	*/
	
	public void writeLoginFields(Props p) throws IOException
	{
		writeLoginFields(p.getString("name"), p.getString("password"));
	}


	/**
	* Writes a multiline field (TEXTAREA) to the form output.
	*/
	
	public void writeMultilineField(String label, String name, int columns, int rows) throws IOException
	{
		writeMultilineField(label, name, "", columns, rows);
	}


	/**
	* Writes a multiline field (TEXTAREA) to the form output displaying the specified value.
	*/
	
	public void writeMultilineField(String label, String name, String value, int columns, int rows) throws IOException
	{
//		if (rows < 3)
//		{
//			rows = 3;	// Avoids IE 4.01 Mac crashing problem.
//		}

		println("<TR>\n<TD ALIGN=RIGHT NOWRAP>" + label + ":</TD>");
		println("<TD><TEXTAREA name=\"" + name + "\" cols=" + columns + " rows=" + rows + ">" + encode(value) + "</TEXTAREA></TD>\n</TR>");
	}


	/**
	* Writes a multiline field (TEXTAREA) to the form output displaying the specified value
	* as retrieved from the Props using the specified name as a key.
	*/
	
	public void writeMultilineField(String label, String name, Props p, int columns, int rows) throws IOException
	{
		writeMultilineField(label, name, p.getString(name), columns, rows);
	}


	/**
	* Writes a password input field to the form output.
	*/

	public void writePassword(String label, String name, int size) throws IOException
	{
		writePassword(label, name, "", size);
	}


	/**
	* Writes a password input field to the form output containing the specified value.
	*/
	
	public void writePassword(String label, String name, String value, int size) throws IOException
	{
		println("<TR>\n<TD ALIGN=RIGHT NOWRAP>" + label + ":</TD>");
		println("<TD><INPUT type=password name=\"" + name + "\" value=\"" + value + "\" size=" + size + "></TD>\n</TR>");
	}


	/**
	* Writes a table containing the Props keys and associated values to the output.
	* This is useful for debugging the contents of AgentRequest Props.
	*/
	
	public void writeProps(Props p) throws IOException
	{
		Enumeration e = p.enumerateKeys();

		println("<TABLE BORDER>");
		println("<TR>\n<TH>Key<TH>Value<TH>Class");

		while (e.hasMoreElements())
		{
			String	key = (String) e.nextElement();

			print("<TR><TD>");
			print(key);
			print("</TD><TD><PRE>");
			print(p.getString(key));
			print("</PRE></TD><TD>");
			print(p.getProperty(key).getClass().getName());
			println("</TD></TR>");
		}

		println("</TABLE>");
	}


	/**
	* Writes a reset button and description to the form output that resets changes to the form when pressed.
	*/
	
	public void writeResetButton(String title, String description) throws IOException
	{
		println("<TR>\n<TD ALIGN=RIGHT><INPUT type=reset value=\"" + encode(title) + "\"></TD>");
		println("<TD>" + description + "</TD>\n</TR>");
	}


	/**
	* Writes the footer HTML for a SELECT to the form output.
	*/
	
	public void writeSelectFooter() throws IOException
	{
		println("</SELECT></TD>\n</TR>");
	}


	/**
	* Writes the header HTML for a SELECT to the form output to display a popup menu
	* or selection list. The multiple parameter should be set to false for a popup; set it
	* to true to create a selection list. A call to this method
	* is typically followed by multiple calls to writeSelectItem followed by a call
	* to writeSelectFooter to terminate the SELECT.
	*/
	
	public void writeSelectHeader(String label, String name, boolean multiple) throws IOException
	{
		println("<TR>\n<TD ALIGN=RIGHT>" + label + ":</TD>");

		if (multiple)
		{
			println("<TD><SELECT name=" + name + " multiple>");
		}
		else
		{
			println("<TD><SELECT name=" + name + ">");
		}
	}


	/**
	* Writes a SELECT OPTION to the form output. Calls to this method must be
	* surrounded by calls to writeSelectHeader and writeSelectFooter.
	*/
	
	public void writeSelectItem(String name, String value, boolean selected) throws IOException
	{
		if (selected)
		{
			println("<OPTION value=\"" + value + "\" selected>" + name + "</OPTION>");
		}
		else
		{
			println("<OPTION value=\"" + value + "\">" + name + "</OPTION>");
		}
	}


	/**
	* Writes a SELECT OPTION to the form output. If the value is equal to the currentSelection
	* the item is selected; otherwise it is not selected. Calls to this method must be
	* surrounded by calls to writeSelectHeader and writeSelectFooter.
	*/
	
	public void writeSelectItem(String name, String value, String currentSelection) throws IOException
	{
		writeSelectItem(name, value, currentSelection.equals(value));
	}


	/**
	* Writes a SELECT OPTION to the form output. If the value is equal to the currentSelection
	* the item is selected; otherwise it is not selected. Calls to this method must be
	* surrounded by calls to writeSelectHeader and writeSelectFooter.
	*/
	
	public void writeSelectItem(String name, int value, int currentSelection) throws IOException
	{
		writeSelectItem(name, "" + value, (currentSelection == value));
	}


	/**
	* Writes a list of SELECT OPTIONs to the form output based on the specified Props List.
	*/
	
	public void writeSelectItemList(String label, String name, List propsList, String nameKey, String valueKey, String currentValue) throws IOException
	{
		writeSelectHeader(label, name, false);

		int count = propsList.size();

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) propsList.get(i);
			String title = p.getTrimmedString(nameKey);
			String value = p.getTrimmedString(valueKey);

			writeSelectItem(title, value, currentValue);
		}

		writeSelectFooter();
	}


	/**
	* Writes a SELECT and OPTIONS for selecting a US state code.
	*/

	public void writeSelectUSState(String label, String name, String value) throws IOException
	{
		writeSelectHeader(label, name, false);
		writeStateSelectItems(value);
		writeSelectFooter();
	}


	/**
	* Writes select items for all 50 US states.
	*/

	public void writeStateSelectItems(String state) throws IOException
	{
		if (true)
		{
			writeSelectItem("-", "-", false);
			writeSelectItem("AL", "AL", state);
			writeSelectItem("AK", "AK", state);
			writeSelectItem("AR", "AR", state);
			writeSelectItem("AZ", "AZ", state);
			writeSelectItem("CA", "CA", state);
			writeSelectItem("CO", "CO", state);
			writeSelectItem("CT", "CT", state);
			writeSelectItem("DE", "DE", state);
			writeSelectItem("DC", "DC", state);
			writeSelectItem("FL", "FL", state);
			writeSelectItem("GA", "GA", state);
			writeSelectItem("HI", "HI", state);
			writeSelectItem("IA", "IA", state);
			writeSelectItem("ID", "ID", state);
			writeSelectItem("IL", "IL", state);
			writeSelectItem("IN", "IN", state);
			writeSelectItem("KS", "KS", state);
			writeSelectItem("KY", "KY", state);
			writeSelectItem("LA", "LA", state);
			writeSelectItem("MA", "MA", state);
			writeSelectItem("MD", "MD", state);
			writeSelectItem("ME", "ME", state);
			writeSelectItem("MI", "MI", state);
			writeSelectItem("MN", "MN", state);
			writeSelectItem("MO", "MO", state);
			writeSelectItem("MS", "MS", state);
			writeSelectItem("MT", "MT", state);
			writeSelectItem("NC", "NC", state);
			writeSelectItem("ND", "ND", state);
			writeSelectItem("NE", "NE", state);
			writeSelectItem("NH", "NH", state);
			writeSelectItem("NJ", "NJ", state);
			writeSelectItem("NM", "NM", state);
			writeSelectItem("NV", "NV", state);
			writeSelectItem("NY", "NY", state);
			writeSelectItem("OH", "OH", state);
			writeSelectItem("OK", "OK", state);
			writeSelectItem("OR", "OR", state);
			writeSelectItem("PA", "PA", state);
			writeSelectItem("RI", "RI", state);
			writeSelectItem("SC", "SC", state);
			writeSelectItem("SD", "SD", state);
			writeSelectItem("TN", "TN", state);
			writeSelectItem("TX", "TX", state);
			writeSelectItem("UT", "UT", state);
			writeSelectItem("VA", "VA", state);
			writeSelectItem("VT", "VT", state);
			writeSelectItem("WA", "WA", state);
			writeSelectItem("WI", "WI", state);
			writeSelectItem("WV", "WV", state);
			writeSelectItem("WY", "WY", state);
		}
		else
		{
			writeSelectItem("AL - Alabama", "AL", state);
			writeSelectItem("AK - Alaska", "AK", state);
			writeSelectItem("AR - Arkansas", "AR", state);
			writeSelectItem("AZ - Arizona", "AZ", state);
			writeSelectItem("CA - California", "CA", state);
			writeSelectItem("CO - Colorado", "CO", state);
			writeSelectItem("CT - Connecticut", "CT", state);
			writeSelectItem("DE - Delaware", "DE", state);
			writeSelectItem("DC - Washington DC", "DC", state);
			writeSelectItem("FL - Florida", "FL", state);
			writeSelectItem("GA - Georgia", "GA", state);
			writeSelectItem("HI - Hawaii", "HI", state);
			writeSelectItem("IA - Iowa", "IA", state);
			writeSelectItem("ID - Idaho", "ID", state);
			writeSelectItem("IL - Illinois", "IL", state);
			writeSelectItem("IN - Indiana", "IN", state);
			writeSelectItem("KS - Kansas", "KS", state);
			writeSelectItem("KY - Kentucky", "KY", state);
			writeSelectItem("LA - Louisiana", "LA", state);
			writeSelectItem("MA - Massachusetts", "MA", state);
			writeSelectItem("MD - Maryland", "MD", state);
			writeSelectItem("ME - Maine", "ME", state);
			writeSelectItem("MI - Michigan", "MI", state);
			writeSelectItem("MN - Minnesota", "MN", state);
			writeSelectItem("MO - Missouri", "MO", state);
			writeSelectItem("MS - Mississippi", "MS", state);
			writeSelectItem("MT - Montana", "MT", state);
			writeSelectItem("NC - North Carolina", "NC", state);
			writeSelectItem("ND - North Dakota", "ND", state);
			writeSelectItem("NE - Nebraska", "NE", state);
			writeSelectItem("NH - New Hampshire", "NH", state);
			writeSelectItem("NJ - New Jersey", "NJ", state);
			writeSelectItem("NM - New Mexico", "NM", state);
			writeSelectItem("NV - Nevada", "NV", state);
			writeSelectItem("NY - New York", "NY", state);
			writeSelectItem("OH - Ohio", "OH", state);
			writeSelectItem("OK - Oklahoma", "OK", state);
			writeSelectItem("OR - Oregon", "OR", state);
			writeSelectItem("PA - Pennsylvania", "PA", state);
			writeSelectItem("RI - Rhode Island", "RI", state);
			writeSelectItem("SC - South Carolina", "SC", state);
			writeSelectItem("SD - South Dakota", "SD", state);
			writeSelectItem("TN - Tennessee", "TN", state);
			writeSelectItem("TX - Texas", "TX", state);
			writeSelectItem("UT - Utah", "UT", state);
			writeSelectItem("VA - Virginia", "VA", state);
			writeSelectItem("VT - Vermont", "VT", state);
			writeSelectItem("WA - Washington", "WA", state);
			writeSelectItem("WI - Wisconsin", "WI", state);
			writeSelectItem("WV - West Virginia", "WV", state);
			writeSelectItem("WY - Wyoming", "WY", state);
		}
	}
	

	/**
	* Writes a static text field to the form output.
	*/
	
	public void writeStaticField(String label, String value) throws IOException
	{
		println("<TR>\n<TD ALIGN=RIGHT>" + label + ":</TD>");
		println("<TD>" + value + "</TD>\n</TR>");
	}

	/**
	* Writes a submit button and description to the form output.
	*/
	
	public void writeSubmitButton(String title, String name, String description) throws IOException
	{
		println("<TR>\n<TD ALIGN=RIGHT><INPUT type=submit name=" + name + " value=\"" + title + "\"></TD>");
		println("<TD>" + description + "</TD>\n</TR>");
	}

    protected String encode(String in)
    {
		StringBuffer rval = new StringBuffer();
		int index = in.indexOf("&");
		int lastIndex = 0;

		while (index != -1)
		{
			index++;
			rval.append(in.substring(lastIndex,index));
			rval.append("amp;");
			lastIndex = index;
			index = in.indexOf("&",index);
		}

		rval.append(in.substring(lastIndex));

		in = rval.toString();
		rval = new StringBuffer();
		index = in.indexOf("\"");
		lastIndex = 0;

		while (index != -1)
		{
			rval.append(in.substring(lastIndex,index));
			index++;
			rval.append("&quot;");
			lastIndex = index;
			index = in.indexOf("\"",index);
		}

		rval.append(in.substring(lastIndex));

		return(rval.toString());
    }
}

