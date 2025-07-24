
package com.cometway.util;


import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Set of utilities for manipulating date info.
 */

public class DateTools
{
	public final static int kDaysPerWeek = 7;
	public final static int kMonthsPerYear = 12;

	protected final static String daysTable[] = 
	{
		"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	};

	protected final static String   monthsTable[] = 
	{
		"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
	};


	public final static SimpleDateFormat ISO8601_DATETIMEFORMAT = new SimpleDateFormat("yyyyMMdd HHmmss");
	public final static SimpleDateFormat ISO8601_DATEFORMAT = new SimpleDateFormat("yyyyMMdd");
	public final static SimpleDateFormat ISO8601_TIMEFORMAT = new SimpleDateFormat("HHmmss");


	public static String toISO8601String(Date d) 
	{
		// It's annoying here that the 'T' is a reserved character in SimpleDateFormat.

		return (ISO8601_DATEFORMAT.format(d) + "T" + ISO8601_TIMEFORMAT.format(d));
	}


	public static Date parseISO8601String(String dateStr) throws ParseException
	{
		Date d = null;

		if (dateStr.indexOf('T') >= 0)
		{
			dateStr = dateStr.replace('T', ' ');

			d = ISO8601_DATETIMEFORMAT.parse(dateStr);
		}
		else
		{
			d = ISO8601_DATEFORMAT.parse(dateStr);
		}

		return (d);
	}



	/**
	 * Returns a date string in this formate: MMM D, YYYY  or  MMM DD, YYYY 
	 * where M=month, D=date, and Y = year.
	 * @param date This is the date object to convert to string.
	 * @return A string in the above format.
	 */

	public static String getAbbrDate(Date date)
	{
		Calendar cal = Calendar.getInstance();

		cal.setTime(date);

		return (DateTools.getMonth(cal.get(cal.MONTH), 3) + " " + String.valueOf(cal.get(cal.DATE)) + ", " + String.valueOf(cal.get(cal.YEAR)));
	}


	public static String getAbbrDate(Date date, TimeZone tz)
	{
		Calendar cal = Calendar.getInstance(tz);

		cal.setTime(date);

		return (DateTools.getMonth(cal.get(cal.MONTH), 3) + " " + String.valueOf(cal.get(cal.DATE)) + ", " + String.valueOf(cal.get(cal.YEAR)));
	}


	/**
	 * Retuns a string of the Day, no abbreviation, given the day int.
	 */


	public static String getDay(int theDay)
	{
		return (daysTable[theDay]);
	}


	/**
	 * Returns a string of the Day, abbreivate to maxLength characters.
	 */


	public static String getDay(int theDay, int maxLength)
	{
		return (new String(daysTable[theDay].substring(0, maxLength)));
	}


	/**
	 * Returns a date string like getAbbrDate except the month is not abbreviated.
	 */


	public static String getLongDate(Date date)
	{
		String returnDate = new String();
		Calendar cal = Calendar.getInstance();

		cal.setTime(date);

		returnDate += DateTools.getMonth(cal.get(cal.MONTH)) + " ";
		returnDate += String.valueOf(cal.get(cal.DATE)) + ", ";
		returnDate += String.valueOf(cal.get(cal.YEAR));

		return (returnDate);
	}


	public static String getLongDate(Date date, TimeZone tz)
	{
		String returnDate = new String();
		Calendar cal = Calendar.getInstance(tz);

		cal.setTime(date);

		returnDate += DateTools.getMonth(cal.get(cal.MONTH)) + " ";
		returnDate += String.valueOf(cal.get(cal.DATE)) + ", ";
		returnDate += String.valueOf(cal.get(cal.YEAR));

		return (returnDate);
	}


	/**
	 * get the String name of the month from the int value of the month (0-11)
	 */


	public static String getMonth(int theMonth)
	{
		return (monthsTable[theMonth]);
	}


	/**
	 * get the String name of the month and truncate it to maxLength if needed
	 */


	public static String getMonth(int theMonth, int maxLength)
	{
		return (monthsTable[theMonth].substring(0, maxLength));
	}


	/**
	 * returns a date string in the format: MM/DD/YY, where MM is the month int+1, DD is the date, and YY is the year.
	 */


	public static String getShortDate(Date date)
	{
		Calendar cal = Calendar.getInstance();

		cal.setTime(date);

		return (String.valueOf(cal.get(cal.MONTH) + 1) + "/" + String.valueOf(cal.get(cal.DATE)) + "/" + String.valueOf(cal.get(cal.YEAR) % 100));
	}


	public static String getShortDate(Date date, TimeZone tz)
	{
		Calendar cal = Calendar.getInstance(tz);

		cal.setTime(date);

		return (String.valueOf(cal.get(cal.MONTH) + 1) + "/" + String.valueOf(cal.get(cal.DATE)) + "/" + String.valueOf(cal.get(cal.YEAR) % 100));
	}


	/**
	 * returns an abbreviated date string with the time
	 */


	public static String getShortDateTime(Date date, boolean wantYear, boolean wantSeconds)
	{
		StringBuffer b = new StringBuffer();
		Calendar cal = Calendar.getInstance();

		cal.setTime(date);
		b.append((cal.get(cal.MONTH) + 1) + "/" + cal.get(cal.DATE));

		if (wantYear)
		{
			b.append("/" + (cal.get(cal.YEAR) % 100));
		}

		b.append("  ");
		b.append(DateTools.getTime(date, wantSeconds));

		return (b.toString());
	}


	public static String getShortDateTime(Date date, boolean wantYear, boolean wantSeconds, TimeZone tz)
	{
		StringBuffer b = new StringBuffer();
		Calendar cal = Calendar.getInstance(tz);

		cal.setTime(date);
		b.append((cal.get(cal.MONTH) + 1) + "/" + cal.get(cal.DATE));

		if (wantYear)
		{
			b.append("/" + (cal.get(cal.YEAR) % 100));
		}

		b.append("  ");
		b.append(DateTools.getTime(date, wantSeconds, tz));

		return (b.toString());
	}


	/**
	 * returns a time string
	 */


	public static String getTime(Date date, boolean wantSeconds)
	{
		StringBuffer b = new StringBuffer();
		Calendar cal = Calendar.getInstance();

		cal.setTime(date);

		int h = cal.get(cal.HOUR_OF_DAY);
		int m = cal.get(cal.MINUTE);
		int s = cal.get(cal.SECOND);

		if (h == 0)
		{
			b.append(12);
		}
		else
		{
			if (h > 12)
			{
				b.append(h - 12);
			}
			else
			{
				b.append(h);
			}
		}

		if (m < 10)
		{
			b.append(":0" + m);
		}
		else
		{
			b.append(":" + m);
		}

		if (wantSeconds)
		{
			if (s < 10)
			{
				b.append(":0" + s);
			}
			else
			{
				b.append(":" + s);
			}
		}

		if (h < 12)
		{
			b.append(" AM");
		}
		else
		{
			b.append(" PM");
		}

		b.append(" " + TimeZone.getDefault().getID());

		return (b.toString());
	}


	public static String getTime(Date date, boolean wantSeconds, TimeZone tz)
	{
		StringBuffer b = new StringBuffer();
		Calendar cal = Calendar.getInstance(tz);

		cal.setTime(date);

		int h = cal.get(cal.HOUR_OF_DAY);
		int m = cal.get(cal.MINUTE);
		int s = cal.get(cal.SECOND);

		if (h == 0)
		{
			b.append(12);
		}
		else
		{
			if (h > 12)
			{
				b.append(h - 12);
			}
			else
			{
				b.append(h);
			}
		}

		if (m < 10)
		{
			b.append(":0" + m);
		}
		else
		{
			b.append(":" + m);
		}

		if (wantSeconds)
		{
			if (s < 10)
			{
				b.append(":0" + s);
			}
			else
			{
				b.append(":" + s);
			}
		}

		if (h < 12)
		{
			b.append(" AM");
		}
		else
		{
			b.append(" PM");
		}

		b.append(" " + tz.getID());

		return (b.toString());
	}


	public static Date today()
	{
		return (new Date());
	}


	/**
	 * Returns a date string specified by RFC 822:
   * \"Standard for the format of ARPA Internet Messages\".
   * @param inDate This is the Date to turn into a string, null will give current date
   * @return Returns a string in the form of: day, date month year time zone-offset <=> 3*ALPHA ", " 2*DIGIT " " 3*ALPHA " " 4*DIGIT " " 2*DIGIT ":" 2*DIGIT ":" 2*DIGIT " " ("+"/"-")4*DIGIT
	 */


	public static String toRFC822String(Date inDate)
	{
		if (inDate == null)
		{
			inDate = new Date();
		}

		StringBuffer rval = new StringBuffer();
		int temp;
		Calendar cal = Calendar.getInstance();

		cal.setTime(inDate);		// Find day of week

		temp = cal.get(cal.DAY_OF_WEEK) - 1;

		if (temp == 0)
		{
			rval.append("Sun, ");
		}
		else if (temp == 1)
		{
			rval.append("Mon, ");
		}
		else if (temp == 2)
		{
			rval.append("Tue, ");
		}
		else if (temp == 3)
		{
			rval.append("Wed, ");
		}
		else if (temp == 4)
		{
			rval.append("Thu, ");
		}
		else if (temp == 5)
		{
			rval.append("Fri, ");
		}
		else if (temp == 6)
		{
			rval.append("Sat, ");
		}		// Find date

		temp = cal.get(cal.DATE);

		if (temp > 9)
		{
			rval.append(temp + " ");
		}
		else
		{
			rval.append("0" + temp + " ");
		}		// Find month of year

		temp = cal.get(cal.MONTH);

		if (temp == 0)
		{
			rval.append("Jan ");
		}
		else if (temp == 1)
		{
			rval.append("Feb ");
		}
		else if (temp == 2)
		{
			rval.append("Mar ");
		}
		else if (temp == 3)
		{
			rval.append("Apr ");
		}
		else if (temp == 4)
		{
			rval.append("May ");
		}
		else if (temp == 5)
		{
			rval.append("Jun ");
		}
		else if (temp == 6)
		{
			rval.append("Jul ");
		}
		else if (temp == 7)
		{
			rval.append("Aug ");
		}
		else if (temp == 8)
		{
			rval.append("Sep ");
		}
		else if (temp == 9)
		{
			rval.append("Oct ");
		}
		else if (temp == 10)
		{
			rval.append("Nov ");
		}
		else if (temp == 11)
		{
			rval.append("Dec ");
		}		// Find year

		temp = cal.get(cal.YEAR);

		rval.append(temp + " ");	// Find hour

		temp = cal.get(cal.HOUR_OF_DAY);

		if (temp < 10)
		{
			rval.append("0" + temp + ":");
		}
		else
		{
			rval.append(temp + ":");
		}		// Find minutes

		temp = cal.get(cal.MINUTE);

		if (temp < 10)
		{
			rval.append("0" + temp + ":");
		}
		else
		{
			rval.append(temp + ":");
		}		// Find seconds

		temp = cal.get(cal.SECOND);

		if (temp < 10)
		{
			rval.append("0" + temp + " ");
		}
		else
		{
			rval.append(temp + " ");
		}		// Find timezone offset

		temp = cal.get(cal.ZONE_OFFSET) / (1000 * 60);

		if (temp == 0)
		{
			rval.append("+00");
		}
		else if ((temp / 60) > 9)
		{
			rval.append("+" + (temp / 60) + "");
		}
		else if ((temp / 60) < -9)
		{
			rval.append((temp / 60) + "");
		}
		else if ((temp / 60) > 0)
		{
			rval.append("+0" + Math.abs((int) (temp / 60)));
		}
		else
		{
			rval.append("-0" + Math.abs((int) (temp / 60)));
		}

		if (temp % 60 == 0)
		{
			rval.append("00");
		}
		else
		{
			rval.append(Math.abs((int) temp % 60));
		}

		return (rval.toString());
	}


	public static String toRFC822String(Date inDate, TimeZone tz)
	{
		if (inDate == null)
		{
			inDate = new Date();
		}

		StringBuffer rval = new StringBuffer();
		int temp;
		Calendar cal = Calendar.getInstance(tz);

		cal.setTime(inDate);		// Find day of week

		temp = cal.get(cal.DAY_OF_WEEK) - 1;

		if (temp == 0)
		{
			rval.append("Sun, ");
		}
		else if (temp == 1)
		{
			rval.append("Mon, ");
		}
		else if (temp == 2)
		{
			rval.append("Tue, ");
		}
		else if (temp == 3)
		{
			rval.append("Wed, ");
		}
		else if (temp == 4)
		{
			rval.append("Thu, ");
		}
		else if (temp == 5)
		{
			rval.append("Fri, ");
		}
		else if (temp == 6)
		{
			rval.append("Sat, ");
		}		// Find date

		temp = cal.get(cal.DATE);

		if (temp > 9)
		{
			rval.append(temp + " ");
		}
		else
		{
			rval.append("0" + temp + " ");
		}		// Find month of year

		temp = cal.get(cal.MONTH);

		if (temp == 0)
		{
			rval.append("Jan ");
		}
		else if (temp == 1)
		{
			rval.append("Feb ");
		}
		else if (temp == 2)
		{
			rval.append("Mar ");
		}
		else if (temp == 3)
		{
			rval.append("Apr ");
		}
		else if (temp == 4)
		{
			rval.append("May ");
		}
		else if (temp == 5)
		{
			rval.append("Jun ");
		}
		else if (temp == 6)
		{
			rval.append("Jul ");
		}
		else if (temp == 7)
		{
			rval.append("Aug ");
		}
		else if (temp == 8)
		{
			rval.append("Sep ");
		}
		else if (temp == 9)
		{
			rval.append("Oct ");
		}
		else if (temp == 10)
		{
			rval.append("Nov ");
		}
		else if (temp == 11)
		{
			rval.append("Dec ");
		}		// Find year

		temp = cal.get(cal.YEAR);

		rval.append(temp + " ");	// Find hour

		temp = cal.get(cal.HOUR_OF_DAY);

		if (temp < 10)
		{
			rval.append("0" + temp + ":");
		}
		else
		{
			rval.append(temp + ":");
		}		// Find minutes

		temp = cal.get(cal.MINUTE);

		if (temp < 10)
		{
			rval.append("0" + temp + ":");
		}
		else
		{
			rval.append(temp + ":");
		}		// Find seconds

		temp = cal.get(cal.SECOND);

		if (temp < 10)
		{
			rval.append("0" + temp + " ");
		}
		else
		{
			rval.append(temp + " ");
		}		// Find timezone offset

		temp = cal.get(cal.ZONE_OFFSET) / (1000 * 60);

		if (temp == 0)
		{
			rval.append("+00");
		}
		else if ((temp / 60) > 9)
		{
			rval.append("+" + (temp / 60) + "");
		}
		else if ((temp / 60) < -9)
		{
			rval.append((temp / 60) + "");
		}
		else if ((temp / 60) > 0)
		{
			rval.append("+0" + Math.abs((int) (temp / 60)));
		}
		else
		{
			rval.append("-0" + Math.abs((int) (temp / 60)));
		}

		if (temp % 60 == 0)
		{
			rval.append("00");
		}
		else
		{
			rval.append(Math.abs((int) temp % 60));
		}

		return (rval.toString());
	}
}


