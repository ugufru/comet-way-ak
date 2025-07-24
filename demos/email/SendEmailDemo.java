
import com.cometway.ak.Agent;
import com.cometway.email.Message;
import com.cometway.email.SendEmailInterface;


public class SendEmailDemo extends Agent
{
	public void initProps()
	{
		setDefault("recipient", "support@cometway.com");
		setDefault("reply_to", "support@cometway.com");
		setDefault("subject", "Message From Your Agent");
		setDefault("message", "Hello! This is an email message from your agent!");
	}


	public void start()
	{
		Message message = new Message();
		message.setHeaderInfo("To", getString("recipient"));
		message.setHeaderInfo("From", getString("reply_to"));
		message.setHeaderInfo("Subject", getString("subject"));
		message.setMessage(getString("message"));

		SendEmailInterface emailer = (SendEmailInterface) getServiceImpl("send_email");
		emailer.sendEmailMessage(message);
	}
}




