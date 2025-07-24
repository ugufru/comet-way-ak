
package com.cometway.httpd;

import com.cometway.ak.AgentRequest;
import com.cometway.ak.RequestAgent;
import com.cometway.props.Props;
import com.cometway.util.Base64Encoding;
import com.cometway.util.StringTools;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.Vector;


/**
	This agent is used by the WebServer to handle authorization of password
	protected accesses.

	<P>

	The authentication file defines the realms and the usernames and 
	passwords which are used to authenticate accesses to the paths 
	associated with each realm. The authentication file is delimited 
	by newlines and must end with a newline. Each realm is declared 
	with a PATH and the REALM name, space seperated, followed by 
	usernames and passwords, in pairs which are seperated by a space.
	If multiple hostnames require authentication, a HOSTNAME (no spaces)
	can be provided in the beginning of the PATH/REALM/username/password
	list to denote which host those realms should apply to. A special 
	HOSTNAME can be used to denote a default set of realms, if there are
	no matches to any other HOSTNAMEs, the default is used. If only one
	set of realms are required (no multiple hostnames), the HOSTNAME field
	can be left out.

	<P>
	
	The fields in the authentication file has these restrictions:

	<PRE>
HOSTNAME - cannot contain any spaces, must be either <b>default</b> or 
           a qualified hostname that is passed to the webserver via the
           <i>Host:</i> field. The HOSTNAME can also contain a ':' followed
           by a port number. If the port number is excluded, and a port number
           is included in the <i>Host:</i> field, then all ports will be mapped.
PATH     - must start with AND end with a<b>/</b>, cannot contain any spaces,
           if the PATH contains escape characters (for example: a space), it
           must be escaped using a <i>%</i> and a 2 digit hex value representing
           the ASCII character. Characters that will need to be escaped are as
           follows: spaces and everything EXCEPT letters, numbers, the '*', '-',
           '.', '@', '_', and '/'. Spaces MUST be encoded as <b>%32</b>.
REALM    - The realm name can by any alphanumeric string and can include spaces,
           commas, periods, colons, semicolons, exclaimation points, and dashes.
username - This is an alphanumeric string that CANNOT include spaces or punctuation.
password - This is a string that can contain all letters, numbers, punctuation, and
           spaces.
   </PRE>
	<P>

	The general format of the authentication file is as follows:

	<PRE>
HOSTNAME0
PATH0 REALM0
username0 password0
username1 password1
username2 password2
username3 password3
PATH1 REALM1
username0 password0
username1 password1

HOSTNAME1
PATH2 REALM2
username0 password0
username1 password1
PATH3 REALM3
username0 password0

etc...
	</PRE>
	<P>

	Examples:

	<P>

	Here we have an example of a webserver serving a single domain

	<PRE>
default
/protected/ Protected Realm
foo bar
/images/protected%32images/ Protected Image Realm
foo bar
   </PRE>

	So here the URI '/protected/sensitive_stuff' will fall under the <b>Protected Realm</b>,
	and a username/password of <b>foo/bar</b> will authenticate with this realm. The
	URI '/images/mypicture.gif' does not fall under a realm and will not need authentication,
	however; the URI '/images/protected images/private_picture.gif' does fall under the
	<b>Protected Image Realm</b> and will require the foo/bar password. Note the space in the
	directory 'protected images', and how it is required that the PATH be listed as 
	'protected%32images' in the authentication file.

	<P>

	In this example, we have a webserver that serves 2 domains as well as an HTTPS
	service. Since the HTTPS runs on a different port, special attention is given to
	the port numbers in the PATH field of the authentication file.

	<PRE>
host.domain1.org:80
/protected/ Protected Realm
abc 123
xyz 321

host.domain1.org:443
/protected/ Protected HTTPS Realm
abc 789

host.domain2.org
/protected/ Domain2 Protected Realm
foo bar
   </PRE>

	The domain 'host.domain1.org' has been split into 2 seprated HOSTNAME entries: one for the
	HTTP requests (port 80) and one for the HTTPS requests (port 443). Given the URL
	'http://host.domain1.org/protected/data.txt' a username/password of <b>abc/123</b> or 
	<b>xyz/321</b> must be given to be authenticated. Given the URL 
	'https://host.domain1.org/protected/secure_data.txt', the username/password <b>abc/789</b>
	must be used because it is being accessed through port 443 (https). However, the second
	domain, host.domain2.org, doesn't have a port listed. Therefore, the 2 URLs
	'http://host.domain2.org/protected/data.txt' AND 'https://host.domain2.org/protected/secure.txt'
	may use the same username/password to be authenticated.


*/
public class HTTPAuthenticationAgent extends WebServerExtension
{
	protected final static String DEFAULT_HOME = "default";
	protected Hashtable hosts;
	protected DigestAuthenticationTracker digests;
    
	/**
	* Initializes properties for this agent: "service_name" is the
	* name this agent uses to register with Service Manager (default:
	* http_authentication.agent), "authentication_file" is the location
	* of the file to authenticate requests with (default: ./http.authentication).
	*/
    
	public void initProps()
	{
		setDefault("service_name","extension://.*");
		setDefault("webserver_service_name","none");
		setDefault("domains","none");

		// This can be set to "digest" or anything else. "digest" will cause this extension to use DIGEST authentication
		setDefault("authentication_type","digest");

		// This is the timeout of a DIGEST authenticated session. This only applies when "digest" is set. This cannot be zero or negative.
		setDefault("digest_timeout","3600000");  // default 1 hour
	
		setDefault("authentication_files", "./http.authentication");
	}


	public void start()
	{
		hosts = new Hashtable();
		String[] authFiles = StringTools.commaToArray(getString("authentication_files"));

		for (int x = 0; x < authFiles.length; x++)
		{
			readAuthenticationFile(authFiles[x]);
		}

		if(getString("authentication_type").equalsIgnoreCase("digest")) {
			digests = new DigestAuthenticationTracker();
			digests.start();
		}

		super.start();
	}

	public void stop()
	{
		hosts = null;
		super.stop();
	}


	/**
	* Reads Authentication file, stores file in buffer, and prints Head Node
	* of Authentication hash table.
	*/
    
	public void readAuthenticationFile(String filename)
	{
		BufferedReader in = null;
		String hostname = DEFAULT_HOME;
		AuthHash authHash = null;

		try
		{
			in = new BufferedReader(new FileReader(new File(filename)));
	    
			StringBuffer buffer = new StringBuffer();
			String line = in.readLine();

			if (!line.toLowerCase().equals(DEFAULT_HOME))
			{
				hostname = line.toLowerCase();
			}

			line = in.readLine();

			try
			{
				while (line != null)
				{
					buffer.append(line);
					buffer.append('\n');
					line = in.readLine();

					if (line.trim().indexOf(' ') == -1)
					{
						if (line.trim().length() > 0)
						{
							authHash = new AuthHash(buffer.toString() + '\n');
							hosts.put(hostname, authHash);
							buffer = new StringBuffer();
							hostname = line.toLowerCase().trim();
							line = in.readLine();
						}
					}
				}
			}
			catch(Exception e) {;}

			if (buffer.length() > 0)
			{
				authHash = new AuthHash(buffer.toString().trim() + '\n');
				hosts.put(hostname, authHash);
			}
		}
		catch (Exception e)
		{
			error("Cannot read HTTP Authentication file: " + filename, e);
		}
		finally
		{
			try { in.close(); } catch (Exception e) {;}
		}

		Enumeration e = hosts.keys();

		while (e.hasMoreElements())
		{
			String host = (String)e.nextElement();
			debug("HOST = " + host);
			debug("AUTH HASH:\n" + hosts.get(host).toString());
		}
	}


	/**
	* Tests to see if path is Authenticated.
	*/
    
	public boolean isAuthenticated(AgentRequest request, String path, String hostname)
	{
		debug("isAuthenticated: " + path + " " + hostname);

		// we need to update the nonce count if there is one if this is set to digest.
		if(getString("authentication_type").equalsIgnoreCase("digest")) {
			String authorization = request.getString("http_headers:authorization");
			if(authorization.toLowerCase().startsWith("digest")) {
				String opaque = digests.parseField("opaque",authorization);
				if(opaque!=null) {
					digests.incrementCount(opaque);
				}
			}
		}

		hostname = hostname.toLowerCase();

		if (path.indexOf('?') != -1)
		{
			path = path.substring(0, path.indexOf('?'));
		}

		Object o = hosts.get(hostname);

		if (o == null)
		{
			if (hostname.indexOf(':') != -1)
			{
				hostname = hostname.substring(0, hostname.indexOf(':'));
				o = hosts.get(hostname);
			}
		}

		if (o != null && o instanceof AuthHash)
		{
			return(((AuthHash)o).contains(path));
		}
		else
		{
			o = hosts.get(DEFAULT_HOME);

			if (o == null)
			{
				return(false);
			}

			return (((AuthHash)o).contains(path));
		}
	}


	/**
	* This is a temporary workaround to provide an alternate authentication method to subclasses.
	*/
    
	public boolean authenticateUser(String realm, String username, String password)
	{
		return(authenticateUser(DEFAULT_HOME,realm,username,password));
	}

	public boolean authenticateUser(String hostname, String realm, String username, String password)
	{
		AuthHash authHash = getAuthHashByHostname(hostname);
		return (authHash.authenticate(realm, username, password));
	}

	public String getUserPassword(String hostname, String realm, String username)
	{
		AuthHash authHash = getAuthHashByHostname(hostname);
		return(authHash.getPassword(realm,username));
	}




	public boolean authenticateUser(AgentRequest request, String authorization, String realm) throws HTTPDigestException
	{
		return(authenticateUser(request,authorization,DEFAULT_HOME,realm));
	}

	public boolean authenticateUser(AgentRequest request, String authorization, String hostname, String realm) throws HTTPDigestException
	{
		boolean rval = false;

		if(authorization.toLowerCase().startsWith("basic")) {
			String userpass = new String(Base64Encoding.decode(authorization.substring(5).trim()));
			int index = userpass.indexOf(':');
					
			if (index != -1)
			{
				String username = userpass.substring(0, index);
				String password = userpass.substring(index + 1);
						
				request.setProperty("user_id", username);
						
				rval = authenticateUser(hostname, realm, username, password);
			}
		}
		else if(authorization.toLowerCase().startsWith("digest") && getString("authentication_type").equalsIgnoreCase("digest")) {
			String digestString = authorization.substring(6).trim();
			String username = digests.parseField("username",digestString);
			String uri = digests.parseField("uri",digestString);
			String qop = digests.parseField("qop",digestString);
			String cnonce = digests.parseField("cnonce",digestString);
			String nonceCount = digests.parseField("nc",digestString);
			String response = digests.parseField("response",digestString);
			String opaque = digests.parseField("opaque",digestString);
			String clientRealm = digests.parseField("realm",digestString);
			String clientNonce = digests.parseField("nonce",digestString);

			// these are the minimum required parameters
			if(opaque!=null && username!=null && uri!=null && clientNonce!=null && clientRealm!=null && response!=null) {
				String nonce = digests.getNonce(opaque);
				if(nonce!=null) {
					if(nonce.equals(clientNonce) && realm.equals(clientRealm)) {
						String password = getUserPassword(hostname,realm,username);
						if(password!=null) {
							String method = request.getString("request");
							method = method.substring(0,method.indexOf(" "));
							String a1 = username+":"+realm+":"+password;
							String a2 = method+":"+uri;
							if(qop!=null) {
								if(cnonce!=null && nonceCount!=null) {
									if(digests.checkCount(opaque,nonceCount)) {
										String unhashed = digests.hash(a1)+":"+nonce+":"+nonceCount+":"+cnonce+":"+qop+":"+digests.hash(a2);
										String hash = digests.hash(unhashed);
										if(hash.equals(response)) {
											request.setProperty("user_id", username);
											rval = true;
										}
									}
									else {
										warning("Digest Authentication: Nonce count mismatch, possible replay attack");
										// remove the offending opaque
										digests.opaques.remove(opaque);
									}
								}
								else {
									throw(new HTTPDigestException());
								}
							}
							else {
								String hash = digests.hash(digests.hash(a1)+":"+nonce+":"+digests.hash(a2));
								//										debug("hash="+hash+" response="+response);
								if(hash.equals(response)) {
									request.setProperty("user_id", username);
									rval = true;
								}
							}
						}
					}
				}
			}
			else {
				throw(new HTTPDigestException());
			}
		}

		return(rval);
	}

	public void writeNeedAuthenticationResponse(AgentRequest request, String realm)
	{
		try {
			String s = "WWW-Authenticate: Basic realm=\""+ realm+ "\"\n";
			if(getString("authentication_type").equalsIgnoreCase("digest")) {
				String opaque = digests.getNewOpaque();
				String nonce = digests.getNonce(opaque);
				s = "WWW-Authenticate: Digest realm=\""+realm+"\", qop=\"auth\", nonce=\""+nonce+"\", opaque=\""+opaque+"\", algorithm=\"MD5\"";
			}
			request.print(WebServer.getHTMLByCode(WebServer.UNAUTHORIZED,null,s));
		}
		catch (Exception e) {
			error("Exception while replying to request", e);
		}
	}


	public AuthHash getAuthHashByHostname(String hostname)
	{
		AuthHash authHash = null;
		Object o = hosts.get(hostname);

		if (o == null)
		{
			if (hostname.indexOf(':') != -1)
			{
				hostname=hostname.substring(0,hostname.indexOf(':'));
				o = hosts.get(hostname);
			}
		}

		if (o != null && o instanceof AuthHash)
		{
			authHash = (AuthHash)o;
		}
		else
		{
			authHash = (AuthHash)hosts.get(DEFAULT_HOME);
		}

		return(authHash);
	}


	/**
	* Authenticates HTTP request. 
	*/
    
	public boolean authenticate(AgentRequest request) throws HTTPDigestException
	{
		boolean rval = false;
		String path = request.getString("path");
		String hostname = request.getString("host");
		hostname = hostname.toLowerCase();

		if (path.indexOf('?') != -1)
		{
			path = path.substring(0,path.indexOf('?'));
		}

		AuthHash authHash = getAuthHashByHostname(hostname);
		String realm = authHash.getRealm(path);
	
		if (realm == null)
		{
			rval = true;
		}
		else
		{
			request.setProperty("realm", realm);

			try
			{
				String authorization = request.getString("http_headers:authorization");
				debug("authorization="+authorization);

				rval = authenticateUser(request, authorization, hostname, realm);
			}
			catch (Exception e)
			{
				error("Could not authenticate", e);
			}					
		}									 
	
		return(rval);
	}


	/**
	* Pass in the Props of an HTTPAgentRequest, get the username that was used to HTTP authenticate this request.
	* null if no username was used to authenticate. NOTE: This method does not check if the username/password
	* is actually valid for the given path.
	*/

	public String getUsername(Props request)
	{
		String rval = null;

		try
		{
			String authorization = request.getString("http_headers:authorization");
			if(authorization.startsWith("Basic") || authorization.startsWith("basic")) {
				String userpass = new String(Base64Encoding.decode(authorization.substring(5).trim()), "ISO-8859-1");
				int index = userpass.indexOf(':');
				
				if (index != -1)
				{
					rval = userpass.substring(0, index);
				}
			}
			else if(authorization.startsWith("Digest") || authorization.startsWith("digest")) {
					String digestString = authorization.substring(6).trim();
					rval = digests.parseField("username",digestString);
			}
		}
		catch (Exception e)
		{
			error("Exception when authenticating", e);
		}					

		return (rval);
	}
    
    
	/**
	* This method is used only when the client is not authorized.
	*/

	public boolean handleRequest(HTTPAgentRequest request)
	{
		boolean rval = true;
		OutputStream out = request.getOutputStream();
		String hostname = request.getString("host").toLowerCase();
		String path = request.getString("path");
		AuthHash authHash = null;
		Object o = hosts.get(hostname);
		boolean authorized = false;

		if(path.indexOf("?")!=-1) {
			path = path.substring(0,path.indexOf("?"));
		}

		if(isAuthenticated(request,path,hostname)) {
			try {
				if(request.getString("request").indexOf("Authorization: ")==-1) {
					authorized = false;
				}
				else {
					authorized = authenticate(request);
				}

				if(!authorized) {
					if (o == null)	{
						if (hostname.indexOf(":") != -1)	{
							hostname = hostname.substring(0, hostname.indexOf(":"));
							o = hosts.get(hostname);
						}
					}
					
					if ((o != null) && (o instanceof AuthHash)) {
						authHash = (AuthHash) o;
					}
					else {
						authHash = (AuthHash) hosts.get(DEFAULT_HOME);
					}

					writeNeedAuthenticationResponse(request,authHash.getRealm(path));
				}
				else {
					// Successfully authorized, so don't handle request
					rval = false;
				}
			}
			catch(HTTPDigestException de) {
				// This means the the Authorization field is malformed
				try {
					out.write(WebServer.getHTMLByCode(WebServer.BAD_REQUEST).getBytes());
					out.flush();
				}
				catch (Exception e) {
					error("Exception while replying to request", e);
				}
			}
		}
		else {
			// No authorization needed, so don't handle request
			rval = false;
		}
		
		return(rval);
	}
    
    
    
	class AuthHash
	{
		protected PathNode headNode;
		protected Hashtable realmHash; // maps paths to realm names
		protected Hashtable userHash;  // maps realm names to a Hashtable of usernames mapping to passwords
	
	
		public String toString()
		{
			String rval = "HeadNode: " + headNode + "\nREALMHASH:";
			Enumeration realms = realmHash.keys();

			while (realms.hasMoreElements())
			{
				String realm = (String) realms.nextElement();
				rval = rval + "\n  " + realm + '=' + realmHash.get(realm);
			}

			rval = rval + "\nUSERHASH:";

			Enumeration users = userHash.keys();

			while (users.hasMoreElements())
			{
				String user = (String) users.nextElement();
				rval = rval + "\n  " + user + '=' + userHash.get(user);
			}

			return (rval);
		}


		public AuthHash(String authfile)
		{
			headNode = new PathNode();
			headNode.pathName = "/";
			headNode.realmName = null;
	    
			realmHash = new Hashtable();
			userHash = new Hashtable();
	    
			try
			{
				int newline = authfile.indexOf('\n');
				String line = authfile.substring(0, newline);
				authfile = authfile.substring(newline + 1);

				while (true)
				{
					int tmp = line.indexOf(' ');
					String path = line.substring(0, tmp);
					String realm = line.substring(tmp).trim();

					try
					{
						if (realmHash.containsKey(path))
						{
							error("Authentication file contains more than one path: " + path);
						}
						else if (userHash.containsKey(realm))
						{
							error("Authentication file contains more than one realm pair: " + realm);
						}
						else
						{
							addRealm(path,realm);
			    
							realmHash.put(path, realm);
							userHash.put(realm, new Hashtable());
			    
							newline = authfile.indexOf('\n');
							line = authfile.substring(0,newline);

							while (!line.startsWith("/") || line.equals(""))
							{
								authfile = authfile.substring(newline + 1);
								tmp = line.indexOf(' ');

								String username = line.substring(0, tmp);
								String password = line.substring(tmp).trim();
								Hashtable h = (Hashtable) userHash.get(realm);

								if (h.containsKey(username))
								{
									error("Authentication file contains more than one entry for user '" + username + "' in the realm '" + realm + "'");
								}
								else
								{
									h.put(username, password);
								}
				
								if (authfile.trim().equals(""))
								{
									break;
								}
								else
								{
									newline = authfile.indexOf('\n');
									line = authfile.substring(0, newline);
								}
							}
						}
					}
					catch (Exception e)
					{
						error("Error parsing realm information for " + path + ":" + realm, e);
					}

					if (authfile.trim().equals(""))
					{
						break;
					}
					else
					{
						newline = authfile.indexOf('\n');
						line = authfile.substring(0, newline);
						authfile = authfile.substring(newline + 1);
					}
				}
			}
			catch (Exception e)
			{
				error("Error parsing authentication file");
			}
		}

	
		// This method adds a realm to the PathNode tree. The subsequent paths
		// that lead to the last pathname in the entire path are added into
		// the tree. 
		private void addRealm(String path, String realm)
		{
			if (path.equals("/"))
			{
				// give headNode a realm name

				headNode.realmName = realm;
			}
			else
			{
				PathNode pointer = headNode;
				path = path.substring(1);
		
				int slash = path.indexOf('/');
				String tmpRealm = pointer.realmName;

				// recurse the full path 

				while (slash != -1)
				{
					String subpath = path.substring(0, slash);
					path = path.substring(slash + 1);
					slash = path.indexOf('/');
		    
					PathNode checkNode = null;

					// recurse the PathNode tree

					for (int x = 0; x < pointer.pathNodes.size(); x++)
					{
						checkNode = (PathNode) pointer.pathNodes.get(x);

						if (checkNode.pathName.equals(subpath))
						{
							break;
						}
						else
						{
							checkNode = null;
						}
					}

					if (checkNode != null)
					{
						// There is a PathNode that exists for the subpath

						pointer = checkNode;

						if (pointer.realmName != null)
						{
							tmpRealm = pointer.realmName;
						}
					}
					else
					{
						// There is no PathNode that exists for the subpath, create one.

						checkNode = new PathNode();
						checkNode.pathName = subpath;

						// check if this is the last subpath in the entire path

						if (path.equals(""))
						{
							checkNode.realmName = realm;
						}
						else
						{
							checkNode.realmName = tmpRealm;
						}

						pointer.pathNodes.add(checkNode);
						pointer = checkNode;
					}
				}
			}
		}


		public boolean contains(String path)
		{
			return(getRealm(path) != null);
		}


		public String getRealm(String path)
		{
			if (path.trim().equals("/"))
			{
				return(headNode.realmName);
			}

			else if (path.length() > 0)
			{
				if (path.indexOf('?') != -1)
				{
					path = path.substring(0, path.indexOf('?'));
				}

				return(headNode.getRealm(path.substring(1)));
			}

			else
			{
				return(headNode.getRealm(path));
			}
		}

		public String getPassword(String realm, String username)
		{
	    	Hashtable users = (Hashtable) userHash.get(realm);
			String pass = null;

			if (users != null)
			{
				pass = (String) users.get(username);
			}

			return(pass);
		}

		public boolean authenticate(String realm, String username, String password)
		{
			boolean rval = false;
	    	Hashtable users = (Hashtable) userHash.get(realm);

			if (users != null)
			{
				String pass = (String) users.get(username);

				if (pass != null)
				{
					if (password.trim().equals(pass.trim()))
					{
						rval = true;
					}
				}
			}
	    
			return (rval);
		}
	}


	// Represents the realm associated with a path.

	class PathNode
	{
		protected List pathNodes;
		protected String pathName;
		protected String realmName;

	
		public PathNode()
		{
			pathNodes = new Vector();
		}


		private String prepPath(String path)
		{
			if ((path.indexOf('%') == -1) && (path.indexOf('+') == -1))
			{
				path = HTMLStringTools.encode(path);
			}

			while (path.indexOf('+') != -1)
			{
				path = path.substring(0, path.indexOf('+')) + "%32" + path.substring(path.indexOf('+') + 1);
			}

			return (path);
		}


		// Returns the name of the realm associated with the specified path.

		public String getRealm(String path)
		{
			path = prepPath(path);
			int slash = path.indexOf('/');

			if (slash == 1)
			{
				path = path.substring(1);
				slash = path.indexOf('/');
			}

			String subpath = null;

			if (path.length() == 0)
			{
				return(realmName);
			}				

			else if (slash != -1)
			{
				subpath = path.substring(0, slash);
				path = path.substring(slash + 1);
			}

			else
			{
				subpath = path;
				path = "";
			}

			PathNode checkNode = null;
			boolean foundNode = false;

			for (int x = 0; x < pathNodes.size(); x++)
			{
				checkNode = (PathNode) pathNodes.get(x);

				if (checkNode.pathName.equals(subpath))
				{
					foundNode = true;
					break;
				}
			}

			if (foundNode && checkNode != null)
			{
				return(checkNode.getRealm(path));
			}

			else if (path.equals(""))
			{
				return(realmName);
			}

			else
			{
				return(realmName);
			}
		}


		// Returns this node as a String.

		public String toString()
		{
			String rval = pathName + " (" + realmName + ")\n";

			for (int x = 0; x < pathNodes.size(); x++)
			{
				rval = rval+pathNodes.get(x);
			}

			return(rval);
		}
	}


	class DigestSession
	{
		String opaque;
		String nonce;
		long lastAccess;
		int count;

		public DigestSession(String newopaque, String newnonce)
		{
			opaque = newopaque;
			nonce = newnonce;
			lastAccess = System.currentTimeMillis();
			count = 0;
		}

		public boolean checkCount(String s)
		{
			return(Integer.valueOf(s,16).intValue()==count);
		}
	}

	class DigestAuthenticationTracker extends Thread
	{
		public boolean stopRunning;
		Hashtable opaques;
		Object sync;
		SecureRandom random;

		private char[] DIGITS = {
			'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
		};

		public DigestAuthenticationTracker()
		{
			opaques = new Hashtable();
			sync = new Object();
			try {
				random = SecureRandom.getInstance("SHA1PRNG");
			}
			catch(NoSuchAlgorithmException e) {
				error("Could not locate SHA-1 PRNG security provider. Digest authentication will be broken.",e);
			}
		}

		public String getNewOpaque()
		{
			String opaque = UUID.randomUUID().toString();
			String nonce = generateNonce();
			while(opaque.indexOf("-")!=-1) {
				opaque = opaque.substring(0,opaque.indexOf("-"))+opaque.substring(opaque.indexOf("-")+1);
			}
			DigestSession session = new DigestSession(opaque,nonce);
			synchronized(sync) {
				opaques.put(opaque,session);
			}
			return(opaque);
		}

		public String getNonce(String opaque)
		{
			String rval = null;
			synchronized(sync) {
				if(opaques.containsKey(opaque)) {
					DigestSession session = (DigestSession)opaques.get(opaque);
					rval = session.nonce;
				}
			}
			return(rval);
		}

		public boolean checkCount(String opaque, String countHex)
		{
			boolean rval = false;
			synchronized(sync) {
				if(opaques.containsKey(opaque)) {
					DigestSession session = (DigestSession)opaques.get(opaque);
					rval = session.checkCount(countHex);
				}
			}
			return(rval);
		}

		public void incrementCount(String opaque)
		{
			synchronized(sync) {
				if(opaques.containsKey(opaque)) {
					DigestSession session = (DigestSession)opaques.get(opaque);
					session.count++;
				}
			}
		}

		protected String generateNonce()
		{
			StringBuffer rval = new StringBuffer();
			byte[] buffer = new byte[16];
			random.nextBytes(buffer);
			for(int x=0;x<buffer.length;x++) {
				int val = (int)(buffer[x]+128);
				if(val<16) {
					rval.append("0");
				}
				rval.append(Integer.toHexString(val));
			}
			return(rval.toString());
		}

		public String parseField(String fieldName, String digestString)
		{
			String rval = null;
			int index = digestString.indexOf(fieldName+"=");
			if(index!=-1) {
				String tmp = digestString.substring(index+fieldName.length()+1);
				//				index = tmp.indexOf("=");
				//				if(index!=-1) {
				//					tmp = tmp.substring(index+1).trim();
					index = tmp.indexOf(",");
					if(index==-1) {
						index = tmp.length();
					}
					rval = tmp.substring(0,index).replaceAll("\"","");
					//				}
			}
			return(rval);
		}

		public String hash(String in)
		{
			StringBuffer hexString = new StringBuffer();

			try {
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				byte[] hashed = md5.digest(in.getBytes());
				for (int i=0;i<hashed.length;i++) {
					hexString.append(DIGITS[(0xF0 & hashed[i]) >>> 4 ]);
					hexString.append(DIGITS[ 0x0F & hashed[i] ]);
					//					hexString.append(Integer.toHexString(0xFF & hashed[i]));
				}
			}
			catch(NoSuchAlgorithmException e1) {
				error("This security provider doesn't provide MD5 message digests, Digest authentication will be broken",e1);
			}
			catch(Exception e) {
				error("Error while hashing",e);
			}

			return(hexString.toString());
		}

		public void run()
		{
			while(!stopRunning) {
				try {
					sleep(5000);
					synchronized(sync) {
						Enumeration keys = opaques.keys();
						while(keys.hasMoreElements()) {
							String opaque = (String)keys.nextElement();
							DigestSession session = (DigestSession)opaques.get(opaque);
							if(System.currentTimeMillis()-session.lastAccess > getInteger("digest_timeout")) {
								opaques.remove(opaque);
							}
						}
					}
				}
				catch(Exception e) {
					error("Error cleaning old Digest opaques",e);
				}
			}
		}
	}
}


