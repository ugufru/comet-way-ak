

package com.cometway.util;

import com.cometway.util.*;
import java.util.*;
import java.util.regex.*;


/**
 * This class provides static methods for quick and easy regular expression
 * matching and replacement. This class uses Java's built-in regex API.
 */
public class jGrep
{		// protected static Perl5Util perl;
	protected String	pattern;
	protected String	content;
	protected boolean       ignoreCase;
	protected Vector	matches;

	public jGrep(String pattern, String text, boolean ignoreCase)
	{
		this.pattern = pattern;
		this.content = text;
		this.ignoreCase = ignoreCase;
		matches = jGrep.grepText(pattern, text, ignoreCase);
	}


	public void searchAgain(int start, int end)
	{
		matches = jGrep.grepText(pattern, content.substring(start, end), ignoreCase);

		if (start > 0)
		{
			Vector  temp = new Vector();

			for (int x = 0; x < matches.size(); x++)
			{
				IntegerPair     pair = (IntegerPair) matches.elementAt(x);

				temp.addElement(new IntegerPair(pair.firstInt() + start, pair.secondInt() + start));
			}

			matches = temp;
		}
	}


	public void searchAgain(int start, int end, String content)
	{
		matches = jGrep.grepText(pattern, content.substring(start, end), ignoreCase);

		if (start > 0)
		{
			Vector  temp = new Vector();

			for (int x = 0; x < matches.size(); x++)
			{
				IntegerPair     pair = (IntegerPair) matches.elementAt(x);

				temp.addElement(new IntegerPair(pair.firstInt() + start, pair.secondInt() + start));
			}

			matches = temp;
		}
	}


	public void searchAgain(String text)
	{
		matches = jGrep.grepText(pattern, text, ignoreCase);
	}


	public int getNumberOfMatches()
	{
		return (matches.size());
	}


	public Vector getIndexOfMatches()
	{
		return (matches);
	}


	public static IntegerPair indecesOf(String pattern, String content, boolean ignoreCase, Pattern compiledPattern)
	{
		if (compiledPattern == null)
		{
			int flags = Pattern.MULTILINE;
			if (ignoreCase)
			{
				flags |= Pattern.CASE_INSENSITIVE;
			}
			compiledPattern = Pattern.compile(pattern, flags);
		}

		Matcher matcher = compiledPattern.matcher(content);
		if (matcher.find())
		{
			return (new IntegerPair(matcher.start(), matcher.end()));
		}

		return (null);
	}


	public static IntegerPair indecesOf(String pattern, String content, boolean ignoreCase)
	{
		int flags = Pattern.MULTILINE;
		if (ignoreCase)
		{
			flags |= Pattern.CASE_INSENSITIVE;
		}
		Pattern compiledPattern = Pattern.compile(pattern, flags);
		return (indecesOf(pattern, content, ignoreCase, compiledPattern));
	}


	public static Vector grepText(String pattern, String content, boolean ignoreCase, Pattern compiledPattern)
	{
		Vector		matches = new Vector();
		int		offset = 0;
		IntegerPair     pair = jGrep.indecesOf(pattern, content, ignoreCase, compiledPattern);

		while (pair != null)
		{
			content = content.substring(pair.secondInt());

			matches.addElement(new IntegerPair(pair.firstInt() + offset, pair.secondInt() + offset));

			offset = offset + pair.secondInt();
			pair = jGrep.indecesOf(pattern, content, ignoreCase, compiledPattern);
		}

		return (matches);
	}


	public static Vector grepText(String pattern, String content, boolean ignoreCase)
	{
		int flags = Pattern.MULTILINE;
		if (ignoreCase)
		{
			flags |= Pattern.CASE_INSENSITIVE;
		}
		Pattern compiledPattern = Pattern.compile(pattern, flags);
		return (grepText(pattern, content, ignoreCase, compiledPattern));
	}


	public static String grepAndReplaceText(String pattern, String replacementString, String content, boolean ignoreCase, Pattern compiledPattern)
	{
		if (compiledPattern == null)
		{
			int flags = Pattern.MULTILINE;
			if (ignoreCase)
			{
				flags |= Pattern.CASE_INSENSITIVE;
			}
			compiledPattern = Pattern.compile(pattern, flags);
		}

		Matcher matcher = compiledPattern.matcher(content);
		return matcher.replaceAll(replacementString);
	}


	public static String grepAndReplaceText(String pattern, String replacementString, String content, boolean ignoreCase)
	{
		int flags = Pattern.MULTILINE;
		if (ignoreCase)
		{
			flags |= Pattern.CASE_INSENSITIVE;
		}
		Pattern compiledPattern = Pattern.compile(pattern, flags);
		return (grepAndReplaceText(pattern, replacementString, content, ignoreCase, compiledPattern));
	}


	public static int[] searchText(String pattern, String content)
	{
		Vector  i = new Vector();		// So VERY large strings don't cause OutOfMemory Errors.

		System.gc();

		long    fm = Runtime.getRuntime().freeMemory();

		if ((long) pattern.length() > fm)
		{
			char[]  temp = new char[(int) fm];

			pattern.getChars(0, (int) fm, temp, 0);

			pattern = new String(temp);
		}

		String  lpattern = pattern.toLowerCase();
		String  lcontent = content.toLowerCase();
		int     index = 0;
		int     count = 0;

		index = lcontent.indexOf(lpattern);

		while (index > -1)
		{
			count++;

			Integer temp = Integer.valueOf(index);

			i.addElement(temp);

			index = lcontent.indexOf(lpattern, index + 1);
		}

		int[]   rval = new int[count];

		for (int x = 0; x < rval.length; x++)
		{
			rval[x] = ((Integer) i.elementAt(x)).intValue();
		}

		return (rval);
	}


	public static void main(String[] args)
	{
		String			content = "";		// System.out.println(jGrep.indecesOf(args[0],content,false));
		java.io.BufferedReader  reader = null;

		try
		{
			for (int x = 2; x < args.length; x++)
			{
				reader = new java.io.BufferedReader(new java.io.FileReader(new java.io.File(args[x])));

				StringBuffer    sb = new StringBuffer();
				String		line = reader.readLine();

				while (line != null)
				{
					sb.append(line);
					sb.append("\n");

					line = reader.readLine();
				}

				content = sb.toString(); /* int[] newlines = jGrep.searchText("\n",content);
				int linenum = 0;

				System.out.println(args[x]+":::::::::");
				Vector matches = jGrep.grepText(args[0],content,false);
				int begin = 0;
				int end = 0;
				for(int z =0; z<matches.size() ; z++) {
					IntegerPair p = (IntegerPair)matches.elementAt(z);

					if(linenum+1<newlines.length) {
						while(newlines[linenum+1]<p.firstInt()) {
							linenum++;
							if(linenum+1>=newlines.length) {
								break;
							}
						}
					}
					begin = linenum;

					end=begin;
					while(newlines[end]<p.secondInt()) {
						end++;
						if(end>=newlines.length) {
							break;
						}
					}

					System.out.println("match #"+(z+1)+"  line "+begin+"  at ("+(p.firstInt()-newlines[begin])+","+(p.secondInt()-newlines[begin])+")");
					System.out.println(content.substring(newlines[begin]+1,newlines[end]));
				} */

				System.out.println(grepAndReplaceText(args[0], args[1], content, false));
			}
		}
		catch (Exception e)
		{
			System.out.println(e);
			e.printStackTrace();
		}
	}


}

