
package com.cometway.text;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * A text finder which searches for the first range of text matching
* a given Regular Expression (Java regex)
 */

public class RegExpTextFinder implements ITextFinder
{
	private Pattern pattern;


	/**
	 * Initializes a RegExpTextFinder to search for the specified regular expression. 
	* @param pattern a String containing the search expression.
	 */

	public RegExpTextFinder(String pattern)
	{
		this.pattern = Pattern.compile(pattern, Pattern.MULTILINE);
	}


	/**
	 * Initializes a RegExpTextFinder to search for the specified regular expression. 
	* @param pattern a String containing the regular expression.
	* @param ignoreCase case-insensitive searches when true; exact searches otherwise.
	 */


	public RegExpTextFinder(String pattern, boolean ignoreCase)
	{
		int flags = Pattern.MULTILINE;
		if (ignoreCase)
		{
			flags |= Pattern.CASE_INSENSITIVE;
		}
		this.pattern = Pattern.compile(pattern, flags);
	}


	/**
	 * Returns the first matching text range beginning at the specified location
	* using the specified regular expression.
	*
	* @param buffer a character array where the text to be searched is located.
	* @param bufferLength the number of valid characters in the character array.
	* @param fromIndex the index from where the search should begin.
	* @return an TextFinderResult representing the matching range of text.
	 */


	public TextFinderResult findText(char[] buffer, int bufferLength, int fromIndex)
	{
		String content = String.valueOf(buffer, fromIndex, bufferLength - fromIndex);
		Matcher matcher = pattern.matcher(content);

		if (matcher.find())
		{
			return (new TextFinderResult(matcher.start() + fromIndex, matcher.end() + fromIndex));
		}

		return (null);
	}


}

