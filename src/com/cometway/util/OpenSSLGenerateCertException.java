package com.cometway.util;

public class OpenSSLGenerateCertException extends RuntimeException
{
	public OpenSSLGenerateCertException(String message)
	{
		super(message);
	}

	public OpenSSLGenerateCertException(String message, Throwable exception)
	{
		super(message,exception);
	}

}
