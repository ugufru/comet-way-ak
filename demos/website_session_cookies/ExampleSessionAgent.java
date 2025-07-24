import com.cometway.website.WebsiteSessionAgent;
import com.cometway.httpd.HTTPAgentRequest;
import com.cometway.props.Props;


/**
 * This is an example implementation of com.cometway.website.WebsiteSessionAgent.
 * The username/passwords are hardcoded along with the path/realms which the username/password
 * belongs to. This implementation was designed to work with the ExampleSessionLoginPage and
 * ExampleSessionRedirectPage.
 */
public class ExampleSessionAgent extends WebsiteSessionAgent
{

	public void initProps()
	{
		setDefault("service_name","cookie_session_manager");
		setDefault("cleanup_interval","10000");

		setDefault("logout_key","logout");
	}

	/**
	 * Depending on the request path and properties, does this request require authentication?
	 * If authentication is required, return a String that uniquiely represents the 'realm' which
	 * requires authentication. Session cookies will be tied to this realm as well as named after
	 * the realm. Realm Strings can only contain alphanumeric characters and underscores. If
	 * authentication is not required, return null.
	 */
	public String requireAuthentication(HTTPAgentRequest request)
	{
		String rval = null;
		String path = request.getString("path");

		// This example has 2 realms, one for /pub/ and one for /music/

		// Here we check for /pub/ and set a redirect path so that after authentication, the client gets
		// redirected to the page they were attempting to access
		if(path.startsWith("/pub/")) {
			request.setProperty("redirect",path);
			rval = "pub_dir_session";
		}
		// We must also include the page that the login form submits to, in order for the authentication
		// mechanism to work. Note that we must NOT include the login form itself (otherwise the session agent
		// will trap it and the client will never see the login page)
		else if(path.startsWith("/pub_redirect.agent")) {
			rval = "pub_dir_session";
		}

		// Here we check for /music/ and set a redirect path
		else if(path.startsWith("/music/")) {
			request.setProperty("redirect",path);
			rval = "music_dir_session";
		}
		// same for the page that the login form submits to, sans redirect path.
		else if(path.startsWith("/music_redirect.agent")) {
			rval = "music_dir_session";
		}
		debug("requireAuthentication()="+rval);
		//		System.out.println(request.getString("request"));
		return(rval);
	}

	/**
	 * If the request requires authentication, attempt to authenticate. If the request authenticates
	 * successfully, return the session time (positive number greater than 0), otherwise return 0 to
	 * indicate an infinite session time or a negative number to indicate that authentication failed.
	 */
	public int authenticate(HTTPAgentRequest request, String realm, Props p)
	{
		int rval = -1;

		// This is where the authentication method is applied. In this example, there is one hardcoded
		// username and password per realm.
		debug("username="+request.getString("username")+" password="+request.getString("password")+" realm="+request.getString("realm")+" realm="+realm);

		if(realm.equals("pub_dir_session")) {
			if(request.getString("username").equals("pub") &&
				request.getString("password").equals("pass1")) {
				p.setProperty("id","60");
				rval = 60000;
			}
		}
		else if(realm.equals("music_dir_session")) {
			if(request.getString("username").equals("music") &&
				request.getString("password").equals("pass1")) {
				p.setProperty("id","58");
				rval = 0;
			}
		}
			
		debug("authentication()="+rval);
		return(rval);
	}

	/**
	 * If authentication is needed and no session has been created (or a session expired), the
	 * client needs to be redirected to a page that allows a method of authentication. After the
	 * client authenticates on this page, authenticate() is called on the returned request. This
	 * method needs to redirect the browser by writing the redirect HTTP response using the
	 * given AgentRequest and realm. Return true if the redirect was successfully written.
	 */
	public boolean authenticationRedirect(HTTPAgentRequest request, String realm)
	{
		// In this example, all we ever do is redirect to the page that has the login form. Here,
		// you can choose whether you want to redirect to a different page depending on certain
		// circumstances such as too many failed logins

		Props p = new Props();

		// These properties are specifically tailored for the login page, 
		// - realm is passed in the event that the realm information is needed
		// - redirect is the page that the client attempted to access, this will be used by the redirect page
		// - the login_count is passed back and forth to determine the number of login attempts
		p.setProperty("realm",realm);
		p.setProperty("redirect",request.getString("redirect"));
		p.setProperty("login_count",request.getString("login_count"));

		// In this example, we use a separate login page for the 2 realms. The login page will
		// submit to the redirect page which gets passed through this WebServerExtension.
		if(realm.equals("pub_dir_session")) {
			redirectBrowser(request,"/pub_login.agent",p);
		}
		else if(realm.equals("music_dir_session")) {
			redirectBrowser(request,"/music_login.agent",p);
		}
		debug("authenticationRedirect()");
		return(true);
	}

	public boolean logoutRedirect(HTTPAgentRequest request, String realm)
	{
		boolean rval = false;
		if(realm.equals("pub_dir_session")) {
			request.removeProperty(getString("logout_key"));
			redirectBrowser(request,"/",new Props());
			rval = true;
		}
		
		return(rval);
	}


}
