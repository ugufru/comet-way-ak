package com.cometway.jdbc;

public class JDBCPropsListException extends RuntimeException
{
	public JDBCPropsListException(String message, Throwable cause)
	{
		super(message,cause);
	}

	public JDBCPropsListException(String message)
	{
		super(message);
	}
}
