
package com.cometway.text;


/**
 * A representation of a range of text in an ITextBuffer.
 */

public class TextRange
{
	private ITextBuffer     b;
	private TextPointer     start;
	private TextPointer     end;


	/**
	 * Creates an empty TextRange.
	 */

	public TextRange() {}


	/**
	 * Creates a TextRange with the given ITextBuffer.
	 */


	public TextRange(ITextBuffer b)
	{
		setTextBuffer(b);
	}


	/**
	 * Creates a TextRange with the given ITextBuffer and the start and end
	 * positions of the range.
	 */


	public TextRange(ITextBuffer b, int fromIndex, int toIndex)
	{
		setTextBuffer(b);
		start.setPosition(fromIndex);
		end.setPosition(toIndex);
	}


	/**
	 * Creates a TextRange with the given two TextPointers.
	 */


	public TextRange(TextPointer from, TextPointer to)
	{
		if (from.getTextBuffer() != to.getTextBuffer())
		{
			throw new IllegalArgumentException("TextPointers must reference the same ITextBuffer.");
		}

		this.b = from.getTextBuffer();
		start = from;
		end = to;
	}


	/**
	 * Deletes the range of text in the ITextBuffer associated with this TextRange.
	 */


	public void delete()
	{
		b.delete(start.getPosition(), end.getPosition());
	}


	/**
	 * Disposes this TextRange.
	 * This must be called in order for this object to get garbage collected.
	 */


	public void dispose()
	{
		b = null;
		start = null;
		end = null;
	}


	/**
	 * overrides Object.equals()
	 */


	public boolean equals(Object obj)
	{
		return (b.getText(start.getPosition(), end.getPosition()).equals(obj));
	}


	@Deprecated
	protected void finalize() throws Throwable
	{
		dispose();
	}


	/**
	 * @return Returns the position at the end of this TextRange.
	 */


	public int getEnd()
	{
		return (end.getPosition());
	}


	/**
	 * @return Returns the TextPointer pointing to the position at the end of this TextRange.
	 */


	public TextPointer getEndPointer()
	{
		return (end);
	}


	/**
	 * @return Returns the length of this TextRange.
	 */


	public int getLength()
	{
		int     length = end.getPosition() - start.getPosition();

		if (length < 0)
		{
			length = 0 - length;
		}

		return (length);
	}


	/**
	 * @return Returns the position at the beginning of this TextRange.
	 */


	public int getStart()
	{
		return (start.getPosition());
	}


	/**
	 * @return Returns the TextPointer pointing to the position at the beginning of this TextRange.
	 */


	public TextPointer getStartPointer()
	{
		return (start);
	}


	/**
	 * @return Returns the range of text in the ITextBuffer associated with this TextRange.
	 */


	public String getText()
	{
		return (b.getText(start.getPosition(), end.getPosition()));
	}


	/**
	 * @return Returns the ITextBuffer associated with this TextRange.
	 */


	public ITextBuffer getTextBuffer()
	{
		return (b);
	}


	/**
	 * Offsets the current range by offsetting the TextPointers pointing to the
	 * beginning and end of the range.
	 */


	public void offset(int relativeStart, int relativeEnd)
	{
		start.offset(relativeStart);
		end.offset(relativeEnd);
	}


	/**
	 * Sets the end position of this TextRange.
	 */


	public void setEnd(int end)
	{
		this.end.setPosition(end);
	}


	/**
	 * Sets the end position of this TextRange.
	 */


	public void setEndPointer(TextPointer p)
	{
		end = p;
	}


	/**
	 * Sets the length of this TextRange with respect to the beginning position
	 * of the range.
	 */


	public void setLength(int size)
	{
		end.setPosition(start.getPosition() + size);
	}


	/**
	 * Sets the start position of this TextRange.
	 */


	public void setStart(int start)
	{
		this.start.setPosition(start);
	}


	/**
	 * Sets the start position of this TextRange.
	 */


	public void setStartPointer(TextPointer p)
	{
		start = p;
	}


	/**
	 * Replaces the range of text in the ITextBuffer associated with this TextRange
	 * with the given String parameter.
	 */


	public void setText(String s)
	{
		b.delete(start.getPosition(), end.getPosition());
		b.insertText(s, start.getPosition());
	}


	/**
	 * Sets the ITextBuffer which is associated with this TextRange.
	 */


	public void setTextBuffer(ITextBuffer b)
	{
		dispose();

		this.b = b;
		this.start = new TextPointer(b);
		this.end = new TextPointer(b);
	}


	/**
	 * overrides Object.toString().
	 */


	public String toString()
	{
		return (b.getText(start.getPosition(), end.getPosition()));
	}


}

