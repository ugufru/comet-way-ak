
import com.cometway.website.AbstractPage;
import com.cometway.ak.AgentRequest;
import com.cometway.props.Props;
import com.cometway.net.HTTPClient;

/**
 * This AbstractPage is used with the ExampleSessionAgent as the page that
 * generates the login FORM. The FORM gets a username and password and submits
 * it to the ExampleSessionRedirectPage. This page must NOT be part of the
 * requests that requests that require authentication otherwise an unauthenticated
 * client would not be able to log in.
 */
public class ExampleSessionLoginPage extends AbstractPage
{
	public void initProps()
	{
		setDefault("title","");
		setDefault("action","");  // this property is the URI of the ExampleSessionRedirectPage, it is used as the ACTION in the form
	}

	public void handleRequest(AgentRequest request)
	{
		// The realm and count should be passed from the request, if the count is empty, then we start with "1"
		String realm = request.getString("realm");
		String count = ""+(request.getInteger("login_count")+1);

		if(request.getInteger("login_count")>2) {
			// Optionally, we can take care of the "too many incorrect logins" here by keeping track of the login_count
			request.println("<H2>You've attempted to log in 3 times, please contact an administrator for help and then try again</H2>");
			request.println("<A HREF='"+getString("service_name")+"?realm="+HTTPClient.convert(request.getString("realm"))+"&redirect="+HTTPClient.convert(request.getString("redirect"))+"'>Try Logging in again</A>");
		}
		else {
			// Otherwise, we send the client the form, and set the action to the redirect page
			request.println("<H1>Welcome to the "+getString("title")+"</H1>");
			request.println("<FORM METHOD=POST ACTION='"+getString("action")+"'>");
			request.println("<H2>You Need to Log In to view "+request.getString("redirect")+"</H2>");
			request.println("Username: <INPUT TYPE=TEXT NAME=username><BR>");
			request.println("Password: <INPUT TYPE=PASSWORD NAME=password><BR>");
			request.println("<INPUT TYPE=SUBMIT VALUE='Log in'>");
			request.println("<INPUT TYPE=HIDDEN NAME=realm VALUE='"+realm+"'>");
			request.println("<INPUT TYPE=HIDDEN NAME=redirect VALUE='"+request.getString("redirect")+"'>");
			request.println("<INPUT TYPE=HIDDEN NAME=login_count VALUE="+count+">");
			request.println("</FORM>");
		}
	}
}
