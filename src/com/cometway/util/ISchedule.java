
package com.cometway.util;

import java.util.*;


/**
 * A class can implement ISchedule when it wants to represent
* a list of scheduled times. Keep in mind that implementations
* of this interface have no concept of the current date, only
* the dates which are given.
 */

public interface ISchedule
{


/**
 * Returns the maximum number of milliseconds each event in this schedule can
	* be delayed before timing out.
	*
	* @return the maximum number of milliseconds the schedule can be delayed.
 */

	public long getMaximumLatency();


	/**
	 * Returns the next Date after <TT>startDate</TT> when a scheduled event should occur.
	* If the schedule has expired (has no future events) this method should return null.
	*
	* @param startDate a Date reference.
	* @return a reference to the first Date after <TT>startDate</TT>; null if no future dates are scheduled.
	 */

	public Date getNextDate(Date startDate);


	/**
	 * Returns a String representation of the schedule. This String should be in a format
	* which the setSchedule method can process.
	*
	* @return a String reference.
	 */

	public String getScheduleString();


	/**
	 * Returns true if the schedule has been properly initialized.
	* A schedule is only invalid when its internal data has not been initialized
	* or has somehow become incomprehensible to the point where getNextDate fails.
	*
	* @return true if the schedule is valid; false otherwise.
	 */

	public boolean isValid();


	/**
	 * Sets the schedule based on a syntax-dependent String description.
	* If the description is invalid, this method returns false and subsequent
	* calls to isValid will return false as well. A true result indicates
	* the schedule description was valid, and subsequent calls to isValid will
	* return true.
	*
	* @param s a String reference
	* @return true if the schedule is valid; false otherwise.
	 */

	public boolean setSchedule(String s);
}

;
