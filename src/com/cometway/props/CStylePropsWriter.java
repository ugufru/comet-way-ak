
package com.cometway.props;

import com.cometway.util.DateTools;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;


/*
* Implement this interface to implement a class capable of writing Props.
*/

public class CStylePropsWriter implements PropsWriterInterface
{
	protected final static SimpleDateFormat ISO8601_DATEFORMAT = DateTools.ISO8601_DATEFORMAT;
	protected final static SimpleDateFormat ISO8601_TIMEFORMAT = DateTools.ISO8601_TIMEFORMAT;
	protected final static String EOL = System.getProperty("line.separator");

	protected Writer w;
	protected StringBuffer b;
	protected boolean mapped;
	protected boolean sorted;
	protected boolean typed;
	protected boolean whitespace;


	public CStylePropsWriter()
	{
		StringWriter w = new StringWriter();

		b = w.getBuffer();

		this.w = w;
	}


	public CStylePropsWriter(Writer w)
	{
		this.w = w;
	}


	public String toString()
	{
		String s = b.toString();

		if (s == null) s = w.toString();

		return (s);
	}


	/**
	* Inserts backslash before \n character.
	* Inserts backslash before \r character.
	* Inserts backslash before backslash.
	* Inserts backslash before semi-colon.
	*/

	public static String encode(String s)
	{
		StringBuilder b = new StringBuilder();
		int length = s.length();

		for (int i = 0; i < length; i++)
		{
			char c = s.charAt(i);

			if (c == '\\')
			{
				b.append('\\');
				b.append('\\');
			}

			else if (c == ';')
			{
				b.append('\\');
				b.append(';');
			}

			else if (c == '\n')
			{
				b.append('\\');
				b.append('n');
			}

			else if (c == '\r')
			{
				b.append('\\');
				b.append('r');
			}

			else b.append(c);
		}

		return (b.toString());
	}


	/**
	* Removes backslash before \n character.
	* Removes backslash before \r character.
	* Removes backslash before backslash.
	* Removes backslash before semi-colon.
	*/

	public static String decode(String s)
	{
		StringBuilder b = new StringBuilder();
		int length = s.length();

		for (int i = 0; i < length; i++)
		{
			char c = s.charAt(i);

			if (c == '\\')
			{
				if ((i + 1) < length)
				{
					if (s.charAt(i + 1) == '\\')
					{
						b.append('\\');
						i++;
					}

					else if (s.charAt(i + 1) == ';')
					{
						b.append(';');
						i++;
					}

					else if (s.charAt(i + 1) == 'n')
					{
						b.append('\n');
						i++;
					}

					else if (s.charAt(i + 1) == 'r')
					{
						b.append('\r');
						i++;
					}
				}
			}

			else
			{
				b.append(c);
			}
		}

		return (b.toString());
	}


	public void setWhiteSpace(boolean enabled)
	{
		whitespace = enabled;
	}


	public void setSorted(boolean enabled)
	{
		sorted = enabled;
	}
	
	
	public void setMapped(boolean enabled)
	{
		mapped = enabled;
	}
	
	
	public void setTyped(boolean enabled)
	{
		typed = enabled;
	}
	
	
	/**
	* String, Number, and Boolean are written as strings.
	* Dates are written as ISO-8601 compatible.
	* IPropsContainers are ignored at the moment (no nesting).
	* Object serialization is not yet supported.
	*/

	public void writeProperty(String key, Object o) throws PropsException
	{
		try
		{
			if ((o instanceof String) || (o instanceof Number))
			{
				String s = encode(o.toString());

				if (whitespace) w.write("\t");

				w.write(key);
				w.write(':');

				if (whitespace) w.write(' ');

				w.write(s);
				w.write(';');

				if (whitespace) w.write(EOL);
			}

			else if (o instanceof Boolean)
			{
				Boolean b = (Boolean) o;

				if (b.equals(true))
				{
					if (whitespace) w.write("\t");

					w.write(key);
					w.write(':');

					if (whitespace) w.write(' ');

					w.write("T");

					w.write(';');

					if (whitespace) w.write(EOL);
				}
			}

			else if (o instanceof Date)
			{
				String s = null;
				Date d = (Date) o;
				Calendar c = Calendar.getInstance();
				c.setTime(d);

				if ((c.get(c.HOUR_OF_DAY) + c.get(c.MINUTE) + c.get(c.SECOND)) == 0)
				{
					s = ISO8601_DATEFORMAT.format(d);
				}
				else
				{
					s = ISO8601_DATEFORMAT.format(d) + "T" + ISO8601_TIMEFORMAT.format(d);
				}

				if (whitespace) w.write("\t");

				w.write(key);
				w.write(':');

				if (whitespace) w.write(' ');

				w.write(s);
				w.write(';');

				if (whitespace) w.write(EOL);
			}

			else if (o instanceof IPropsContainer)
			{
				// We can't really support this yet.
			}

			else
			{
				// We don't support Object serialization so far.
			}
		}
		catch (Exception e)
		{
			throw new PropsException("Could not write property: " + key + " = " + o, e);
		}
	}


	public void writeProps(Props p) throws PropsException
	{
		try
		{
			List keyList = p.getKeys();
			int count = keyList.size();

			if (sorted)
			{
				Collections.sort(keyList);
			}

			String key = p.getTrimmedString("key");

			if (key.length() > 0)
			{
				w.write(key);

				if (whitespace) w.write(EOL);
			}

			w.write('{');

			if (whitespace) w.write(EOL);

			for (int i = 0; i < count; i++)
			{
				key = (String) keyList.get(i);

				if (key.equals("key") == false)
				{
					Object o = p.getProperty(key);
					
					writeProperty(key, o);
				}
			}

			if (whitespace)
			{
				w.write("}");
				w.write(EOL);
				w.write(EOL);
			}
			else
			{
				w.write('}');
			}
		}
		catch (Exception e)
		{
			throw new PropsException("Could not write Props:\n" + p, e);
		}
	}

	
	protected Props getMapFromList(List list)
	{
		Props map = new Props();


		// Get a list of unique keys that are used in the list.
		
		List keyList = new Vector();
		int count = list.size();

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) list.get(i);
			List keys = p.getKeys();
			int keyCount = keys.size();
			
			for (int x = 0; x < keyCount; x++)
			{
				String key = (String) keys.get(x);

				if (keyList.contains(key) == false) keyList.add(key);
			}
		}

		// Create a numbered key map from the unique key list.
		
		count = keyList.size();

		if (count > 0)
		{
			if (sorted) Collections.sort(keyList);

			for (int i = 0; i < count; i++)
			{
				String key = (String) keyList.get(i);

				map.setProperty(Integer.toString(i), key);
			}
			
			map.setProperty("key", "map");
		}

		return (map);
	}
	
	
	protected Props getMappedProps(Props map, Props p)
	{
		Props pp = new Props();
		List keys = map.getKeys();
		int count = keys.size();
		
		for (int i = 0; i < count; i++)
		{
			String key = (String) keys.get(i);
			String mappedKey = map.getString(key);
			Object o = p.getProperty(mappedKey);

			pp.setProperty(key, o);
		}

		return (pp);
	}


	protected static Props getInvertedProps(Props p)
	{
		Props pp = new Props();
		List keys = p.getKeys();
		int count = keys.size();

		for (int i = 0; i < count; i++)
		{
			String key = (String) keys.get(i);
			Object value = p.getProperty(key);

			if (value != null)
			{
				String s = value.toString();

				pp.setProperty(s, key);
			}
		}

		return (pp);
	}

	
	public void writePropsList(List list) throws PropsException
	{
		int count = list.size();

		if (count > 0)
		{
			if (mapped && (count > 1))
			{
				writeMappedPropsList(list);
			}
			else
			{
				if (typed) writeTypeProps(list);

				for (int i = 0; i < count; i++)
				{
					Props p = (Props) list.get(i);

					writeProps(p);
				}
			}
		}
	}


	public void writeMappedPropsList(List list) throws PropsException
	{
		Props map = getMapFromList(list);
		
		writeProps(map);

		if (typed) writeTypeProps(list, map);

		int count = list.size();

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) list.get(i);

			p = getMappedProps(map, p);
			
			writeProps(p);
		}
	}


	public void writeTypeProps(List list) throws PropsException
	{
		writeTypeProps(list, null);
	}


	public void writeTypeProps(List list, Props map) throws PropsException
	{
		Props tp = new Props();
		int count = list.size();

		if (map != null)
		{
			map = getInvertedProps(map);
		}

		for (int i = 0; i < count; i++)
		{
			Props p = (Props) list.get(i);

			List keys = p.getKeys();
			int keyCount = keys.size();

			for (int k = 0; k < keyCount; k++)
			{
				String fromKey = (String) keys.get(k);
				String toKey = null;
				Object value = null;

				if (map == null)
				{
					toKey = fromKey;
				}
				else
				{
					toKey = map.getString(fromKey);
				}

				value = p.getProperty(fromKey);

				if (tp.hasProperty(toKey) == false)
				{
					if (value instanceof Date) tp.setProperty(toKey, "Date");
					else if (value instanceof Number) tp.setProperty(toKey, "Number");
					else if (value instanceof Boolean) tp.setProperty(toKey, "Boolean");
				}
			}
		}

		tp.setProperty("key", "type");
		writeProps(tp);
	}
}


