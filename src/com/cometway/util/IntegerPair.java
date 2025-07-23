
package com.cometway.util;

import java.lang.*;
import java.util.*;


/**
 * This is a Pair object that specializes in Integers (simple type int).
 * @see Pair
 */

public class IntegerPair extends Pair
{
	public IntegerPair() {}


	/**
	 * Sets the Pair.first() and Pair.second() as an Integer object from firstInt and secondInt respectively
	 */


	public IntegerPair(int firstInt, int secondInt)
	{
		setFirst(firstInt);
		setSecond(secondInt);
	}


	/**
	 * Return the first int
	 */


	public int firstInt()
	{
		return (((Integer) first()).intValue());
	}


	/**
	 * Return the second int
	 */


	public int secondInt()
	{
		return (((Integer) second()).intValue());
	}


	/**
	 * sets the first int
	 */


	public void setFirst(int i)
	{
		setFirst(Integer.valueOf(i));
	}


	/**
	 * sets the second int
	 */


	public void setSecond(int i)
	{
		setSecond(Integer.valueOf(i));
	}


}

