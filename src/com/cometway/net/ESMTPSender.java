
package com.cometway.net;

import java.io.*;
import java.net.*;
import java.util.*;

import com.cometway.util.*;
import com.cometway.email.*;


/**
 * This class implements the ESMTP protocol for the client side. Given a 
 * socket, it will connect to a server which talks ESMTP/SMTP and send messages
 *
 */
public class ESMTPSender implements IMessageSender
{
   public final static String kCRLF = "\r\n";

   // these represent the response code classes;  these are the values that
   //  receiverResponseType will store
   public final static int kSuccess = 2;
   public final static int kFailure = 3;
   public final static int kTempFailure = 4;
   public final static int kError   = 5;
	public final static int kAuthFailed = 6;

   // this is a special case of the above classes and may be used by a send command if for
   //  some reason a send or a recieve failed entirely 
   public final static int kCommError = 7;

   // this is another special case of the above classes and may be used by a send command
   //  if the reciever SMTP does not seem to implement the protocol properly 
   public final static int kBadESMTP = 11;

   // this is another case of the above classes and may be used by certain commands
   //  to indicate that an error occured that was local to this sender 
   public final static int kSenderError = 13;

   // this is another case of the above classes to indicate that the sender is in an
   //  initial state.  this is another value taken by receiverResponseType, but perhaps
   //  this should be dealt with some other way.  Q.
   public final static int kInitialState = 17;

   public final static int kDefaultSMTPPort = 25;

   // This is the default SMTP host to forward to if hostname cannot be queried
   public final static String kDefaultSMTPHostName = "macmail.cometway.com";

   // the default blocksize of characters written to the connection (but not
   //  necessarily flushed).
   public final static int kDefaultBlockSize = 128;

   // the default blocksize of characters written to the connection (but not
   //  necessarily flushed)
   protected int blockSize = kDefaultBlockSize;
   
   // the InetAddress associated with kDefaultSMTPHostName
   InetAddress defaultSMTPHost;

   // This is the local hostname stored locally
   protected String myHost = "localhost.localdomain";

   // Name of the host server
   protected String SMTPHostName = null;

   // an InetAddress for the host server
   private InetAddress SMTPHost = null;
   
   // out points of communication: a socket to the SMTPHostName, and a reader and writer
   // XXXX make the socket NOT protected
   protected Socket       SMTPSocket;
   private BufferedReader in;
   private PrintWriter    out;
   
   // Flag for whether HELO/EHLO command was issued successfully. True = has succesfully
   //  sentHelo with HELO/EHLO 
   protected boolean sentHelo = false;

   // Flag for expert mode.  in expert mode all commands will be executed as literally as
   //  possible with respect to RFC (ie there will be no connection attempt if not
   //  connected, etc).  
   protected boolean expert = false;

   // isConnected is true if we think that we have good I/O with the server, else false.
   private boolean isConnected = false;

   // String buffer storing the last receiver response (if any).  if no receiver
   //  responses have been read or if an attempt to read a response from the receiver was
   //  unseccessful, this should be an empty String
   protected String receiverResponse = "";
   
   // an int storing the last receiver response code (should be zero if no server
   //  responses have been read or if last attempt to read response was unsuccessful)
   protected int receiverResponseCode = 0;

   // this variable notes the type of response code returned by the line from the
   //  sender.
   protected int receiverResponseType = kInitialState;
   
   // the port used for our SMTP communication
   protected int port = kDefaultSMTPPort;

   // verbose flag: print out info about server's responses if set to true.
   private boolean verbose = false;

   // debug flag: print out debugging info if true
   private boolean debug = false;
   
   /** this is somewhat of a hack to satisfy a subclasses needs XXXX
    */
   public ESMTPSender()
   {
      ;
   }

   /**
    */
   public ESMTPSender(String SMTPHostName)
   {
      this.SMTPHostName = SMTPHostName;

      setDefaultSMTPHost();

      try 
      {
	 myHost = InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException e) 
      {
	 print("Error looking up local host: " + e.toString());
      }

      // connect returns a boolean, but it will set isConnected, which is all we care
      //  about here
      connect();
   }
   
   /**
    */
   public ESMTPSender(String SMTPHostName, int port)
   {
      this.SMTPHostName = SMTPHostName;
      this.port         = port;

      setDefaultSMTPHost();
	 
      try 
      {
	 myHost = InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException e) 
      {
	 print("Error looking up local host: " + e.toString());
      }

      // connect returns a boolean, but it will set isConnected, which is all we care
      //  about here
      if (!connect())
	 print("Error connecting to host");
   }
   
   /**
    */
   public ESMTPSender(Socket sock)
   {
      this.SMTPHostName = sock.getInetAddress().getHostName();
      this.SMTPSocket   = sock;
      this.port         = sock.getPort();

      setDefaultSMTPHost();

      // connect returns a boolean, but it will set isConnected, which is all we care
      //  about here
      connect(SMTPSocket);
   }

	// a couple of INTERNAL exceptions.  these will usually generate the
	//  appropriate EXTERNAL exceptions
	class ESMTPSendException extends Exception
	{
		private String line;

		public ESMTPSendException(String line)
	   {
			this.line = line;
		}

		public String getLine()
	   {
			return line;
		}
	}

	class ESMTPReadException extends Exception
	{
		private String line;

		public ESMTPReadException(String line)
	   {
			this.line = line;
		}

		public String getLine()
	   {
			return line;
		}
	}

   /**
    */
   protected void setDefaultSMTPHost()
   {
      try
      {
			defaultSMTPHost = InetAddress.getByName(kDefaultSMTPHostName);
      }
      catch (UnknownHostException e)
      {
			error("unable to set default SMTP host.  there may be troubles later");
      }
   }

   /* various accessor/settor methods: 
    */

   /** getSMTPHostName: method returns the current SMTPHostName
    */
   public String getSMTPHostName()
   {
      return this.SMTPHostName;
   }

   /** setSMTPHost: method sets the SMTPHostName.  THIS METHOD RESETS THE SMTP
    *   CONNECTION: it is assumed that the caller of this method desires a connection to
    *   a new SMTP receiver.  this version takes the hostname
    */
   public boolean setSMTPHost(String SMTPHostName)
   {
      this.SMTPHostName = SMTPHostName;

      if (isConnected)
      {
			// try to quit, ignoring success/failure
			sendQuit();
			disconnect();
      }

      return connect();
   }

   /** setSMTPHost: method sets the SMTPHostName.  THIS METHOD RESETS THE SMTP
    *   CONNECTION: it is assumed that the caller of this method desires a connection to
    *   a new SMTP receiver.  this version takes the InetAddress 
    */
   public boolean setSMTPHost(InetAddress SMTPHost)
   {
      return setSMTPHost(SMTPHost.getHostName());
   }

   /**
    */
   public boolean setPort(int port)
   {
      this.port = port;

      if (isConnected)
      {
			// try to quit, ignoring success/failure
			sendQuit();
			disconnect();
      }

      return connect();
   }

   /**
    */
   public int getPort()
   {
      return this.port;
   }

   /**
    */
   public String getResponse()
   {
      return this.receiverResponse;
   }

   /**
    */
   public int getResponseCode()
   {
      return this.receiverResponseCode;
   }
   
   /**
    */
   public int getResponseType()
   {
      return this.receiverResponseType;
   }
   
   /** setVerbose: method sets the verbosity flag
    */
   public void setVerbose(boolean verbose)
   {
      this.verbose = verbose;
   }

   /** setDebug: method sets the debugging flag
    */
   public void setDebug(boolean debug)
   {
      this.debug = debug;
   }

   /**
    */
   public boolean isConnected()
   {
      return this.isConnected;
   }

   /**
    * Connect() opens a socket to SMTPHostName (SimpleMailServer) and instantiates in/out
    *  streams.
    */
   public boolean connect()
   {
      verbosePrint("BEGIN connect() method");

      // upon connecting we should reset all instance state
      receiverResponse     = "";
      receiverResponseCode = 0;
      receiverResponseType = kInitialState;
      isConnected = false;
      sentHelo    = false;

      // XXXX make sure that all of this state is kosher wrt to socket etc.
      if (SMTPHost == null)
      {
			try
			{
				SMTPHost = InetAddress.getByName(SMTPHostName);
			}
			catch(UnknownHostException uhe)
			{
				verbosePrint(SMTPHostName + " not \"A\" DNS entry, defaulting to " +
								 kDefaultSMTPHostName);
				
				// this violates the ability of the user of this receiver to set and maintain
				//  SMTP communication to a particular host, but it comes closer to
				//  gauranteed delivery.  just be sure that kDefaultSMTPHostName is set to a
				//  good receiver 
				SMTPHostName = kDefaultSMTPHostName;
				SMTPHost     = defaultSMTPHost;
			}
      }

      try
      {
			SMTPSocket = new Socket(SMTPHost, port);
      }
      catch (IOException e)
      {
			verbosePrint("troubles opening socket to host: " + SMTPHostName +
							 ", using port: " + port);
			return false;
      }

      return connect(SMTPSocket);
   }

   // XXXXX getting messy here with alternate sockets, etc...  and should these
   //  connect() meths be public?
   /**
    */
   public boolean connect(Socket altSocket) throws ESMTPException
   {
      try
      {
			in  = new BufferedReader(new InputStreamReader(SMTPSocket.getInputStream()));
			out = new PrintWriter(SMTPSocket.getOutputStream(), true);
      }
      catch (IOException e)
      {
			verbosePrint("troubles opening io streams to host: " + SMTPHostName);
			// all state should be in init yet
			return false;
      }

		try
		{
			if (readLine())
			{
				if (receiverResponseCode == 421)
				{
					// server responded with service not available, closing transmission channel
					receiverResponseType = kError;
				}
				else 
				{
					isConnected = true;
					receiverResponseType = kSuccess;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.CONNECT);
		}

		if (receiverResponseType != kSuccess)
      {
			isConnected = false;
      }
      else isConnected = true;

      if (isConnected)
			verbosePrint("Connection Established: " + receiverResponse);
      else verbosePrint("Connection Not Established: " + receiverResponse);

      verbosePrint("END connect() method: " + SMTPHostName + ":" + port);

      return isConnected;
   }

   /**
    */
   public void disconnect()
   {
      verbosePrint("BEGIN disconnect() method");

      try {in.close();} catch(Exception e) {;}
      try {out.close();} catch(Exception e) {;}
      try {SMTPSocket.close();} catch(Exception e) {;}

			//      {
			//			error("failed to close connection properly: " + e.toString());
			//      }
		
      isConnected = false;
      // Q.  might as well set this here
      sentHelo    = false;

      verbosePrint("END disconnect() method");
   }
   
   /** readLine: method reads one response from the SMTP receiver (a response may
    *   consist of multiple lines of text).
    *  @return returns true if a response was successfully read and the receiverResponse
    *            and receiverResponseCode state variables were correctly set, false
    *            otherwise 
    */
   protected boolean readLine() throws ESMTPReadException
   {
      verbosePrint("readLine(): reading line...");
      String response = "";

      try
		{
			String line = in.readLine();
			response    = line;

			// this loop handles multi-line replies...
			while (line.charAt(3) == '-')
			{
				line     = in.readLine();
				// possibly more efficient to use a StringBuffer here
				response = response + "\n" + line;
			}
      }
      catch (Exception e)
		{
			error("readLine() failed to read: " + e);
			error("but got this far: " + response);
			e.printStackTrace();

			throw new ESMTPReadException(response);
      }

      // if we reach this point we should have a good response
      receiverResponse = response;
      try
		{
			receiverResponseCode = Integer.parseInt(response.substring(0, 3));
      }
      catch (NumberFormatException e)
		{
			receiverResponseCode = 0;
			error("readLine() failed to parse three-digit response code");

			return false;
      }

      verbosePrint("readLine(): read: " + receiverResponse + ", code: " +
						 receiverResponseCode);

      return true;
   }

   /**
    */
   protected boolean sendLine(String s) throws ESMTPSendException
   {
		

		try
		{
			verbosePrint("sendLine(): sending string: " + s);

			char[] readBuffer = new char[blockSize];
			char   lastChar   = ' ';
			char   thisChar   = ' ';
			int    srcBegin   = 0;
			int    readIndex  = 0;
			int    lastIndex  = s.length() - 1;

			while ((srcBegin + blockSize - 1) < lastIndex)
			{
				s.getChars(srcBegin, (srcBegin + blockSize), readBuffer, 0);

				for (readIndex = 0; readIndex < blockSize; readIndex++)
				{
					thisChar = readBuffer[readIndex];

					if (thisChar == '\n')
					{
						if (lastChar != '\r')
							out.print('\r');

						out.print(thisChar);
						out.flush();
					}
					else out.print(thisChar);

					srcBegin++;

					lastChar = thisChar;
				}
			}

			int leftOver = lastIndex - srcBegin;

			s.getChars(srcBegin, lastIndex + 1, readBuffer, 0);

			for (readIndex = 0; readIndex <= leftOver; readIndex++)
			{
				thisChar = readBuffer[readIndex];

				if (thisChar == '\n')
				{
					if (lastChar != '\r')
						out.print('\r');

					out.print(thisChar);
					out.flush();
				}
				else out.print(thisChar);
	 
				lastChar = thisChar;
			}

			out.print(kCRLF);
			out.flush();

			verbosePrint("sendLine(): line sent");
		}
		catch (Exception e)
		{
			throw new ESMTPSendException(s);
		}

		return true;
   }

   /**
    */
   public boolean sendData(String dataString)
   {
		try
		{
			if (sendLine("DATA") && readLine())
			{
				switch (receiverResponseCode)
				{
				case 354:
					// sendline() will take care of the final newline here
					if (sendLine(dataString + kCRLF + ".") && readLine())
					{
						switch (receiverResponseCode)
						{
						case 250:
							receiverResponseType = kSuccess;
							break;
						case 552: // Requested mail action aborted: exceeded storage allocation
						case 554: // Transaction failed 
							receiverResponseType = kFailure;
							break;
						case 451: // Requested action aborted; local error in processing
						case 452: // Requested acion not taken; insufficient system storage
							// not much that we can do about these in the middle of a send
							receiverResponseType = kTempFailure;
							break;
						}
					}
					else receiverResponseType = kCommError;
					break;
				case 451:
					receiverResponseType = kTempFailure;
					break;
				case 554:
					receiverResponseType = kFailure;
					break;
				case 421: // Service not available, closing transmission channel
					disconnect();
					receiverResponseType = kError;
					break;
				case 500: // Syntax error, command unrecognized
				case 501: // Syntax error in parameters or arguments
				case 503: // Bad sequence of commands
					receiverResponseType = kError;
					break;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.DATA);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.DATA);
		}

		handleResponseCode();

      if (receiverResponseType != kSuccess)
			return false;
      else return true;
   }

   /**
    */
   public boolean sendEhlo(boolean tryHelo)
   {
		try
		{
			if (sendLine("EHLO " + myHost) && readLine())
			{
				switch (receiverResponseCode)
				{
				case 250:
					receiverResponseType = kSuccess;
					break;
				case 500:
					if (tryHelo)
					{
						// just let sendHelo() set our state
						return sendHelo();
					}
					else
					{
						sendQuit();
						receiverResponseType = kBadESMTP;
					}
					break;
					// this command also may generate response codes 421, 501, 504 
				case 421:
					disconnect();
					receiverResponseType = kError;
					break;
				case 501:
				case 504:
					// this guy seems to have a problem with parameters to ehlo?  thats odd
					//  try just giving the receiver ehlo without args.
					//  also try it for 554: Transaction failed.  can't hurt 
				case 554:
					if (sendLine("EHLO") && readLine())
					{
						if (receiverResponseCode != 250)
							receiverResponseType = kSuccess;
						else receiverResponseType = kError;  // to hell with them
					}
					else receiverResponseType = kCommError;
					break;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.EHLO);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.EHLO);
		}

      //      XXXXset list of extended commands here...

      handleResponseCode();

      if (receiverResponseType != kSuccess)
			sentHelo = false;
      else sentHelo = true;

      return sentHelo;
   }
   
   /**
    */
   public boolean sendEhlo()
   {
      return sendEhlo(true);
   }

   /**
    */
   public String sendExpn(String listName)
   {
      String retString = "";
      
		try
		{
			if (sendLine("EXPN " + listName) && readLine())
			{
				switch (receiverResponseCode)
				{
				case 250:
					retString = receiverResponse.substring(3, receiverResponse.length());
					receiverResponseType = kSuccess;
					break;
				case 251:
					// XXXX this is questionable
					retString = receiverResponse.substring(3, receiverResponse.length());
					receiverResponseType = kSuccess;
					break;
				case 551:
					// XXXX takqe care of forward path here
				case 550:
				case 553:
					receiverResponseType = kFailure;
					break;
				case 421:
					disconnect();
					receiverResponseType = kError;
					break;
				case 500:
				case 501:
				case 502:
				case 504:
					receiverResponseType = kError;
					break;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.EXPN);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.EXPN);
		}

      handleResponseCode();

      return retString;      
   }

   /**
    */
   public boolean sendFrom(String from)
   {
 		// doing this to cover RFC 821 specs
 		from = from.trim();

		try
		{
			if(from.indexOf("<")!=-1 || from.indexOf(">")!=-1) {
				from = from.substring(from.indexOf("<"));
				from = from.substring(0,from.indexOf(">")+1);
			}

			if (sendLine("MAIL FROM:" + from) && readLine())
			{
				switch (receiverResponseCode)
				{
				case 250:
					receiverResponseType = kSuccess;
					break;
				case 451:
				case 452:
					receiverResponseType = kTempFailure;
					break;
				case 552:
					receiverResponseType = kFailure;
					break;
				case 421:
					disconnect();
					receiverResponseType = kError;
					break;
				case 500:
				case 501:
				case 502:
					receiverResponseType = kError;
					break;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.FROM, from);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.FROM, from);
		}
		
      handleResponseCode();
		
      if (receiverResponseType != kSuccess)
			return false;
      else return true;
   }

   /**
    */
   public boolean sendHelo()
   {
		try
		{
			if (sendLine("HELO " + myHost) && readLine())
			{
				switch (receiverResponseCode)
				{
				case 250:
					receiverResponseType = kSuccess;
					break;
				case 421:
					disconnect();
					receiverResponseType = kError;
					break;
				case 501:
				case 504:
					// this guy seems to have a problem with parameters to ehlo?  thats odd
					//  try just giving the receiver ehlo without args.
					if (sendLine("HELO") && readLine())
					{
						if (receiverResponseCode != 250)
							receiverResponseType = kSuccess;
						else receiverResponseType = kError;  // to hell with them
					}
					else receiverResponseType = kCommError;
					break;
				case 500: // if the receiver does not recognize helo, there is no hope
					receiverResponseType = kError;
					break;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.HELO);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.HELO);
		}

      handleResponseCode();

      if (receiverResponseType != kSuccess)
			sentHelo = false;
      else sentHelo = true;

      return sentHelo;
   }
      
   /**
    */
   public boolean sendMail(String to, String from, String message, String cc) throws ESMTPException
   {
      if ((from == null) || from.equals(""))
      {
			// this is covered in RFC 821.  somewhat specious in this case
			from = "<>";
      }
      if ((to == null) || to.equals(""))
      {
			receiverResponseType = kSenderError;
			return false;
      }

      if(message == null)
			message = "";

      if (!sendFrom(from))
			return false;

      Enumeration users = EmailHeader.getSendToUsers(to);
      boolean goodUser  = false;

      while (users.hasMoreElements()) 
      {
			if (sendTo((String) users.nextElement()))
				goodUser = true;
      }

		users = EmailHeader.getSendToUsers(cc);
		while(users.hasMoreElements())
		{
			sendTo((String)users.nextElement());
		}

      if (!goodUser)
			return false;

      return sendData(message);
   }

   /** 
    */
   public boolean sendMessage(IMessage message) throws ESMTPException
   {
      String to   = message.getHeaderInfo("To");
      String from = message.getHeaderInfo("From");
		String cc   = message.getHeaderInfo("CC");
      String data = message.toString();

      // use mail() here for the sentHelo check (see mail())
      return mail(to, from, data, cc);
   }
   
   /**
    */
   public boolean sendNoop()
   {
		try
		{
			if (sendLine("NOOP") && readLine())
			{
				switch (receiverResponseCode)
				{
				case 250:
					receiverResponseType = kSuccess;
					break;
				case 421:
					disconnect();
					receiverResponseType = kError;
					break;
				case 500:
					receiverResponseType = kError;
					break;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.NOOP);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.NOOP);
		}
      
      handleResponseCode();      

      if (receiverResponseType != kSuccess)
			return false;
      else return true;
   }

   /**
    */
   public boolean sendQuit()
   {
		try
		{
			if (!sendLine("QUIT"))
				receiverResponseType = kCommError;
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.QUIT);
		}

		/* we may as well try to disconnect() here.  If QUIT was successful then the
		 *  receiver has dropped the connection, so it is no good and nothing should be
		 *  monitoring out.  if QUIT failed then damn the connection anyway
		 */
		disconnect();

      handleResponseCode();

      if (receiverResponseType != kSuccess)
			return false;
      else return true;
   }

	/**
	 */
	public boolean sendAuthLogin(String username, String password)
	{
		try {
			boolean intermediateSuccess = false;
			if(sendLine("AUTH LOGIN") && readLine()) {
				switch (receiverResponseCode) {
				case 334:
					intermediateSuccess = true;
				case 500:
				case 501:
				case 502:
				case 504:
					receiverResponseType = kError;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else {
				receiverResponseType = kCommError;
				intermediateSuccess = false;
			}

			if(intermediateSuccess && sendLine(Base64Encoding.encode(username)) && readLine()) {
				intermediateSuccess = false;
				switch(receiverResponseCode) {
				case 334:
					intermediateSuccess = true;
				case 500:
				case 501:
				case 502:
				case 504:
					receiverResponseType = kError;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else {
				receiverResponseType = kCommError;
				intermediateSuccess = false;
			}

			if(intermediateSuccess && sendLine(Base64Encoding.encode(password)) && readLine()) {
				switch(receiverResponseCode) {
				case 235:
					receiverResponseType = kSuccess;
				case 500:
					receiverResponseType = kAuthFailed;
				case 501:
				case 502:
				case 504:
					receiverResponseType = kError;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else {
				receiverResponseType = kCommError;
			}
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.AUTH);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.AUTH);
		}

		handleResponseCode();

		return(receiverResponseType==kSuccess);
	}


   /**
    */
   public boolean sendRset()
   {
		try
		{
			if (sendLine("RSET") && readLine())
			{
				switch (receiverResponseCode)
				{
				case 250:
					receiverResponseType = kSuccess;
					break;
				case 421:
					disconnect();
					receiverResponseType = kError;
					break;
				case 500:
				case 501:
				case 504:
					receiverResponseType = kError;
					break;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.RSET);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.RSET);
		}

      handleResponseCode();

      if (receiverResponseType != kSuccess)
			return false;
      else return true;
   }

   /**
    */
   public boolean sendTo(String toField)
   {
		// doing this to cover RFC 821 specs
 		toField = toField.trim();

		try
		{
			if (sendLine("RCPT TO:" + toField) && readLine())
			{
				switch (receiverResponseCode)
				{
				case 250:
				case 251:
					receiverResponseType = kSuccess;
					break;
				case 551:
					// XXXX deal with forward-path here
				case 552:
					// XXXX deal with multiple transactions here
				case 450:
				case 451:
				case 452:
					receiverResponseType = kTempFailure;
					break;
				case 550:
				case 553:
					receiverResponseType = kFailure;
					break;
				case 421:
					disconnect();
					receiverResponseType = kError;
					break;
				case 500:
				case 501:
				case 503:
					receiverResponseType = kError;
					break;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.TO, toField);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.TO, toField);
		}
      
      handleResponseCode();
		
      if (receiverResponseType != kSuccess)
			return false;
      else return true;
   }

   /**
    */
   public boolean sendTo(Vector toFields)
   {
      boolean retval = false;

      for (int i = 0; i < toFields.size(); i++)
			retval = sendTo((String) toFields.elementAt(i)) || retval;

      return retval;
   }

   /**
    */
   public String sendVrfy(String userName)
   {
      String retString = "";

		try
		{
			if (sendLine("VRFY " + userName) && readLine())
			{
				switch (receiverResponseCode)
				{
				case 250:
					retString = receiverResponse.substring(3, receiverResponse.length());
					receiverResponseType = kSuccess;
					break;
				case 251:
					// XXXX this is questionable
					retString = receiverResponse.substring(3, receiverResponse.length());
					receiverResponseType = kSuccess;
					break;
				case 551:
					// XXXX take care of forward path here
				case 550:
				case 553:
					receiverResponseType = kFailure;
					break;
				case 421:
					disconnect();
					receiverResponseType = kError;
					break;
				case 500:
				case 501:
				case 502:
				case 504:
					receiverResponseType = kError;
					break;
				default:
					receiverResponseType = kBadESMTP;
					break;
				}
			}
			else receiverResponseType = kCommError;
		}
		catch (ESMTPReadException er)
		{
			throw new ESMTPException(ESMTPException.VRFY);
		}
		catch (ESMTPSendException es)
		{
			throw new ESMTPException(ESMTPException.VRFY);
		}

      handleResponseCode();

      return retString;
   }

   /**
    */
   public boolean data(String dataString)
   {
      // we do NOT want to d othe sentHelo check here, since 'RCPT TO:' is part of a
      //  command sequence (and is not the first command in the sequence)
      return sendData(dataString);
   }
   
   /**
    */
   public boolean eHello()
   {
      if (!sentHelo && !sendEhlo())
			return false;
      else return true;
   }
   
   /**
    */
   public String expand(String listName)
   {
      if (!sentHelo && !sendEhlo())
			return "";
      else return sendExpn(listName);
   }
   
   /**
    */
   public boolean from(String from)
   {
      if (!sentHelo && !sendEhlo())
			return false;
      else return sendFrom(from);
   }   
   
   /**
    */
   public boolean hello()
   {
      if (!sentHelo && !sendHelo())
			return false;
      else return true;
   }

   /**
    */
   public boolean mail(String to, String from, String data, String cc)
   {
      if (!sentHelo && !sendEhlo())
			return false;
      else return sendMail(to, from, data, cc);
   }
   
   /**
    */
   public boolean noop()
   {
      if (!sentHelo && !sendEhlo())
			return false;
      else return sendNoop();
   }
   
   /**
    */
   public boolean quit()
   {
      if (!sentHelo && !sendEhlo())
			return false;
      else return sendQuit();
   }
   
   /**
    */
   public boolean reset()
   {
      if (!sentHelo && !sendEhlo())
			return false;
      else return sendRset();
   }
   
   /**
    */
   public boolean to(String toString)
   {
      if (!sentHelo && !sendEhlo())
			return false;
      else return sendTo(toString);
   }
      
   /**
    */
   public boolean to(Vector toFields)
   {
      // we do NOT want to d othe sentHelo check here, since 'RCPT TO:' is part of a
      //  command sequence (and is not the first command in the sequence)
      return sendTo(toFields);
   }
      
   /**
    */
   public String verify(String userName)
   {
      if (!sentHelo && !sendEhlo())
			return "";
      else return sendVrfy(userName);
   }
   
   /**
    */
   public String qualifyResponseCode(int responseCode)
   {
      switch (responseCode)
      {
		default:
			break;
      }

      return "";
   }

   /**
    */
   void handleResponseCode()
   {
      ;
   }

   public void finalize()
   {
      if (isConnected)
			sendQuit();
   }
   
   /**
    */
   public void print(String s) 
   {
      System.out.println("ESMTPSender: " + s);
   }

   /**
    */
   public void error(String s) 
   {
      print("error: " + s);
   }
   
   /**
    */
   public void debug(String s) 
   {
      if (debug)
	 print(s);
   }
   
   /**
    */
   public void verbosePrint(String s) 
   {
      if (verbose)
	 print(s);
   }
}

