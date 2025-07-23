
package com.cometway.net;

import java.util.*;
import com.cometway.props.*;
import com.cometway.util.*;


/**
 * This PropsContainer is used by the HTTPLoader to store cookies
 *
 */
public class CookiePropsContainer extends AbstractPropsContainer
{
	private Vector  keys;
	private String  name;
	private String  value;
	private String  expires;
	private String  path;
	private String  domain;
	private Boolean secure;

	public CookiePropsContainer()
	{
		keys = new Vector();

		keys.addElement("cookie");
		keys.addElement("name");
		keys.addElement("value");
		keys.addElement("expires");
		keys.addElement("path");
		keys.addElement("domain");
		keys.addElement("secure");
	}


	public CookiePropsContainer(String cookie)
	{
		setCookie(cookie);
	}


	protected String decode(String s)
	{
		if (s == null)
		{
			return ("");
		}

		return (HTTPClient.unconvert(s));
	}


	protected String encode(String s)
	{
		if (s == null)
		{
			return ("");
		}

		return (HTTPClient.convert(s));
	}


	protected String getCookie()
	{
		String  cookie = null;

		if (name != null)
		{
			StringBuffer    b = new StringBuffer();

			b.append(name);
			b.append('=');
			//			b.append(encode(value));
			b.append(value);

			if (expires != null)
			{
				b.append("; expires=");
				b.append(encode(expires));
			}

			if (path != null)
			{
				b.append("; path=");
				b.append(encode(path));
			}

			if (domain != null)
			{
				b.append("; domain=");
				b.append(encode(domain));
			}

			if (secure.booleanValue())
			{
				b.append("; secure");
			}

			cookie = b.toString();
		}

		return (cookie);
	}


	protected Object getCookieValue(String cookie, String name)
	{
		Object  value = null;

		if (cookie != null)
		{
			String  lowerCookie = cookie.toLowerCase();


			// the secure parameter is a special case; we return a Boolean.

			if (name.equals("secure"))
			{
				value = Boolean.valueOf(lowerCookie.trim().endsWith("secure"));
			}
			else
			{


				// parse out the parameter value from the cookie.

				int     i = lowerCookie.indexOf(name + "=");

				if (i != -1)
				{
					i += name.length() + 1;		// start of parameter value

					int     j = lowerCookie.indexOf(';', i);	// end of parameter value
					String  s;

					if (j == -1)
					{
						s = cookie.substring(i).trim();
					}
					else
					{
						s = cookie.substring(i, j).trim();
					}

					if (s.startsWith("\""))
					{
						s = s.substring(1);
					}

					if (s.endsWith("\""))
					{
						s = s.substring(0, s.length() - 1);
					}

					value = decode(s);
					//					value = s;
				}
			}
		}

		return (value);
	}


	public Object getProperty(String key)
	{
		if (key.equals("name"))
		{
			return (name);
		}
		else if (key.equals("value"))
		{
			return (value);
		}
		else if (key.equals("expires"))
		{
			return (expires);
		}
		else if (key.equals("path"))
		{
			return (path);
		}
		else if (key.equals("domain"))
		{
			return (domain);
		}
		else if (key.equals("secure"))
		{
			return (secure);
		}
		else if (key.equals("cookie"))
		{
			return (getCookie());
		}
		else
		{
			return (null);
		}
	}


	public boolean removeProperty(String key)
	{
		boolean success = key.equals("name");

		if (success)
		{
			name = null;
		}
		else
		{
			success = key.equals("value");

			if (success)
			{
				value = null;
			}
			else
			{
				success = key.equals("expires");

				if (success)
				{
					expires = null;
				}
				else
				{
					success = key.equals("path");

					if (success)
					{
						path = null;
					}
					else
					{
						success = key.equals("domain");

						if (success)
						{
							domain = null;
						}
						else
						{
							success = key.equals("secure");

							if (success)
							{
								secure = null;
							}
							else
							{
								success = key.equals("cookie");

								if (success)
								{
									setCookie(null);
								}
							}
						}
					}
				}
			}
		}

		return (success);
	}


	protected void setCookie(String cookie)
	{
		if (cookie == null)
		{
			name = null;
			value = null;
			expires = null;
			path = null;
			domain = null;
			secure = null;
		}
		else
		{
			cookie = cookie.trim();

			int     i = cookie.indexOf('=');

			if (i == -1)
			{
				setCookie(null);	// this method is a bit re-entrant here i guess.
			}
			else
			{
				name = cookie.substring(0, i);
				int j = cookie.indexOf(";");
				value = cookie.substring(i+1,j);
				//				System.out.println("####### COOKIE: "+cookie);
				//				System.out.println("***************************  VALUE="+value);
				//				value = (String) getCookieValue(cookie, name);
				expires = (String) getCookieValue(cookie, "expires");
				path = (String) getCookieValue(cookie, "path");
				domain = (String) getCookieValue(cookie, "domain");
				secure = (Boolean) getCookieValue(cookie, "secure");
			}
		}
	}


	public void setProperty(String key, Object value)
	{
		if (key.equals("name"))
		{
			name = (String) value;
		}
		else if (key.equals("value"))
		{
			this.value = (String) value;
		}
		else if (key.equals("expires"))
		{
			expires = (String) value;
		}
		else if (key.equals("path"))
		{
			path = (String) value;
		}
		else if (key.equals("domain"))
		{
			domain = (String) value;
		}
		else if (key.equals("secure"))
		{
			secure = (Boolean) value;
		}
		else if (key.equals("cookie"))
		{
			setCookie((String) value);
		}
	}


	public void copy(IPropsContainer ipc)
	{
		setCookie((String) ipc.getProperty("cookie"));
	}


	public Enumeration enumerateProps()
	{
		return (keys.elements());
	}


}

