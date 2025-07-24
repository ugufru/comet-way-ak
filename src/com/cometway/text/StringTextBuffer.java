
package com.cometway.text;

import java.util.Enumeration;
import java.util.Vector;


/**
 * An implementatin of the ITextBuffer interface using the String object
* to store the contents of the text buffer.
 */

public class StringTextBuffer implements ITextBuffer
{
	private String  s;
	private Vector  listeners = new Vector();


	/**
	 * Initialize an empty text buffer.
	 */

	public StringTextBuffer()
	{
		s = "";
	}


	/**
	 * Initialize the text buffer with the supplied String.
	* @param s reference to a String.
	 */


	public StringTextBuffer(String s)
	{
		this.s = s;
	}


	/**
	 * Adds an ITextBufferListener to this object to receive notifications of
	* changes which take place when the text buffer is changed.
	*
	* @param l a reference to an object which implements ITextBufferListener.
	 */


	public void addTextBufferListener(ITextBufferListener l)
	{
		if (listeners.contains(l) == false)
		{
			listeners.addElement(l);
		}
	}


	/**
	 * Appends the specified string to the end of this object and calls the
	* <CODE>textChanged</CODE> method of its listeners with the range
	* of the appended text.
	*
	* @param str a reference to a String.
	 */


	public void append(String str)
	{
		synchronized (s)
		{
			int     length = s.length();

			s = s.concat(str);

			textChanged(length, s.length());
		}
	}


	/**
	 * Removes the specified range of text from this object and calls the
	* <CODE>textChanged</CODE> method of its listeners with the location
	* of the removed text. The <CODE>fromIndex</CODE> and <CODE>toIndex</CODE>
	* parameters may be specified in any order.
	*
	* @param fromIndex the starting index from where text will be removed.
	* @param toIndex the ending index to where text will be removed.
	 */


	public void delete(int fromIndex, int toIndex)
	{
		synchronized (s)
		{
			int     start = (fromIndex < toIndex) ? fromIndex : toIndex;
			int     end = (fromIndex > toIndex) ? fromIndex : toIndex;

			s = s.substring(0, start) + s.substring(end, s.length());		// textMoved(start, end - start);

			textMoved(start, -1 * (end - start));
			textChanged(start, start);
		}
	}


	/**
	 * Returns the next text range in the text buffer found by
	* the specified text finder starting at the specified index.
	*
	* @param fromIndex the starting index from where search will begin.
	* @param textFinder a reference to an object implementing ITextFinder.
	* @return a reference to a TextRange if a matching range was found; null otherwise.
	 */


	public TextRange findText(int fromIndex, ITextFinder textFinder)
	{
		TextRange       range = null;

		if (textFinder != null)
		{
			TextFinderResult	result = textFinder.findText(s.toCharArray(), s.length(), fromIndex);

			if (result != null)
			{
				range = new TextRange(this, result.getStart(), result.getEnd());
			}
		}

		return (range);
	}


	/**
	 * Returns the size of the text buffer managed by this object.
	*
	* @return the length of the text buffer.
	 */


	public int getLength()
	{
		return (s.length());
	}


	/**
	 * Returns a string representing the contents of the text buffer.
	*
	* @return a reference to a String.
	 */


	public String getText()
	{
		return (s);
	}


	/**
	 * Returns a string representing the contents of the text buffer
	* contained within the specified range. The <CODE>fromIndex</CODE>
	* and <CODE>toIndex</CODE> parameters may be specified in any order.
	*
	* @param fromIndex the starting index from where text will be taken.
	* @param toIndex the ending index to where text will be taken.
	* @return a reference to a String.
	 */


	public String getText(int fromIndex, int toIndex)
	{
		int     start = (fromIndex < toIndex) ? fromIndex : toIndex;
		int     end = (fromIndex > toIndex) ? fromIndex : toIndex;

		return (s.substring(start, end));
	}


	/**
	 * Returns the index of the first occurence of the specified character starting
	* at the specified location and continuing to the end of the buffer.
	*
	* @param ch the character to match.
	* @param fromIndex the starting index from where the search will begin.
	* @return the index of the next matching character, or -1 if no match was found.
	 */


	public int indexOf(char ch, int fromIndex)
	{
		return (s.indexOf(ch, fromIndex));
	}


	/**
	 * Inserts a string into the text buffer at the specified location and calls the
	* <CODE>textChanged</CODE> method of its listeners with the location
	* of the inserted text.
	*
	* @param str a reference to a String.
	* @param position the index of the location where the text should be inserted.
	 */


	public void insertText(String str, int position)
	{
		synchronized (s)
		{
			int     length;

			s = s.substring(0, position) + str + s.substring(position, s.length());
			length = str.length();

			textMoved(position, length);
			textChanged(position, position + length);
		}
	}


	/**
	 * Returns the index of the previous occurence of the specified character
	* starting at the specified location and continuing to the end of the buffer.
	*
	* @param ch the character to match.
	* @param fromIndex the starting index from where the search will begin.
	* @return the index of the next matching character, or -1 if no match was found.
	 */


	public int prevIndexOf(char ch, int fromIndex)
	{
		int     result = -1;

		synchronized (s)
		{
			int     length = s.length();

			if (fromIndex > length)
			{
				fromIndex = length;
			}

			int     i = 0;

			while (true)
			{
				i = s.indexOf(ch, i);

				if ((i == -1) || (i >= fromIndex))
				{
					break;
				}

				result = i;
				i++;
			}
		}

		return (result);
	}


	/**
	 * Removes an ITextBufferListener from this object.
	*
	* @param l a reference to an object which implements ITextBufferListener.
	 */


	public void removeTextBufferListener(ITextBufferListener l)
	{
		if (listeners.contains(l))
		{
			listeners.removeElement(l);
		}
	}


	/**
	 * Sets the value of this text buffer to the specified String.
	*
	* @param str a reference to a String.
	 */


	public void setText(String str)
	{
		delete(0, s.length());
		append(str);
	}


	/**
	 * Used internally to notify listeners of changed text.
	 */


	private synchronized void textChanged(int start, int end)
	{
		ITextBufferListener     l;
		int			pos;
		Enumeration		e = listeners.elements();

		while (e.hasMoreElements())
		{
			l = (ITextBufferListener) e.nextElement();

			l.textChanged(this, start, end);
		}
	}


	/**
	 * Used internally to notify listeners of moved text.
	 */


	private synchronized void textMoved(int start, int offset)
	{
		ITextBufferListener     l;
		int			pos;
		Enumeration		e = listeners.elements();

		while (e.hasMoreElements())
		{
			l = (ITextBufferListener) e.nextElement();

			l.textMoved(this, start, offset);
		}
	}


}

