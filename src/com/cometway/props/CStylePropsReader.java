
package com.cometway.props;

import com.cometway.ak.AK;
import com.cometway.io.ReaderInputStream;
import com.cometway.util.DateTools;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;


/*
* An implementation of PropsReaderInterface which uses CStyle formatted Props.
*/

public class CStylePropsReader implements PropsReaderInterface
{
	protected final static int ERROR = -1;
	protected final static int STAND_BY = 0;
	protected final static int READ_WHITESPACE_BEFORE_PROPSKEY = 1;
	protected final static int READ_PROPSKEY = 2;
	protected final static int READ_WHITESPACE_BEFORE_KEY = 3;
	protected final static int READ_KEY = 4;
	protected final static int READ_VALUE = 5;

	protected InputStream dataSource;


	public CStylePropsReader(String source)
	{
		dataSource = new ReaderInputStream(new StringReader(source));
	}


	public CStylePropsReader(InputStream source)
	{
		dataSource = source;
	}


	/**
	* Closes the InputStream used by the parser.
	*/

	public void close() throws PropsException
	{
		if (dataSource != null)
		{
			try
			{
				dataSource.close();
			}
			catch (Exception e)
			{
				throw new PropsException("Could not close InputStream: " + dataSource, e);
			}
			finally
			{
				dataSource = null;
			}
		}
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


	public Props nextProps() throws PropsException
	{
		Props p = null;

		try
		{
			int state = READ_WHITESPACE_BEFORE_PROPSKEY;
			boolean appendEscaped = false;
			StringBuffer b = new StringBuffer();
			StringBuffer s = null;
			String propsKey = null;
			String key = null;
			String value = null;

			do
			{
				char c = (char) dataSource.read();


				// Keep reading until we get an EOF character.

				if (c == -1) break;
				if (c == 65535) break;


				boolean isWhitespace = Character.isWhitespace(c);
				boolean isLetterOrNumber = Character.isLetterOrDigit(c) || (c == '_');

				b.append(c);

				switch (state)
				{
					case ERROR:
					case STAND_BY:
						// no transitions from these to other states.
					break;

					case READ_WHITESPACE_BEFORE_PROPSKEY:
						if (isWhitespace) break;

						s = new StringBuffer();

						state = READ_PROPSKEY;

					// falls through

					case READ_PROPSKEY:
						if (isLetterOrNumber && (s != null))
						{
							s.append(c);
						}

						else if ((c == '{') || isWhitespace)
						{
							// We are done reading the propsKey.

							if (s != null)
							{
								propsKey = s.toString();
								s = null;

								if (p == null)
								{
									p = new Props();
								}

								p.setProperty("key", propsKey);
							}
							
							if (c == '{') state = READ_WHITESPACE_BEFORE_KEY;
						}
						
						else
						{
							state = ERROR;

							throw new PropsException("C-style parsing error: Invalid Props key (" + Integer.toString((int) c) + ")\n" + b.toString());
						}
					break;

					case READ_WHITESPACE_BEFORE_KEY:
						if (isWhitespace) break;

						s = new StringBuffer();
						key = null;
						value = null;

						state = READ_KEY;

					// falls through

					case READ_KEY:
						if (isLetterOrNumber)
						{
							if (s != null)
							{
								s.append(c);
							}
							else
							{
								state = ERROR;

								throw new PropsException("C-style parsing error: Uninitialized PropsKey:\n" + b.toString());
							}
						}


						// We are done reading the key.

						else if ((c == ':') || isWhitespace)
						{
							if (s != null)
							{
								key = s.toString().trim();
								s = null;
							}
							
							if (c == ':')
							{
								state = READ_VALUE;
								s = new StringBuffer();
							}
						}


						// We are done reading this props.

						else if (c == '}')
						{
							state = STAND_BY;
						}
						
						else
						{
							state = ERROR;

							throw new PropsException("C-style parsing error: Unexpected key character (" + c + ")\n" + b.toString());
						}
					break;

					case READ_VALUE:
						if (c == ';')
						{
							if (appendEscaped)
							{
								s.append(c);
								appendEscaped = false;
							}
							else
							{
								// We are done reading the value.

								if (s != null)
								{
									value = s.toString().trim();
									s = null;

									// Create a new Props if we haven't yet.

									if (p == null) p = new Props();

									p.setProperty(key, value);
								}
								
								state = READ_WHITESPACE_BEFORE_KEY;
							}
						}

						else if (c == '\\')
						{
							if (appendEscaped)
							{
								s.append(c);
								appendEscaped = false;
							}
							else
							{
								appendEscaped = true;
							}
						}

						else
						{
							if (s != null)
							{
								if (appendEscaped)
								{
									if (c == 'n')
									{
										s.append('\n');
									}

									else if (c == 'r')
									{
										s.append('\r');
									}

									else
									{
										s.append(c);
									}

									appendEscaped = false;
								}
								else
								{
									s.append(c);
								}
							}
							else
							{
								state = ERROR;

								throw new PropsException("C-style parsing error: StringBuffer s is null. \n" + b.toString());
							}
						}
					break;

					default:
						state = ERROR;

						throw new PropsException("C-style parsing error: Unknown state (" + state + ")\n" + b.toString());
				}
			
			}
			while (state > STAND_BY);

		}
		catch (IOException e)
		{
			throw new PropsException("I/O Error: Could not read from InputStream", e);
		}

//AK.getDefaultReporter().println("CstylePropsReader.nextProps()", "\n" + p);

		return (p);
	}


	public List listProps() throws PropsException
	{
		List list = new Vector();

		try
		{
			Props p = null;
			Props mapProps = null;
			Props typeProps = new Props();

			while (true)
			{
				p = nextProps();

				if (p == null) break;

				String key = p.getString("key");
	
				if (key.equals("data") || (key.length() == 0))
				{
					List keys;

					if (mapProps == null)
					{
						keys = p.getKeys();
					}
					else
					{
						keys = mapProps.getKeys();
					}

					Props pp = new Props();
					int keyCount = keys.size();

					for (int i = 0; i < keyCount; i++)
					{
						String fromKey = (String) keys.get(i);
						Object value = p.getProperty(fromKey);
						String toKey;

						if (mapProps == null)
						{
							toKey = fromKey;
						}
						else
						{
							toKey = mapProps.getString(fromKey);
						}

						String toType = typeProps.getString(fromKey);

						if (toType.equals("Date")) // uses ISO8601 by default
						{
							value = DateTools.parseISO8601String((String) value);
						}

						if (toType.equals("Boolean")) // T or F
						{
							String s = (String) value;

							value = new Boolean(value.equals("T"));
						}

						pp.setProperty(toKey, value);
					}

					list.add(pp);
				}

				if (key.equals("map"))
				{
					mapProps = p;
				}

				else if (key.equals("type"))
				{
					typeProps = p;
				}
			}
		}
		catch (Exception e)
		{
			throw new PropsException("Could not read next Props: " + e, e);
		}

		return (list);
	}
}

