
package com.cometway.io;


/**
* This exception is thrown by the com.cometway.io.FileTools class when there is a problem.
*/

public class FileToolsException extends Exception
{
	/**
	* Constructor for this exception.
	*/

	public FileToolsException()
	{
	}


	/**
	* Constructor for this exception.
	* @param message Message associated with this exception.
	*/

	public FileToolsException(String message)
	{
		super(message);
	}


	/**
	* Constructor for this exception.
	* @param message Message associated with this exception.
	* @param cause The original exception associated with this exception.
	*/

	public FileToolsException(String message, Exception cause)
	{
		super(message, cause);
	}
}




