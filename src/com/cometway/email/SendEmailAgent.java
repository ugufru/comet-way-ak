
package com.cometway.email;

import com.cometway.ak.ServiceAgent;
import com.cometway.net.ESMTPSender;
import com.cometway.net.ESMTPException;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Vector;

/**
* This agent queues up email messages and sends them to the smtp_host at
* regular intervals. There are a variety of properties that can be
* configured which define how and when these messages are sent.
* This agent implements the SendEmailInterface and registers with
* the Service Manager for other agents to use.
*/

public class SendEmailAgent extends ServiceAgent implements Runnable, SendEmailInterface
{
	protected Thread emailerThread;
	protected Vector messageQueue;
	protected Object vectorSync;
	protected boolean isRunning;


	/**
	* Initializes the Props for this agent:
	* "service_name" is used to register this agent with the Service Manager (default: send_email),
	* "max_queue_size" is the maximum number of messages allocated for storage in the queue (default: 100),
	* "smtp_sleep_ms" is the interval between attempts to send messages in the queue (default: 5000ms),
	* "smtp_host" is the host name or IP address of the SMTP server,
	* "smtp_port" is the port of the SMTP server (default: 25),
	* "smtp_timeout_ms" is the amount of time this agent will wait for a socket connection (default 500ms)
	*/

	public void initProps()
	{
		setDefault("service_name", "send_email");
		setDefault("max_queue_size", "100");
		setDefault("smtp_sleep_ms", "5000");
		setDefault("smtp_host", "localhost");
		setDefault("smtp_port", "25");
		setDefault("smtp_timeout_ms", "30000");
		setDefault("auth_username","none");
		setDefault("auth_password","none");
	}


	/**
	* Starts this agent by creating the message queue, and the Thread that monitors its
	* contents and sends the messages waiting there, then registers itself with the Service Manager
	* using the "service_name" property.
	*/

	public void start()
	{
		isRunning = true;
		vectorSync = new Object();
		messageQueue = new Vector();

		emailerThread = new Thread(this);
		emailerThread.start();

		register();

		String smtp_host = getString("smtp_host");
		int smtp_port = getInteger("smtp_port");  
		int smtp_sleep_ms = getInteger("smtp_sleep_ms");

		println("Sending to " + smtp_host + ":" + smtp_port + " every " + smtp_sleep_ms + "ms");
	}


	/**
	* Stops this agent by unregistering itself with the Service Manager,
	* releasing the message queue, and setting appropriate state information
	* to cause the Thread that checks for email in the queue to terminate.
	*/

	public void stop()
	{
		unregister();

		isRunning = false;
		messageQueue = null;
		emailerThread = null;
	}


	/**
	* Adds an email message to the queue to be sent to the SMTP server.
	* If adding this message causes the queue to exceed the "max_queue_size"
	* property, the first message in the queue is removed before adding this one.
	*/

	public void sendEmailMessage(IMessage m)
	{
		int max_queue_size = getInteger("max_queue_size");

		/* If the message queue gets to big, start dumping messages. */

		if (messageQueue.size() > max_queue_size)
		{
			synchronized (vectorSync)
			{
				/* This ensures nothing has changed since the synchronization. */

				if (messageQueue.size() > max_queue_size)
				{
					messageQueue.removeElementAt(0);
				}
			}
		}

		/* Add the message to the queue. */

		messageQueue.addElement(m);


		/* Make sure other threads that have been blocking on vectorSynch get their chance. */

		synchronized (vectorSync)
		{
			vectorSync.notifyAll();
		}			
	}


	/**
	* This method runs on a Thread owned by this agent. While the agent is 
	* running, it periodically checks the message queue for new email messages
	* to send to the SMTP server. If so, it opens a socket connection to the
	* SMTP server and attempts to send each message in the queue, removing the
	* messages from the queue if they were sent successfully.
	*/

	public void run()
	{
		while (isRunning)
		{
			Socket smtpSocket = null;

			try
			{
				if (messageQueue.size() > 0)
				{
					int smtp_sleep_ms = getInteger("smtp_sleep_ms");

					Thread.sleep(smtp_sleep_ms);

					synchronized (vectorSync)
					{
						int count = messageQueue.size();

						if (count > 0)
						{
							println("Preparing to send " + count + " messages...");
						}
					}

					String smtp_host = getString("smtp_host");
					int smtp_port = getInteger("smtp_port");
					int smtp_timeout_ms = getInteger("smtp_timeout_ms");

					try
					{
						println("Connecting to " + smtp_host + ":" + smtp_port);

						smtpSocket = new Socket(smtp_host, smtp_port);
						smtpSocket.setSoTimeout(smtp_timeout_ms);
					}
					catch (Exception e)
					{
						error("Cannot connect to " + smtp_host + ":" + smtp_port, e);
						smtpSocket = null;
					}

					if (smtpSocket != null)
					{
						ESMTPSender mail = new ESMTPSender(smtpSocket);

						if (mail.sendEhlo() == false)
						{
							error("Cannot send HELO");
						}
						else
						{
							String auth_username = getTrimmedString("auth_username");
							String auth_password = getTrimmedString("auth_password");

							if ((auth_username.length() > 0) && (auth_password.equals("none") == false))
							{
								boolean authResult = mail.sendAuthLogin(auth_username, auth_password);

								if (authResult == false)
								{
									error("Cannot authenticate with the " + auth_username + " account via AUTH LOGIN");
								}
							}

							Vector v = new Vector();

							synchronized (vectorSync)
							{
								Enumeration e = messageQueue.elements();

								while (e.hasMoreElements())
								{
									v.addElement(e.nextElement());
								}

								messageQueue.removeAllElements();
							}

							boolean ok = true;
							int count = 0;
							Enumeration e = v.elements();

							/* Send messages one at a time, RSET between each message */

							while (e.hasMoreElements())
							{
								IMessage m = (IMessage) e.nextElement();
								String to = m.getHeaderInfo("to");
								String from = m.getHeaderInfo("from");
								String cc = m.getHeaderInfo("cc");

								ok = true;
								count++;

								if (to.length() == 0)
								{
									error("Message " + count + " is missing \"To\" field.\n" + m);
									ok = false;
								}
								else if ((from.length() == 0))
								{
									error("Message " + count + " is missing \"From\" field.\n" + m);
									ok = false;
								}
								else
								{
									try
									{
										// We have to duplicate the ESMTPSender.sendMail() here
										if(mail.sendFrom(from)) {
											Enumeration users = EmailHeader.getSendToUsers(to);
											StringBuffer failedUsers = new StringBuffer();
											boolean goodUser = false;
											boolean isError = false;
											while(users.hasMoreElements()) {
												String user = (String)users.nextElement();
												if(mail.sendTo(user)) {
													goodUser = true;
												}
												else {
													if(mail.getResponseType() == mail.kTempFailure) {
														failedUsers.append(user);
														failedUsers.append(",");
														isError = true;
													}
													error("Rejected envelope TO address: "+user);
												}
											}

											users = EmailHeader.getSendToUsers(cc);
											while(users.hasMoreElements()) {
												String user = (String)users.nextElement();
												if(mail.sendTo(user)) {
													goodUser = true;
												}
												else {
													error("Rejected CC address: "+user);
												}
											}

											if(!goodUser) {
												if(isError) {
													// this means the email was flat out rejected so don't put back into queue
													ok = true;
												}
												else {
													// We have rejected users. We need to set the To field with the rejected ones.
													m.setHeaderInfo("to",failedUsers.toString());
													ok = false;
												}
											}
											else {
												if(!mail.sendData(m.toString())) {
													if(mail.getResponseType() != mail.kTempFailure) {
														// we were rejected, don't put back into queue
														ok = true;
													}
													else {
														ok = false;
													}
												}
												else {
													// if we have rejected users we need to resend this email with the rejected users.
													if(failedUsers.length()>0) {
														m.setHeaderInfo("to",failedUsers.toString());
														ok = false;
													}
													else {
														// message sent successfully
														println("Message " + count + " sent (" + from + " -> " + to + ")");
														ok = true;
													}
												}
											}
										}
										else {
											error("The envelope FROM address was rejected: "+from);
											ok = true;
										}
											
										/*
										if (mail.sendMessage(m) == false)
										{
												error("Message " + count + " could not be sent.\n" + m);
												ok = false;
										}
										else
										{
											println("Message " + count + " sent (" + from + " -> " + to + ")");
										*/
										if (mail.sendRset() == false)
										{
											error("Could not reset SMTP state");
											throw new ESMTPException(ESMTPException.RSET,"Could not reset state");
										}
											//										}
									}
									catch(ESMTPException ex)
									{
										if(ex.getType()==ex.TO)
										{
											// This is a bad email address and shouldn't be sent
											ok = true;
											error("Message "+count+" could not be sent because the recipient email address is invalid");
										}

										if(ex.getType()==ex.FROM)
										{
											ok = true;
											error("Message "+count+" could not be sent because the sender email address is invalid");
										}

										mail.disconnect();

										try
										{
											println("Reconnecting to " + smtp_host + ":" + smtp_port);
												
											smtpSocket = new Socket(smtp_host, smtp_port);
											smtpSocket.setSoTimeout(smtp_timeout_ms);
										}
										catch (Exception ex1)
										{
											error("Cannot connect to " + smtp_host + ":" + smtp_port, ex1);
											smtpSocket = null;
										}

										if (smtpSocket != null)
										{
											mail = new ESMTPSender(smtpSocket);

											if (mail.sendEhlo() == false)
											{
												error("Cannot send HELO");
											}
											else
											{
												if ((auth_username.length() > 0) && (auth_password.equals("none") == false))
												{
													boolean authResult = mail.sendAuthLogin(auth_username, auth_password);
													
													if (authResult == false)
													{
														error("Cannot authenticate with the " + auth_username + " account via AUTH LOGIN");
													}
												}
											}
										}
									}
									if (ok == false)
									{
										error("Broken contact with " + smtp_host + ":" + smtp_port);
										
										sendEmailMessage(m);
										
										while (e.hasMoreElements())
										{
											sendEmailMessage((IMessage) e.nextElement());
										}
									}
								}
							}

							if (ok)
							{
								mail.disconnect();
							}
							else
							{
								warning("Failed messages were returned to send queue");
							}

							/* MUST release sockets! */

							try
							{
								smtpSocket.close();
								smtpSocket = null;
							}
							catch (Exception ex)
							{
								error("Releasing SMTP socket", ex);
							}

							println("Disconnected from " + smtp_host + ":" + smtp_port);
						}
					}
					else
					{
						error("Cannot connect to " + smtp_host + ":" + smtp_port);
					}
				}
			}
			catch (Exception ex)
			{
				error("Exception caught in polling loop", ex);
			}

			if(smtpSocket!=null) {
				try {smtpSocket.close();}catch(Exception e) {;}
			}

			try
			{
				synchronized (vectorSync)
				{
					if (messageQueue.size() == 0)
					{
						vectorSync.wait();
					}
				}
			}
			catch (Exception exc)
			{
				error("Exception caught in synch block", exc);
			}
		}

		synchronized (vectorSync)
		{
			vectorSync.notifyAll();
		}
	}
}


