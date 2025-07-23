
package com.cometway.props;

import com.cometway.util.DateTools;
import com.cometway.util.ObjectSerializer;
import com.cometway.util.Pair;
import com.cometway.util.StringTools;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;


/**
* The Props object keeps track of a set of properties. Similar to a hashtable but includes
* many useful utilities. Props allows listeners to be added so that other objects can
* be notified when properties in this Props object get changed. There are also methods
* that return specific types of objects instead of the Object type and set properties
* with specific types.
*/

public class Props
{
	private final static DateFormat ISO8601_DATEFORMAT = DateTools.ISO8601_DATEFORMAT;
	private final static String EOL = System.getProperty("line.separator");

	private IPropsContainer container;
	private int suspendNotify;
	private Vector listeners;
	private Vector changedProps;
	private Object listenerSync;


	/**
	* Creates a new Props based on a PropsContainer.
	* @see IPropsContainer
	* @see PropsContainer
	*/

	public Props()
	{
		container = new PropsContainer();
	}


	/**
	* Creates a new Props based on a given IPropsContainer.
	* @param c a reference to an IPropsContainer.
	* @see IPropsContainer
	*/

	public Props(IPropsContainer container)
	{
		if (container == null)
		{
			throw new NullPointerException("container is null");
		}

		this.container = container;
	}


	/**
	* Adds an IPropsChangeListener to the list of listeners for this object.
	* @param l a reference to an IPropsChangeListener.
	* @return true if the listener already exists.
	*/

	public boolean addListener(IPropsChangeListener l)
	{
		boolean exists = listeners.contains(l);

		if (exists == false)
		{
			synchronized (listenerSync)
			{
				listeners.addElement(l);
			}
		}

		return (exists);
	}


	/**
	* If the specified object is a String, it is
	* the specified String is appended to it.
	* Otherwise the call is ignored.
	*/

	public void append(String key, String s)
	{
		Object o = container.getProperty(key);

		if (o instanceof String)
		{
			String str = (String) o;

			container.setProperty(key, str + s);
		}
	}


	/** Used to format the Props toString() representation. */

	private void appendPropsContainer(StringBuffer b, String key, IPropsContainer pc, int level)
	{
		Enumeration e = pc.enumerateProps();

		while (e.hasMoreElements())
		{
			String k = (String) e.nextElement();
			Object o = pc.getProperty(k);
			String classname = "null";

			if (o != null)
			{
				classname = o.getClass().getName();

				int i = classname.lastIndexOf('.');

				if (i > 0)
				{
					classname = classname.substring(i + 1);
				}
			}

			indent(b, level);
			b.append(classname);
			b.append(' ');
			b.append(k);
			b.append(" = ");

			if (o instanceof IPropsContainer)
			{
				IPropsContainer ipc = (IPropsContainer) o;

				if (o instanceof VectorPropsContainer)
				{
					b.append(" (");
					b.append(ipc.getProperty("size"));
					b.append(" elements)");
				}

				b.append('\n');
				indent(b, level);
				b.append("{\n");

				appendPropsContainer(b, k, ipc, level + 1);

				indent(b, level);
				b.append("}\n");
			}
			else
			{
				b.append('"');
				b.append(o);
				b.append("\";\n");
			}
		}
	}


	/**
	* Copies the value of the property with the same name as key
	* from srcProps into the IPropsContainer for this object.
	* @param srcProps a reference to the source Props.
	* @param key the name of the property to copy.
	*/

	public boolean copy(Props srcProps, String key)
	{
		Object value = srcProps.container.getProperty(key);

		if (value == null)
		{
			container.removeProperty(key);
		}
		else
		{
			container.setProperty(key, value);
		}

		if (key != null)
		{
			notifyPropsChanged(key);
		}

		return (value != null);
	}
	
	/** Copies the contents of the specified Props into this one. */


	public void copyFrom(Props srcProps)
	{
		suspendNotify();

		Enumeration e = srcProps.getKeys().elements();

		while (e.hasMoreElements())
		{
			setProperty((String) e.nextElement(), srcProps);
		}

		resumeNotify();
	}


	/**
	* Copies the contents of this Props into the one specified.
	*/

	public void copyTo(Props destProps)
	{
		destProps.suspendNotify();

		Enumeration e = container.enumerateProps();

		while (e.hasMoreElements())
		{
			String key = (String) e.nextElement();

			destProps.setProperty(key, container.getProperty(key));
		}

		destProps.resumeNotify();
	}


	/** Decrements the integer value of the specified key by 1. */

	public void decrementInteger(String key)
	{
		int i = getInteger(key);

		setInteger(key, i - 1);
	}


	/**
	* Dumps a list of properties and their values to System.out.
	*/

	public void dump()
	{
		StringBuffer b = new StringBuffer();
		Enumeration e = getKeys().elements();

		while (e.hasMoreElements())
		{
			String key = (String) e.nextElement();

			b.append(key);
			b.append(" = \"");
			b.append(getString(key));
			b.append("\"\n");
		}

		System.out.println(b.toString());
	}


	/**
	* Returns an Enumeration of keys representing properties contained
	* by this object's IPropsContainer.
	* @return an Enumeration of String objects.
	*/

	public Enumeration enumerateKeys()
	{
		return (container.enumerateProps());
	}


	public void enableListeners()
	{
		if (listeners == null)
		{
			listeners = new Vector();
			changedProps = new Vector();
			listenerSync = new Object();
		}
	}


	/**
	* Uses the specified MessageFormat pattern this method generates a
	* string containing the inserted values from the cooresponding
	* properties.
	* The pattern string is passed to java.text.MessageFormat.format()
	* after substituting appropriate argument indexes for property names.
	* The pattern string contains number format patterns in the syntax
	* { PropsKey, FormatType, FormatStyle } where
	* formatType 'number' uses the NumberFormat class (none, integer, currency, percent, custom NumberFormat pattern),
	* formatType 'date' uses the DateFormat class (none, short, medium, long, full, custom DateFormat pattern),
	* formatType 'time' uses the DateFormat class (none, short, medium, long, full,  custom DateFormat pattern), and
	* formatType 'choice' uses the ChoiceFormat (custom ChoiceFormat pattern) class.
	* eg: "At {my_date,time} on {my_date,date}, there was {my_event_name} on planet {my_planet_name,number,integer}."
	* eg: "The disk \"{disk_name}\" contains {file_count} file(s)."
	*/

	public String format(String pattern) throws PropsException
	{
		String s = null;
		Object[] arguments = null;

		try
		{
			Vector keyList = new Vector();
			int pos = pattern.indexOf('{');

			while (pos >= 0)
			{
				String key = getNextKey(pattern, pos + 1);

				if (key == null) break;

				if (keyList.contains(key) == false)
				{
					keyList.add(key);
				}

				pos = pattern.indexOf('{', pos + 1);
			}

			int keyCount = keyList.size();

			arguments = new Object[keyCount];

			for (int i = 0; i < keyCount; i++)
			{
				String keyName = (String) keyList.get(i);
				arguments[i] = getProperty(keyName);

				String key = "\\{" + keyName;
				String idx = "\\{" + Integer.toString(i);

				pattern = pattern.replaceAll(key, idx);
			}

			s = MessageFormat.format(pattern, arguments);
		}
		catch (Exception e)
		{
			String message = "Could not format pattern (" + e.toString() + ")\n" + pattern;
			throw new PropsException(message, e);
		}

		return (s);
	}


	/**
	* Returns the boolean value of the requested property
	* @param key the name of the property to process and retrieve.
	* @return the boolean value of the requested property.
	*/

	public boolean getBoolean(String key)
	{
		Object o = getProperty(key);

		if (o != null)
		{
			if (o instanceof Boolean)
			{
				return (((Boolean) o).booleanValue());
			}

			if (o instanceof String)
			{
				String  s = o.toString();

				return (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("Y") || s.equalsIgnoreCase("T"));
			}
		}

		return (false);
	}


	/**
	* Returns specified object as a byte array. If the object
	* is a String its getBytes methods is called to obtain a byte array.
	* Otherwise, the object's String value is used to obtain a byte array.
	*/

	public byte[] getByteArray(String key)
	{
		Object o = container.getProperty(key);

		if (o != null)
		{
			if (o instanceof byte[])
			{
				return ((byte[]) o);
			}
			else if (o instanceof String)
			{
				return (((String) o).getBytes());
			}
			else
			{
				return (o.toString().getBytes());
			}
		}

		return (new byte[0]);
	}


	/**
	* Returns the char value of the requested property
	* @param key the name of the property to process and retrieve.
	* @return the char value of the requested property.
	*/

	public char getCharacter(String key)
	{
		Object o = container.getProperty(key);

		if (o != null)
		{
			if (o instanceof Character)
			{
				return (((Character) o).charValue());
			}

			if (o instanceof String)
			{
				return (((String) o).charAt(0));
			}
		}

		return ('\0');
	}


	/**
	* Returns the Date value of the requested property. If the property
	* value is a String, it is automatically parsed as a ISO8601 date using
	* the DateFormat 'yyyyMMddThhmmss'.
	* @param key the name of the property to process and retrieve.
	* @return the Date value of the requested property.
	*/

	public Date getDate(String key)
	{
		Object o = container.getProperty(key);

		try
		{
			if (o != null)
			{
				if (o instanceof Date)
				{
					return ((Date) o);
				}

				if (o instanceof String)
				{
					return (ISO8601_DATEFORMAT.parse((String) o));
				}
			}
		}
		catch (ParseException e)
		{
			// Could not parse a ISO8601 date from this.
		}

		return (null);
	}


	/**
	* Returns the Date value of the requested property. If the property
	* value is a String, it is automatically parsed as a date using
	* the specified DateFormat.
	* @param key the name of the property to process and retrieve.
	* @param df the DateFormat used to parse this property if it is a String.
	* @return the Date value of the requested property.
	*/

	public Date getDate(String key, DateFormat df)
	{
		Object o = container.getProperty(key);

		try
		{
			if (o != null)
			{
				if (o instanceof Date)
				{
					return ((Date) o);
				}

				if (o instanceof String)
				{
					return (df.parse((String) o));
				}
			}
		}
		catch (ParseException e)
		{
			// Could not parse the date from this.
		}

		return (null);
	}


	/**
	* Returns a formatted date using the Date stored in the specified
	* property. If the property is not a Date, the returned String will
	* be zero length.
	* @param key the name of the property to format and return.
	* @param df the DateFormat used to format this property if it is a Date.
	* @return the formatted String value of the requested property.
	*/

	public String getDateString(String key, DateFormat df)
	{
		Object o = container.getProperty(key);

		if (o != null)
		{
			if (o instanceof Date)
			{
				return (df.format((Date) o));
			}
		}

		return (new String());
	}


	/**
	* Returns the double value of the requested property
	* @param key the name of the property to process and retrieve.
	* @return the double value of the requested property.
	*/

	public double getDouble(String key)
	{
		Object o = container.getProperty(key);

		if (o != null)
		{
			if (o instanceof Double)
			{
				return (((Double) o).doubleValue());
			}

			if (o instanceof Float)
			{
				return (((Float) o).doubleValue());
			}

			if (o instanceof Integer)
			{
				return (((Integer) o).doubleValue());
			}

			if (o instanceof Long)
			{
				return (((Long) o).doubleValue());
			}

			if (o instanceof String)
			{
				return (Double.valueOf((String) o).doubleValue());
			}
		}

		return (0);
	}


	/**
	* Returns the float value of the requested property
	* @param key the name of the property to process and retrieve.
	* @return the float value of the requested property.
	*/

	public float getFloat(String key)
	{
		Object o = container.getProperty(key);

		if (o != null)
		{
			if (o instanceof Float)
			{
				return (((Float) o).floatValue());
			}

			if (o instanceof Double)
			{
				return (((Double) o).floatValue());
			}

			if (o instanceof Integer)
			{
				return (((Integer) o).floatValue());
			}

			if (o instanceof Long)
			{
				return (((Long) o).floatValue());
			}

			if (o instanceof String)
			{
				return (Float.valueOf((String) o).floatValue());
			}
		}

		return (0);
	}


	/**
	* Returns the specified object as a String of hexidecimal bytes.
	*/

	public String getHexString(String key)
	{
		String hexStr = new String();
		byte[] b = getByteArray(key);

		for (int i = 0; i < b.length; i++)
		{
			hexStr += Integer.toHexString((int) (b[i] & 0xFF)).toUpperCase();
		}

		return (hexStr);
	}


	/**
	* Returns the int value of the requested property
	* @param key the name of the property to process and retrieve.
	* @return the int value of the requested property.
	*/

	public int getInteger(String key)
	{
		Object o = container.getProperty(key);

		if (o != null)
		{
			if (o instanceof Integer)
			{
				return (((Integer) o).intValue());
			}

			if (o instanceof String)
			{
				return (Integer.parseInt((String) o));
			}

			if (o instanceof Long)
			{
				return (((Long) o).intValue());
			}

			if (o instanceof Float)
			{
				return (((Float) o).intValue());
			}

			if (o instanceof Double)
			{
				return (((Double) o).intValue());
			}
		}

		return (0);
	}


	/**
	* Returns the long value of the requested property
	* @param key the name of the property to process and retrieve.
	* @return the long value of the requested property.
	*/

	public long getLong(String key)
	{
		Object o = container.getProperty(key);

		if (o != null)
		{
			if (o instanceof Long)
			{
				return (((Long) o).longValue());
			}

			if (o instanceof String)
			{
				return (Long.parseLong((String) o));
			}

			if (o instanceof Integer)
			{
				return (((Integer) o).longValue());
			}

			if (o instanceof Float)
			{
				return (((Float) o).longValue());
			}

			if (o instanceof Double)
			{
				return (((Double) o).longValue());
			}
		}

		return (0);
	}


	/**
	* Returns a Vector containing the names of property keys currently
	* accessible by getProperty.
	*/

	public Vector getKeys()
	{
		Vector v = new Vector();
		Enumeration e = container.enumerateProps();

		while (e.hasMoreElements())
		{
			v.addElement(e.nextElement());
		}

		return (v);
	}


	/** Used by parse() method to find the next key to replace. */

	protected String getNextKey(String source, int start)
	{
		String key = null;
		int a = source.indexOf('}', start);
		int b = source.indexOf(',', start);
		int pos = -1;

		if ((start < a) && ((a < b) || (b == -1)))
		{
			pos = a;
		}
		else if ((start < b) && ((b < a) || (a == -1)))
		{
			pos = b;
		}

		if (pos >= 0)
		{
			key = source.substring(start, pos);
		}

		return (key);
	}
	

	/**
	* Returns the property with the same name as the key parameter if available.
	* If the requested property does not exist, null is returned instead.
	* @param key the name of the property to retrieve.
	* @return an Object reference; null if the requested property does not exist.
	*/

	public Object getProperty(String key)
	{
		return (container.getProperty(key));
	}


	/**
	* Returns the property with the same name as the key parameter if available.
	* If the requested property does not exist, null is returned instead.
	* @param key the name of the property to retrieve.
	* @param schema the PropsSchema used to determine the returned object type and style.
	* @return an Object reference; null if the requested property does not exist.
	*/

	public Object getProperty(String key, PropsSchema schema) throws PropsException
	{
		Object value = container.getProperty(key);

		try
		{
			Props fieldSchema = schema.getFieldSchema(key);

			if (fieldSchema != null)
			{
				String type = fieldSchema.getString("type");

				// Dates are either Dates or stored in a DateFormat parseable format.

				if (type.equals("date") || type.equals("time"))
				{
					if ((value instanceof Date) == false)
					{
						String style = fieldSchema.getString("style");
						String pattern = "{0," + type + "," + style + "}";

						MessageFormat f = new MessageFormat(pattern);
						Object[] values = f.parse(value.toString());

						value = values[0];
					}
				}

				// Booleans are either Booleans or a String.

				else if (type.equals("boolean"))
				{
					if ((value instanceof Boolean) == false)
					{
						value = Boolean.valueOf(getBoolean(key));
					}
				}

				// Integers are either Integers or a String.

				else if (type.equals("integer"))
				{
					if ((value instanceof Integer) == false)
					{
						value = Integer.valueOf(getInteger(key));
					}
				}

				// Numbers and Choices are basically treated like strings for now.

				else if (type.equals("number") || type.equals("choice"))
				{
					String style = fieldSchema.getString("style");
					String pattern = "{0," + type + "," + style + "}";
					MessageFormat f = new MessageFormat(pattern);
					Object[] values = f.parse(value.toString());

					value = values[0];
				}
			}	
		}
		catch (Exception e)
		{
			throw new PropsException("Cannot interpret property: " + key + " = \"" + value + "\"");
		}

		return (value);
	}


	/**
	* Returns a reference to the IPropfsContainer referenced by this object.
	* @return a refernce to an IPropscontiner.
	*/

	public IPropsContainer getPropsContainer()
	{
		return (container);
	}


	/** Returns the size of the specified object. If the
	* object is a Vector or VectorPropsContainer, the number of elements it contains is returned.
	* If the object is a List, its length is returned.
	* For other objects a value of 1 is returned.
	* If the object does not exist, zero is returned.
	*/

	public int getSize(String key)
	{
		Object o = container.getProperty(key);

		if (o == null)
		{
			return (0);
		}
		else if (o instanceof List)
		{
			return (((List) o).size());
		}
		else if (o instanceof VectorPropsContainer)
		{
			return (((VectorPropsContainer) o).getVector().size());
		}
		else
		{
			return (1);
		}
	}


	/**
	* Returns the entire Props as a formatted String.
	*/

	public String getString()
	{
		StringBuffer b = new StringBuffer();

		appendPropsContainer(b, "", container, 1);

		return (b.toString());
	}


	/**
	* Returns the String value of the requested property
	* @param key the name of the property to process and retrieve.
	* @return the String value of the requested property.
	*/

	public String getString(String key)
	{
		Object  o = container.getProperty(key);

		if (o != null)
		{
			if (o instanceof String)
			{
				return ((String) o);
			}
			else
			{
				return (o.toString());
			}
		}

		return ("");
	}


	/**
	* Returns a Vector of Strings representing the tokens stored
	* stored in the specified property separated by a comma, 
	* semi-colon, carriage return, or line-feed.
	*/

	public Vector getTokens(String key)
	{
		return (getTokens(key, ";,\r\n"));
	}


	/**
	* Returns a Vector of Strings representing the tokens stored
	* stored in the specified property separated by any of the
	* characters specified by tokenSeparators.
	*/

	public Vector getTokens(String key, String tokenSeparators)
	{
		Vector v = new Vector();
		String paramList = getString(key);
		StringTokenizer t = new StringTokenizer(paramList, tokenSeparators);

		while (t.hasMoreTokens())
		{
			v.add(t.nextToken().trim());
		}

		return (v);
	}



	/** Returns the specified object as a trimmed String. */

	public String getTrimmedString(String key)
	{
		return (getString(key).trim());
	}


	/**
	* Returns the specified object as a Vector. If the specified
	* object is not a Vector, it is placed in a Vector and returned.
	* if the object does not exist, an empty Vector is returned.
	*/

	public Vector getVector(String key)
	{
		Object  o = container.getProperty(key);

		if (o != null)
		{
			if (o instanceof Vector)
			{
				return ((Vector) o);
			}
			else if (o instanceof VectorPropsContainer)
			{
				return (((VectorPropsContainer) o).getVector());
			}
			else if (o instanceof IPropsContainer)
			{
				IPropsContainer pc = (IPropsContainer) o;

				Vector v = new Vector();

				v.add(pc);

				return (v);
			}
			else
			{
				Vector v = new Vector();

				v.add(o);

				return (v);
			}
		}

		return (new Vector());
	}

	/**
	* Tests the existence of the specified property.
	* @param key the name of the property to test.
	* @return true if the property exists; false otherwise.
	*/

	public boolean hasProperty(String key)
	{
		return (container.getProperty(key) != null);
	}


	/** Increments the integer value of the specified key by 1. */

	public void incrementInteger(String key)
	{
		int i = getInteger(key);

		setInteger(key, i + 1);
	}


	/** Used to format the Props toString() representation. */

	private void indent(StringBuffer b, int level)
	{
		for (int i = 0; i < level; i++)
		{
			b.append("   ");
		}
	}


	/**
	* Inserts property String values into the passed string where property
	* names are surrounded by angle brackets.
	* @param str a String containing text with with property names surrounded by angle brackets.
	* @returns a String with the property names replaced by their values.
	*/

	public String insertProps(String str)
	{
		StringBuffer b = new StringBuffer();
		String key;
		int from;
		int to;

		from = 0;

		while (from < str.length())
		{
			to = str.indexOf('<', from);

			if (to == -1)
			{
				/* No more keys were found. Just copy the rest of the string. */
				
				b.append(str.substring(from, str.length()));

				break;
			}
			else
			{
				/* Append text up to the next key. */
				if (from < to)
				{
					b.append(str.substring(from, to));

					from = to;
				}

				to = str.indexOf('>', from);

				if (to == -1)
				{	
					/* If the key has no closing angle bracket,
						we just copy the rest of the string. */

					b.append(str.substring(from, str.length()));

					break;
				}
				else
				{		
					/* Substitute the key for its value. */

					key = str.substring(from + 1, to);

					if (hasProperty(key))
					{
						b.append(getString(key));
					}
					else
					{
						b.append("<" + key + ">");
					}

					from = to + 1;
				}
			}
		}

		return (b.toString());
	}


	/**
	* Can be called externally to manually send props changes messages
	* to all registered IPropsChangeListeners.
	* @param key the property that has changed.
	*/

	public void notifyPropsChanged(String key)
	{
		if (listeners == null) return;

		if (key == null)
		{
			/* make sure this isn't happening while the changedProps Vector is being emptied...*/
			throw new NullPointerException("key is null");
		}

		synchronized (listenerSync)
		{
			changedProps.addElement(key);
		}

		if (suspendNotify == 0)
		{
			notifyPropsChangedIgnore(null);
		}
	}


	/**
	* Called internally to notify its IPropsChangeListeners of changed properties.
	* The listener passed in l is not sent a notification. If the listener is null
	* all listeners will be notified.
	* @param l a reference to a listener.
	*/

	private void notifyPropsChangedIgnore(IPropsChangeListener ignore)
	{
		if (listeners == null) return;

		/* These two temp Vectors grabs duplicates the listeners Vector and the changedProps Vector. */
		
		Vector tmpListeners = new Vector();
		Vector tmpChangedProps = new Vector();

		synchronized (listenerSync)
		{
			/* Duplicate the changedProps Vector and remove all the elements. */
			
			if (changedProps.size() > 0)
			{
				Enumeration e = changedProps.elements();

				while (e.hasMoreElements())
				{
					tmpChangedProps.addElement(e.nextElement());
				}

				changedProps.removeAllElements();
			}
		}
		
		/* Give up the lock in case someone else needs it */

		synchronized (listenerSync)
		{
			/* Duplicate the listeners Vector */
			
			Enumeration e = listeners.elements();

			while (e.hasMoreElements())
			{
				tmpListeners.addElement(e.nextElement());
			}
		}

		/* Give up the lock for good, we only need it for duplicating the Vectors... */

		int changedCount = tmpChangedProps.size();

		if (changedCount > 0)
		{
			String keys[] = new String[changedCount];
			IPropsChangeListener l;

			tmpChangedProps.copyInto(keys);

			Enumeration e = tmpListeners.elements();

			while (e.hasMoreElements())
			{
				l = (IPropsChangeListener) e.nextElement();

				if (l != ignore)
				{
					l.propsChanged(this, keys);
				}
			}
		}
	}


	/**
	* Can be called externally to manually send props changes messages
	* to all registered IPropsChangeListeners, except the one passed.
	* Passing a null IPropsChangeLister will result in the notification of
	* all registered listeners; the same as notifyPropsChanged.
	* @param key the property that has changed.
	* @param l a reference to an IPropsChangeListener.
	*/

	public void notifyPropsChangedIgnore(String key, IPropsChangeListener ignore)
	{
		if (listeners == null) return;
                
		if (key == null)
		{
			throw new NullPointerException("key is null");
			/* Make sure this isn't happening while the changedProps Vector is being emptied */
		}

		synchronized (listenerSync)
		{
			changedProps.addElement(key);
		}

		/* The object referenced by "ignore" may be notified if notification is suspended. */
		/* This could potentially cause a loopback notification, but should not cause */
		/* an infinite loop. This should be addressed at some point, but its not critical. */

		if (suspendNotify == 0)
		{
			notifyPropsChangedIgnore(ignore);
		}
	}


	/**
	* Can be called externally to manually send props changes messages
	* to all registered IPropsChangeListeners, except the one passed.
	* Passing a null IPropsChangeLister will result in the notification of
	* all registered listeners; the same as notifyPropsChanged.
	* @param key the property that has changed.
	* @param l a reference to an IPropsChangeListener.
	*/

	public void notifyPropsChangedIgnore(String changedKeys[], IPropsChangeListener ignore)
	{
		if (listeners == null) return;

		if (changedKeys == null)
		{
			throw new NullPointerException("changedKeys is null");
		}

		for (int i = 0; i < changedKeys.length; i++)
		{
			synchronized (listenerSync)
			{
				changedProps.addElement(changedKeys[i]);
			}
		}

		/* The object referenced by "ignore" may be notified if notification is suspended. */
		/* This could potentially cause a loopback notification, but should not cause */
		/* an infinite loop. This should be addressed at some point, but its not critical. */

		if (suspendNotify == 0)
		{
			notifyPropsChangedIgnore(ignore);
		}
	}


	/**
	* Parses the source using the specified pattern, setting the specified properties
	* with their coresponding values. The pattern string is passed to java.text.MessageFormat
	* after substituting the property names for variable indexes. This allows the parser
	* to set the corresponding properties based on the results.
	* Throws java.text.ParseException if there is an error.
	*/

	public void parse(String source, String pattern) throws ParseException
	{
		Vector keyList = new Vector();
		int pos = pattern.indexOf('{');

		while (pos >= 0)
		{
			String key = getNextKey(pattern, pos + 1);

			if (key == null) break;

			if (keyList.contains(key) == false)
			{
				keyList.add(key);
			}

			pos = pattern.indexOf('{', pos + 1);
		}

		int len = keyList.size();

		for (int i = 0; i < len; i++)
		{
			String key = "\\{" + (String) keyList.get(i);
			String idx = "\\{" + Integer.toString(i);

			pattern = pattern.replaceAll(key, idx);
		}

		MessageFormat f = new MessageFormat(pattern);
		Object[] values = f.parse(source);

		for (int i = 0; i < values.length; i++)
		{
			setProperty((String) keyList.get(i), values[i]);
		}
	}


	/**
	* Matches the string against the regular expression
	* stored in the specified property.
	* @param key the property containing the String to match against.
	* @param regex the regular expression used for matching.
	* @return true if the match was successful; false otherwise.
	*/

	public boolean propertyMatchesRegEx(String key, String regex)
	{
		String str = getString(key);

		return (str.matches(regex));
	}



	/**
	* Matches the string against the regular expression
	* stored in the specified property.
	* @param key the property that contains a regular expression
	* @param str the String that the regular expression is matched against.
	* @return true if the match was successful; false otherwise.
	*/

	public boolean regExPropertyMatches(String key, String str)
	{
		String regex = getString(key);

		return (str.matches(regex));
	}



	/**
	* Removes all properties referenced by this class.
	*/

	public void removeAll()
	{
		Enumeration e = container.enumerateProps();

		while (e.hasMoreElements())
		{
			container.removeProperty((String) e.nextElement());
		}
	}


	/**
	* Removes an IPropsChangeListener from the list of listeners for this object.
	* @param l a reference to an IPropsChangeListener.
	* @return true if the listener was removed; false otherwise.
	*/

	public boolean removeListener(IPropsChangeListener l)
	{
		boolean rval = false;

		if (listeners != null)
		{
			synchronized (listenerSync)
			{
				rval = listeners.removeElement(l);
			}
		}

		return (rval);
	}


	/**
	* Removes the property with the same name as the key parameter if available.
	* If the requested property does not exist, false is returned.
	* @param key the name of the property to retrieve.
	* @return true if the property was removed; false otherwise.
	*/

	public boolean removeProperty(String key)
	{
		boolean result = container.removeProperty(key);

		notifyPropsChanged(key);

		return (result);
	}		


	/** Resumes the automatic notification of IPropsChangeListeners.
	* Calling this method will immediately call the propsChanged method of its
	* IPropsChangeListeners passing an array of property names which have changed
	* since suspendNotify was called.
	*/


	public void resumeNotify()
	{
		if (suspendNotify > 0)
		{
			suspendNotify--;

			if (suspendNotify == 0)
			{
				notifyPropsChangedIgnore(null);
			}
		}
	}


	/**
	* Sets the property key to the passed value. Calling this method automatically
	* calls notifyChanged with a reference to the key parameter.
	* <P>
	* <B>This method id deprecated. Use IPropsChangeListeners instead.</B>
	* @param key the name of the property to set.
	* @param value a reference to an Object.
	*/

	public void set(String key, Object value)
	{
		setProperty(key, value);
	}


	/**
	* Sets the property key to a Boolean of the passed value.
	* @param key the name of the property to set.
	* @param value a boolean.
	*/

	public void setBoolean(String key, boolean value)
	{
		container.setProperty(key, Boolean.valueOf(value));
		notifyPropsChanged(key);
	}


	/**
	* Sets the property key to a Character of the passed value.
	* @param key the name of the property to set.
	* @param value a char.
	*/

	public void setCharacter(String key, char value)
	{
		container.setProperty(key, Character.valueOf(value));
		notifyPropsChanged(key);
	}


	/** If there is no value for the specified object,
	* it is set to the value specified as default.
	*/

	public void setDefault(String key, Object defaultValue)
	{
		Object o = container.getProperty(key);

		if (o == null)
		{
			setProperty(key, defaultValue);
		}
	}


	/**
	* Sets the property key to a Double of the passed value.
	* @param key the name of the property to set.
	* @param value a double.
	*/

	public void setDouble(String key, double value)
	{
		container.setProperty(key, Double.valueOf(value));
		notifyPropsChanged(key);
	}


	/**
	* Sets the property key to a Float of the passed value.
	* @param key the name of the property to set.
	* @param value a float.
	*/

	public void setFloat(String key, float value)
	{
		container.setProperty(key, Float.valueOf(value));
		notifyPropsChanged(key);
	}


	/**
	* Sets the property key to a Integer of the passed value.
	* @param key the name of the property to set.
	* @param value an int.
	*/

	public void setInteger(String key, int value)
	{
		container.setProperty(key, Integer.valueOf(value));
		notifyPropsChanged(key);
	}


	/**
	* Sets the property key to a Long of the passed value.
	* @param key the name of the property to set.
	* @param value a long.
	*/

	public void setLong(String key, long value)
	{
		container.setProperty(key, Long.valueOf(value));
		notifyPropsChanged(key);
	}


	/**
	* Sets the property key to the passed value.
	* @param key the name of the property to set.
	* @param value a reference to an Object.
	*/

	public void setProperty(String key, Object value)
	{
		if ((value == null) || ((value instanceof String) && (((String) value).length() == 0)))
		{
			container.removeProperty(key);
		}
		else
		{
			container.setProperty(key, value);
		}

		notifyPropsChanged(key);
	}


	/**
	* Sets the property value for key to the value retrieved using the same key from srcProps.
	* @param key the name of the property to set.
	* @param value a reference to an Object.
	*/

	public void setProperty(String key, Props srcProps)
	{
		setProperty(key, srcProps.getProperty(key));
	}


	/**
	* Sets the property value for key to the value retrieved using srcKey from srcProps.
	* @param key the name of the property to set.
	* @param value a reference to an Object.
	*/

	public void setProperty(String key, Props srcProps, String srcKey)
	{
		setProperty(key, srcProps.getProperty(srcKey));
	}		


	/**
	* Sets the IPropsContainer used to access and store properties for this object.
	* @param container a reference to an IPropsContainer.
	*/

	public void setPropsContainer(IPropsContainer container)
	{
		this.container = container;
	}


	/**
	* Suspends the automatic notification of IPropsChangeListeners.
	* This is useful for when several property changes need to be made without
	* calling the propsChanged method of the IPropsChangeListeners for each one.
	* After this method is called, property changes are queued until the resumeNotify
	* method is called. Each successive call to this method must be matched with a
	* call to resumeNotify in order for notification to work correctly.
	*/

	public void suspendNotify()
	{
		suspendNotify++;
	}



	/**
	* Returns the entire Props as a formatted String.
	*/

	public String toString()
	{
		return (getString());
	}


	

	/* Static methods. */


	/** 
	* Loads a Props from the specified plain text file.
	* Each line of the file contains a <key>=<value> statement that will be evaluated as a String.
	*/

	public static Props loadProps(String file)
	{
		Props p = new Props();
		BufferedReader dis = null;

		try
		{
			File f = new File(file);
			dis = new BufferedReader(new FileReader(f));

			while (true)
			{
				String line = dis.readLine();

				if (line == null)
				{
					break;
				}

				int i = line.indexOf("=");

				if (i == -1)
				{
					continue;
				}

				String key = line.substring(0, i);
				String value = line.substring(i + 1, line.length()).trim();

				p.setProperty(key, value);
			}

			if (dis != null)
			{
				dis.close();
			}
		}
		catch (Exception e)
		{
			System.err.println("Properties could not be loaded.");
			System.err.println(e);

			try
			{
				if (dis != null)
				{
					dis.close();
				}
			}
			catch (Exception ex)
			{
				;
			}
		}

		return (p);
	}



	/**
	* Reads a serialized Props from a file.
	* This method will always return a Props even if the specified file was not found.
	*/

	public static Props readFromFile(String file)
	{
		Props p = new Props();
		FileInputStream in = null;

		try
		{
			File f = new File(file);

			in = new FileInputStream(f);

			byte[] b = new byte[(int) f.length()];

//			System.out.println("f.length() = " + f.length());
//			System.out.println("in.read(b) = " + in.read(b));

			IPropsContainer pc = (IPropsContainer) ObjectSerializer.unserialize(b);

			p.setPropsContainer(pc);
			in.close();
		}
		catch (Exception e)
		{
			System.err.println(e);

			try
			{
				if (in != null)
				{
					in.close();
				}
			}
			catch (Exception ex)
			{
				System.err.println(ex);
			}
		}

		return (p);
	}


	/**
	* Saves the specified Props to a plain text file.
	* Each line of the file contains a <key>=<value> statement that will be evaluated as a String.
	* Returns true on success; false otherwise.
	*/

	public static boolean saveProps(String file, Props p)
	{
		BufferedWriter bw = null;

		try
		{
			File f = new File(file);

			/* Create a vector of who keys and values. */
			bw = new BufferedWriter(new FileWriter(f));

			Vector v = new Vector();
			Enumeration e = p.getKeys().elements();
			String key;
			String value;
			Pair pair;
			StringBuffer line;

			while (e.hasMoreElements())
			{
				key = (String) e.nextElement();

				v.addElement(new Pair(key, p.getString(key)));
			}
			
			/* Alphabetize the keys and write them to the file. */

			e = StringTools.pairSort(v).elements();

			while (e.hasMoreElements())
			{
				pair = (Pair) e.nextElement();
				key = (String) pair.first();
				value = (String) pair.second();
				line = new StringBuffer();

				line.append(key);
				line.append("=");
				line.append(value.trim());
				line.append(EOL);

				bw.write(line.toString(), 0, line.length());
			}

			bw.flush();
			bw.close();
		}
		catch (Exception e)
		{
			/* System.err.println("failed to save props: "+e.toString()); */
			
			if (bw != null)
			{
				try
				{
					bw.close();
				}
				catch (Exception e3)
				{
					;
				}
			}

			return (false);
		}

		return (true);
	}


	/**
	* Writes the serialized Props to a plain text file.
	*/

	public static void writeToFile(String file, Props p)
	{
		FileOutputStream out = null;

		try
		{
			byte[]  b = ObjectSerializer.serialize(p.getPropsContainer());

			if (b != null)
			{
				out = new FileOutputStream(file);

				out.write(b);
				out.flush();
				out.close();
			}
		}
		catch (Exception e)
		{
			if (out != null)
			{
				System.err.println(e);

				try
				{
					out.close();
				}
				catch (Exception e3)
				{
					;
				}
			}
		}
	}
}

