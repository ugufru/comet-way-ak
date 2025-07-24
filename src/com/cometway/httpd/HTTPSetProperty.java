package com.cometway.httpd;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Vector;

import com.cometway.ak.*;
import com.cometway.util.jGrep;
import com.cometway.util.Pair;
import com.cometway.util.IntegerPair;

import java.util.regex.Pattern;

/**
 * This WebServerExtension reads in a file of patterns and properties pairs, separated
 * by a space. The patterns are regular expressions and will be applied to the entire
 * request header (the 'request' property in the HTTPAgentRequest). If there is a match
 * the name/value pair given will be set in the HTTPAgentRequest. If the value of the 
 * name/value pair is '$MATCH', the value will be the matched String. All patterns are
 * checked even if there are multiple matches. This WebServerExtension will always
 * pass through regardless of any matches. Example match file:
 * 
 * Firefox/2.0.0.8 browser_type=newest firefox
 * Mozilla/[0-9]\.[0-9] mozilla_version=$MATCH
 *
 * The 'browser_tyoe' property will be set to 'newest firefox' if the String 'Firefox/2.0.0.8'
 * appears anywhere in the HTTP request header. The 'mozilla_version' property will be
 * set to the matching String if 'Mozilla/[0-9\.[0-9]' matches anything in the HTTP request
 * header.
 */
public class HTTPSetProperty extends WebServerExtension
{
	Vector pairs;
	// No longer needed - Pattern compilation is done on demand

	public void initProps()
	{
		setDefault("service_name","extension://.*");
		setDefault("match_file","http.set");

		setDefault("webserver_service_name","none");
		setDefault("domains","none");
	}

	public void start()
	{
		readMatchFile();

		super.start();
	}

	public void stop()
	{
		pairs = null;
	}

	public void readMatchFile()
	{
		File f = new File(getString("match_file"));
		pairs = new Vector();

		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(f));
			String line = in.readLine();
			while(line!=null) {
				if(!line.startsWith("#")) {
					if(line.trim().length()>0) {
						int x = line.lastIndexOf(" ");
						if(x==-1) {
							x = line.lastIndexOf("\t");
						}
						if(x!=-1) {
							String match = line.substring(0,x).trim();
							String prop = line.substring(x).trim();
							x = prop.indexOf("=");
							if(x!=-1) {
								pairs.addElement(new Pair(match,new Pair(prop.substring(0,x).trim(),prop.substring(x+1).trim())));
							}
						}
					}
				}
				line = in.readLine();
			}
		}
		catch(Exception e) {
			error("Error reading match file: "+getString("match_file"),e);
		}
	}

	public boolean handleRequest(HTTPAgentRequest request)
	{
		String header = request.getString("request");
		for(int x=0;x<pairs.size();x++) {
			String match = (String)((Pair)pairs.elementAt(x)).first();
			Pair prop = (Pair)((Pair)pairs.elementAt(x)).second();

			IntegerPair p = (IntegerPair)jGrep.indecesOf(match,header,false);
			if(p!=null) {
				if(prop.second().equals("$MATCH")) {
					String value = header.substring(p.firstInt(),p.secondInt());
					debug("Setting property '"+prop.first()+"' to '"+value+"'");
					request.setProperty((String)prop.first(),value);
				}
				else {
					debug("Setting property '"+prop.first()+"' to '"+prop.second()+"'");
					request.setProperty((String)prop.first(),prop.second());
				}
			}
		}
		return(false);
	}
}
