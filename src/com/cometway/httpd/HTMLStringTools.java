
package com.cometway.httpd;

import java.util.Vector;
import java.io.UnsupportedEncodingException;

import com.cometway.props.Props;
import com.cometway.xml.XML;
import com.cometway.xml.XMLToken;
import com.cometway.xml.XMLParser;
import com.cometway.xml.XMLParserException;
import com.cometway.util.*;


/**
 * This class has a collection of static methods for HTML/HTTP related string manipulation.
 *
 */
public class HTMLStringTools
{
	protected final static String EOL = System.getProperty("line.separator");


	/**
	 * Escapes a HTML String from double quotes
	 */
	public static String escapeDoubleQuotes(String in)
	{
		StringBuffer rval = new StringBuffer();
		int index = in.indexOf("\"");
		int index2 = 0;
		while(index!=-1) {
			rval.append(in.substring(index2,index));
			rval.append("&quot;");
			index2 = index+1;
			index = in.indexOf("\"",index2);
		}
		rval.append(in.substring(index2));

		return(rval.toString());
	}

	/**
	 * Escapes a HTML String from single quotes
	 */
	public static String escapeSingleQuotes(String in)
	{
		StringBuffer rval = new StringBuffer();
		int index = in.indexOf("'");
		int index2 = 0;
		while(index!=-1) {
			rval.append(in.substring(index2,index));
			rval.append("&#39;");
			index2 = index+1;
			index = in.indexOf("'",index2);
		}
		rval.append(in.substring(index2));

		return(rval.toString());
	}


	/**
	 * This method extracts the first img tag that is in the data
	 */
	public static String extractIMGTag(String data, java.util.regex.Pattern compiledPattern)
	{
		IntegerPair p = null;
		if(compiledPattern == null) {
			p = jGrep.indecesOf("<[\\s]*img[^>]*>",data,true);
		}
		else {
			p = jGrep.indecesOf("<[\\s]*img[^>]*>",data,true,compiledPattern);
		}

		if(p==null) {
			return("");
		}
		else {
			return(data.substring(p.firstInt(),p.secondInt()));
		}
	}

	/**
	 * This method extracts the first img tag that is in the data
	 */
	public static String extractIMGTag(String data)
	{
		return(extractIMGTag(data,null));
	}


	/**
	 * This method removes all the HTML tags in the data
	 */
	public static String removeHTMLTags(String text)
	{
		return(removeHTMLTags(text,null));
	}

	/**
	 * This method removes all the HTML tags in the data
	 */
	public static String removeHTMLTags(String text, java.util.regex.Pattern compiledPattern)
	{
		if(compiledPattern == null) {
			return (jGrep.grepAndReplaceText("<[A-Za-z\\s]*[^>]*>","",text,true));
		}
		else {
			return (jGrep.grepAndReplaceText("<[A-Za-z\\s]*[^>]*>","",text,true,compiledPattern));
		}
	}


	/** 
	 * This utility method removes form tags.
	 */
	public static String removeFormTags(String text)
	{
		String rval = jGrep.grepAndReplaceText("</*input[^>]*>","",text,true);
		rval = jGrep.grepAndReplaceText("</*select[^>]*>","",rval,true);
		rval = jGrep.grepAndReplaceText("</*option[^>]*>","",rval,true);
		rval = jGrep.grepAndReplaceText("</*form[^>]*>","<BR>",rval,true);

		return (rval);

	}


	/** 
	 * This utility method removes form tags.
	 */
	public static String removeFormTags(String text, java.util.regex.Pattern compiledPattern)
	{
		String rval = jGrep.grepAndReplaceText("</*input[^>]*>","",text,true,compiledPattern);
		rval = jGrep.grepAndReplaceText("</*select[^>]*>","",rval,true,compiledPattern);
		rval = jGrep.grepAndReplaceText("</*option[^>]*>","",rval,true,compiledPattern);
		rval = jGrep.grepAndReplaceText("</*form[^>]*>","<BR>",rval,true,compiledPattern);

		return (rval);

	}


	/** 
	 * This utility method removes table tags.
	 */
	public static String removeTableTags(String text)
	{
		String rval = jGrep.grepAndReplaceText("</*table[^>]*>","<BR>",text,true);
		rval = jGrep.grepAndReplaceText("</*t[rdhc][^>]*>","<BR>",rval,true);
		rval = jGrep.grepAndReplaceText("</*span[^>]*>","",rval,true);

		return (rval);
	}

	/** 
	 * This utility method removes table tags.
	 */
	public static String removeTableTags(String text, java.util.regex.Pattern compiledPattern)
	{
		String rval = jGrep.grepAndReplaceText("</*table[^>]*>","<BR>",text,true,compiledPattern);
		rval = jGrep.grepAndReplaceText("</*t[drhc][^>]*>","<BR>",rval,true,compiledPattern);
		rval = jGrep.grepAndReplaceText("</*span[^>]*>","",rval,true,compiledPattern);

		return (rval);
	}

	/** 
	 * This utility method removes horizontal rule tags.
	 */
	public static String removeHRTags(String text)
	{
		return (jGrep.grepAndReplaceText("<hr[^>]*>","<BR>",text,true));
	}

	/** 
	 * This utility method removes horizontal rule tags.
	 */

	public static String removeHRTags(String text, java.util.regex.Pattern compiledPattern)
	{
		return (jGrep.grepAndReplaceText("<hr[^>]*>","<BR>",text,true,compiledPattern));
	}

	/** 
	 * This utility method removes list tags.
	 */
	public static String removeListTags(String text)
	{
		String rval = jGrep.grepAndReplaceText("</*[oud]l[^>]*>","<BR>",text,true);
		rval = jGrep.grepAndReplaceText("</*li[^>]*>","<BR>",rval,true);
		rval = jGrep.grepAndReplaceText("</*d[td][^>]*>","<BR>",rval,true);

		return (rval);
	}

	/**
	 * This utility method removes list tags.
	 */

	public static String removeListTags(String text, java.util.regex.Pattern compiledPattern)
	{
		String rval = jGrep.grepAndReplaceText("</*[dou]l[^>]*>","<BR>",text,true,compiledPattern);
		rval = jGrep.grepAndReplaceText("</*li[^>]*>","<BR>",rval,true,compiledPattern);
		rval = jGrep.grepAndReplaceText("</*d[dt][^>]*>","<BR>",rval,true,compiledPattern);

		return (rval);
	}




	/**
	 * * This method returns the first url that it finds in data.
	 */
	public static String extractURL(String data)
	{
		String  ldata = data.toLowerCase();
		int     index = ldata.indexOf("http");

		if (index == -1)
		{
			index = data.length();
		}

		if ((ldata.indexOf("ftp") != -1) && (index > ldata.indexOf("ftp")))
		{
			index = ldata.indexOf("ftp");
		}

		if ((ldata.indexOf("file") != -1) && (index > ldata.indexOf("file")))
		{
			index = ldata.indexOf("file");
		}

		if ((ldata.indexOf("news") != -1) && (index > ldata.indexOf("news")))
		{
			index = ldata.indexOf("news");
		}

		if ((ldata.indexOf("telnet") != -1) && (index > ldata.indexOf("telnet")))
		{
			index = ldata.indexOf("telnet");
		}

		if ((ldata.indexOf("mail") != -1) && (index > ldata.indexOf("mail")))
		{
			index = ldata.indexOf("mail");
		}

		if (index == data.length())
		{
			return ("");
		}

		int     index2 = data.indexOf("\"", index);

		if (index2 == -1)
		{
			index2 = data.length();
		}

		if ((ldata.indexOf(" ", index) != -1) && (index2 > ldata.indexOf(" ", index)))
		{
			index2 = ldata.indexOf(" ", index);
		}

		if ((ldata.indexOf(">", index) != -1) && (index2 > ldata.indexOf(">", index)))
		{
			index2 = ldata.indexOf(">", index);
		}

		if ((ldata.indexOf("'", index) != -1) && (index2 > ldata.indexOf("'", index)))
		{
			index2 = ldata.indexOf("'", index);
		}

		if ((ldata.indexOf("\n", index) != -1) && (index2 > ldata.indexOf("\n", index)))
		{
			index2 = ldata.indexOf("\n", index);
		}

		if ((ldata.indexOf(";", index) != -1) && (index2 > ldata.indexOf(";", index)))
		{
			index2 = ldata.indexOf(";", index);
		}

		return (data.substring(index, index2));
	}



	/**
	 * This method completes all the relative URLs found in the data based off reference URL passed in.
	 */
	public static String completeURLs(String data, String refURL)
	{
		String		append = "";
		String		shortAppend = "";
		StringBuffer    out = new StringBuffer();
		try {
			int		lastSlash = refURL.lastIndexOf("/");
			int		startHost = refURL.indexOf("://");

			System.gc();
			if ((lastSlash == -1) || (startHost == -1))
			{		// Bad URL passed in;
				return (data);
			}
			else if ((startHost + 2 == lastSlash) && (refURL.length() > 10))
			{
				shortAppend = refURL;
			}		

			else if (refURL.indexOf("?", lastSlash) == -1)
			{
				if(refURL.indexOf(".",lastSlash)==-1 && refURL.substring(lastSlash+1).trim().length()>0) {
					shortAppend = refURL;
				}
				else {
					shortAppend = refURL.substring(0, lastSlash);
				}
			}
			else if (refURL.indexOf("?", startHost) != -1)
			{
				int     startCGI = refURL.indexOf("?", startHost);
				String  tempRef = refURL.substring(0, startCGI);

				lastSlash = tempRef.lastIndexOf("/");

				if ((startHost + 2 == lastSlash) && (tempRef.length() > 10))
				{
					shortAppend = tempRef;
				}
				else
				{
					shortAppend = tempRef.substring(0, lastSlash);
				}
			}
			else
			{
				shortAppend = refURL;
			}

			if (startHost != -1)
			{
				int     firstSlash = refURL.indexOf("/", startHost + 3);

				if (firstSlash == -1)
				{
					append = refURL;
				}
				else
				{
					append = refURL.substring(0, firstSlash);
				}
			}	

			if (append.charAt(append.length() - 1) == '/')
			{
				append = append.substring(0, append.length() - 1);
			}

			if (shortAppend.charAt(shortAppend.length() - 1) == '/')
			{
				shortAppend = shortAppend.substring(0, shortAppend.length() - 1);
			}
		
		


			String  ldata = data.toLowerCase();
			int     index = 0;
			int     index2 = 0;
			int     temp = 0;

			String tmp = "";
			index = ldata.indexOf("href");
			while(index>-1 && ldata.length()>0) {
				String quot = "";
				temp = ldata.indexOf("=", index);
				if(temp!=-1) {
					out.append(data.substring(0,temp+1));
					data = data.substring(temp+1);
					ldata = ldata.substring(temp+1);

					index=0;

					while ((index != data.length()) && (Character.isWhitespace(ldata.charAt(index))))
					{
						index++;
					}

					if(ldata.charAt(index)=='"') {
						quot = "\"";
						index2 = ldata.indexOf("\"",index+1);
						tmp = data.substring(index+1,index2);
						data = data.substring(index2+1);
						ldata = ldata.substring(index2+1);
					}
					else if(ldata.charAt(index)=='\'') {
						quot = "'";
						index2 = ldata.indexOf("'",index+1);
						tmp = data.substring(index+1,index2);
						data = data.substring(index2+1);
						ldata = ldata.substring(index2+1);
					}				
					else {
						index2 = ldata.indexOf(" ",index+1);
						temp = ldata.indexOf(">",index+1);
						if(index2==-1) {
							index2=ldata.length();
						}
						if(index2>temp) {
							index2 = temp;
						}
						tmp = data.substring(index,index2);
						data = data.substring(index2);
						ldata = ldata.substring(index2);
					}

					if(!tmp.startsWith("http://") && !tmp.startsWith("mailto:") && !tmp.startsWith("ftp:")) {
						if(tmp.startsWith("/")) {
							out.append(quot+append+tmp+quot);
						}
						else {
							out.append(quot+shortAppend+"/"+tmp+quot);
						}
					}
					else {
						out.append(quot+tmp+quot);
					}

				}
				else {
				    out.append(data.substring(0,1));
				    data = data.substring(1);
				    ldata = ldata.substring(1);
				}
				index = ldata.indexOf("href");
			}
			out.append(data);

			data = out.toString();
			ldata = data.toLowerCase();
			out = new StringBuffer();

			index = ldata.indexOf("img ");
			while(index>-1 && ldata.length()>0) {
				String quot = "";
				temp = ldata.indexOf("src",index);
				if(temp!=-1) {
					temp = ldata.indexOf("=", temp);
					if(temp!=-1) {
						out.append(data.substring(0,temp+1));
						data = data.substring(temp+1);
						ldata = ldata.substring(temp+1);
					
						index=0;
					
						while ((index != data.length()) && (Character.isWhitespace(ldata.charAt(index))))
						{
							index++;
						}
					
						if(ldata.charAt(index)=='"') {
							quot = "\"";
							index2 = ldata.indexOf("\"",index+1);
							tmp = data.substring(index+1,index2);
							data = data.substring(index2+1);
							try {
								ldata = ldata.substring(index2+1);
							}
							catch(Exception e) {
								System.out.println("DATA = "+data);
								System.out.println("LDATA = "+ldata);
							}
						}
						else if(ldata.charAt(index)=='\'') {
							quot = "'";
							index2 = ldata.indexOf("'",index+1);
							tmp = data.substring(index+1,index2);
							data = data.substring(index2+1);
							ldata = ldata.substring(index2+1);
						}				
						else {
							index2 = ldata.indexOf(" ",index+1);
							temp = ldata.indexOf(">",index+1);
							if(index2>temp) {
								index2 = temp;
							}
							tmp = data.substring(index,index2);
							data = data.substring(index2);
							ldata = ldata.substring(index2);
						}

						if(!tmp.startsWith("http://") && !tmp.startsWith("mailto:") && !tmp.startsWith("ftp:")) {
							if(tmp.startsWith("/")) {
								out.append(quot+append+tmp+quot);
							}
							else {
								out.append(quot+shortAppend+"/"+tmp+quot);
							}
						}
						else {
							out.append(quot+tmp+quot);
						}
					}
					else {
					    out.append(data.substring(0,1));
					    data = data.substring(1);
					    ldata = ldata.substring(1);
					    //					    index++;
					}
				}
				else {
				    out.append(data.substring(0,1));
				    data = data.substring(1);
				    ldata = ldata.substring(1);
				    //				    index = index+1;
				}
				index = ldata.indexOf("img ");
			}
			out.append(data);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return (out.toString());
	}







	/**
	 * This method encodes a URL and the data embedded in the URL so that it can be requested.
	 */
	public static String encode(String in)
	{
		return(encode(in,false,false));
	}

	public static String encode(String in, boolean fullEncode, boolean encodeForwardSlash)
	{
		return(encode(in,false,false,"UTF-8"));
	}

	/**
	 * This method encodes a URL and the data embedded in the URL so that it can be requested.
	 * If the boolean parameter, fullEncode, is set to true, every character in the String will
	 * be encoded regardless of whether it needs to be. If the boolean parameter, encodeForwardSlash
	 * is set to true, forward slashes will also be encoded.<p>
	 * Currently, the encoding scheme works like this:<br>
	 * spaces are encoded as '+'  <br>
	 * all letters, numbers, and the '*', '-', '.', '@', and '_' characters are not encoded   <br>
	 * everything else will be encoded as '%XY' where X and Y are the hex value of the character
	 */
	public static String encode(String in, boolean fullEncode, boolean encodeForwardSlash, String charEncoding)
	{
		StringBuffer rval = new StringBuffer();

		try {
			byte[] inBytes = in.getBytes(charEncoding);
			if(fullEncode) {
				for(int x=0;x<inBytes.length;x++) {
					rval.append("%");
					rval.append(intToHex((int)inBytes[x]));
				}
			}
			else {
				for(int x=0;x<inBytes.length;x++) {
					int b = (int)inBytes[x] & 0xFF;
					if(b == 42 ||
						b == 45 ||
						b == 46 ||
						(b >= 48 && b <= 57) ||
						(b >= 64 && b <= 90) ||
						b == 95 ||
						(b >= 97 && b <= 122)) {
						rval.append((char)inBytes[x]);
					}
					else if(b == 47) {
						if(encodeForwardSlash) {
							rval.append("%");
							rval.append(intToHex(b));
						}
						else {
							rval.append(in.charAt(x));
						}
					}
					else if(b == 32) {
						rval.append("+");
					}
					else {
						rval.append("%");
						rval.append(intToHex(b));
					}
				}
			}

		}
		catch(UnsupportedEncodingException e) {
			System.err.println("Unsupported Character Encoding: "+charEncoding);
			e.printStackTrace();
		}

			/*
		int[] inArray = new int[in.length()];
		for(int x=0;x<inArray.length;x++) {
			inArray[x] = (int)in.charAt(x);
		}

		if(fullEncode) {
			for(int x=0;x<inArray.length;x++) {
				rval.append("%");
				rval.append(intToHex(inArray[x]));
			}
		}
		else {
			for(int x=0;x<inArray.length;x++) {
				if(inArray[x] == 42 ||
					inArray[x] == 45 ||
					inArray[x] == 46 ||
					(inArray[x] >= 48 && inArray[x] <= 57) ||
					(inArray[x] >= 64 && inArray[x] <= 90) ||
					inArray[x] == 95 ||
					(inArray[x] >= 97 && inArray[x] <= 122)) {
					rval.append(in.charAt(x));
				}
				else if(inArray[x] == 47) {
					if(encodeForwardSlash) {
						rval.append("%");
						rval.append(intToHex(inArray[x]));
					}
					else {
						rval.append(in.charAt(x));
					}
				}
				else if(inArray[x] == 32) {
					rval.append("+");
				}
				else {
					rval.append("%");
					rval.append(intToHex(inArray[x]));
				}
			}
		}
			*/
		return(rval.toString());
	}

	/**
	 * This method decodes an encoded URL.
	 */
	public static String decode(String in)
	{
		StringBuffer rval = new StringBuffer();

		for(int x=0;x<in.length();x++) {
			char c = in.charAt(x);
			if(c=='+') {
				rval.append(" ");
			}
			else if(c=='%') {
				if(in.length()>=x+3) {
					rval.append((char)hexToInt(in.substring(x+1,x+3)));
					x = x+2;
				}
				else {
					// We assume this URL is malformed and we drop the end
					x=in.length();
				}
			}
			else {
				rval.append(c);
			}
		}				

		return(rval.toString());
	}




	protected static int hexToInt(String hex)
	{
		if(hex.length()>2 || hex.length()==0) {
			return(-1);
		}
		char first = hex.charAt(0);
		char second = hex.charAt(1);

		int firstInt = 0;
		int secondInt = 0;

		if(first == '0') {
			firstInt = 0;
		}
		else if(first == '1') {
			firstInt = 1;
		}
		else if(first == '2') {
			firstInt = 2;
		}
		else if(first == '3') {
			firstInt = 3;
		}
		else if(first == '4') {
			firstInt = 4;
		}
		else if(first == '5') {
			firstInt = 5;
		}
		else if(first == '6') {
			firstInt = 6;
		}
		else if(first == '7') {
			firstInt = 7;
		}
		else if(first == '8') {
			firstInt = 8;
		}
		else if(first == '9') {
			firstInt = 9;
		}
		else if(first == 'A' || first == 'a') {
			firstInt = 10;
		}
		else if(first == 'B' || first == 'b') {
			firstInt = 11;
		}
		else if(first == 'C' || first == 'c') {
			firstInt = 12;
		}
		else if(first == 'D' || first == 'd') {
			firstInt = 13;
		}
		else if(first == 'E' || first == 'e') {
			firstInt = 14;
		}
		else if(first == 'F' || first == 'f') {
			firstInt = 15;
		}


		if(second == '0') {
			secondInt = 0;
		}
		else if(second == '1') {
			secondInt = 1;
		}
		else if(second == '2') {
			secondInt = 2;
		}
		else if(second == '3') {
			secondInt = 3;
		}
		else if(second == '4') {
			secondInt = 4;
		}
		else if(second == '5') {
			secondInt = 5;
		}
		else if(second == '6') {
			secondInt = 6;
		}
		else if(second == '7') {
			secondInt = 7;
		}
		else if(second == '8') {
			secondInt = 8;
		}
		else if(second == '9') {
			secondInt = 9;
		}
		else if(second == 'A' || second == 'a') {
			secondInt = 10;
		}
		else if(second == 'B' || second == 'b') {
			secondInt = 11;
		}
		else if(second == 'C' || second == 'c') {
			secondInt = 12;
		}
		else if(second == 'D' || second == 'd') {
			secondInt = 13;
		}
		else if(second == 'E' || second == 'e') {
			secondInt = 14;
		}
		else if(second == 'F' || second == 'f') {
			secondInt = 15;
		}

		return((firstInt*16)+secondInt);
	}

	protected static String intToHex(int i)
	{
		String rval = "";

		int second = i%16;
		int first = (int)(i/16);

		if(first<10) {
			rval = rval+first;
		}
		else if(first == 10) {
			rval = rval+"A";
		}
		else if(first == 11) {
			rval = rval+"B";
		}
		else if(first == 12) {
			rval = rval+"C";
		}
		else if(first == 13) {
			rval = rval+"D";
		}
		else if(first == 14) {
			rval = rval+"E";
		}
		else if(first == 15) {
			rval = rval+"F";
		}

		if(second<10) {
			rval = rval+second;
		}
		else if(second == 10) {
			rval = rval+"A";
		}
		else if(second == 11) {
			rval = rval+"B";
		}
		else if(second == 12) {
			rval = rval+"C";
		}
		else if(second == 13) {
			rval = rval+"D";
		}
		else if(second == 14) {
			rval = rval+"E";
		}
		else if(second == 15) {
			rval = rval+"F";
		}

		return(rval);
	}

	// blank line = new paragraph
	// *bold*
	// "italics"
	// # numbered list item
	// ## nested numbered list item
	// * unordered list item
	// ** nested unordered list item
	// [image.jpg] nested image
	// http://www.hostname.com
	// user@hostname.com

	public static String convertHTMLToPlainText(String html)
	{
		if(html.trim().length()==0) {
			return("");
		}
		StringBuffer b = new StringBuffer();
		XMLParser parser = new XMLParser(html);
		//		parser.setDecodeEscapeCodes(false);

		try {
			XMLToken t = parser.nextToken();

			while(t!=null) {
				if(t.data.equals("<OL>")||t.data.equals("<UL>")) {
					convertHTMLListToPlainText(t,parser,b,0);
					// We only want to end a paragraph at the end of the outermost list
					b.append(EOL);
				}
				else {
					convertHTMLObjectToPlainText(t,parser,b);
				}
				t = parser.nextToken();
			}
		}
		catch(Exception e) {
			;
		}
		// incoming nasty hack
		String rval = b.toString();
		String threeEOL = EOL+EOL+EOL;
		while(rval.indexOf(threeEOL)!=-1) {
			rval = rval.substring(0,rval.indexOf(threeEOL))+EOL+EOL+rval.substring(rval.indexOf(threeEOL)+threeEOL.length());
		}
		while(rval.indexOf("  ")!=-1) {
			rval = rval.substring(0,rval.indexOf("  "))+rval.substring(rval.indexOf("  ")+1);
		}
		while(rval.indexOf(EOL+" ")!=-1) {
			rval = rval.substring(0,rval.indexOf(EOL+" ")+EOL.length())+rval.substring(rval.indexOf(EOL+" ")+EOL.length()+1);
		}
		while(rval.length()>0 && rval.charAt(0)==' ') {
			rval = rval.substring(1);
		}
		while(rval.indexOf("* .")!=-1) {
			rval = rval.substring(0,rval.indexOf("* ."))+"*."+rval.substring(rval.indexOf("* .")+3);
		}
		while(rval.indexOf("* ,")!=-1) {
			rval = rval.substring(0,rval.indexOf("* ,"))+"*,"+rval.substring(rval.indexOf("* ,")+3);
		}
		while(rval.indexOf("* ?")!=-1) {
			rval = rval.substring(0,rval.indexOf("* ?"))+"*?"+rval.substring(rval.indexOf("* ?")+3);
		}
		while(rval.indexOf("* !")!=-1) {
			rval = rval.substring(0,rval.indexOf("* !"))+"*!"+rval.substring(rval.indexOf("* !")+3);
		}
		while(rval.indexOf("\" .")!=-1) {
			rval = rval.substring(0,rval.indexOf("\" ."))+"\"."+rval.substring(rval.indexOf("\" .")+3);
		}
		while(rval.indexOf("\" ,")!=-1) {
			rval = rval.substring(0,rval.indexOf("\" ,"))+"\","+rval.substring(rval.indexOf("\" ,")+3);
		}
		while(rval.indexOf("\" ?")!=-1) {
			rval = rval.substring(0,rval.indexOf("\" ?"))+"\"?"+rval.substring(rval.indexOf("\" ?")+3);
		}
		while(rval.indexOf("\" !")!=-1) {
			rval = rval.substring(0,rval.indexOf("\" !"))+"\"!"+rval.substring(rval.indexOf("\" !")+3);
		}
		
		return(rval);
	}

	protected static void convertHTMLListToPlainText(XMLToken t, XMLParser parser, StringBuffer b, int depth) throws XMLParserException
	{
		String prefix = "";
		if(t.data.equals("<UL>")) {
			for(int x=0;x<depth+1;x++) {
				prefix = prefix+"-";
			}
		}
		else {
			for(int x=0;x<depth+1;x++) {
				prefix = prefix+"#";
			}
		}
		prefix = prefix+" ";

		t = parser.nextToken();
		while(t!=null && !t.data.equals("</UL>") && !t.data.equals("</OL>")) {
			if(t.data.equals("<LI>")) {
				t = parser.nextToken();
				//				System.out.println("1 token="+t.data);
				b.append(prefix);
				while(t!=null && !t.data.equals("<LI>")) {
					if(t.data.equals("<OL>") || t.data.equals("<UL>")) {
						if(b.lastIndexOf(EOL)!=b.length()-EOL.length()) {
							b.append(EOL);
						}
						convertHTMLListToPlainText(t,parser,b,depth+1);
						t = parser.nextToken();
						//				System.out.println("2 token="+t.data);
					}
					else {
						convertHTMLObjectToPlainText(t,parser,b);
						if(t.data.equals("<LI>")||t.data.equals("</OL>")||t.data.equals("</UL>")) {
							break;
						}
						else {
							t = parser.nextToken();
							//				System.out.println("3 token="+t.data);
						}
					}
				}
				if(!t.data.equals("</OL>")&&!t.data.equals("</UL>")) {
					b.append(EOL);
				}
			}
			else {
				// We'll ignore anything else inside a list that isn't a <LI>
				t = parser.nextToken();
				//				System.out.println("4 token="+t.data);
			}
		}
		if(depth==0) {
			b.append(EOL);
		}
	}


	protected static void convertHTMLObjectToPlainText(XMLToken t, XMLParser parser, StringBuffer b) throws XMLParserException
	{
		if (t.data.equals("</P>")) {
			b.append(EOL + EOL);
		}
		else if (t.data.equals("<B>")) {
			b.append(" *");
		}
		else if (t.data.equals("</B>")) {
			b.append("* ");
		}
		else if (t.data.equals("<I>")) {
			b.append(" \"");
		}
		else if (t.data.equals("</I>")) {
			b.append("\" ");
		}
		else if (t.data.startsWith("<IMG ")) {
			Props p = t.getProps();
			String src = p.getString("SRC");
			if (src.length() == 0) {
				src = p.getString("src");
			}
			b.append(" [");
			b.append(src);
			b.append("] ");
		}
		else if(t.data.startsWith("<A HREF")) {
			Props p = t.getProps();
			String href = p.getString("HREF");
			if(href.length()==0) {
				href = p.getString("href");
			}
			if(href.startsWith("mailto:")) {
				b.append(" ");
				b.append(href.substring(href.indexOf(":")+1));
				b.append(" ");
			}
			else {
				b.append(" ");
				b.append(href);
				b.append(" ");
			}
			while(t!=null && !t.data.equals("</A>")) {
				t = parser.nextToken();
			}
		}
		else if (t.type == XML.ELEMENT_CONTENT) {
			b.append(XML.decode(t.data));
			//			System.out.println("Object="+t.data);
		}
	}

	// blank line = new paragraph
	// *bold*
	// "italics"
	// # numbered list item
	// ## nested numbered list item
	// * unordered list item
	// ** nested unordered list item
	// [image.jpg] nested image
	// http://www.hostname.com
	// user@hostname.com

	protected static String convertPlainTextLineToHTML(String s)
	{
		StringBuffer b = new StringBuffer();
		// this means paragraph change
		if(s.trim().length()==0) {
			b.append("</P>"+EOL+"<P>");
		}
		else {
			Vector words = new Vector();
			int index1 = 0;
			int index2 = 0;
			while(index2<s.length()) {
				if(s.charAt(index2)==' ') {
					if(s.substring(index1,index2).trim().length()>0) {
						words.addElement(s.substring(index1,index2).trim());
					}
					index1 = index2;
				}
				index2++;
			}
			if(index2>index1) {
				words.addElement(s.substring(index1,index2).trim());
			}

			boolean boldOn = false;
			boolean italicsOn = false;
			for(int x=0;x<words.size();x++) {
				String word = (String)words.elementAt(x);
				// check if email address
				if(word.indexOf("@")>0 && word.indexOf("@")!=word.length()-1) {
					b.append("<A HREF='mailto:");
					b.append(word);
					b.append("'>");
					b.append(XML.encode(word));
					b.append("</A> ");
				}
				// check if image
				else if(word.startsWith("[") && word.charAt(word.length()-1)==']') {
					word = word.substring(1,word.length()-1);
					b.append("<IMG SRC='");
					b.append(word);
					b.append("'> ");
				}
				// check if link
				else if(word.indexOf(":")>0 && (word.indexOf(":") + 3 < word.length()) && 
						  (word.startsWith("mailto:") || word.startsWith("https://") || word.startsWith("http://") ||
							word.startsWith("nntp://") || word.startsWith("ntp://") || word.startsWith("ftp://") ||
							word.startsWith("rsync://") || word.startsWith("file://") || word.startsWith("telnet://") ||
							word.startsWith("javascript:") || word.startsWith("irc://") || word.startsWith("mms://") ||
							word.startsWith("about:") || word.startsWith("dict://") || word.startsWith("dav:") ||
							word.startsWith("dns:") || word.startsWith("im:") || word.startsWith("ldap://") ||
							word.startsWith("pop://") || word.startsWith("snmp://") || word.startsWith("aim:") ||
							word.startsWith("cvs://") || word.startsWith("sftp://") || word.startsWith("ssh://"))) {
					b.append("<A HREF='");
					b.append(word);
					b.append("' TARGET='_blank'>");
					b.append(XML.encode(word));
					b.append("</A> ");
				}
				// bold checks start here
				else if(word.startsWith("*") && !word.equals("*") && !boldOn) {
					boolean isBold = false;
					if(!word.equals("**")) {
						if(word.lastIndexOf("*")!=word.indexOf("*")) {
							isBold = true;
						}
						else {					
							for(int z=x;z<words.size();z++) {
								String tmpWord = (String)words.elementAt(z);
								if(tmpWord.lastIndexOf("*")==tmpWord.length()-1 ||
									(tmpWord.length()>1 && tmpWord.indexOf("*")!=-1)) {
									isBold = true;
									break;
								}							
							}
						}
					}
					if(isBold) {
						b.append("<B>");
						// extra check here to see if bold is only for this word
						if(word.lastIndexOf("*")==word.length()-1) {
							System.out.println(word);
							b.append(word.substring(1,word.length()-1));
							b.append("</B> ");
						}
						else if(word.length()>1 && word.lastIndexOf("*")!=word.indexOf("*")) {
							b.append(word.substring(1,word.lastIndexOf("*")));
							b.append("</B>");
							b.append(word.substring(word.lastIndexOf("*")+1));
						}
						else {
							boldOn = true;
							b.append(word.substring(1));
							b.append(" ");
						}
					}
				}
				else if(boldOn && word.indexOf("*")!=-1) {
					//word.lastIndexOf("*")==word.length()-1 && boldOn) {
					b.append(word.substring(0,word.indexOf("*")));
					b.append("</B> ");
					b.append(word.substring(word.indexOf("*")+1));
					boldOn = false;
				}
				// italics checks here
				else if(word.startsWith("\"") && !word.equals("\"") && !boldOn && !italicsOn) {
					boolean isItalics = false;
					if(!word.equals("\"\"")) {
						if(word.lastIndexOf("\"")!=word.indexOf("\"")) {
							isItalics = true;
						}
						else {					
							for(int z=x;z<words.size();z++) {
								String tmpWord = (String)words.elementAt(z);
								if(tmpWord.lastIndexOf("\"")==tmpWord.length()-1 ||
									(tmpWord.length()>1 && tmpWord.indexOf("\"")!=-1)) {
									isItalics = true;
									break;
								}							
							}
						}
					}
					if(isItalics) {
						b.append("<I>");
						// extra check here to see if bold is only for this word
						if(word.lastIndexOf("\"")==word.length()-1) {
							System.out.println(word);
							b.append(word.substring(1,word.length()-1));
							b.append("</I> ");
						}
						else if(word.length()>1 && word.lastIndexOf("\"")!=word.indexOf("\"")) {
							b.append(word.substring(1,word.lastIndexOf("\"")));
							b.append("</I>");
							b.append(word.substring(word.lastIndexOf("\"")+1));
						}
						else {
							italicsOn = true;
							b.append(word.substring(1));
							b.append(" ");
						}
					}
				}
				else if(italicsOn && word.indexOf("\"")!=-1) {
					//word.lastIndexOf("*")==word.length()-1 && boldOn) {
					b.append(word.substring(0,word.indexOf("\"")));
					b.append("</I> ");
					b.append(word.substring(word.indexOf("\"")+1));
					italicsOn = false;
				}
				// everything else goes here:
				else {
					b.append(XML.encode(word));
					b.append(" ");
				}
			}
			if(boldOn) {
				b.append("</B> ");
			}
			else if(italicsOn) {
				b.append("</I> ");
			}
		}

		return(b.toString());
	}

	public static String convertPlainTextToHTML(String s)
	{
		if(s.trim().length()==0) {
			return("");
		}
		StringBuffer b = new StringBuffer();
		boolean hasFirstP = false;

		// first we need newlines
		Vector lines = new Vector();
		int index1 = 0;
		int index2 = 0;
		while(index2<s.length()) {
			if(s.charAt(index2)=='\n') {
				lines.addElement(s.substring(index1,index2).trim());
				index1 = index2;
			}
			index2++;
		}
		if(index2>index1) {
			lines.addElement(s.substring(index1,index2).trim());
			lines.addElement("");
		}

		// a special check to make sure first line isn't a list
		if(lines.size()>0 && (((String)lines.elementAt(0)).startsWith("#") || ((String)lines.elementAt(0)).startsWith("-"))) {
			b.append("<P>");
			hasFirstP = true;
		}
		
		//		System.out.println(lines);
		for(int x=0;x<lines.size();x++) {
			String line = (String)lines.elementAt(x);
			if(line.startsWith("#") || line.startsWith("-")) {
				boolean isOrdered = false;
				// this is so lists are outside of <P> </P> blocks
				if(hasFirstP) {
					b.append("</P>"+EOL);
					hasFirstP = false;
				}
				if(line.startsWith("#")) {
					isOrdered = true;
					b.append("<OL>"+EOL);
				}
				else {
					b.append("<UL>"+EOL);
				}
				while(line.startsWith("#") || line.startsWith("-")) {
					boolean lineRead = false;
					if(line.startsWith("##")) {
						b.append("<OL>"+EOL);
						while(line.startsWith("##") || line.startsWith("--")) {
							if(line.startsWith("###")) {
								b.append("<OL>"+EOL);
								while(line.startsWith("###")) {
									b.append("<LI>");
									b.append(convertPlainTextLineToHTML(line.substring(3)));
									b.append(EOL);
									// gets next line
									x++;
									if(x<lines.size()) {
										line = (String)lines.elementAt(x);
									}
									else {
										break;
									}
								}
								b.append("</OL>"+EOL);
							}
							else if(line.startsWith("---")) {
								b.append("<UL>"+EOL);
								while(line.startsWith("---")) {
									b.append("<LI>");
									b.append(convertPlainTextLineToHTML(line.substring(3)));
									b.append(EOL);
									// gets next line
									x++;
									if(x<lines.size()) {
										line = (String)lines.elementAt(x);
									}
									else {
										break;
									}
								}
								b.append("</UL>"+EOL);
							}
							else {
								b.append("<LI>");
								b.append(convertPlainTextLineToHTML(line.substring(2)));
								b.append(EOL);
								// gets next line
								x++;
								if(x<lines.size()) {
									line = (String)lines.elementAt(x);
								}
								else {
									break;
								}
							}
						}
						b.append("</OL>"+EOL);
					}
					else if(line.startsWith("--")) {
						b.append("<UL>"+EOL);
						while(line.startsWith("##") || line.startsWith("--")) {
							if(line.startsWith("###")) {
								b.append("<OL>"+EOL);
								while(line.startsWith("###")) {
									b.append("<LI>");
									b.append(convertPlainTextLineToHTML(line.substring(3)));
									b.append(EOL);
									// gets next line
									x++;
									if(x<lines.size()) {
										line = (String)lines.elementAt(x);
									}
									else {
										break;
									}
								}
								b.append("</OL>"+EOL);
							}
							else if(line.startsWith("---")) {
								b.append("<UL>"+EOL);
								while(line.startsWith("---")) {
									b.append("<LI>");
									b.append(convertPlainTextLineToHTML(line.substring(3)));
									b.append(EOL);
									// gets next line
									x++;
									if(x<lines.size()) {
										line = (String)lines.elementAt(x);
									}
									else {
										break;
									}
								}
								b.append("</UL>"+EOL);
							}
							else {
								b.append("<LI>");
								b.append(convertPlainTextLineToHTML(line.substring(2)));
								b.append(EOL);
								// gets next line
								x++;
								if(x<lines.size()) {
									line = (String)lines.elementAt(x);
								}
								else {
									break;
								}
							}
						}
						b.append("</UL>"+EOL);
					}
					if((isOrdered && line.startsWith("#")) || (!isOrdered && line.startsWith("-"))) {
						b.append("<LI>");
						b.append(convertPlainTextLineToHTML(line.substring(1)));
						b.append(EOL);	
						x++;

						while(x<lines.size()) {
							line = (String)lines.elementAt(x);
							// This is a special case, where a list element happens to span more than one line
							if(!line.startsWith("#") && !line.startsWith("-") && line.trim().length()>0) {
								//								b.append("<BR>");
								b.append(convertPlainTextLineToHTML(line));
								b.append(EOL);
								x++;
							}
							else {
								break;
							}
						}
						if(x>=lines.size()) {
							break;
						}

						/*
						if(x<lines.size()) {
							line = (String)lines.elementAt(x);
						}
						else {
							break;
						}
						*/
					}
					else {
						// we've already read a line that isn't a list element, since this is a for loop, we gotta go back
						x--;
						break;
					}
				}
				if(isOrdered) {
					b.append("</OL>"+EOL);
				}
				else {
					b.append("</UL>"+EOL);
				}
			}
			else {
				if(!hasFirstP) {
					b.append("<P>");
					hasFirstP = true;
				}
				b.append(convertPlainTextLineToHTML(line));
			}
		}
		
		// incoming nasty hack
		String rval = b.toString();
		if(rval.lastIndexOf("<P>")>rval.lastIndexOf("</P>")) {
			rval = rval+"</P>";
		}
		// incoming nasty hack #2
		while(rval.indexOf("<P></P>")!=-1) {
			rval = rval.substring(0,rval.indexOf("<P></P>"))+rval.substring(rval.indexOf("<P></P>")+7).trim();
		}
		
		return(rval);
	}



	// bolds and italics are auto closed when a list is encountered
	/*
	public static String convertPlainTextToHTML(String s)
	{
		StringBuffer b = new StringBuffer();
		char[] input = s.toCharArray();
		boolean boldOn = false;
		boolean italicsOn = false;
		boolean imageOn = false;
		boolean linkOn1 = false;
		boolean linkOn2 = false;
		boolean linkOn3 = false;
		boolean linkOn4 = false;
		boolean linkOn5 = false;
		boolean linkOn6 = false;
		boolean linkOn7 = false;
		boolean emailOn = false;
		boolean dontIncrement = false;
		int lastIndex = 0;
		int index = 0;

		while(index<input.length) {
			//				System.out.println("imageOn="+imageOn+" linkOn1="+linkOn1+" linkOn2="+linkOn2+" linkOn3="+linkOn3+" linkOn4="+linkOn4+" linkOn5="+linkOn5+" linkOn6="+linkOn6+" linkOn7="+linkOn7+" boldOn="+boldOn+" italicsOn="+italicsOn);
			if(input[index]=='"') {
				if(italicsOn) {
					b.append(input,lastIndex,index-lastIndex);
					b.append("</I>");
					lastIndex = index+1;
					italicsOn = false;
				}
				else {
					b.append(input,lastIndex,index-lastIndex);
					b.append("<I>");
					lastIndex= index+1;
					italicsOn = true;
				}
			}
			else if(input[index]=='*') {
				if(!boldOn) {
					// if this isn't the beginning of a line
					if(index>0 && (input[index-1]=='\r' || input[index-1]=='\n')) {
						index = convertPlainTextListToHTML(input,index,b,0);
						dontIncrement = true;
						lastIndex = index;
					}
					// well, I guess this MUST be bold...
					else {
						boldOn = true;
						b.append(input,lastIndex,index-lastIndex);
						lastIndex = index+1;
					}
				}
				else {
					boldOn = false;
					b.append("<B>");
					b.append(input,lastIndex,index-lastIndex);
					b.append("</B>");
					lastIndex = index+1;
				}
			}
			else if(input[index]=='#') {
				index = convertPlainTextListToHTML(input,index,b,0);
				dontIncrement = true;
				lastIndex = index;
			}
			// images are easy, nesting is not allowed, a nested attempt will be ignored
			else if(input[index]=='[') {
				if(imageOn) {
					b.append('[');   // we may want to remove this, this was the falsestart of image state but could be intentional
					b.append(input,lastIndex,index-lastIndex);
					lastIndex = index+1;
				}
				else {
					imageOn = true;
					if(lastIndex>index) {
						b.append(input,lastIndex,index-lastIndex);
					}
					lastIndex = index+1;
				}
			}
			else if(input[index]==']') {
				if(imageOn) {
					imageOn = false;
					b.append("<img src='");
					b.append(input,lastIndex,index-lastIndex);
					b.append("'> ");
					lastIndex = index+1;
				}
				// if the image state wasn't on, we ignore it and assume it was intentional
			}
			else if(input[index]==' ' || input[index]=='\n') {
				if(emailOn) {
					b.append("<a href=mailto:");
					b.append(input,lastIndex,index-lastIndex);
					b.append(">");
					b.append(input,lastIndex,index-lastIndex);
					b.append("</a> ");
					emailOn = false;
					lastIndex = index+1;
				}
				else if(linkOn7) {
					b.append("<a href='");
					b.append(input,lastIndex,index-lastIndex);
					b.append("'>");
					b.append(input,lastIndex,index-lastIndex);
					b.append("</a> ");
					// reset all the link states
					linkOn1 = false;
					linkOn2 = false;
					linkOn3 = false;
					linkOn4 = false;
					linkOn5 = false;
					linkOn6 = false;
					linkOn7 = false;
					lastIndex = index+1;
				}
				else {
					b.append(input,lastIndex,index-lastIndex);
					lastIndex = index+1;
					// reset all the link states
					linkOn1 = false;
					linkOn2 = false;
					linkOn3 = false;
					linkOn4 = false;
					linkOn5 = false;
					linkOn6 = false;
					b.append(' ');
				}
			}
			else if(input[index]=='@') {
				// if there are two '@' in one word, clearly this isn't an email address
				if(emailOn) {
					emailOn = false;
				}
				else {
					emailOn = true;
				}
			}
			// The image state MUST NOT BE ON in order for this to be a link
			else if(!imageOn) {
				// Must check if beginning of word for link states to activate
				if(lastIndex == index && input[index]=='h') {
					linkOn1 = true;
				}
				else if(linkOn1 && !linkOn2 && input[index]=='t') {
					linkOn2 = true;
				}
				else if(linkOn2 && input[index]=='t') {
					linkOn3 = true;
				}
				else if(linkOn3 && input[index]=='p') {
					linkOn4 = true;
				}
				else if(linkOn4 && input[index]==':') {
					linkOn5 = true;
				}
				else if(linkOn5 && !linkOn6 && input[index]=='/') {
					linkOn6 = true;
				}
				else if(linkOn6 && input[index]=='/') {
					linkOn7 = true;
				}
				else if(!linkOn7){
					// reset all the link states
					linkOn1 = false;
					linkOn2 = false;
					linkOn3 = false;
					linkOn4 = false;
					linkOn5 = false;
					linkOn6 = false;
					linkOn7 = false;
				}
			}
			if(!dontIncrement) {
				index++;
			}
			else {
				dontIncrement = false;
			}
		}
	
		return (b.toString());
	}
    
	protected static int convertPlainTextListToHTML(char[] input, int startingIndex, StringBuffer b, int depth)
	{
		boolean ordered = input[startingIndex]=='#';
		int index = startingIndex + 1;

		if(ordered){b.append("<OL>\n");}else {b.append("<UL>\n");}
		while(index<input.length) {
			System.out.println("START OF LINE: "+new String(input,index,3));
			if(index+depth<input.length) {
				if(ordered) {
					if(input[index+depth]=='#') {
						index = convertPlainTextListToHTML(input,index,b,depth+1);
						continue;
					}
					if(input[index+(depth-1)]!='#') {
						break;
					}
				}
				else {
					if(input[index+depth]=='*') {
						index = convertPlainTextListToHTML(input,index,b,depth+1);
						continue;
					}
					if(input[index+(depth-1)]!='*') {
						break;
					}
				}
			}

			index = index+(depth-1);

			boolean boldOn = false;
			boolean italicsOn = false;
			boolean imageOn = false;
			boolean linkOn1 = false;
			boolean linkOn2 = false;
			boolean linkOn3 = false;
			boolean linkOn4 = false;
			boolean linkOn5 = false;
			boolean linkOn6 = false;
			boolean linkOn7 = false;
			boolean emailOn = false;
			int lastIndex = index;

			b.append("<LI> ");
			while(input[index]!='\n') {
				if(input[index]=='"') {
					if(italicsOn) {
						b.append(input,lastIndex,index-lastIndex);
						b.append("</I>");
						lastIndex = index+1;
						italicsOn = false;
					}
					else {
						b.append(input,lastIndex,index-lastIndex);
						b.append("<I>");
						lastIndex= index+1;
						italicsOn = true;
					}
				}
				else if(input[index]=='*') {
					if(!boldOn) {
						// if this is the end of the String, I guess they fucked up
						boldOn = true;
						b.append(input,lastIndex,index-lastIndex);
						lastIndex = index+1;
					}
					else {
						boldOn = false;
						b.append("<B>");
						b.append(input,lastIndex,index-lastIndex);
						b.append("</B>");
						lastIndex = index+1;
					}
				}
				// images are easy, nesting is not allowed, a nested attempt will be ignored
				else if(input[index]=='[') {
					if(imageOn) {
						b.append('[');   // we may want to remove this, this was the falsestart of image state but could be intentional
						b.append(input,lastIndex,index-lastIndex);
						lastIndex = index+1;
					}
					else {
						imageOn = true;
						if(lastIndex>index) {
							b.append(input,lastIndex,index-lastIndex);
						}
						lastIndex = index+1;
					}
				}
				else if(input[index]==']') {
					if(imageOn) {
						imageOn = false;
						b.append("<img src='");
						b.append(input,lastIndex,index-lastIndex);
						b.append("'> ");
						lastIndex = index+1;
					}
					// if the image state wasn't on, we ignore it and assume it was intentional
				}
				else if(input[index]==' ' || input[index]=='\n') {
					if(emailOn) {
						b.append("<a href=mailto:");
						b.append(input,lastIndex,index-lastIndex);
						b.append(">");
						b.append(input,lastIndex,index-lastIndex);
						b.append("</a> ");
						emailOn = false;
						lastIndex = index+1;
					}
					else if(linkOn7) {
						b.append("<a href='");
						b.append(input,lastIndex,index-lastIndex);
						b.append("'>");
						b.append(input,lastIndex,index-lastIndex);
						b.append("</a> ");
						// reset all the link states
						linkOn1 = false;
						linkOn2 = false;
						linkOn3 = false;
						linkOn4 = false;
						linkOn5 = false;
						linkOn6 = false;
						linkOn7 = false;
						lastIndex = index+1;
					}
					else {
						b.append(input,lastIndex,index-lastIndex);
						b.append(' ');
						lastIndex = index+1;
						// reset all the link states
						linkOn1 = false;
						linkOn2 = false;
						linkOn3 = false;
						linkOn4 = false;
						linkOn5 = false;
						linkOn6 = false;
					}
				}
				else if(input[index]=='@') {
					// if there are two '@' in one word, clearly this isn't an email address
					if(emailOn) {
						emailOn = false;
					}
					else {
						emailOn = true;
					}
				}
				// The image state MUST NOT BE ON in order for this to be a link
				else if(!imageOn) {
					// Must check if beginning of word for link states to activate
					if(lastIndex == index && input[index]=='h') {
						linkOn1 = true;
					}
					else if(linkOn1 && !linkOn2 && input[index]=='t') {
						linkOn2 = true;
					}
					else if(linkOn2 && input[index]=='t') {
						linkOn3 = true;
					}
					else if(linkOn3 && input[index]=='p') {
						linkOn4 = true;
					}
					else if(linkOn4 && input[index]==':') {
						linkOn5 = true;
					}
					else if(linkOn5 && !linkOn6 && input[index]=='/') {
						linkOn6 = true;
					}
					else if(linkOn6 && input[index]=='/') {
						linkOn7 = true;
					}
					else if(!linkOn7) {
						// reset all the link states
						linkOn1 = false;
						linkOn2 = false;
						linkOn3 = false;
						linkOn4 = false;
						linkOn5 = false;
						linkOn6 = false;
						linkOn7 = false;
					}
				}
				index++;
			}
			if(boldOn) {
				b.append("</B>");
			}
			if(italicsOn) {
				b.append("</I>");
			}
			b.append("</LI>\n");
			index++;

			if((ordered && input[index]=='#') || (!ordered && input[index]=='*')) {
				index++;
			}
			else {
				break;
			}
		}
		if(ordered){b.append("</OL>\n");}else{b.append("</UL>\n");}

		return(index);
	}
	*/

	public static void main(String[] args)
	{
		String s = "This'll *be* fun! *yes*...\n\nThis is going to be \"fun\"!";
		String b = convertPlainTextToHTML(s);
		System.out.println(s);
		System.out.println(b);
		System.out.println(convertHTMLToPlainText(b));

	}
}


