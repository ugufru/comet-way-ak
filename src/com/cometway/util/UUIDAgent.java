package com.cometway.util;

import java.util.UUID;

import com.cometway.ak.ServiceAgent;

public class UUIDAgent extends ServiceAgent implements UniqueIDInterface
{
	public void initProps()
	{
		setDefault("service_name","unique_id");
	}

	public String getNewIDString()
	{
		return(UUID.randomUUID().toString());
	}

	public byte[] getNewID()
	{
		UUID id = UUID.randomUUID();
		byte[] rval = new byte[8];

		System.arraycopy(longToBytes(id.getLeastSignificantBits()),0,rval,0,4);
		System.arraycopy(longToBytes(id.getMostSignificantBits()),0,rval,4,4);

		return(rval);
	}

	public String getIDString(byte[] uniqueID)
	{
		String rval = null;

		if(uniqueID.length==8) {
			byte[] tmp = new byte[4];
			System.arraycopy(uniqueID,0,tmp,0,4);
			long bottom = bytesToLong(tmp);
			System.arraycopy(uniqueID,4,tmp,0,4);
			long top = bytesToLong(tmp);

			UUID id = new UUID(top,bottom);
			rval = id.toString();
		}

		return(rval);
	}

	public byte[] getID(String uniqueIDString)
	{
		byte[] rval = null;
		try {
			UUID id = UUID.fromString(uniqueIDString);
			rval = new byte[8];

			System.arraycopy(longToBytes(id.getLeastSignificantBits()),0,rval,0,4);
			System.arraycopy(longToBytes(id.getMostSignificantBits()),0,rval,4,4);
		}
		catch(java.lang.IllegalArgumentException e) {
			error("Could not convert '"+uniqueIDString+"' to a UUID",e);
		}

		return(rval);
	}



	protected byte[] longToBytes(long l)
	{
		byte[] b = new byte[4];
		for(int i= 0; i < 4; i++){
			b[3 - i] = (byte)(l >>> (i * 8));
		}
		return(b);
	}

	protected long bytesToLong(byte[] b)
	{
		long l = 0;
		for(int i =0; i < 4; i++){    
			l <<= 8;
			l ^= (long)b[i] & 0xFF;    
		}
		return(l);
	}
}
