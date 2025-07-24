
import com.cometway.ak.Agent;
import com.cometway.email.BugReportInterface;


public class BugReportDemo extends Agent
{
	public void start()
	{
		BugReportInterface reporter = (BugReportInterface) getServiceImpl("bug_report_agent");

		reporter.submitBugReport(toString(), "This is a bug report from your agent.");
	}
}



