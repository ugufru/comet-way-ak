
import com.cometway.ak.AgentRequest;
import com.cometway.ak.RequestAgent;

public class UserAgentTest extends RequestAgent
{
	public void initProps()
	{
		setDefault("service_name", "test.agent");
	}

	public void handleRequest(AgentRequest request)
	{
		request.println("<HTML>\n<BODY>");

		String s = request.getString("request");

		request.println("<PRE>" + s + "</PRE>");
		request.println("<HR>");

		String userAgent = getUserAgent(request);
		println("User-Agent = \"" + userAgent + "\"");
		request.println(userAgent);

		request.println("<P>");



		request.println("</BODY>\n</HTML>");
	}


	protected String getUserAgent(AgentRequest request)
	{
		String userAgent = null;

		String s = request.getString("request");
		String ss = s.toLowerCase();
		int start = ss.indexOf("user-agent:");

		if (start != -1)
		{
			start += 11; // length of "User-Agent:"

			int end = ss.indexOf("\n", start);

			if (end != -1)
			{
				userAgent = s.substring(start, end).trim();
			}
		}

		return (userAgent);
	}
}

