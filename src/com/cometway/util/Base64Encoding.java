package com.cometway.util;


import java.util.*;

	/**
	* This helper class contains the MIME/64 encoding and decoding methods when dealing with password 
	* authentication.  These methods can be used by any agent to encode or decode whatever they wish.
	*/

public class Base64Encoding
{
    public final static char[] encodeArray = { 'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/' };

	public static String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	public Base64Encoding()
	{

	}


	/**
	* Encodes a String to Base64. 
	*/
	public static String encode(String in)
	{
		String rval = "";

		boolean[] buffer = new boolean[24];
		boolean[] tmpBuffer = new boolean[6];
		boolean more = false;
		boolean more2 = false;
		for(int x=0;x<in.length();x++) {
		    char c = in.charAt(x);
		    System.arraycopy(intToBin((c & 0x00FF),8),0,buffer,0,8);
		    x++;
		    if(x<in.length()) {
			c = in.charAt(x);
			System.arraycopy(intToBin((c & 0x00FF),8),0,buffer,8,8);
			x++;
			if(x<in.length()) {
			    c = in.charAt(x);
			    System.arraycopy(intToBin((c & 0x00FF),8),0,buffer,16,8);
			}
			else {
			    more = true;
			    break;
			}
		    }
		    else {
			more2 = true;
			break;
		    }

		    System.arraycopy(buffer,0,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,6,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,12,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,18,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];

		    for(int z=0;z<24;z++) {
			buffer[z]=false;
		    }
		}

		if(more2) {
		    System.arraycopy(buffer,0,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,6,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)]+"==";
		}
		else if(more) {
		    System.arraycopy(buffer,0,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,6,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,12,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)]+"=";
		}

		return(rval);
	}


	/**
	* Encodes a String to Base64. 
	*/
	public static String encode(byte[] in)
	{
		String rval = "";

		boolean[] buffer = new boolean[24];
		boolean[] tmpBuffer = new boolean[6];
		boolean more = false;
		boolean more2 = false;
		for(int x=0;x<in.length;x++) {
		    System.arraycopy(intToBin((in[x] & 0x00FF),8),0,buffer,0,8);
		    x++;
		    if(x<in.length) {
			System.arraycopy(intToBin((in[x] & 0x00FF),8),0,buffer,8,8);
			x++;
			if(x<in.length) {
			    System.arraycopy(intToBin((in[x] & 0x00FF),8),0,buffer,16,8);
			}
			else {
			    more = true;
			    break;
			}
		    }
		    else {
			more2 = true;
			break;
		    }

		    System.arraycopy(buffer,0,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,6,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,12,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,18,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];

		    for(int z=0;z<24;z++) {
			buffer[z]=false;
		    }
		}

		if(more2) {
		    System.arraycopy(buffer,0,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,6,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)]+"==";
		}
		else if(more) {
		    System.arraycopy(buffer,0,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,6,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)];
		    System.arraycopy(buffer,12,tmpBuffer,0,6);
		    rval = rval+encodeArray[binToInt(tmpBuffer)]+"=";
		}

		return(rval);
	}



	/**
	* Decodes a String from Base64 encoding.
	*/
	public static byte[] decode(String in)
	{
		Vector rval = new Vector();

		int bufferIndex = 0;
		boolean[] buffer = new boolean[8];

		boolean pad = false;
		for(int z=0;z<in.length();z++) {
			boolean skip = false;
			char c = in.charAt(z);
			int baseIndex = base64.indexOf(c);
			if(baseIndex==-1) {
				if(!Character.isWhitespace(c)) {
					if(c=='=') {
						pad = true;
					}
					else {
						System.err.println("Unknown character in the encoded string "+c+" ("+((int)c)+")");
						break;
					}
				}
				else {
					skip = true;
				}
			}
			if(!skip) {
				boolean[] binChar = { false,false,false,false,false,false };

				if(!pad) {
					binChar = intToBin(baseIndex,6);
					for(int x=0;x<binChar.length;x++) {
						if(bufferIndex>=buffer.length) {
							byte decodeChar = (byte)(binToInt(buffer));
							rval.addElement(Byte.valueOf(decodeChar));
							bufferIndex = 0;
							buffer = new boolean[8];
							buffer[bufferIndex++] = binChar[x];
						}
						else {
							buffer[bufferIndex++] = binChar[x];
						}

					}
				}
				else {
					if(bufferIndex>0) {
						for(int x=bufferIndex;x<8;x++) {
							buffer[x] = false;
						}
						byte decodeChar = (byte)binToInt(buffer);
						if((int)decodeChar!=0) {
							rval.addElement(Byte.valueOf(decodeChar));
						}
					}
				}

				if((bufferIndex==buffer.length) && z==in.length()-1) {
				    byte decodeChar = (byte)(binToInt(buffer));
				    rval.addElement(Byte.valueOf(decodeChar));
				}				    
			}
		}

		byte[] r = new byte[rval.size()];
		for(int x=0;x<r.length;x++) {
			r[x] = ((Byte)rval.elementAt(x)).byteValue();
		}

		return(r);
	}


	/**
	* Converts an integer to a binary value.
	*/
	public static boolean[] intToBin(int in, int radix)
	{
		boolean[] rval = new boolean[radix];
		for(int x=0;x<rval.length;x++) {
			rval[x] = false;
		}
		int index = radix-1;
		
		while(in>1) {
			if(in%2==0) {
				rval[index]=false;
			}
			else {
				rval[index]=true;
			}
			index--;
			in = in>>1;
		}
		if(in%2==0) {
			rval[index]=false;
		}
		else {
			rval[index]=true;
		}

		return(rval);
	}


	/**
	* Converts a binary value to an integer.
	*/
	public static int binToInt(boolean[] bin)
	{
		int rval = 0;

		for(int x=0;x<bin.length;x++) {
			if(bin[(bin.length-1)-x]) {
				rval = rval + (int)Math.pow(2,x);
			}
		}

		return(rval);
	}


	/**
	* Prints a binary number. 
	*/
	protected static void printBin(boolean[] bin)
	{
		for(int x=0;x<bin.length;x++) {
			if(bin[x]) {
				System.out.print("1");
			}
			else {
				System.out.print("0");
			}
		}
		System.out.println();
	}






    public static void main(String[] args)
    {
		 //	System.out.println("ENCODE:"+encode(args[0].getBytes()));
		 args[0] = "";

		 args[0]=args[0]+"/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsK";
		 args[0]=args[0]+"CwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQU";
		 args[0]=args[0]+"FBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wAARCABVAFUDASIA";
		 args[0]=args[0]+"AhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQA";
		 args[0]=args[0]+"AAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3";
		 args[0]=args[0]+"ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWm";
		 args[0]=args[0]+"p6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEA";
		 args[0]=args[0]+"AwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSEx";
		 args[0]=args[0]+"BhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElK";
		 args[0]=args[0]+"U1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3";
		 args[0]=args[0]+"uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD5rM/l";
		 args[0]=args[0]+"3V0scyxFSCIlDcjuA2M598ZPpUDzxzRrCEaVQSzAkhZj6kcZPb14pWZpFBhZzEsi7gcAtz2Peqfi";
		 args[0]=args[0]+"LxBa+EtNW5vVkdG8wW8QxuEgJKZ9icfrX51Cn7SSjBXZ/aWLr4bL6EsRX0jHct6iLlrO9vYI4pGt";
		 args[0]=args[0]+"4vtKQXHKsFGCh6BW9PXFeb6j8Yrya5uZLO0jht3g2FbojesuPvA9x1xn0rA8W+Ptb8RWxstSnWEq";
		 args[0]=args[0]+"RGwt22h0HKhsfeHPbvmudFpPdo5IDsuPLhm4APXPvwDX2GDyqFBOWI95s/n/AD/jXEY6cYZdNwg9";
		 args[0]=args[0]+"/N+Xkb134y1vXTaqZIYwLcwo+7b5uQcO/PXp19KfD8S9VguZnVYpla5S7dSNuHAK7SffPT2FVNWv";
		 args[0]=args[0]+"bOz0V0gLpdXYBkLD5Yx2A/u1y+ZYPtEZCAtzx0BGCP5160cJRlG3JofETzfHxqOf1iV0u7PUtJ+I";
		 args[0]=args[0]+"V/baVFFp2mi5uIpWnvUEbSgEtwvHKAdM+1dRqfxa0K5vdtzpE9uNoL4QMC2BnPr6Dp0FeLtrVzbf";
		 args[0]=args[0]+"azbvLabl2zPEwXeeoB/OtPR7SWG6mub6NWneINKc7lCkYUn0JIP5Vy18qw0+lj3cNxZmuGkoxqp7";
		 args[0]=args[0]+"bq57ppGp6Zr9nGIrUWzvGPs8d0UGEy3U8ELnPHI96ma0WN5Bth86JVCRIFdGYj73Tr789q8QVFDS";
		 args[0]=args[0]+"3z3sUUBZYZmVyzxr3CKK9d8NajBqunyNY2zwWCAQ24nk3TMB96THXb/KvmcblqoLmg9D9h4Z4q/t";
		 args[0]=args[0]+"ap9VxMFGa2e3N8uht2EGySZyiRK+MFPlLY65I68+wxn3oqBdQmDnZarLEQNpVs+o/XGfxorxuQ/R";
		 args[0]=args[0]+"XHX4SBRCmy41CSRbETedcM5J2ADjjqM1498QfGp8Q3MdnHHGDFL5KyxtlZ4ixKnHYjPX6V6v4uNx";
		 args[0]=args[0]+"Z6TLNpFzbRGEzLK1wQq3cfqGbj6V8720STKknKp5ilMsCVG7nP6819bk+GjJe1Z+FeIOb4hTjlqT";
		 args[0]=args[0]+"hHd7NS7NM2NNuop9Nu7cpK2oJKoiaJ9rRrnBHuOM5960tMCSXl1Ld3DGG3wIzO5O9s42nH1NZek+";
		 args[0]=args[0]+"Hp9V8m6iWK483e5jV9hhCYyzfUdM+lP1HT5o7Xy7e2mlJc4YKctk5DfQZ619LO2yPx6mmopNbdSL";
		 args[0]=args[0]+"xlKFMUcSsHLFGiIJXb2Cn3rDuIEEExZJIDuURwOBtGevzHr0rutTtEl8NwxXF49rZrjzGKmSSWTp";
		 args[0]=args[0]+"twOQOBzXJLctds7tbiS1tHUrBIzMpOGqqcnKJNSOpq+HtOi8Tabf2ZYfby6LbyFPu46jjrRodvqW";
		 args[0]=args[0]+"jao0kqRWUkbjfJqEZeKIqfvEYJI7jjHvUcXjHV4pGgR/Jt5LjItoIVUD5vUdK+gfC95o11b3tzcW";
		 args[0]=args[0]+"lzrGrwxxrGtxAqQhuuXVly4Xrx61wV6kqW534ShHEdbOJ4PrNtG2tXF9Hq/2+OaN51vhZeT9ocfw";
		 args[0]=args[0]+"pH1HPfA+lafgXW7HStYlmv3kmvgkcVqjvujBdgHLN2wCePY13PjLQZ9Rum1dzu1i/wD3cSQxKgVg";
		 args[0]=args[0]+"RyUAAVNoOBjP5ivL9ZsIbM3sUxVXhchAvG4dR9CO9NqGJpOnLqdOHr1srxccTCN2nc920Yf2uJ5b";
		 args[0]=args[0]+"J3v4EYRi5VWCtjPTPpz0orC8G3stl4P0eOyud8Zh3MkQxsYkkg+v1or8/qxjTm4J7H9bYCpXx2Fp";
		 args[0]=args[0]+"4mc7OaTslpqYvxg1htN0W007zomnuyHktGGXjjHIIx0ye3tXk9xbwwwaU0c6SXN2jvN/diAJAQfl";
		 args[0]=args[0]+"n8a7/wCL+jQRPZaus0iX12OLaVSQ4API9B0rzmeMxlZdxeUY3RqvDEqCR7dcfhX3mWxUaC5D+X+M";
		 args[0]=args[0]+"6+IrZ1V+sK3LZK2ui2PQfD8NktmY7ZkkYFEluEPJVgcpj26596xdSTUYLi2uo7iR0cmCEDrEgOMY";
		 args[0]=args[0]+"7/X3qjo12dIS2uhCib8osfmfLj+Ik+uMYrvtFspdburP7DbFru7lSzt7WNiWmlkYKiqB9QTXVUbp";
		 args[0]=args[0]+"u58vBxnHlPN9Qa4W0jgkZ4ZYpiqRnks24803TprNpg8gltbhrhVkKncpwDkkema/R7wf+wL8LR4d";
		 args[0]=args[0]+"t7vxj8RHm1a6uRpRXRbkRW9vfNkfZ0fd+8YN94ex96+Yf2of2T7r9nHxfpMU2pPrPhjVI2uNM1V4";
		 args[0]=args[0]+"VimLr96OZMn5uce/FefRzjC1pujDSRVTB1qKU57HgWuCS01W4aKbz/LmLq6LgDn73vivYPhvqd0s";
		 args[0]=args[0]+"873ty3l3MfF2MYGADn8f6V57remDV5VubRRNviEp2bVKjqylc9ea9r+B/wAHfEnxU1u203wtp8ur";
		 args[0]=args[0]+"XhgSa4iyqW9rCpJDzP0UZ3ADkscjFdeLq0/ZpzNcJd1bod4m06WLS5Uv45IrlUWX7dA+VePPDe3o";
		 args[0]=args[0]+"a8R8Uwx2cR84SRSBipJbO/PTsa+5Pix+y38Qvht4Ml1vWrGK/wBJtISJLjSZPOWKJ/vlgRuCcDJU";
		 args[0]=args[0]+"Z5AwMV8R+I1eC0jg3+YSx2FlGNmcgH8+vpjNedga0aqbjrY7cx5bqzPRfAHnN4bthHp1sqKMAi95";
		 args[0]=args[0]+"b3IA460Ve8B6fHbaIiJpNvYk4ZmF0G80kfexniivBxFT97LRH9N5DSrf2Xh9X8KOJ+IWiWVrFDq0";
		 args[0]=args[0]+"mo31xqbSbVjK7lRcElc9COOgrzu1kjR4o4J1VpDvkZhw2VB4/PFfRd1Yabf2aW0kZv7VozILe5UI";
		 args[0]=args[0]+"Y5MEfIR97rXhes+A9Y0qaN59IuUiBO0yJujZemR6YAr38pxalD2VR6o/FON8jqYTHfWMNC9OfSKb";
		 args[0]=args[0]+"s+t99fMytHtA1oJUJaODMrhuepI6ds4Fep/AzxXpvg/4m+Adb1NNljpOtQXV6WBfy4ywHmFe6qDu";
		 args[0]=args[0]+"I9FPrXMfDLT4F8SxW0gLWd1DLbxyyJt3TYBUEexNZN1btmEXUe27Vislwy/JkdEI9DjJPt716tVR";
		 args[0]=args[0]+"rp0+jPzqFNwiprpufYmv/sp/EzxN8b7TStPjufEvw51nxHNrun+I9HnMmk20E0hkafemRHKA20ZP";
		 args[0]=args[0]+"JXit7/gpX8ZtA1bUfC3w50G6j1Wbw7J9p1S9DiQI5CBYg3c/LzXyt4Nh8Zp4Tv00LxlqOi2NyxSW";
		 args[0]=args[0]+"xgvJEhn4GflBwuSa881jSLrQNUjj1XTpo5EmWSUtlg685bcck9R27V4VHCxrV4uVRNQ2to/me/jM";
		 args[0]=args[0]+"NjaGEVedOShUV0+h1nhiC3utSku7CBDHbzJOYZztLd2VR9a+3Phl8Tp/gt+xtoPirw6qzatr3jGN";
		 args[0]=args[0]+"NXnj2osgR1kFrJJ0jWRFVMnH3z618J+H9X0yOyurDSdMuLeW5ZVkvZ7nzGJHPChV2jqfx616z8GP";
		 args[0]=args[0]+"jFq/wzsdTs7fTrDxF4V10LFrHhjWot1neRqxAkCdUlU8hl5454ArfHUpVoRkujOPCWpvlWrdj9Cf";
		 args[0]=args[0]+"2I/GXiP4k+HviFr2ssr6Rqev3FzbWhkDpbhkxPAu7h4wFTkcYNfmB8SrS3h8e6zZaGhaxi1S6TT4";
		 args[0]=args[0]+"4IPNDQmU+WE98V9SeMP24vEb+CV+H3hLwbpPw+0q4hkh3aTnLQfL+5iPGxiCdzHNfLOl6ZD4g1CN";
		 args[0]=args[0]+"luQtrbbQXUsmACAY93QkkHLDHrXnYOk8M6mInpF7I9R4epja0cNSXvykl/mz0DTYZ5LcYSRXUKrq";
		 args[0]=args[0]+"6YYMAMg470VYS4igjDefw5ONpJ4B9T1+tFfM1Gqk3Puf1lg4Tw+Hp0YuyikvuPWfgLqPwyXxBc23";
		 args[0]=args[0]+"xO0lp9H1GCKCw1MzSRrprgYdWWM7vnP8WOK+jtY/Ye8E6tbpqvhvxPq2lw3EfmWl2kyXlpMv8OHJ";
		 args[0]=args[0]+"Jb0554r4is3FpJDD5quDGCsZGSxNemfB/wCP2pfAnU5Zre6a78Eqytq2jXEheERZXdLbk8pIuclD";
		 args[0]=args[0]+"1GKU4tyXLv5H5pn2W4yi54/A15RS1lFvTTt2O41j/gmbr+rTw6tpnj/ToNSQPLIqWDLlh9zGOATg";
		 args[0]=args[0]+"8+x9K+Sviv8ACPxL8LPEN7oviae2/tiK1jmtlaJmW73FVLq2MEfNjjuK/ZfTPEen63pVvqGlX41T";
		 args[0]=args[0]+"TdTtvttpe264ikhIyOn8Q4B+teA/ta/s+w/HPw/avp91FYeNNGif+yri4kUW10XG57SY9iei9wce";
		 args[0]=args[0]+"tehhc0qUqqhWn7uz7o/EMTT+tKc5fE9f6S3PzY8BavHfO2izWv2WeNvMSeEZ3MD8wA6np26V3vh/";
		 args[0]=args[0]+"wv4j8dJc2Vl4cGsTQHY8MMqSPJGTwyK7ZY9RhATxXk+vxar4J1e60+9tLjSL+xmlt7xCPmguU4dF";
		 args[0]=args[0]+"b0BH4g10mhfFG3ktLN9RuprCfPy3lruTeVxjLJh4jz1z+FevjsJKb9th1deX9dT9B4dz+m8LHL8T";
		 args[0]=args[0]+"UUJwuk56wlG+zXR9md/pPwd1e3mcw/D3XrbUPM/0hpNInSJYlVh8ztGoUD1P51Q0i3t/7XEUEEdo";
		 args[0]=args[0]+"EZkkmyoLDPOw9B15bpVqX486xqFt/Z978RdYvtMPyvEL+VkZMch/nBcdsk5pnguy1H4h2Ug0OHT9";
		 args[0]=args[0]+"P0WLzY7qXUV2s/B4hXAbZjkk5JPfivLhHET/AIqaXmfVyzHBYD35Om6i2jTSbf8AkjnzNe3mpXsG";
		 args[0]=args[0]+"j3IjW8Y/bI50UqqEhRsB5ZiVP3au6FokWgWU9pEJdizE/PJt83Poo4P86+uf2UPgT4H1/wCG+qx6";
		 args[0]=args[0]+"h4eh1TxOmpTabr1xqM7rmWMDy5LQqw2xlSRwecc5xXLfHP8AZTk+GmkL4q8OXNzq/hC2LG4ttRCJ";
		 args[0]=args[0]+"e6auO7oAjoOPcZOTmssRjV/u/wBlfifM8MYnAUMW6uLg1Vm20+ib6eR86wGdhJGmyMRuRhlHtRVh";
		 args[0]=args[0]+"XitbWJVTzVJLLIO4OCP0NFcXNE/eE5SV47EKQmeS3tZmEiSuVB24wpdQBx1AOD9QKxdUt5dW0bXt";
		 args[0]=args[0]+"buZlk03w7fvaW2jlP3U0gYr58rZyz5+bpjNFFenhPdqtrpY/MOOqk6eCowg7KTd7dbLqdX+yR+1T";
		 args[0]=args[0]+"4p+FGraH4XEaa34U1y+Nu+lXcjAWrMwDPC/JUncMjBHy9q/U660k2Wp6XEs7MtxEcEqAVCqxXOOC";
		 args[0]=args[0]+"cp1xzmiiubiGnCnUi4K13r5n41hHz4eE5b3aPyZu9YuJtU8RWspW4mPiDUXmuJ1DtKfOccjHHAFY";
		 args[0]=args[0]+"fiD4caZrsVzLb5027hLOssIyvPX5emT60UV9HGTp1IKOm35I58VCP1fbp+pY+B/wp0rxr4bkvNTx";
		 args[0]=args[0]+"LDYSFEt9n+sycfvDn5h3xgfWvoq4ks9B8ZTeEYbBFs9IsE8poW8tBlVbai9VX5jwWbOTz2BRXnZh";
		 args[0]=args[0]+"KU6lpO+h7GX0adKkpQjZuxueDvjNP8Ep/itLpmkW93L/AGxp2lQLLIVijcpMROyAfMR/dBXPXNeT";
		 args[0]=args[0]+"/EL4q+KvihqN5f8Ai7WbjWlt7giCwLeVaQhwGwsa+m7HJPSiiuGFOKj7S2tz6nhelCvmNNVVfV7n";
		 args[0]=args[0]+"ItB5FpBg7t5ZssOnQY/SiiivDc5Ntn7ZKcotxT0R/9k=";


		 //	System.out.println("DECODE:"+new String(decode(args[0])));
		 try {
			 java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File("TEST.JPG"));
			 out.write(decode(args[0]));
			 out.flush();
			 out.close();
		 }
		 catch(Exception e) {e.printStackTrace();}
    }
}









