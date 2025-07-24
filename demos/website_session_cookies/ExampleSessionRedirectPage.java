
import com.cometway.website.AbstractPage;
import com.cometway.ak.AgentRequest;
import com.cometway.props.Props;

/**
 * This AbstractPage is used with the ExampleSessionAgent as the page that
 * the username/password from in the ExampleSessionLoginPage submits to. This
 * page must be part of the requests that require authentication in order for
 * the ExampleSessionAgent to authenticate and set the session cookies.
 */
public class ExampleSessionRedirectPage extends AbstractPage
{
	public void handleRequest(AgentRequest request)
	{
		// We simply display a message that the login was successful. If the login
		// credentials were not successful, the Session agent would have trapped the
		// attempt and redirected to the login page again.
		request.println("<META HTTP-EQUIV=Refresh CONTENT=\"5; URL='"+request.getString("redirect")+"'\">");
		request.println("<H2>Login is successful, wait a moment to be redirected to the page or <a href='"+request.getString("redirect")+"'>click here if you don't want to wait</a></H2>");
		request.println("Test ID = "+request.getString("id"));
	}
}
