
package com.cometway.props;


import com.cometway.httpd.HTMLFormWriter;
import com.cometway.httpd.HTMLStringTools;
import com.cometway.jdbc.StatementAgentInterface;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;


/**
* This class contains static methods for parsing a Props Schema
* into a Vector of Props.
*/

/*
* RECOGNIZED TYPES:
* =================
* type: boolean { label, list }
* type: button { title, description }
* type: caption { caption }
* type: choice { label, options, list }
* type: date { label, lines, size, list, style, format }
* type: hidden { value }
* type: html { label, foramt, size, lines, list }
* type: integer { label, list }
* type: password { label, size }
* type: string { label, lines, size, list }
* type: space {}
* type: time { label, lines, size, list, style }
* type: image { label }
*/

public class PropsSchema
{
	protected final static SimpleDateFormat SDF = new SimpleDateFormat("yyyy/MM/dd");
	protected final static String EOL = System.getProperty("line.separator");
	protected final static int DEFAULT_SIZE = 96;


	protected List schema;
	protected StatementAgentInterface statementAgent;


	/**
	* Constructs an empty schema.
	* Use the add methods to fill in this schema.
	*/

	public PropsSchema()
	{
		schema = new Vector();
	}


	/**
	* Constructs a schema based on the specified List containing Props.
	*/

	public PropsSchema(List l)
	{
		schema = l;
	}


	/**
	* Constructs a schema containing the parsed Props from the string.
	*/

	public PropsSchema(String s) throws PropsException
	{
		schema = parse(s);
	}


	/**
	* Adds the specified Props to the schema.
	*/

	public void add(Props p)
	{
		schema.add(p);
	}


	/**
	* Adds a boolean type to the schema.
	*/

	public Props addBoolean(String label, String key)
	{
		Props p = new Props();

		p.setProperty("key", key);
		p.setProperty("label", label);
		p.setProperty("type", "boolean");

		schema.add(p);

		return (p);
	}


	/**
	* Adds a button type to the schema.
	*/

	public Props addButton(String key, String title, String description)
	{
		Props p = new Props();

		p.setProperty("key", key);
		p.setProperty("title", title);
		p.setProperty("description", description);
		p.setProperty("type", "button");

		schema.add(p);

		return (p);
	}


	/**
	* Adds a caption type to the schema.
	*/

	public Props addCaption(String caption)
	{
		Props p = new Props();

		p.setProperty("caption", caption);
		p.setProperty("key", "*");
		p.setProperty("type", "caption");

		schema.add(p);

		return (p);
	}


	/**
	* Adds a choice type to the schema.
	*/

	public Props addChoice(String label, String key, String options)
	{
		Props p = new Props();

		p.setProperty("options", options);
		p.setProperty("key", key);
		p.setProperty("label", label);
		p.setProperty("type", "choice");

		schema.add(p);

		return (p);
	}


	/**
	* Adds an integer type to the schema.
	*/

	public Props addInteger(String label, String key)
	{
		Props p = new Props();

		p.setProperty("key", key);
		p.setProperty("label", label);
		p.setProperty("type", "integer");

		schema.add(p);

		return (p);
	}


	/**
	* Adds a string type to the schema. Use for single lines.
	*/

	public Props addString(String label, String key)
	{
		Props p = new Props();

		p.setProperty("key", key);
		p.setProperty("label", label);
		p.setProperty("type", "string");

		schema.add(p);

		return (p);
	}


	/**
	* Adds a string type to the schema. Use for multiple lines.
	*/

	public Props addString(String label, String key, int lines)
	{
		Props p = new Props();

		p.setProperty("key", key);
		p.setProperty("label", label);
		p.setInteger("lines", lines);
		p.setProperty("type", "string");

		schema.add(p);

		return (p);
	}


	/**
	* Adds a space type to the schema.
	*/

	public Props addSpace()
	{
		Props p = new Props();

		p.setProperty("key", "*");
		p.setProperty("type", "space");

		schema.add(p);

		return (p);
	}


	/**
	* Returns a DateFormat corresponding to the specified format:
	* short, medium, long, full, or any valid DateFormat string.
	*/

	public static DateFormat getDateFormat(String format)
	{
		DateFormat sdf = SDF;

		if (format.length() > 0)
		{
			if (format.equals("short"))
			{
				 sdf = DateFormat.getDateInstance(DateFormat.SHORT);
			}
			else if (format.equals("medium"))
			{
				sdf = DateFormat.getDateInstance(DateFormat.DEFAULT);
			}
			else if (format.equals("long"))
			{
				sdf = DateFormat.getDateInstance(DateFormat.LONG);
			}
			else if (format.equals("full"))
			{
				sdf = DateFormat.getDateInstance(DateFormat.FULL);
			}
			else
			{
				sdf = new SimpleDateFormat(format);
			}
		}

		return (sdf);
	}


	/*
	* RECOGNIZED TYPES:
	* =================
	* type: boolean { label, list }
	* type: button { title, description }
	* type: caption { caption }
	* type: choice { label, options, list }
	* type: date { format, label, lines, size, list, style }
	* type: file { label }
	* type: hidden { value }
	* type: html { label, foramt, size, lines, list }
	* type: integer { label, list }
	* type: password { label, size }
	* type: string { label, lines, size, list }
	* type: space {}
	* type: time { label, lines, size, list, style }
	*/

	public String copyProps(Props fromProps, Props toProps) throws Exception
	{
		return (copyProps(fromProps, toProps, false));
	}


	/**
	* Copies properties from the source to the destination as specified by the schema.
	* Properties which do not match the specified type, format, or are required but missing, are returned
	* in a comma separated list of keys.
	*/

	public String copyProps(Props fromProps, Props toProps, boolean ignorePrimaryKey) throws Exception
	{
		StringBuffer b = new StringBuffer();
		String primaryKey = getPrimaryKey();
		int count = schema.size();

		for (int i = 0; i < count; i++)
		{
			Props fieldProps = (Props) schema.get(i);
			String key = fieldProps.getString("key");
			String type = fieldProps.getTrimmedString("type");
			String format = fieldProps.getTrimmedString("format");
			boolean required = fieldProps.getBoolean("required");
			String value = fromProps.getTrimmedString(key);
			Object o = null;

			if ((ignorePrimaryKey && key.equals(primaryKey)) == false)
			{
				try
				{
					if (type.equals("html"))
					{
						if (format.equals("plaintext"))
						{
							o = HTMLStringTools.convertPlainTextToHTML(value);
						}
						else
						{
							o = value;
						}
					}

					else if (type.equals("date"))
					{
						if (format.equals("menus"))
						{
							int year = fromProps.getInteger(key + "_year");
							int month = fromProps.getInteger(key + "_month");
							int day = fromProps.getInteger(key + "_day");

							Calendar cal = Calendar.getInstance();

							cal.set(year, month, day);

							o = cal.getTime();						
						}
						else
						{
							DateFormat sdf = getDateFormat(format);

							o = sdf.parse(value);
						}
					}

					else if (type.equals("integer"))
					{
						o = Integer.valueOf(fromProps.getInteger(key));
					}

					else if (type.equals("boolean"))
					{
						o = Boolean.valueOf(fromProps.getBoolean(key));
					}
					
					else if (type.equals("file"))
					{
						// This allows us to receive a byte array when files
						// are submitted to the webserver.

						o = fromProps.getProperty(key);
						
						if (o instanceof Hashtable)
						{
							Hashtable ht = (Hashtable) o;
							
							if (ht != null)
							{
								o = ht.get("filedata");
							}
							else
							{
								o = null;
							}
						}
					}
					
					else if (type.equals("address"))
					{
						toProps.setProperty(key + "_street", fromProps);
						toProps.setProperty(key + "_city", fromProps);
						toProps.setProperty(key + "_zip", fromProps);

						// For the state popup menu, a dash is used
						// to indicate that no state has been selected.

						if (fromProps.getProperty(key + "_state").equals("-") == false)
						{
							toProps.setProperty(key + "_state", fromProps);
						}

						key = null; // don't need to set anything else below.
					}

					else if (type.equals("choice"))
					{
						// For choices (popup menus) dashes are used
						// to indicate that no choice has been made.

						if ((value.length() > 0) && (value.equals("-") == false))
						{
							o = value;
						}
					}

					else
					{
						// For all other types, use the String value.

						if (value.length() > 0) o = value;
					}
					
					if (key != null)
					{
						toProps.setProperty(key, o);

						if (required && (toProps.hasProperty(key) == false))
						{
							b.append(key);
							b.append(',');
						}
					}
				}
				catch (Exception e)
				{
					b.append(key);
					b.append(',');
//					error("Could not convert property \"" + key + "\" (" + value + ") to " + type + " (" + format + ")", e);
				}
			}
		}

		return (b.toString());
	}


	/**
	* Returns the schema for the specified field.
	* If more than one schema exists, the first one is returned.
	*/

	public Props getFieldSchema(String key)
	{
		Props p = null;
		int count = schema.size();

		for (int i = 0; i < count; i++)
		{
			Props fieldProps = (Props) schema.get(i);
			String fieldKey = fieldProps.getString("key");

			if (fieldKey.equals(key))
			{
				p = fieldProps;
				break;
			}
		}

		return (p);
	}



	/**
	* Returns a list of schema types that contain a list:true; property.
	*/

	public List getListSchema()
	{
		List keys = new Vector();
		int count = schema.size();

		for (int i = 0; i < count; i++)
		{
			Props fieldProps = (Props) schema.get(i);

			if (fieldProps.getBoolean("list"))
			{
				keys.add(fieldProps);
			}
		}

		return (keys);
	}


	/**
	* Returns the name of the key that contains a primary:true; property.
	*/

	public String getPrimaryKey()
	{
		String primaryKey = null;
		int count = schema.size();

		for (int i = 0; i < count; i++)
		{
			Props fieldProps = (Props) schema.get(i);

			if (fieldProps.getBoolean("primary"))
			{
				primaryKey = fieldProps.getString("key");
				break;
			}
		}

		return (primaryKey);
	}


	/**
	* Return a list of Props that define the schema.
	*/

	public List getSchema()
	{
		return (schema);
	}


	/**
	* Uses a schema containing the parsed Props from the string.
	*/

	public void setSchema(String s) throws PropsException
	{
		schema = parse(s);
	}


	/**
	* Sets the StatementAgent used for rendering OPTION lists based on a database query.
	*/

	public void setStatementAgent(StatementAgentInterface agent) throws PropsException
	{
		statementAgent = agent;
	}


	/**
	* Returns the schema as a parseable string.
	*/

	public String toString()
	{
		return (toString(schema));
	}


	/**
	* Writes the schema using the specified HTMLFormWriter.
	*/

	public void writeForm(HTMLFormWriter w) throws IOException
	{
		writeForm(w, new Props(), DEFAULT_SIZE);
	}


	/**
	* Writes the schema using the specified HTMLFormWriter and Props values.
	*/

	public void writeForm(HTMLFormWriter w, Props p) throws IOException
	{
		writeForm(w, p, DEFAULT_SIZE);
	}


	/**
	* Writes the schema using the specified HTMLFormWriter and Props values.
	* The size of fields will not exceed the specified default size.
	*/

	public void writeForm(HTMLFormWriter w, Props p, int defaultSize) throws IOException
	{
		int count = schema.size();

		for (int i = 0; i < count; i++)
		{
			Props fieldProps = (Props) schema.get(i);

			writeFormElement(w, fieldProps, p, defaultSize);
		}
	}


	/**
	* Writes the schema element using the specified HTMLFormWriter and Props values.
	* The size of fields will not exceed the specified default size.
	*/

	public void writeFormElement(HTMLFormWriter w, Props fieldProps, Props p, int defaultSize) throws IOException
	{
		String key = fieldProps.getString("key");
		String label = fieldProps.getString("label");
		String type = fieldProps.getString("type");
		String help = fieldProps.getString("help");

		if (help.length() > 0)
		{
			w.writeHelp(help);
		}

		if (type.equals("address"))
		{
			int size = fieldProps.getInteger("size");

			w.writeAddressFields(key, p, size);
		}

		else if (type.equals("boolean"))
		{
			w.writeCheckbox(label, key, p.getBoolean(key));
		}

		else if (type.equals("button"))
		{
			String title = fieldProps.getString("title");
			String description = fieldProps.getString("description");

			w.writeSubmitButton(title, key, description);
		}

		else if (type.equals("caption"))
		{
			String caption = fieldProps.getString("caption");

			w.writeCaption(caption);
		}

		else if (type.equals("choice"))
		{
			String currentValue = p.getString(key);
			String format = fieldProps.getString("format");

			if (format.equals("US_STATES"))
			{
				w.writeSelectUSState(label, key, currentValue);
			}
			else
			{
				Vector options = fieldProps.getTokens("options");
				boolean select_multiple = fieldProps.getBoolean("select_multiple");

				w.writeSelectHeader(label, key, select_multiple);

				int c = options.size();

				for (int j = 0; j < c; j++)
				{
					String s = (String) options.get(j);

					w.writeSelectItem(s, s, currentValue.equals(s));
				}

				if (format.equals("statement") && (statementAgent != null))
				{
					String statement_name = fieldProps.getString("statement_name");
					String name_key = fieldProps.getString("name_key");
					String value_key = fieldProps.getString("value_key");

					if (name_key.length() == 0) name_key = "name";
					if (value_key.length() == 0) value_key = "value";

					List items = statementAgent.executeQuery(statement_name);
					int count = items.size();

					for (int i = 0; i < count; i++)
					{
						Props rp = (Props) items.get(i);
						String name = rp.getString(name_key);
						String value = rp.getString(value_key);

						w.writeSelectItem(name, value, currentValue.equals(value));
					}
				}

				w.writeSelectFooter();
			}
		}

		else if (type.equals("date"))
		{
			String format = fieldProps.getString("format");

			if (format.equals("menus"))
			{
				w.writeDateFields(label, key, p);
			}
			else
			{
				int size = fieldProps.getInteger("size");
				DateFormat df = getDateFormat(format);
				String value = p.getDateString(key, df);

				if (value == null)
				{
					w.getAgentRequest().append("field_errors", key + ",");

					value = format;
				}

				if (size == 0) size = defaultSize;

				w.writeField(label, key, value, size);
			}
		}

		else if (type.equals("hidden"))
		{
			String value = fieldProps.getTrimmedString("value");

			if (value.length() > 0)
			{
				w.writeHiddenField(key, value);
			}
			else
			{
				w.writeHiddenField(key, p);
			}
		}

		else if (type.equals("html"))
		{
			int lines = fieldProps.getInteger("lines");
			int size = fieldProps.getInteger("size");
			String format = fieldProps.getTrimmedString("format");
			String value = p.getString(key);

			if (size == 0) size = defaultSize;

			if (format.equals("plaintext"))
			{
				value = HTMLStringTools.convertHTMLToPlainText(value);
			}

			if (lines < 2)
			{
				w.writeField(label, key, value, size);
			}
			else
			{
				w.writeMultilineField(label, key, value, size, lines);
			}
		}

		else if (type.equals("password"))
		{
			String currentValue = p.getString(key);
			int size = fieldProps.getInteger("size");

			if (size == 0) size = defaultSize;

			w.writePassword(label, key, currentValue, size);
		}

		else if (type.equals("file"))
		{
			w.writeFileUpload(label, key);
		}

		else if (type.equals("space"))
		{
			w.writeSpace();
		}

		else
		{
			int lines = fieldProps.getInteger("lines");
			int size = fieldProps.getInteger("size");
			String value = p.getString(key);

			if (size == 0) size = defaultSize;

			if (lines < 2)
			{
				w.writeField(label, key, value, size);
			}
			else
			{
				w.writeMultilineField(label, key, value, size, lines);
			}
		}
	}


	/**
	* Parses the formatted schema string and returns it as a List of Props.
	*/

	public static List parse(String schema) throws PropsException
	{
		List v = new Vector();
		Props p = null;
		String key = null;
		String value = null;
		StringTokenizer t = new StringTokenizer(schema, "{:;}", true);

		while (t.hasMoreTokens())
		{
			String s = t.nextToken().trim();

//System.out.println("token: " + s);

			if (s.equals("{"))
			{
				if (p == null) throw new PropsException("Unexpected '{' in PropsSchema (key = " + key + ").");
			}

			else if (s.equals("}"))
			{
				if (p == null) throw new PropsException("Unexpected '}' in PropsSchema (key = " + key + ").");

				v.add(p);

				p = null;
			}

			else if (s.equals(":"))
			{
				if ((p == null) || (key == null))
				{
					throw new PropsException("Unexpected ':' in PropsSchema (key = " + key + ").");
				}
				else
				{
					if (value != null)
					{
						value = value + ':';
					}
					else
					{
						value = new String();
					}
				}
			}

			else if (s.equals(";"))
			{
				if ((p == null) || (key == null) || (value == null)) throw new PropsException("Unexpected ';' in PropsSchema (key = " + key + ").");

				p.setProperty(key, value);

				key = null;
				value = null;
			}

			else
			{
				if (p == null)
				{
					p = new Props();
					p.setProperty("key", s);
				}
				else
				{
					if (s.length() > 0)
					{
						if (key == null)
						{
							key = s;
//System.out.println("key = " + key);
						}
						else
						{
							if (value == null)
							{
								value = s;
							}
							else
							{
								value = value + s;
							}
//System.out.println("value = " + value);
						}
					}
				}
			}
		}

		return (v);
	}


	/**
	* Returns the specified List of Props as a formatted schema string.
	*/

	public static String toString(List schema)
	{
		StringBuffer str = new StringBuffer();
		int count = schema.size();

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) schema.get(i);
			String key = p.getString("key");

			str.append(key);
			str.append(" { ");

			Vector keys = p.getKeys();
			int keyCount = keys.size();

			Collections.sort(keys);

			for (int j = 0; j < keyCount; j++)
			{
				key = (String) keys.get(j);

				if (key.equals("key") == false)
				{
					String value = p.getTrimmedString(key);

					str.append(key);
					str.append(": ");
					str.append(value);
					str.append("; ");
				}
			}

			str.append('}');
			str.append(EOL);
		}

		return (str.toString());
	}
}



