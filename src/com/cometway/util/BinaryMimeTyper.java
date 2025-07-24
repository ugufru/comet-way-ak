package com.cometway.util;

import com.cometway.ak.ServiceAgent;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is a generic binary MIME TYPE mapper. The types are assumed to be some kind of
 * binary form.
 */
public class BinaryMimeTyper extends ServiceAgent implements BinaryMimeTyperInterface
{

	/**
	 * Initializes this agent's properties.
	 * <P>
	 *
	 * service_name - the service name String that this agent will register itself as.  <BR>
	 * default_mime_type - the default String MIME TYPE if the type could not be determined, null will be returned if this property is missing.  <BR>
	 */
	public void initProps()
	{
		setDefault("service_name","mime_typer");

		setDefault("default_mime_type","application/octet-stream");
	}



	/**
	 * Determine the mime type of the given data
	 *
	 * @param data The byte array to be typed
	 *
	 * @return Returns a String representing the MIME-TYPE
	 */
	public String getMimeType(byte[] data)
	{
		return(getMimeType(data,0));
	}

	/**
	 * Determine the mime type of the given data starting from the offset
	 *
	 * @param data The byte array to be typed
	 * @param offset Where in the byte array the data starts
	 *
	 * @return Returns a String representing the MIME-TYPE
	 */
	public String getMimeType(byte[] data, int offset)
	{
		// We need about 550 bytes.
		int length = 550;
		if(data.length - offset < 550) {
			length = data.length - offset;
		}
		byte[] buffer = new byte[length];
		System.arraycopy(data,offset,buffer,0,length);
		return(determineMimeType(buffer));
	}

	/**
	 * Determine the mime type of the data in the InputStream. The InputStream will 
	 * be marked before any data is read, and then reset so that the same data may be
	 * re-read by the caller. If the InputStream does not support marking, an exception
	 * will be thrown.
	 *
	 * @param in The InputStream to read the data from
	 *
	 * @return Returns a String representing the MIME-TYPE
	 */
	public String getMimeType(InputStream in) throws IOException
	{
		// We need about 550 bytes.
		int length = 550;
		if(in.available() < length) {
			length = in.available();
		}
		byte[] buffer = new byte[length];
		in.mark(length);
		in.read(buffer);
		in.reset();
		return(determineMimeType(buffer));
	}


	/**
	 * Used to test internally
	 */
	public static void main(String[] args)
	{
		BinaryMimeTyper typer = new BinaryMimeTyper();
		typer.initProps();
		try {
			java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(new java.io.File(args[0])));
			System.out.println(typer.getMimeType(in));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * This method does the signature matching. Other implementations or subclasses can override this method.
	 *
	 * @param buffer The byte array containing the data to be typed, it may or may not be incomplete.
	 *
	 * @return Returns a String representing the MIME-TYPE
	 */
	protected String determineMimeType(byte[] buffer)
	{
		// default type will be just octet-stream, i.e. we don't know
		String rval = null;
		if(hasProperty("default_mime_type")) {
			rval = getString("default_mime_type");
		}

		if(buffer.length>10 &&
			buffer[0]==(byte)0xFF &&
			buffer[1]==(byte)0xD8 &&
			buffer[2]==(byte)0xFF) {
			// Seems these are extraneous
			/*
			buffer[3]==(byte)0xE0 &&
			buffer[6]==(byte)0x4A &&
			buffer[7]==(byte)0x46 &&
			buffer[8]==(byte)0x49 &&
			buffer[9]==(byte)0x46 &&
			buffer[10]==(byte)0x00) {
			*/
			rval = "image/jpeg";
		}
		else if(buffer.length>3 &&
				  buffer[0]==(byte)0x47 &&
				  buffer[1]==(byte)0x49 &&
				  buffer[2]==(byte)0x46) {
			rval = "image/gif";
		}

		else if(buffer.length>8 &&
				  buffer[0]==(byte)0x89 &&
				  buffer[1]==(byte)0x50 &&
				  buffer[2]==(byte)0x4E &&
				  buffer[3]==(byte)0x47 &&
				  buffer[4]==(byte)0x0D &&
				  buffer[5]==(byte)0x0A &&
				  buffer[6]==(byte)0x1A &&
				  buffer[7]==(byte)0x0A) {
				  rval = "image/png";
		}
		else if(buffer.length>2 &&
				  buffer[0]==(byte)0x42 &&
				  buffer[1]==(byte)0x4D) {
			rval = "image/x-xbitmap";
		}
		else if(buffer.length>4 &&
				  buffer[0]==(byte)0x49 &&
				  buffer[1]==(byte)0x49 &&
				  buffer[2]==(byte)0x2A &&
				  buffer[3]==(byte)0x00) {
			rval = "image/tiff";
		}
		else if(buffer.length>3 &&
				  buffer[0]==(byte)0x49 &&
				  buffer[1]==(byte)0x20 &&
				  buffer[2]==(byte)0x49) {
			rval = "image/tiff";
		}
		else if(buffer.length>4 &&
				  buffer[0]==(byte)0x4D &&
				  buffer[1]==(byte)0x4D &&
				  buffer[2]==(byte)0x00 &&
				  (buffer[3]==(byte)0x2A ||
					buffer[3]==(byte)0x2B)) {
			rval = "image/tiff";
		}
		else if(buffer.length>3 &&
				  buffer[0]==(byte)0x49 &&
				  buffer[1]==(byte)0x44 &&
				  buffer[2]==(byte)0x33) {
			rval = "audio/mpeg";
		}
		else if(buffer.length>4 &&
				  buffer[0]==(byte)0x00 &&
				  buffer[1]==(byte)0x00 &&
				  buffer[2]==(byte)0x01 &&
				  buffer[3]==(byte)0x00) {
			rval = "image/x-icon";
		}
		else if(buffer.length>4 &&
				  buffer[0]==(byte)0x25 &&
				  buffer[1]==(byte)0x50 &&
				  buffer[2]==(byte)0x44 &&
				  buffer[3]==(byte)0x46) {
			rval = "application/pdf";
		}
		else if(buffer.length>6 &&
				  buffer[0]==(byte)0x7B &&
				  buffer[1]==(byte)0x5C &&
				  buffer[2]==(byte)0x72 &&
				  buffer[3]==(byte)0x74 &&
				  buffer[4]==(byte)0x66 &&
				  buffer[5]==(byte)0x31) {
			rval = "application/rtf";
		}
		// Microsoft Office signatures
		else if(buffer.length>522 &&
				  buffer[0]==(byte)0xD0 &&
				  buffer[1]==(byte)0xCF &&
				  buffer[2]==(byte)0x11 &&
				  buffer[3]==(byte)0xE0 &&
				  buffer[4]==(byte)0xA1 &&
				  buffer[5]==(byte)0xB1 &&
				  buffer[6]==(byte)0x1A &&
				  buffer[7]==(byte)0xE1) {
			if(buffer[512]==(byte)0xEC &&
				buffer[513]==(byte)0xA5 &&
				buffer[514]==(byte)0xC1 &&
				buffer[515]==(byte)0x00) {
				rval = "application/msword";
			}
			// Not 100% about this one
			else if(buffer[512]==(byte)0xFD &&
					  buffer[513]==(byte)0xFF &&
					  buffer[514]==(byte)0xFF &&
					  buffer[515]==(byte)0xFF &&
					  buffer[516]==(byte)0x10 &&
					  buffer[517]==(byte)0x1F &&
					  buffer[518]==(byte)0x22 &&
					  buffer[519]==(byte)0x23 &&
					  buffer[520]==(byte)0x28 &&
					  buffer[521]==(byte)0x29 &&
					  (buffer[522]==(byte)0x00 ||
						buffer[522]==(byte)0x02)) {
				rval = "application/vnd.ms-excel";
			}
			else if(buffer[512]==(byte)0xFD &&
					  buffer[513]==(byte)0xFF &&
					  buffer[514]==(byte)0xFF &&
					  buffer[515]==(byte)0xFF &&
					  buffer[516]==(byte)0x20 &&
					  buffer[517]==(byte)0x00 &&
					  buffer[518]==(byte)0x00 &&
					  buffer[519]==(byte)0x00) {
				rval = "application/vnd.ms-excel";
			}
			// 09 08 10 00 00 06 05 00
			else if(buffer[512]==(byte)0x09 &&
					  buffer[513]==(byte)0x08 &&
					  buffer[514]==(byte)0x10 &&
					  buffer[515]==(byte)0x00 &&
					  buffer[516]==(byte)0x00 &&
					  buffer[517]==(byte)0x06 &&
					  buffer[518]==(byte)0x05 &&
					  buffer[519]==(byte)0x00) {
				rval = "application/vnd.ms-excel";
			}
			// 00 6E 1E F0
			else if(buffer[512]==(byte)0x00 &&
					  buffer[513]==(byte)0x6E &&
					  buffer[514]==(byte)0x1E &&
					  buffer[515]==(byte)0xF0) {
				rval = "application/vnd.ms-powerpoint";
			}
			// 0F 00 E8 03
			else if(buffer[512]==(byte)0x0F &&
					  buffer[513]==(byte)0x00 &&
					  buffer[514]==(byte)0xE8 &&
					  buffer[515]==(byte)0x03) {
				rval = "application/vnd.ms-powerpoint";
			}

		}
		// 1F 8B 08
		else if(buffer.length>3 &&
				  buffer[0]==(byte)0x1F &&
				  buffer[1]==(byte)0x8B &&
				  buffer[2]==(byte)0x08) {
			rval = "application/x-gzip";
		}
		// 42 5A 68
		else if(buffer.length>3 &&
				  buffer[0]==(byte)0x42 &&
				  buffer[1]==(byte)0x5A &&
				  buffer[2]==(byte)0x68) {
			// Seems there are several mime types for this
			rval = "application/x-bzip2";
		}
		// 50 4B 03 04
		// PK formats
		else if(buffer.length>13 &&
				  buffer[0]==(byte)0x50 &&
				  buffer[1]==(byte)0x4B &&
				  buffer[2]==(byte)0x03 &&
				  buffer[3]==(byte)0x04) {
			// 14 00 08 00 08 00
			if(buffer[4]==(byte)0x14 &&
				buffer[5]==(byte)0x00 &&
				buffer[6]==(byte)0x08 &&
				buffer[7]==(byte)0x00 &&
				buffer[8]==(byte)0x08 &&
				buffer[9]==(byte)0x00) {
				rval = "application/java-archive";
			}
			// 14 00 01 00 63 00 00 00 00 00
			else if(buffer[4]==(byte)0x14 &&
					  buffer[5]==(byte)0x00 &&
					  buffer[6]==(byte)0x01 &&
					  buffer[7]==(byte)0x00 &&
					  buffer[8]==(byte)0x63 &&
					  buffer[9]==(byte)0x00 &&
					  buffer[10]==(byte)0x00 &&
					  buffer[11]==(byte)0x00 &&
					  buffer[12]==(byte)0x00 &&
					  buffer[13]==(byte)0x00) {
				rval = "application/zip";
			}
			else if(buffer[4]==(byte)0x14 &&
					  buffer[5]==(byte)0x00 &&
					  buffer[6]==(byte)0x00 &&
					  buffer[7]==(byte)0x00) {
				rval = "application/zip";
			}
			else if(buffer[4]==(byte)0x0A &&
					  buffer[5]==(byte)0x00 &&
					  buffer[6]==(byte)0x00 &&
					  buffer[7]==(byte)0x00) {
				rval = "application/zip";
			}
		}
		// RIFF  52 49 46 46
		else if(buffer.length> 16 &&
              buffer[0]==(byte)0x52 &&
              buffer[1]==(byte)0x49 &&
              buffer[2]==(byte)0x46 &&
              buffer[3]==(byte)0x46) {
			// 41 56 49 20 4C 49 53 54
			if(buffer[8]==(byte)0x41 &&
				buffer[9]==(byte)0x56 &&
				buffer[10]==(byte)0x49 &&
				buffer[11]==(byte)0x20 &&
				buffer[12]==(byte)0x4C &&
				buffer[13]==(byte)0x49 &&
				buffer[14]==(byte)0x53 &&
				buffer[15]==(byte)0x54) {
				rval = "video/x-msvideo";
			}
			// 57 41 56 45 66 6D 74 20
			else if(buffer[8]==(byte)0x57 &&
					  buffer[9]==(byte)0x41 &&
					  buffer[10]==(byte)0x56 &&
					  buffer[11]==(byte)0x45 &&
					  buffer[12]==(byte)0x66 &&
					  buffer[13]==(byte)0x6D &&
					  buffer[14]==(byte)0x74 &&
					  buffer[15]==(byte)0x20) {
				rval = "audio/x-wav";
			}
		}
		// 75 73 74 61 72
		else if(buffer.length>261 &&
				  buffer[257]==(byte)0x75 &&
				  buffer[258]==(byte)0x73 &&
				  buffer[259]==(byte)0x74 &&
				  buffer[260]==(byte)0x61 &&
				  buffer[261]==(byte)0x72) {
			rval = "application/x-tar";
		}
		else if(buffer.length>4 &&
              buffer[0]==(byte)0xCA &&
              buffer[1]==(byte)0xFE &&
              buffer[2]==(byte)0xBA &&
              buffer[3]==(byte)0xBE) {
			rval = "application/java";
		}
		else if(buffer.length>2 &&
              buffer[0]==(byte)0x25 &&
              buffer[1]==(byte)0x21) {
			rval = "application/postscript";
		}
		else if(buffer.length>4 &&
				  buffer[0]==(byte)0x00 &&
              buffer[1]==(byte)0x00 &&
              buffer[2]==(byte)0x25 &&
              buffer[3]==(byte)0x21) {
			rval = "application/postscript";
		}
		else if(buffer.length>4 &&
				  buffer[0]==(byte)0x00 &&
              buffer[1]==(byte)0x00 &&
              buffer[2]==(byte)0x01 &&
              (buffer[3]&(byte)0xB0) == (byte)0xB0) {
			rval = "video/mpeg";
		}
		else if(buffer.length>3 &&
				  (buffer[0]==(byte)0x43 ||
					buffer[0]==(byte)0x46) &&
              buffer[1]==(byte)0x57 &&
              buffer[2]==(byte)0x53) {
			rval = "application/x-shockwave-flash";
		}
		else if(buffer.length>4 &&
				  buffer[0]==(byte)0x46 &&
              buffer[1]==(byte)0x4C &&
              buffer[2]==(byte)0x56 &&
              buffer[3]==(byte)0x01) {
			rval = "video/x-flv";
		}
		// 66 4C 61 43 00 00 00 22
		else if(buffer.length>8 &&
				  buffer[0]==(byte)0x66 &&
				  buffer[1]==(byte)0x4C &&
				  buffer[2]==(byte)0x61 &&
				  buffer[3]==(byte)0x43 &&
				  buffer[4]==(byte)0x00 &&
				  buffer[5]==(byte)0x00 &&
				  buffer[6]==(byte)0x00 &&
				  buffer[7]==(byte)0x22) {
			rval = "audio/flac";
		}
		else if(buffer.length>4 &&
				  buffer[0]==(byte)0x4D &&
              buffer[1]==(byte)0x54 &&
              buffer[2]==(byte)0x68 &&
              buffer[3]==(byte)0x64) {
			rval = "application/x-midi";
		}




		return(rval);
	}
	
}