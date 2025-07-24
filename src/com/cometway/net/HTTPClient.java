/*--- formatted by Jindent 2.1, (www.c-lab.de/~jindent) ---*/

package com.cometway.net;

import java.util.*;
import java.io.*;



/**
 * This class supplies the static methods for the HTTPLoader. The actual
 * HTTP/1.0 protocol is implemented in this class.
 */
public class HTTPClient
{
	public static int readBufferSize = 5120;

	public static HTTPResponse sendRequest(HTTPRequest request, InputStream in, OutputStream out, OutputStream dataOut, HTTPLoader loader) throws IOException
	{
		HTTPResponse    rval = new HTTPResponse();
		//System.out.println("request: '"+((int)request.toString().charAt(request.toString().length()-4))+","+((int)request.toString().charAt(request.toString().length()-3))+","+((int)request.toString().charAt(request.toString().length()-2))+","+((int)request.toString().charAt(request.toString().length()-1))+"'");

		//		out.write(request.toString());
		request.writeRequest(out);
		//		out.flush();


		if (request.requestType != request.SHORT_GET_REQUEST_TYPE)
		{
			readHeader(in, rval,loader);
		}
		else
		{
			rval.headers = new Hashtable();
		}
		if (rval.headers.containsKey("http"))
		{
			String  resultCode = ((String[]) rval.getHeader("HTTP"))[0].trim();

			if (resultCode.startsWith("200"))
			{
				rval.resultCode = rval.CODE_200;
			}
			else if(resultCode.startsWith("201")) {
				rval.resultCode = rval.CODE_201;
			}
			else if (resultCode.startsWith("204"))
			{
				rval.resultCode = rval.CODE_204;
			}
			else if (resultCode.startsWith("301"))
			{
				rval.resultCode = rval.CODE_301;
			}
			else if (resultCode.startsWith("302"))
			{
				rval.resultCode = rval.CODE_302;
			}
			else if (resultCode.startsWith("400"))
			{
				rval.resultCode = rval.CODE_400;
			}
			else if (resultCode.startsWith("401"))
			{
				rval.resultCode = rval.CODE_401;
			}
			else if (resultCode.startsWith("403"))
			{
				rval.resultCode = rval.CODE_403;
			}
			else if (resultCode.startsWith("404"))
			{
				rval.resultCode = rval.CODE_404;
			}
			else if (resultCode.startsWith("50"))
			{
				rval.resultCode = rval.CODE_500;
			}
			else {
				rval.resultCode = "null";
			}
		}		
		
		if (request.requestType == request.SHORT_GET_REQUEST_TYPE)
		{
			rval.data = read(in,dataOut,loader);
			rval.resultCode = rval.CODE_200;
		}
		else if (request.requestType == request.HEAD_REQUEST_TYPE)
		{
			rval.data = "";
		}
		else if (rval.resultCode.equals(rval.CODE_200))
		{
		    //		    System.out.println("+++++++++++++");
			if (rval.headers.containsKey("content-length"))
			{
				rval.data = read(in, Integer.parseInt(((String[]) rval.getHeader("Content-length"))[0].trim()),dataOut,loader);
			}
			else
			{
				rval.data = read(in, dataOut,loader);
			}
			//		    System.out.println("================");
		}
		else if (rval.resultCode.startsWith("30"))
		{
			if (rval.headers.containsKey("location"))
			{
				rval.redirectLocation = ((String[]) rval.getHeader("Location"))[0];
			}
		}

		return (rval);
	}


	/**
	 * * This method reads from a InputStream the response header after a request was sent to
	 * the associated BufferedWriter. Lines are read until a double newline is encountered
	 * which signifies the end of the header. Header fields and values are stored in a Hashtable
	 * which is returned. The HTTP result is stored as the key 'HTTP'. This will always be a 3
	 * digit value stored as a String.
	 */


	public static void readHeader(InputStream in, HTTPResponse rval, HTTPLoader loader) throws IOException
	{
		try
		{
			String  line = "";

			int c = in.read();
			//			System.out.println("DATA = "+c+"  CHAR="+((char)c));
			while(c!=10 && c!=-1) {
				if(c!=13) {
					line = line+((char)c);
				}
				c = in.read();
				//				System.out.println("DATA = "+c+"  CHAR="+((char)c));
			}

			if(line.startsWith("HTTP/1.1 100")) {
			    boolean newline = false;
			    line = "";
			    while(c!=-1) {
				if(c==10) {
				    if(newline) {
					break;
				    }
				    newline = true;
				}
				else if(c!=13) {
				    newline = false;
				}
				c = in.read();
			    }
			    c = in.read();
			    while(c!=10 && c!=-1) {
				if(c!=13) {
				    line = line+((char)c);
				}
				c = in.read();
			    }
			}

			while ((line != null) && (!line.equals("")))
			{
				if (line.startsWith("HTTP"))
				{
					String  temp = line.substring(line.indexOf(" ", line.indexOf("/")) + 1);

					if (temp.indexOf(" ") > 0)
					{
						temp = temp.substring(0, temp.indexOf(" "));
					}

					rval.addHeader("HTTP", temp.trim());
				}
				else
				{
					if (line.indexOf(":") > 0)
					{
						String  temp = line.substring(0, line.indexOf(":"));

						line = line.substring(line.indexOf(":") + 1);

						if (!temp.equals("") &&!line.equals(""))
						{
							rval.addHeader(temp.trim(), line.trim());
						}
					}
				}

				//				line = reader.readLine();
				line = "";
				c = in.read();
				//				if(c==10 || c==13) {
				//					break;
				//				}
				while(c!=10 && c!=-1) {
					if(c!=13) {
						line = line+((char)c);
					}
					c = in.read();
					//					System.out.println("DATA = "+c+"  CHAR="+((char)c));
				}
			}
			//System.out.println("done");

		}
		catch (java.io.InterruptedIOException iio)
		{
			//			System.out.println("[HTTPClient] Socket timed out. Returning 500 result.");
			loader.error("Socket timed out. Returning 500 result: "+loader.requestURL+", timeout="+loader.requestTimeout);
			rval.addHeader("HTTP", "500");
		}
		catch (Exception t)
		{
			//			System.out.println("[HTTPClient] read header exception: " + t);
			//			t.printStackTrace();
			loader.exception("read header exception",t);
			throw(new IOException("Read header exception"));
		}
	}


	/**
	 * * This method reads from the InputStream until there is nothing left to read. Do not
	 * use this method if the last line does not end with a newline or if the connection is
	 * going to hang, otherwise, this method will block.
	 * @param reader This is the InputStream to read from.
	 * @return Returns as a String the data read from the reader.
	 */


	public static String read(InputStream reader, OutputStream dataOut, HTTPLoader loader) throws IOException
	{
		StringBuffer    rval = new StringBuffer();

		try
		{
			if(dataOut==null) {
				try {
					byte[] buffer = new byte[5120];
					int bytesRead = reader.read(buffer);
					while(bytesRead > 0) {
					    if(bytesRead == buffer.length) {
							rval.append(new String(buffer,loader.charset));
						}
						else {
							rval.append(new String(buffer,0,bytesRead,loader.charset));
						}
						bytesRead = reader.read(buffer);
					}
				}
				catch(java.io.EOFException e) {
					;
				}
				catch(java.net.SocketException e) {
				    ;
				}
				//				catch (java.io.InterruptedIOException iio) {
				    //				    if(rval.length()>0) {
			}
			else {
			    byte[] buffer = null;
			    buffer = new byte[5120];
				try {
					int bytesRead = reader.read(buffer);
					while(bytesRead > 0) {
						dataOut.write(buffer,0,bytesRead);
						bytesRead = reader.read(buffer);
					}
				}
				catch(java.io.EOFException e) {
					;
				}
				catch(java.net.SocketException e) {
				    ;
				}
				dataOut.flush();
			}
		}
		catch (java.io.InterruptedIOException iio)
		{
			//			System.out.println("[HTTPClient] Socket timed out.");
			//			rval.append("!!![HTTPClient] Socket timed out.");
			loader.error("Socket timed out: "+loader.requestURL+", timeout="+loader.requestTimeout);
		}
		catch (Exception t)
		{		// This is the way things happen when using SSL it seems.
			if (t.getClass().getName().equals("crysec.SSL.SSLIOException") == false)
			{
				//				System.out.println("[HTTPClient] read exception: " + t);
				//				t.printStackTrace();
				loader.exception("Exception while reading form stream",t);
				throw(new IOException("Read header exception"));
			}
		}
		//		System.out.println(rval);
		return (rval.toString());
	}


	/**
	 * * This methods reads only a certain number of bytes from the InputStream then returns.
	 * @param reader This is the InputStream to read from.
	 * @param bytes This is the number of bytes (Characters) to read.
	 * @return Returns as a String the data read from the reader.
	 */


	public static String read(InputStream reader, int bytes, OutputStream dataOut, HTTPLoader loader) throws IOException
	{
		int		count = 0;
		StringBuffer    out = new StringBuffer();
		int		bytesread = 0;
		byte[]		rval = new byte[5120];

		try
		{
			if(dataOut==null) {
				while (true)
				{
					try {
						if (count + 1024 > bytes)
						{
							bytesread = reader.read(rval, 0, bytes - count);
						}
						else
						{
							bytesread = reader.read(rval);
						}
						if(bytesread==-1) {return(out.toString());}
					}
					catch(java.io.EOFException e) {;}

					if (bytesread > 0)
					{
						out.append(new String(rval, 0, bytesread, loader.charset));
						
						count = count + bytesread;
					}


					if (count >= bytes)
					{
						return (out.toString());
					}
				}
			}
			else {
				while (true)
				{
					try {
						if (count + 1024 > bytes)
						{
							bytesread = reader.read(rval, 0, bytes - count);
						}
						else
						{
							bytesread = reader.read(rval);
						}
						if(bytesread==-1) {return(out.toString());}
					}
					catch(java.io.EOFException e) {;}

					
					if (bytesread > 0)
					{
						dataOut.write(rval,0,bytesread);
						dataOut.flush();
						
						count = count + bytesread;
					}

					if (count >= bytes)
					{
						dataOut.flush();
						return ("");
					}
					//System.out.println("reading: "+bytes);

					/*
					while(count<bytes) {
						int c = reader.read();
						//
						//						System.out.println("DATA: "+c);
						dataOut.write(c);
						dataOut.flush();
						count++;
					}
					return("");
					*/
				}
			}

		}
		catch (java.io.InterruptedIOException iio)
		{
			//			System.out.println("[HTTPClient] Socket timed out.");
			//			out.append("!!![HTTPClient] Soecket timed out.");
			loader.error("Socket timed out: "+loader.requestURL+", timeout="+loader.requestTimeout);
		}
		catch (Exception t)
		{
			//			System.out.println("[HTTPClient] read exception: " + t);
			//			t.printStackTrace();
			loader.exception("Exception occured while reading from stream",t);
			throw(new IOException("Read header exception"));
		}

		return ("");
	}


	/**
	 * * This method converts a string to a POST param value encoding
	 */


	public static String convert(String in)
	{
		StringBuffer    buffer;
		StringBuffer    valueBuffer;
		char		theChar;
		char		theChar2;

		if(in==null) {
			return(null);
		}

		buffer = new StringBuffer();
		valueBuffer = new StringBuffer(in);

		for (int i = 0; i < valueBuffer.length(); i++)
		{
			theChar = valueBuffer.charAt(i);

			if (theChar == ' ')
			{
				buffer.append("+");
			}
			else if (theChar == '*')
			{
				buffer.append(theChar);
			}
			else if (theChar == ',')
			{
				buffer.append("%2C");
			}
			else if (theChar == '/')
			{
				buffer.append("%2F");
			}
			else if (theChar == ':')
			{
				buffer.append("%3A");
			}
			else if (theChar == '<')
			{
				buffer.append("%3C");
			}
			else if (theChar == ';')
			{
				buffer.append("%3B");
			}
			else if (theChar == '=')
			{
				buffer.append("%3D");
			}
			else if (theChar == '>')
			{
				buffer.append("%3E");
			}
			else if (theChar == '?')
			{
				buffer.append("%3F");
			}
			else if (theChar == '[')
			{
				buffer.append("%5B");
			}
			else if (theChar == '\\')
			{
				buffer.append("%5C");
			}
			else if (theChar == ']')
			{
				buffer.append("%5D");
			}
			else if (theChar == '^')
			{
				buffer.append("%5E");
			}
			else if (theChar == '`')
			{
				buffer.append("%60");
			}
			else if (theChar == '{')
			{
				buffer.append("%7B");
			}
			else if (theChar == '|')
			{
				buffer.append("%7C");
			}
			else if (theChar == '}')
			{
				buffer.append("%7D");
			}
			else if (theChar == '~')
			{
				buffer.append("%7E");
			}
			else if ((theChar > '+') && (theChar < 0x80))
			{
				buffer.append(theChar);
			}
			else
			{
				theChar2 = (char) ((theChar >> 4) + '0');
				theChar = (char) ((theChar & 0xF) + '0');

				if (theChar2 > '9')
				{
					theChar2 += ('A' - '9' - 1);
				}

				if (theChar > '9')
				{
					theChar += ('A' - '9' - 1);
				}

				buffer.append('%');
				buffer.append(theChar2);
				buffer.append(theChar);
			}
		}

		return (buffer.toString());
	}


	public static String unconvert(String in)
	{
		StringBuffer    buffer = new StringBuffer();
		char		c;

		for (int x = 0; x < in.length(); x++)
		{
			c = in.charAt(x);

			if (c == '+')
			{
				buffer.append(" ");
			}
			else if (c == '%')
			{
				try
				{
					String  hex = "" + in.charAt(++x);

					hex = hex + in.charAt(++x);

					if(hex.equals("20")) {
						buffer.append(" ");
					}
					else if (hex.equals("21"))
					{
						buffer.append("!");
					}
					else if (hex.equals("22"))
					{
						buffer.append("\"");
					}
					else if (hex.equals("23"))
					{
						buffer.append("#");
					}
					else if (hex.equals("24"))
					{
						buffer.append("$");
					}
					else if (hex.equals("25"))
					{
						buffer.append("%");
					}
					else if (hex.equals("26"))
					{
						buffer.append("&");
					}
					else if (hex.equals("27"))
					{
						buffer.append("'");
					}
					else if (hex.equals("28"))
					{
						buffer.append("(");
					}
					else if (hex.equals("29"))
					{
						buffer.append(")");
					}
					else if (hex.equals("2A"))
					{
						buffer.append("*");
					}
					else if (hex.equals("2B"))
					{
						buffer.append("+");
					}
					else if (hex.equals("2C"))
					{
						buffer.append(",");
					}
					else if (hex.equals("2D"))
					{
						buffer.append("-");
					}
					else if (hex.equals("2E"))
					{
						buffer.append(".");
					}
					else if (hex.equals("2F"))
					{
						buffer.append("/");
					}
					else if (hex.equals("30"))
					{
						buffer.append("0");
					}
					else if (hex.equals("31"))
					{
						buffer.append("1");
					}
					else if (hex.equals("32"))
					{
						buffer.append("2");
					}
					else if (hex.equals("33"))
					{
						buffer.append("3");
					}
					else if (hex.equals("34"))
					{
						buffer.append("4");
					}
					else if (hex.equals("35"))
					{
						buffer.append("5");
					}
					else if (hex.equals("36"))
					{
						buffer.append("6");
					}
					else if (hex.equals("37"))
					{
						buffer.append("7");
					}
					else if (hex.equals("38"))
					{
						buffer.append("8");
					}
					else if (hex.equals("39"))
					{
						buffer.append("9");
					}
					else if (hex.equals("3A"))
					{
						buffer.append(":");
					}
					else if (hex.equals("3B"))
					{
						buffer.append(";");
					}
					else if (hex.equals("3C"))
					{
						buffer.append("<");
					}
					else if (hex.equals("3D"))
					{
						buffer.append("=");
					}
					else if (hex.equals("3E"))
					{
						buffer.append(">");
					}
					else if (hex.equals("3F"))
					{
						buffer.append("?");
					}
					else if (hex.equals("40"))
					{
						buffer.append("@");
					}
					else if (hex.equals("41"))
					{
						buffer.append("A");
					}
					else if (hex.equals("42"))
					{
						buffer.append("B");
					}
					else if (hex.equals("43"))
					{
						buffer.append("C");
					}
					else if (hex.equals("44"))
					{
						buffer.append("D");
					}
					else if (hex.equals("45"))
					{
						buffer.append("E");
					}
					else if (hex.equals("46"))
					{
						buffer.append("F");
					}
					else if (hex.equals("47"))
					{
						buffer.append("G");
					}
					else if (hex.equals("48"))
					{
						buffer.append("H");
					}
					else if (hex.equals("49"))
					{
						buffer.append("I");
					}
					else if (hex.equals("4A"))
					{
						buffer.append("J");
					}
					else if (hex.equals("4B"))
					{
						buffer.append("K");
					}
					else if (hex.equals("4C"))
					{
						buffer.append("L");
					}
					else if (hex.equals("4D"))
					{
						buffer.append("M");
					}
					else if (hex.equals("4E"))
					{
						buffer.append("N");
					}
					else if (hex.equals("4F"))
					{
						buffer.append("O");
					}
					else if (hex.equals("50"))
					{
						buffer.append("P");
					}
					else if (hex.equals("51"))
					{
						buffer.append("Q");
					}
					else if (hex.equals("52"))
					{
						buffer.append("R");
					}
					else if (hex.equals("53"))
					{
						buffer.append("S");
					}
					else if (hex.equals("54"))
					{
						buffer.append("T");
					}
					else if (hex.equals("55"))
					{
						buffer.append("U");
					}
					else if (hex.equals("56"))
					{
						buffer.append("V");
					}
					else if (hex.equals("57"))
					{
						buffer.append("W");
					}
					else if (hex.equals("58"))
					{
						buffer.append("X");
					}
					else if (hex.equals("59"))
					{
						buffer.append("Y");
					}
					else if (hex.equals("5A"))
					{
						buffer.append("Z");
					}
					else if (hex.equals("5B"))
					{
						buffer.append("[");
					}
					else if (hex.equals("5C"))
					{
						buffer.append("\\");
					}
					else if (hex.equals("5D"))
					{
						buffer.append("]");
					}
					else if (hex.equals("5E"))
					{
						buffer.append("^");
					}
					else if (hex.equals("5F"))
					{
						buffer.append("_");
					}
					else if (hex.equals("60"))
					{
						buffer.append("`");
					}
					else if (hex.equals("61"))
					{
						buffer.append("a");
					}
					else if (hex.equals("62"))
					{
						buffer.append("b");
					}
					else if (hex.equals("63"))
					{
						buffer.append("c");
					}
					else if (hex.equals("64"))
					{
						buffer.append("d");
					}
					else if (hex.equals("65"))
					{
						buffer.append("e");
					}
					else if (hex.equals("66"))
					{
						buffer.append("f");
					}
					else if (hex.equals("67"))
					{
						buffer.append("g");
					}
					else if (hex.equals("68"))
					{
						buffer.append("h");
					}
					else if (hex.equals("69"))
					{
						buffer.append("i");
					}
					else if (hex.equals("6A"))
					{
						buffer.append("j");
					}
					else if (hex.equals("6B"))
					{
						buffer.append("k");
					}
					else if (hex.equals("6C"))
					{
						buffer.append("l");
					}
					else if (hex.equals("6D"))
					{
						buffer.append("m");
					}
					else if (hex.equals("6E"))
					{
						buffer.append("n");
					}
					else if (hex.equals("6F"))
					{
						buffer.append("o");
					}
					else if (hex.equals("70"))
					{
						buffer.append("p");
					}
					else if (hex.equals("71"))
					{
						buffer.append("q");
					}
					else if (hex.equals("72"))
					{
						buffer.append("r");
					}
					else if (hex.equals("73"))
					{
						buffer.append("s");
					}
					else if (hex.equals("74"))
					{
						buffer.append("t");
					}
					else if (hex.equals("75"))
					{
						buffer.append("u");
					}
					else if (hex.equals("76"))
					{
						buffer.append("v");
					}
					else if (hex.equals("77"))
					{
						buffer.append("w");
					}
					else if (hex.equals("78"))
					{
						buffer.append("x");
					}
					else if (hex.equals("79"))
					{
						buffer.append("y");
					}
					else if (hex.equals("7A"))
					{
						buffer.append("z");
					}
					else if (hex.equals("7B"))
					{
						buffer.append("{");
					}
					else if (hex.equals("7C"))
					{
						buffer.append("|");
					}
					else if (hex.equals("7D"))
					{
						buffer.append("}");
					}
					else if (hex.equals("7E"))
					{
						buffer.append("~");
					}
					else if (hex.equals("7F"))
					{
						buffer.append("D");
					}
				}
				catch (Exception e)
				{
					;
				}
			}
			else
			{
				buffer.append(c);
			}
		}

		return (buffer.toString());
	}


}

