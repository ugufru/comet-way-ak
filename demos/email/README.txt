This directory contains an ak.xstartup file, as well as some demo agents,
which demonstrate many of the different ways you can use agents to interact
with your email.

To get a quick overview of the agents, you can use the Agent Browser.
This is a swing-based utility for configuring agents stored in the ak.xstartup file.

To start the Agent Browser, use the command:
   ab

   (If all you have is a console, just edit the ak.xstartup file directly as XML.)


In the Agent Browser, you will see the following list of agents:

   100 ServiceManager
   110 Scheduler
   120 ReceiveEmailAgent
   130 SendEmailAgent
   140 AutoReplyAgent
   150 EmailBugReportAgent
   160 ReceivePOP3EmailAgent
   170 RequestDumperAgent
   180 BugReportDemo
   190 SendEmailDemo
   200 StatusRequestAgent


Using the Agent Browser, you can put a checkmark next to each agent that you
wish to run when the Agent Kernel is started. It is not necessary to run all
of these demo agents at once, and running them in selective groups may help
if you're not exactly sure what's going on. Having this ability to selectively
active portions of an agent application is one of its greatest strengths.


ReceiveEmailAgent

This agent will startup a SMTP-compatible server on port 25 and receive
SMTP-based messages. These messages are automatically forwarded to any
RequestAgent who is registered as the coresponding email address in
the Service Manager. Look at AutoReplyAgent and StatusRequestAgent for
demonstrations of this functionality.

Notes for UNIX users:
If you are running sendmail you will have to kill it first. Make sure this
is OK, if you are not the system administrator -- You might interrupt
somebody's email. You'll also need to run the Agent Kernel as 'root'
(using 'sudo' is recommended) in order to bind to port 25.


SendEmailAgent

This agent will queue and periodically send email messages on behalf of
other agents in the Agent Kernel. You should make sure that the
smtp_server property is set to your local email server before using it;
otherwise you will not receive any messages. The AutoReplyAgent,
EmailBugReportAgent, BugReportDemo, SendEmailDemo, and StatusRequestDemo
all depend on the SendEmailAgent directly or indirectly.


AutoReplyAgent

This agent is useful if you need to send a standard reply from a particular
email address. Just set the service_name to the agent's email address, and
the default_reply to the message you want it to send in reply. Look at the
StatusRequestAgent for an example of how you can subclass this agent that
can dynamically generate its own reply.


EmailBugReportAgent

Start this agent as a service to other agents that are capable of reporting
their own bugs. Look at the BugReportDemo agent for an example of how to
lookup and implement the EmailBugReportAgent.


ReceivePOP3EmailAgent

If you have access to a POP3 account, you can use this agent to periodically
check your email and download new messages. These messages are forwarded
to agents that have registered their email address with the Service Manager
for further handling. This agent can be used instead of a ReceiveEmailAgent
when it is not possible to access port 25.


RequestDumperAgent

This agent can be used to examine the contents of an AgentRequest. This can
be useful to debug the parameters sent to a RequestAgent, and can even be
used to examine requests from the com.cometway.httpd.WebServer agent.


BugReportDemo

This agent is an example of how you can submit bug reports from an agent
via email. Upon startup this agent looks up the EmailBugReportAgent
through the Service Manager, and then sends it a single bug report.
After the bug report is submitted, you will need to wait at least 1 minute
before the bug report email is scheduled to be sent.


SendEmailDemo

This agent is an example of how you can send email messages from any agent.
First a Message is created, and its headers are populated with information
contained in the agent's properties. Second, the SendEmailAgent is retrieved
from the Service Manager and used to send the message. Emails can be sent
at any time using this method.


StatusRequestAgent

This is a subclass of AutoReplyAgent which dynamically generates its response
by overriding the getReply() method. A unique response is created for
each request it receives by returning the current time and status. Using
agents like this, you can quickly create an army of agents that intelligently
respond to any email message.




