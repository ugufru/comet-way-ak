package com.cometway.util;

public interface UniqueIDInterface
{
	public String getNewIDString();

	public byte[] getNewID();

	public String getIDString(byte[] uniqueID);

	public byte[] getID(String uniqueIDString);

}
