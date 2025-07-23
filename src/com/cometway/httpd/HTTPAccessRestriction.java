package com.cometway.httpd;

import com.cometway.ak.*;
import com.cometway.util.StringTools;
import com.cometway.util.jGrep;
import com.cometway.util.Pair;

import java.util.regex.Pattern;


/**
 * This WebServerExtension controls access restriction. Access can be granted or denied based
 * on the path URI, request headers, the client's IP address, or properties set in the
 * HTTPAgentRequest (possibly set by another extension). There are a set deny and allow
 * properties that contains properties for matching against each of these 3 things. If the 
 * 'default_behavior' is set to 'allow', the deny properties are used, if it is set to 'deny',
 * the allow properties are used. The order to determine if a request is allowed or denied is
 * URI path, request headers, IP, then properties. This extension can also apply to a specific path 
 * by using the 'affected_path' property. If this property is set, this extension will only apply
 * to requests with a URI path that starts with this property. Restrictions can be further narrowed
 * by using the 'affected_method' property. By default, all methods (GET, POST, HEAD, etc) are
 * affected, but if this property is set, only that particular method will be affected by this
 * extension.
 *
 * Note: The allow/deny IPs only works if the 'use_inet_address_methods' are set to true in the
 * WebServer, otherwise, no IP information is passed to the extension.
 */
public class HTTPAccessRestriction extends WebServerExtension
{
	String[] paths;
	String[] headers;
	String[] ips;
	Pair[] properties;

	/**
	 * affected_path - If this property is non-empty, this extension will only apply to requests whose URI path start with this property (default: )
	 * affected_method - If this property is non-empty, this extension will only apply to the request method (GET, POST, HEAD, etc.) given by this property (default: )
	 * default_behavior - This determines by default whether all requests should be allowed or denied, can be either "allow" or "deny" (default: allow)
	 * path_matches_allow - A comma separated list of patterns (regexp) that are allowed in the URI path if the default behavior is "deny" (default: )
	 * path_matches_deny - A comma deparated list of patterns (regexp) that are denied in the URI path if the default behavior is "allow" (default: )
	 * header_matches_allow - A comma separated list of patterns (regexp) that are allowed in the header fields if the default behavior is "deny" (default: )
	 * header_matches_deny - A comma separated list of patterns (regexp) that are denied in the header fields if the default behavior is "allow" (default: )
	 * allow_ips - A comma separated list of IP addresses or partial IP addresses that are allowed (or * for all) if the default behavior is "deny" (default: )
	 * deny_ips - A comma separated list of IP addresses or partial IP addresses that are denied (or * for all) if the default behavior is "allow" (default: )
	 * allow_properties - A comma separated list of name=value pairs, if no =value is given, an allow is given if the property exists, otherwise the property must have the given value, used if the default behavior is "deny" (default: )
	 * deny_properties - A comma separated list of name=value pairs, if no =value is given, a deny is given if the property exists, otherwise the property must have the given value, used if the default behavior is "allow" (default: )
	 * deny_response - This is the response if a request is to be denied. This is either a 3 number return code or "drop", where the connection is simply dropped (default: 403)
	 */
	public void initProps()
	{
		setDefault("service_name","extension://.*");

		setDefault("webserver_service_name","none");
		setDefault("domains","none");

		setDefault("affected_path","");
		setDefault("affected_method","");
		setDefault("default_behavior","allow");
		setDefault("path_matches_allow","");
		setDefault("path_matches_deny","");
		setDefault("header_matches_allow","");
		setDefault("header_matches_deny","");
		setDefault("allow_ips","");
		setDefault("deny_ips","");
		setDefault("allow_properties","");
		setDefault("deny_properties","");
		setDefault("deny_response","403");
	}

	public void start()
	{
		String[] tmp_properties = null;
		if(getString("default_behavior").equalsIgnoreCase("allow")) {
			if(getString("path_matches_deny").trim().length()>0) {
				paths = StringTools.commaToArray(getString("path_matches_deny"));
			}
			else {
				paths = new String[0];
			}
			if(getString("header_matches_deny").trim().length()>0) {
				headers = StringTools.commaToArray(getString("header_matches_deny"));
			}
			else {
				headers = new String[0];
			}
			if(getString("deny_ips").trim().length()>0) {
				ips = StringTools.commaToArray(getString("deny_ips"));
			}
			else {
				ips = new String[0];
			}
			if(getString("deny_properties").trim().length()>0) {
				tmp_properties = StringTools.commaToArray(getString("deny_properties"));
			}
			else {
				tmp_properties = new String[0];
			}
		}
		else if(getString("default_behavior").equalsIgnoreCase("deny")) {
			if(getString("path_matches_allow").trim().length()>0) {
				paths = StringTools.commaToArray(getString("path_matches_allow"));
			}
			else {
				paths = new String[0];
			}
			if(getString("header_matches_allow").trim().length()>0) {
				headers = StringTools.commaToArray(getString("header_matches_allow"));
			}
			else {
				headers = new String[0];
			}
			if(getString("allow_ips").trim().length()>0) {
				ips = StringTools.commaToArray(getString("allow_ips"));
			}
			else {
				ips = new String[0];
			}
			if(getString("allow_properties").trim().length()>0) {
				tmp_properties = StringTools.commaToArray(getString("allow_properties"));
			}
			else {
				tmp_properties = new String[0];
			}
		}
		else {
			error("Unknown 'default_behavior' '"+getString("default_behavior")+"', aborting");
			return;
		}
		// Pattern caching is handled by the Java regex engine

		if(tmp_properties!=null) {
			properties = new Pair[tmp_properties.length];
			for(int x=0;x<properties.length;x++) {
				if(tmp_properties[x].indexOf("=")!=-1) {
					int index = tmp_properties[x].indexOf("=");
					properties[x] = new Pair(tmp_properties[x].substring(0,index),tmp_properties[x].substring(index+1));
				}
				else {
					properties[x] = new Pair(tmp_properties[x],null);
				}
			}
		}

		super.start();
	}

	public boolean handleRequest(HTTPAgentRequest request)
	{
		boolean rval = false;
		String path = request.getString("path");
		String header = request.getRequestHeaders();
		boolean match = false;

		if(((getString("affected_path").trim().length()>0 && path.startsWith(getString("affected_path"))) ||
			 getString("affected_path").trim().length()==0) &&
			((getString("affected_method").trim().length()>0 && header.startsWith(getString("affected_method"))) ||
			 getString("affected_method").trim().length()==0)) {
			
			String ip = request.getString("request_remote_addr");
			
			for(int x=0;x<paths.length;x++) {
				if(jGrep.indecesOf(paths[x],path,false)!=null) {
					match = true;
					break;
					}
			}
			
			if(!match) {
				for(int x=0;x<headers.length;x++) {
					if(jGrep.indecesOf(headers[x],header,false)!=null) {
						match = true;
						break;
					}
				}
			}
			
			if(!match && ip.trim().length()>0) {
				for(int x=0;x<ips.length;x++) {
					if(ips[x].equals("*") || ip.startsWith(ips[x])) {
						match = true;
						break;
					}
				}
			}
			
			if(!match) {
				for(int x=0;x<properties.length;x++) {
					if(request.hasProperty((String)properties[x].first())) {
						if(properties[x].second()!=null) {
							if(request.getProperty((String)properties[x].first()).equals(properties[x].second())) {
								match = true;
								break;
							}
						}
						else {
							match = true;
							break;
						}
					}
				}
			}
			
			if((match && getString("default_behavior").equals("allow")) ||
				(!match && getString("default_behavior").equals("deny"))) {
				rval = true;
				try {
					int responseCode = getInteger("deny_response");
					request.returnVal = ""+responseCode;
					try {
						request.getOutputStream().write(WebServer.getHTMLByCode(responseCode).getBytes());
						request.getOutputStream().flush();
						request.getOutputStream().close();
					}
					catch(Exception e2) {;}
				}
				catch(Exception e) {
					try {
						request.getOutputStream().close();
					}
					catch(Exception e3) {;}
				}
			}
		}

		return(rval);
	}
}
