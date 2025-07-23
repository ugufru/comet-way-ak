
package com.cometway.email;


import com.cometway.ak.*;
import com.cometway.props.Props;
import com.cometway.util.*;
import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;


/**
* This agent periodically checks for new POP3 messages, passing routing the message
* to the registered RequestAgent matching the message recipient's email address.
*/

public class ReceivePOP3EmailAgent extends ScheduledAgent
{
	Socket		socket;
	BufferedReader  in;
	PrintWriter     out;
	boolean		connected = false;


	/**
	* Initializes the default Props for this agent:
	* "schedule" is specifies the schedule this agent uses to check the POP3 server (default: every 60 seconds),
	* "pop3_host" is the POP3 server's host name (default: localhost),
	* "pop3_port" is the POP3 server's port (default: 110),
	* "username" is the POP3 account username,
	* "password" is the POP3 account password,
	* "max_trys" is the number of connection attempt to the POP3 server before giving up,
	* "delete_messages" is set to true if the messages are to be deleted after downloading (default: true).
	*/

	public void initProps()
	{
		setDefault("schedule", "between 0:0:0-23:59:59 every 60s");
		setDefault("pop3_host", "localhost");
		setDefault("pop3_port", "110");
		setDefault("username", "agent");
		setDefault("password", "agent");
		setDefault("max_trys", "100");
		setDefault("delete_messages", "true");
	}


	/**
	* On wakeup, this agent connects to the POP3 server and downloads any new messages it finds.
	* New messsages are delivered to RequestAgents using the deliverMessage method of this class.
	*/

	public void wakeup()
	{
		try
		{
			String username = getString("username");
			String password = getString("password"); 

			openConnection();
			logon(username, password);

			Vector v = listMessages();
			int count = v.size();

			if (count > 0)
			{
				for (int i = 0; i < count; i++)
				{
					IntegerPair	pr = (IntegerPair) v.elementAt(i);
					int		n = pr.firstInt();
					int		size = pr.secondInt();
	
					println("Downloading message: " + n + '/' + count + " (" + size + " bytes)");
	
					IMessage m = getMessage(n);
	
					deliverMessage(m);
	
					if (getBoolean("delete_messages"))
					{
						deleteMessage(n);
					}
				}
			}

			closeConnection();
		}
		catch (Exception e)
		{
			error("POP3 Error", e);
		}
	}



	/**
	* This method creates an AgentRequest from the specified IMessage, and delivers
	* it to the RequestAgent that is registered as the message recipient.
	*/

	public void deliverMessage(IMessage m)
	{
		String to = m.getHeaderInfo("to");
		String from = m.getHeaderInfo("from");

		if (to.length() == 0)
		{
			error("Could not route message from " + from + "; no \"To\" field specified");
		}
		else
		{
			println("Received (" + from + " -> " + to + ")");

			try
			{
				Pair pair=EmailTools.parseEmailAddress(to);

				String name = (String) pair.first();

				RequestAgent    agent = (RequestAgent) getServiceImpl(name);

				if (agent == null)
				{
					if (name.indexOf("@") != -1)
					{
						name = name.substring(0, name.indexOf("@"));
						agent = (RequestAgent) getServiceImpl(name);

						if (agent == null)
						{
							error("Could not route message to " + to);
						}
					}
				}

				if (agent != null)
				{
					Props		p = new Props();
					Enumeration     e = m.getHeaders();

					while (e.hasMoreElements())
					{
						String  headerName = (String) e.nextElement();

						p.setProperty(headerName.toLowerCase(), m.getHeaderInfo(headerName));
					}

					p.setProperty("request_type", "POP3");
					p.setProperty("request_id", "POP3:" + from);
					p.setProperty("reply_to", from);
					p.setProperty("message", m);

					AgentRequest request = new AgentRequest(p);

					agent.handleRequest(request);
				}
			}
			catch (Exception e)
			{
				error("Could not route message", e);
			}
		}
	}


	/**
	* Opens a connection to the POP3 server for the agent to use.
	*/

	protected void openConnection()
	{
		int trys = 0;

		while (true)
		{
			String pop3_host = getString("pop3_host");
			int pop3_port = getInteger("pop3_port");
			String serverName = pop3_host + ':' + pop3_port;

			try
			{
				socket = new Socket(pop3_host, pop3_port);;
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
				connected = true;

				// read the welcome line

				getLine();
				println("Connected to " + serverName + " at " + getDateTimeStr());

				break;
			}
			catch (Exception e)
			{
				trys++;

				if (trys >= getInteger("max_trys"))
				{
					error("Cannot connect to " + serverName, e);;
					connected = false;
					break;
				}
				else
				{
					warning("Trying to connect again (" + trys + ")");
				}
			}
		}
	}


	/**
	* Deletes the specified message from the POP3 server.
	*/
	
	protected boolean deleteMessage(int msg_num)
	{
		send("DELE " + msg_num);

		return (okResponse());
	}


	/**
	* Closes the connection to the POP3 server.
	*/

	protected void closeConnection()
	{
		send("QUIT");

		try
		{
			in.close();
			out.close();
			socket.close();
		}
		catch (Exception e)
		{
			error("Error closing socket ", e);
		}

		debug("Connection closed");

		connected = false;
	}


	/**
	* Reads a line of input from the POP3 server.
	*/

	protected String getLine()
	{
		if (connected)
		{
			String  line;
			int     x = 1;

			while (true)
			{
				try
				{
					line = in.readLine();

					if (line != null)
					{
						return line;
					}
				}
				catch (Exception e)
				{
					error("Cannot read line", e);
				}
			}
		}

		return ("");
	}


	/**
	* Returns the number of the highest message accesed.
	*/

	protected int last()
	{
		send("LAST");

		String line = getLine();

		return parseInt(line.substring(4, line.length()));
	}


	/**
	* Returns a vector of pairs. Each pair contains (int {msg_num} ,int {msg_size})<br>
	*/

	protected Vector listMessages()
	{
		Vector  v = new Vector();

		send("LIST");

		String line = getLine();
		line = getLine();

		while (line.equals(".") == false)
		{
			v.addElement(splitInt(line));

			line = getLine();
		}

		return (v);
	}


	/**
	* This method lists a message on the POP3 server by its message number.
	* Returns the pair (int {msg_num} ,int {msg_size})
	* @param msg_num The message number to list.
	* @return Returns an IntegerPair, first is the message number, second is the message size.
	*/

	protected IntegerPair listMessage(int msg_num)
	{
		IntegerPair result = null;

		send("LIST " + msg_num);

		String line = getLine();

		if (okResponse(line))
		{
			result = splitInt(line.substring(4, line.length()));
		}

		return (result);
	}


	/**
	* Sends the logon command to the POP3 server using the specified username and password.
	*/

	protected boolean logon(String username, String password)
	{
		boolean result = false;

		if (connected)
		{
			send("USER " + username);

			result = okResponse();

			if (result)
			{
				send("PASS " + password);
		
				result = okResponse();
			}
		}

		return (result);
	}


	/**
	* Sends a NOOP command to the POP3 server.
	*/

	protected void noop()
	{
		send("NOOP");
		getLine();
	}


	/**
	* Return true if input String starts with '+OK'.
	*/


	protected boolean okResponse(String s)
	{
		if (s == null)
		{
			return false;
		}

		return (s.startsWith("+OK"));
	}


	/** Returns true if the server returned '+OK'. */
	
	protected boolean okResponse()
	{
		return okResponse(getLine());
	}


	/** Parses a String into an int. */
	
	private int parseInt(String s)
	{
		return Integer.parseInt(s);
	}


	/**
	* Return a vector holding  (Integer {msg_num},(String {msg_header},String {msg_body})).
	* This vector contains all the messages.
	*/

	protected Vector getAllMessages()
	{
		int     num_messages = stat().firstInt();
		Vector  v = new Vector();

		for (int idx = 1; idx <= num_messages; idx++)
		{
			v.addElement(new Pair(Integer.valueOf(idx), getMessage(idx)));
		}

		return v;
	}


	/**
	* Return a vector holding  (Integer {msg_num},(String {msg_header},String {msg_body})).
	* start - first message to get
	* stop - last message to get
	*/

	protected Vector getRange(int start, int stop)
	{
		int     num_messages = stat().firstInt();

		if (num_messages > stop)
		{
			num_messages = stop;
		}

		Vector  v = new Vector();

		for (int idx = start; idx <= num_messages; idx++)
		{
			v.addElement(new Pair(Integer.valueOf(idx), getMessage(idx)));
		}

		return v;
	}


	/**
	* Downloads the specified message from the POP3 server and returns it as an IMessage.
	*/
	
	protected IMessage getMessage(int msg_num)
	{
		Message		m = null;
		StringBuffer    b = new StringBuffer();

		send("RETR " + msg_num);

		String line = getLine();

		if (okResponse(line))
		{
			while (true)
			{
				line = getLine();

				if ((line == null) || line.equals("."))
				{
					m = new Message(b.toString());
					break;
				}

				b.append(line + "\n");
			}
		}

		return (m);
	}


	/**
	* Parses a Vector containing Pairs referencing message headers and bodies into a Vector
	* of Hashtables containing message attributes including the message body.
	*/
	
	protected Vector parseAllMessages(Vector v)
	{
		Enumeration     e;
		Vector		retv = new Vector();

		e = v.elements();

		Pair    p, pp;

		while (e.hasMoreElements())
		{
			p = (Pair) e.nextElement();

			retv.addElement(new Pair(p.first(), parseMessage((Pair) p.second())));
		}

		return retv;
	}


	/**
	* Parses the message header and body in the specified Pair into a Hashtable containing attributes,
	* with the "body" attribute set to the specified body.
	*/
	
	protected Hashtable parseMessage(Pair p)
	{
		return parseMessage((String) p.first(), (String) p.second());
	}


	/**
	* Parses the message header and body into a Hastable containing attributes,
	* with the "body" attribute set to the specified body.
	*/

	protected Hashtable parseMessage(String header, String body)
	{
		String		s, a = null, b = null;
		int		i;
		Hashtable       ht = new Hashtable();
		StringBuffer    sb = new StringBuffer("");
		StringTokenizer st = new StringTokenizer(header, "\n", false);

		s = st.nextToken();

		while (st.hasMoreTokens())
		{
			i = s.indexOf(":");

			if (i != -1)
			{
				if (a != null && b != null)
				{
					ht.put(a, b);
				}

				a = s.substring(0, i);
				b = s.substring(i + 1, s.length());
			}
			else
			{
				b = new String(b + "\n" + s);
			}

			s = st.nextToken();
		}

		ht.put("body", body);

		return ht;
	}


	/**
	* End connection to server.
	*/

	protected void quit()
	{
		send("QUIT");
		getLine();
	}


	/**
	* Reset maildrop to previous state.
	*/

	protected void reset()
	{
		send("RESET");
		getLine();
	}


	/**
	* If connected, send a line to the server.
	* pop_msg - the message to send to the server
	*/

	protected int send(String pop_msg)
	{
		if (connected)
		{
			int x = 1;

			while (true)
			{
				debug("Sending: " + pop_msg);

				try
				{
					out.print(pop_msg + "\r\n");
					out.flush();

					return 1;
				}
				catch (Exception e)
				{
					error("Error sending, server is dead");

					connected = false;

					return -1;
				}
			}
		}
		else
		{
			return -1;
		}
	}


	protected IntegerPair splitInt(String s)
	{
		int a = parseInt(s.substring(0, s.indexOf(" ")));
		int b = parseInt(s.substring(s.indexOf(" ") + 1, s.length()));
		IntegerPair result = new IntegerPair(a, b);

		return (result);
	}


	/**
	* Get the status of the current user.
	* Returns a (<num_message>,<total_size>) int pair
	*/

	protected IntegerPair stat()
	{
		String  r = null;

		send("STAT");

		r = getLine();

		if (okResponse(r))
		{
			return splitInt(r.substring(4, r.length()));
		}
		else
		{
			return null;
		}
	}
}

