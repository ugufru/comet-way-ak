
import com.cometway.ak.AgentRequest;
import com.cometway.email.AutoReplyAgent;


public class StatusRequestAgent extends AutoReplyAgent
{
	public void initProps()
	{
		setDefault("service_name", "status@localhost");
		setDefault("service_name", "agent@localhost");
		setDefault("reply_to", "agent@localhost");
		setDefault("send_email_service_name", "send_email");
	}


	public String getReply(AgentRequest request)
	{
		return (getDateTimeStr() + ": Status is OK");
	}
}


