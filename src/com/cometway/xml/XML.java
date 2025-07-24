
package com.cometway.xml;


/**
* This is a final class that contains constants used by the XML package.
*/

public final class XML
{
	/** The standard <TT>&lt;?xml version="1.0"?&gt;</TT> file
        * header for XML 1.0 standard documents.
	*/
	public final static String XML_10_HEADER = "<?xml version=\"1.0\"?>";

	/** An int value of 0 representing an empty element tag. */
	public final static int EMPTY_ELEMENT_TAG = 0;

	/** An int value of 1 representing a start element tag. */
	public final static int START_TAG = 1;

	/** An int value of 2 representing element contents. */
	public final static int ELEMENT_CONTENT = 2;

	/** An int value of 3 representing a end element tag. */
	public final static int END_TAG = 3;

	/** No public constructor for this class. It's purely static. */

	private XML()
	{
	}


	/**
	* Encodes the specified string suitably for element content.
	*/

	public final static String encode(String s)
	{
		StringBuffer b = new StringBuffer();
		int length = s.length();

		for (int i = 0; i < length; i++)
		{
			char c = s.charAt(i);

			if (c < 32)
			{
				b.append("&#");
				b.append(Integer.toString((int) c));
				b.append(";");
			}
			else if(c>126)
			{
				b.append("&#");
				b.append(Integer.toString((int) c));
				b.append(";");
			}
			else if (c == '<')
			{
				b.append("&#60;");
			}
			else if (c == '>')
			{
				b.append("&#62;");
			}
			else if (c == '&')
			{
				b.append("&#38;");
			}
			else
			{
				b.append(c);
			}
		}

		return (b.toString());
	}



	/**
	 * This method decodes all the XML 1.0 escape codes in the String parameter.
	 */
	public static String decode(String in)
	{
		StringBuffer rval = new StringBuffer();
		for(int x=0;x<in.length();x++) {
			if(in.charAt(x)=='&') {
				int index = x;
				try {
					while(in.charAt(++index)!=';') {;}
				}
				catch(IndexOutOfBoundsException ioob) {
					// this isn't an escape code
					rval.append("&");
				}
				String tmp = in.substring(x+1,index);
				int notEscape = x;
				x = index;
				if(tmp.equals("quot")) {
					rval.append("\"");
				}
				else if(tmp.equals("apos")) {
					rval.append("'");
				}
				else if(tmp.equals("gt")) {
					rval.append(">");
				}
				else if(tmp.equals("lt")) {
					rval.append("<");
				}
				else if(tmp.equals("amp")) {
					rval.append("&");
				}
				else if(tmp.startsWith("#")) {
					if(tmp.charAt(1)=='x') {
						int power = 0;
						int accum = 0;
						for(int z=tmp.length()-1;z>1;z--) {
							char hex = tmp.charAt(z);
							if(hex == '1') {
								accum = accum + (int)(pow(16,power));
							}
							else if(hex == '2') {
								accum = accum + (int)(2 * pow(16,power));
							}
							else if(hex == '3') {
								accum = accum + (int)(3 * pow(16,power));
							}
							else if(hex == '4') {
								accum = accum + (int)(4 * pow(16,power));
							}
							else if(hex == '5') {
								accum = accum + (int)(5 * pow(16,power));
							}
							else if(hex == '6') {
								accum = accum + (int)(6 * pow(16,power));
							}
							else if(hex == '7') {
								accum = accum + (int)(7 * pow(16,power));
							}
							else if(hex == '8') {
								accum = accum + (int)(8 * pow(16,power));
							}
							else if(hex == '9') {
								accum = accum + (int)(9 * pow(16,power));
							}
							else if(hex == 'A' || hex == 'a') {
								accum = accum + (int)(10 * pow(16,power));
							}
							else if(hex == 'B' || hex == 'b') {
								accum = accum + (int)(11 * pow(16,power));
							}
							else if(hex == 'C' || hex == 'c') {
								accum = accum + (int)(12 * pow(16,power));
							}
							else if(hex == 'D' || hex == 'd') {
								accum = accum + (int)(13 * pow(16,power));
							}
							else if(hex == 'E' || hex == 'e') {
								accum = accum + (int)(14 * pow(16,power));
							}
							else if(hex == 'F' || hex == 'f') {
								accum = accum + (int)(15 * pow(16,power));
							}
							power++;
						}
						rval.append((char)accum);
					}
					else {
						try {
							int c = Integer.parseInt(tmp.substring(1).trim());
							rval.append((char)c);
						}
						catch(NumberFormatException nfe) {
							// OK... this isn't an escape code
							rval.append("&");
							x = notEscape;
						}
					}
				}
				else {
					rval.append("&"+tmp+";");
				}
			}
			else {
				rval.append(in.charAt(x));
			}
		}
		return(rval.toString());
	}


	private static int pow(int a, int b)
	{
		int tmp = 1;
		for(int x=0;x<b;x++) {
			tmp = tmp*a;
		}
		return(tmp);
	}


}


