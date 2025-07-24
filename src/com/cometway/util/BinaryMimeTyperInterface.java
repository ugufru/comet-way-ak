package com.cometway.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * This interface defines the methods of a binary MIME TYPE mapper. The types are assumed to be
 * some kind of binary form (not text/*).
 */
public interface BinaryMimeTyperInterface
{
	/**
	 * Determine the mime type of the given data
	 *
	 * @param data The byte array to be typed
	 *
	 * @return Returns a String representing the MIME-TYPE
	 */
	public String getMimeType(byte[] data);

	/**
	 * Determine the mime type of the given data starting from the offset
	 *
	 * @param data The byte array to be typed
	 * @param offset Where in the byte array the data starts
	 *
	 * @return Returns a String representing the MIME-TYPE
	 */
	public String getMimeType(byte[] data, int offset);

	/**
	 * Determine the mime type of the data in the InputStream. The InputStream will 
	 * be marked before any data is read, and then reset so that the same data may be
	 * re-read by the caller.
	 *
	 * @param in The InputStream to read the data from
	 *
	 * @return Returns a String representing the MIME-TYPE
	 */
	public String getMimeType(InputStream in) throws IOException;

}