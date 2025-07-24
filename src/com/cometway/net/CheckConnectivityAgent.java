package com.cometway.net;

import com.cometway.ak.ServiceAgent;

import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

/**
 * This ServiceAgent runs on a thread and checks if it can reach a specified host at a set interval.
 * This agent implements the CheckConnectivityInterface which provides a method, isReachable(), that
 * returns true if the specified host can be connected to. There are 2 types of connectivity tests,
 * determined by the "check_method" property. If this property is set to "ping", then an ICMP echo
 * and/or TCP ECHO is sent to the host. If this property is set to "connect", then a TCP connection is
 * attempted to the host on the specified port.
 */
public class CheckConnectivityAgent extends ServiceAgent implements CheckConnectivityInterface, Runnable
{
	Thread runThread;
	boolean stopRunning;
	Object sync;
	boolean isConnected;

	public void initProps()
	{
		setDefault("service_name","check_connectivity");

		setDefault("target_host","66.45.116.49");
		setDefault("check_interval_ms","60000");
		setDefault("timeout_ms","5000");

		// This can be either "ping" or "connect", if it's "connect", a port number is required
		setDefault("check_method","ping");
		setDefault("connect_port","80");
	}


	public void start()
	{
		runThread = new Thread(this);
		stopRunning = false;
		isConnected = false;
		sync = new Object();

		runThread.start();

		// We'll sync here to ensure that a check has been made before registering
		try {
			Thread.sleep(5);
			synchronized(sync) {
				debug("Sync");
			}
		}
		catch(Exception e) {;}

		super.start();
	}

	public void stop()
	{
		stopRunning = true;
		synchronized(sync) {
			sync.notify();
		}

		super.stop();
	}

	/**
	 * Implements method in CheckConnectivityInterface that shows whether the host
	 * that's being monitored can be contacted or not. This method will block if a
	 * connectivity check is currently on the way. It will block until the connectivity
	 * check is completed. The maximum blocking time is the timeout set in the agent
	 * properties.
	 */
	public boolean isReachable()
	{
		boolean rval = false;

		// In case the host *just* lost connectivity, we're willing to wait out the timeout
		synchronized(sync) {
			rval = isConnected;
		}

		return(rval);
	}


	public void run()
	{
		while(!stopRunning) {
			try {
				synchronized(sync) {
					// check which kind of check to use
					if(getString("check_method").equalsIgnoreCase("ping")) {
						// Do a simple ping test
						try {
							debug("Attempting ping/echo test on "+getString("target_host"));
							isConnected = InetAddress.getByName(getString("target_host")).isReachable(getInteger("timeout_ms"));
						}
						catch(IOException ioe) {
							warning("Could not ping "+getString("target_host"));
							isConnected = false;
						}
					}
					else if(getString("check_method").equalsIgnoreCase("connect")) {
						Socket testSock = null;
						try {
							debug("Attempting connect test on "+getString("target_host")+":"+getString("connect_port"));
							testSock = new Socket(getString("target_host"),getInteger("connect_port"));
							isConnected = true;
						}
						catch(Exception e) {
							warning("Could not open connection to "+getString("target_host")+":"+getString("connect_port"));
							isConnected = false;
						}
						try {
							testSock.close();
						}
						catch(Exception e) {;}
					}

					debug("Connected: "+isConnected);

					try {
						sync.wait(getInteger("check_interval_ms"));
					}
					catch(Exception e) {
						// ignore the interrupted exception here
						;
					}
				}
			}
			catch(Exception e) {
				error("Exception in main check loop",e);
			}
		}
	}

}
