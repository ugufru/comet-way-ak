
package com.cometway.util;

import java.util.*; 


/* ->(ND!NAD)[AND: ->(at ???!at4/15/97)[] 
   ->(ND!NAD)[][OR: ->(every ???!NAD)[AND: ->(???!NAD)[]] 
   ->(on ???!NAD)[]] 
   ->(ND!NAD)[][OR: ->(between ???!NAD)[AND: ->(-???!NAD)[AND: ->(every ???!NAD)[AND: ->(???!NAD)[]]]] 
   ->(at ???!NAD)[]] 
   ->(until ???!NAD)[]] */


/**
 * This class implements ISchedule. There is a set of fields which are public that represent
 * what this schedule is. These can be set by individually or you can use the parser to parse
 * A schedule string (see bottom for syntax). Fields which are not used must remain null.
 * Be very careful when setting the fields individually, the getNextDate() method is
 * very picky about how the fields are set. The getScheduleString() method returns a
 * string which is guaranteed to be parsable by the Schedule parser. You can pass a
 * TimeZone object into the constructor and this Schedule will take place in that time zone.
 * <PRE>
 * DateTime Schedule Notation.
 * 
 * date = mm/dd/yyyy
 * time = hh:mm:ss
 *
 * [starting startDateTime]
 * [every n[y|m|w|d]] | [on [[SU][MO][TU][WE][TH][FI][SA]] | [startDay-endDay]]]
 * [between startTime-endTime every n[h|m|s]] |
 * [at time1, time2, time3, ...]
 * [until endDateTime]
 *
 * [starting startDateTime]
 * [[every n[y|m|w|d]] | [on [[SU][MO][TU][WE][TH][FR][SA] | [startDay-endDay]]]]
 * [[between startTime-endTime every n[h|m|s]] | [at time1,time2,time3]]
 * [until endDateTime]
 * </PRE>
 */

public class Schedule implements ISchedule
{
	private static boolean  debug = false;
	private static boolean  verbose = false;
	private static boolean  errors = true;


	/**
	 * startDate: The Year,Month,Week,and Date of the starting Date. Time values are not valid
	 */

	public Date		startDate;


	/**
	 * everyDate: This is the number of iterations to make of everyDateType
	 */

	public int		everyDate;


	/**
	 * everyDateType: This is either y,m,w,d (kYears, kMonths, kWeeks, kDays)
	 */

	public int		everyDateType;


	/**
	 * dayMap[]: This is the days of the week, true=Schedule on this day OK, false=No Schedules on this day
	 */

	public boolean		dayMap[] = new boolean[7];


	/**
	 * startTime: This is the Hour,Minutes,Seconds of the starting time, date values are not valid
	 */

	public Date		startTime;


	/**
	 * endTime: This is the ending time, hours,minutes,seconds. Date values are not valid
	 */

	public Date		endTime;


	/**
	 * everyTime: This is like everyDate, except iterations on time, not dates
	 */

	public int		everyTime;


	/**
	 * everyTimeType: This is either h,m,s (kHours,kMinutes,kSeconds)
	 */

	public int		everyTimeType;


	/**
	 * atTimes: This is a vector of Date objects which hold hours,minutes,and seconds of times to schedule. Date values are not valid.
	 */

	public Vector		atTimes;


	/**
	 * endDate: This is when to end this schedule, time values are not valid.
	 */

	public Date		endDate;


	/**
	 * maximumLatency: This is the maximum number of milliseconds this schedule can be delayed.
	 */

	public int		maximumLatency = 0;


	/**
	 * These are static constants
	 */

	public final static int EVERY_MINUTE = 0;
	public final static int EVERY_TEN_MINUTES = 1;
	public final static int EVERY_HALF_HOUR = 2;
	public final static int EVERY_HOUR = 3;
	public final static int EVERY_3_HOURS = 4;
	public final static int EVERY_12_HOURS = 5;
	public final static int EVERY_DAY = 6;
	public final static int EVERY_WEEK = 7;
	public final static int CUSTOM = 8;
	public static final int kYears = 1;
	public static final int kMonths = 2;
	public static final int kWeeks = 3;
	public static final int kDays = 4;
	public static final int kHours = 5;
	public static final int kMinutes = 6;
	public static final int kSeconds = 7;


	/**
	 * This is the timezone this schedule takes place in.
	 */

	private TimeZone	timezone;
	public boolean		isValid;


	/**
	 * You get a Schedule class that is not valid, with TimeZone set to default.
	 */

	public Schedule()
	{
		isValid = false;
		timezone = TimeZone.getDefault();
	}


	/**
	 * This does the parse for you and sets timezone to host's default.
	 */

		// Theoretically, after you instantiate this, all the data should be ready in public fields


	public Schedule(String input)
	{
		timezone = TimeZone.getDefault();
		isValid = setSchedule(input);
	}


	/**
	 * This does the parse for you and sets the timezone to whatever is passed in.
	 */


	public Schedule(String input, TimeZone tz)
	{
		if (tz == null)
		{
			tz = TimeZone.getDefault();
		}

		timezone = tz;
		isValid = setSchedule(input);
	}


	/**
	 * This returns the validity of the current schedule
   *
   * @return Returns the validity of the schedule
	 */


	public boolean isValid()
	{
		return (isValid);
	}


	/**
	 * This resets the schedule, removing the current settings, then
	* parses the Schedule String and sets the state variables.
   * @param input This string is parsed as the schedule description
   * @return This says whether it worked or not
	 */


	public synchronized boolean setSchedule(String input)
	{
		try
		{
			startDate = null;
			everyDate = 0;
			everyDateType = 0;
			dayMap = new boolean[7];
			for(int x=0;x<7;x++) {
				dayMap[x] = true;
			}
			startTime = null;
			endTime = null;
			everyTime = 0;
			everyTimeType = 0;
			atTimes = null;
			endDate = null;

			parse(prepInput(input));
		}
		catch (Exception exn)
		{
			error("setSchedule", exn);		// There may be some issues with this

			isValid = false;

			return (false);
		}

		isValid = true;

		return (true);
	}


	/**
	 * This sets the timezone for this schedule. In order for the fields to be valid, the
	 * data must be reparsed. The isValid flag is set to false when this method is called.
	 */


	public void setTimeZone(TimeZone tz)
	{
		timezone = tz;
		isValid = false;
	}


	private Date toDate(int y, int m, int d, int h, int mn, int s)
	{
		Calendar	cal = Calendar.getInstance(timezone);

		cal.set(cal.YEAR,y);
		cal.set(cal.MONTH,m);
		cal.set(cal.DATE,d);
		cal.set(cal.HOUR_OF_DAY,h);
		cal.set(cal.MINUTE,mn);
		cal.set(cal.SECOND,s);
		//		cal.set(y, m, d, h, mn, s);

		return (cal.getTime());
	}


	private Date toDate(Calendar c)
	{
		return (toDate(c.get(c.YEAR), c.get(c.MONTH), c.get(c.DATE), c.get(c.HOUR_OF_DAY), c.get(c.MINUTE), c.get(c.SECOND)));
	}


	/**
	 * Returns:
	 *    -1 if d1 is after d2,
	 *     0 if d1 is equal to d2
	 *     1 if d1 is before d2
	 */


	private int compare(Date d1, Date d2)
	{
		Calendar	c1 = Calendar.getInstance(timezone);
		Calendar	c2 = Calendar.getInstance(timezone);

		c1.setTime(d1);
		c2.setTime(d2);		// print("c1: "+c1.get(c1.YEAR) +", c2: "+c2.get(c2.YEAR));

		if (c1.get(c1.YEAR) > c2.get(c2.YEAR))
		{
			return (-1);
		}
		else if (c1.get(c1.YEAR) < c2.get(c2.YEAR))
		{
			return (1);
		}		// print("c1: "+c1.get(c1.MONTH) +", c2: "+c2.get(c2.MONTH));

		if (c1.get(c1.MONTH) > c2.get(c2.MONTH))
		{
			return (-1);
		}
		else if (c1.get(c1.MONTH) < c2.get(c2.MONTH))
		{
			return (1);
		}		// print("c1: "+c1.get(c1.DATE) +", c2: "+c2.get(c2.DATE));

		if (c1.get(c1.DATE) > c2.get(c2.DATE))
		{
			return (-1);
		}
		else if (c1.get(c1.DATE) < c2.get(c2.DATE))
		{
			return (1);
		}		// print("c1: "+c1.get(c1.HOUR_OF_DAY) +", c2: "+c2.get(c2.HOUR_OF_DAY));

		if (c1.get(c1.HOUR_OF_DAY) > c2.get(c2.HOUR_OF_DAY))
		{
			return (-1);
		}
		else if (c1.get(c1.HOUR_OF_DAY) < c2.get(c2.HOUR_OF_DAY))
		{
			return (1);
		}		// print("c1: "+c1.get(c1.MINUTE) +", c2: "+c2.get(c2.MINUTE));

		if (c1.get(c1.MINUTE) > c2.get(c2.MINUTE))
		{
			return (-1);
		}
		else if (c1.get(c1.MINUTE) < c2.get(c2.MINUTE))
		{
			return (1);
		}		// print("c1: "+c1.get(c1.SECOND) +", c2: "+c2.get(c2.SECOND));

		if (c1.get(c1.SECOND) > c2.get(c2.SECOND))
		{
			return (-1);
		}
		else if (c1.get(c1.SECOND) < c2.get(c2.SECOND))
		{
			return (1);
		}		// They are the same as far as we care...

		return (0);
	}


	/**
	 * This returns the next date something is supposed to happen according to the schedule
	 * @param startingAt This is the reference date and time which to base results
	 * @return This returns the next Date which something is supposed to happen
	 */


	public Date getNextDate(Date startingAt)
	{
		return (getNextDate(startingAt, 0));
	}


	/**
	 * This returns the next date something is supposed to happen according to the schedule
	 * @param startingAt This is the reference date and time which to base results
	 * @param recurse A panic detector. 
	 * @return This returns the next Date which something is supposed to happen
	 */


	public Date getNextDate(Date startingAt, int recurse)
	{
		Date    rval = null;
		if (!isValid)
		{
			error("i am not valid but someone is trying to get my next date");

			return null;
		}

		recurse++;

		if (recurse > 10)
		{
			error("A valid next date could not be determined.");
			System.out.println("startDate=" + startDate);
			System.out.println("everyDate=" + everyDate);
			System.out.println("everyDateType=" + everyDateType);
			System.out.println("dayMap=SU:" + dayMap[0] + ",MO:" + dayMap[1] + ",TU:" + dayMap[2] + ",WE:" + dayMap[3] + ",TH:" + dayMap[4] + ",FR:" + dayMap[5] + ",SA:" + dayMap[6]);
			System.out.println("startTime=" + startTime);
			System.out.println("endTime=" + endTime);
			System.out.println("everyTime=" + everyTime);
			System.out.println("everyTimeType=" + everyTimeType);
			System.out.println("atTimes=" + atTimes);
			System.out.println("endDate=" + endDate);
			System.out.println("schedule=" + getScheduleString());

			return (null);
		}

		Calendar	cal = Calendar.getInstance(timezone);
		Calendar	cal2 = Calendar.getInstance(timezone);
		Calendar	startingAtCal = Calendar.getInstance(timezone);

		startingAtCal.setTime(startingAt);		// startingAt = toDate(cal.get(cal.YEAR),cal.get(cal.MONTH),cal.get(cal.DATE),cal.get(cal.HOUR_OF_DAY),cal.get(cal.MINUTE),cal.get(cal.SECOND)+1);


		// First check whether startDate is used

		if (startDate != null)
		{		// OK startDate has a value, is it before or equal to the reference date?
			cal.setTime(startDate);
			if ((startingAtCal.before(cal)) || (startingAtCal.equals(cal)))
			{		// if(compare(startingAt,startDate) != ) {


				// This date must also be on the dayMap


				if (dayMap[cal.get(cal.DAY_OF_WEEK) - 1])
				{
					rval = toDate(cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DATE), 0, 0, 0);

					Calendar tmpCal = Calendar.getInstance(timezone);
					Calendar tmpCal2 = Calendar.getInstance(timezone);
					tmpCal.setTime(rval);
					if(endDate!=null) {
						tmpCal2.setTime(endDate);
					}
					if ((endDate != null) && (!tmpCal2.after(tmpCal)))
					{
						print("WARNING: null because endDate>rval");
						print("enddate: " + endDate);
						print("rval: " + rval);

						return (null);
					}
					else
					{
						cal.setTime(startDate);

						rval = toDate(cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DATE), 0, 0, 0);
					}
				}
				else
				{
					cal.setTime(startDate);

					rval = toDate(cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DATE), 0, 0, 0);
				}
			}
			else
			{
				cal.setTime(startDate);

				rval = toDate(cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DATE), 0, 0, 0);
			}
		}		// Second check whether everyDate is used

		if (everyDate != 0)
		{
			Date    temp = null;		// if startDate is not null, then use startDate as a base case


			// if(startDate!=null) {

			cal.setTime(startingAt);

			Date    temp2 = toDate(cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));
			Date    temp3 = toDate(cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DATE), 0, 0, 0);

			if (startDate == null)
			{
				cal.setTime(new Date());

				cal.set(cal.MONTH,0);
				cal.set(cal.DATE,1);
				cal.set(cal.HOUR_OF_DAY,0);
				cal.set(cal.MINUTE,0);
				cal.set(cal.SECOND,0);
				//				cal.set(cal.get(cal.YEAR), 0, 1, 0, 0, 0);
			}
			else
			{
				cal.setTime(startDate);
			}

			temp = toDate(cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));

			cal.setTime(temp);

			long    iter = 0;

			if (everyDateType == kDays)
			{
				iter = 86400;
			}
			else if (everyDateType == kWeeks)
			{
				iter = 604800;
			}
			else if (everyDateType == kMonths)
			{
				iter = 2592000;
			}
			else if (everyDateType == kYears)
			{
				iter = 31570560;
			}

			//			iter = iter;
			iter = iter * everyDate;

			Calendar tmpCal = Calendar.getInstance(timezone);
			Calendar tmpCal2 = Calendar.getInstance(timezone);
			tmpCal.setTime(temp);
			tmpCal2.setTime(temp3);
			while ((tmpCal.before(tmpCal2)) || (!dayMap[cal.get(cal.DAY_OF_WEEK) - 1]))
			{
				long    addthis = 0;

				if (startDate == null)
				{
					Calendar	tcal = Calendar.getInstance(timezone);

					tcal.setTime(new Date());
					tcal.set(tcal.MONTH,0);
					tcal.set(tcal.DATE,1);
					tcal.set(tcal.HOUR_OF_DAY,0);
					tcal.set(tcal.MINUTE,0);
					tcal.set(tcal.SECOND,0);
					//					tcal.set(tcal.get(tcal.YEAR), 0, 1, 0, 0, 0);

					//					addthis = ((iter*1000 - (temp3.getTime() - (tcal.getTimeInMillis())) % iter)/1000);
					addthis = (iter - (((temp3.getTime() -tcal.getTime().getTime()) / 1000)) % iter);
				}
				else
				{
					//					addthis = ((iter*1000 - (temp3.getTime() - (startDate.getTime())) % iter*1000)/1000);
					addthis = (iter - (((temp3.getTime() / 1000) - (startDate.getTime() / 1000)) % iter));
				}

				int     y, m, d, h, n, s;

				if (addthis == iter)
				{
					s = 0;
					n = 0;
					h = 0;
					d = 0;
					m = 0;
					y = 0;
				}
				else
				{
					s = (int) addthis % 60;
					addthis = addthis / 60;
					n = (int) addthis & 60;
					addthis = addthis / 60;
					h = (int) addthis % 24;
					addthis = addthis / 24;
					d = (int) addthis % 30;
					addthis = addthis / 30;
					m = (int) addthis % 12;
					addthis = addthis / 12;
					y = (int) addthis;
				}

				cal.setTime(temp3);

				temp = toDate(cal.get(cal.YEAR) + y, cal.get(cal.MONTH) + m, cal.get(cal.DATE) + d, 0, 0, 0);		// cal.get(cal.HOUR_OF_DAY)+h,cal.get(cal.MINUTE)+n,cal.get(cal.SECOND)+s);


				// temp = toDate(cal.get(cal.YEAR),cal.get(cal.MONTH),cal.get(cal.DATE),0,0,(int)addthis);//cal.get(cal.HOUR_OF_DAY)+h,cal.get(cal.MINUTE)+n,cal.get(cal.SECOND)+s);

				cal.setTime(temp);

				if(endDate!=null) {
					tmpCal.setTime(endDate);
				}
				else {
					tmpCal.roll(Calendar.YEAR,1000);
				}
				tmpCal2.setTime(temp);
				if ((endDate != null) && (tmpCal.before(tmpCal2)))
				{
					print("WARNING: null because endDate<temp");
					print("enddate: " + endDate);
					print("temp: " + temp);

					temp = null;

					return (null);
				}
			}

			rval = temp;
		}
		else
		{
			Calendar tmpCal = Calendar.getInstance(timezone);
			if(rval!=null) {
				tmpCal.setTime(rval);
			}
			
			if ((rval == null) || (startingAtCal.after(tmpCal)))
			{
				cal.setTime(startingAt);

				rval = toDate(cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), 0);		// rval = toDate(cal.get(cal.YEAR),cal.get(cal.MONTH),cal.get(cal.DATE),cal.get(cal.HOUR_OF_DAY),cal.get(cal.MINUTE),cal.get(cal.SECOND));
			}
		}

		cal.setTime(rval);

		while (!dayMap[cal.get(cal.DAY_OF_WEEK) - 1])
		{
			cal.setTime(rval);

			rval = toDate(cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DATE) + 1, cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));

			cal.setTime(rval);
		}

		if ((startTime == null) && (everyTime == 0) && (atTimes == null))
		{
			cal.setTime(startingAt);
			cal2.setTime(rval);

			if ((cal.get(cal.YEAR) == cal2.get(cal.YEAR)) && (cal.get(cal.MONTH) == cal2.get(cal.MONTH)) && (cal.get(cal.DATE) == cal2.get(cal.DATE)))
			{
				rval = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));
			}
			else
			{
				rval = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE), 0, 0, 0);
			}
		}
		else
		{
			Date    time = getNextTime(startingAt, rval);

			if (time != null)
			{
				cal2.setTime(rval);
				cal.setTime(time);

				rval = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));		// if(rval.before(startingAt)) {

				if (compare(rval, startingAt) != -1)
				{
					print("WARNING: null because rval <= startingAt2");
					print("rval: " + rval);
					print("startingAt: " + startingAt);

					rval = null;
				}
			}
			else
			{
				cal2.setTime(rval);

				rval = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE) + 1, 0, 0, 0);

				while (time == null)
				{
					rval = getNextDate(rval, recurse);

					if (rval == null)
					{
						print("WARNING: null because rval == null");

						return (null);
					}
					else
					{
						time = getNextTime(startingAt, rval);
					}
				}

				cal2.setTime(rval);
				cal.setTime(time);

				rval = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));		// if(rval.before(startingAt)) {

				if (compare(rval, startingAt) != -1)
				{
					print("WARNING: null because rval <= startingAt3");
					print("rval: " + rval);
					print("startingAt: " + startingAt);

					rval = null;
				}
			}
		}

		if (rval != null)
		{
			Calendar tmpCal = Calendar.getInstance(timezone);
			Calendar tmpCal2 = Calendar.getInstance(timezone);
			if(endDate!=null) {
				tmpCal.setTime(endDate);
			}
			tmpCal2.setTime(rval);
			if ((endDate != null) && (tmpCal.before(tmpCal2)))
			{
				print("WARNING: null because endDate < rval");
				print("enddate: " + endDate);
				print("rval: " + rval);

				rval = null;
			}
		}

		if (rval == null)
		{
			print("WARNING: rval is just plain null. leak!");
		}

		return (rval);
	}


	/**
	 * This returns a date with useless information about dates, but valid info on time
	 * @param startingAt This is the reference to try to exceed
	 * @param dateReference This is the date reference to use for comparison
	 * @return returns a date with only valid info on time
	 */


	public Date getNextTime(Date startingAt, Date dateReference)
	{
		Date		rval = null;
		Date		temp = dateReference;
		Calendar	cal = Calendar.getInstance(timezone);
		Calendar	cal2 = Calendar.getInstance(timezone);

		if (!isValid)
		{
			return (null);
		}		// First check if startTime is used

		if (startTime != null)
		{
			cal2.setTime(temp);
			cal.setTime(startTime);

			temp = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));		// if((temp.after(startingAt))||(temp.equals(startingAt))) {


			// if(temp.after(startingAt)) {

			if (compare(temp, startingAt) == -1)
			{
				rval = temp;
			}
			else
			{		// while(!temp.after(startingAt)) {
				while (compare(temp, startingAt) != -1)
				{
					long    iter = 0;

					if (everyTimeType == kSeconds)
					{
						iter = 1000;
					}
					else if (everyTimeType == kMinutes)
					{
						iter = 60000;
					}
					else if (everyTimeType == kHours)
					{
						iter = 3600000;
					}

					cal2.setTime(temp);
					cal.setTime(endTime);

					Date    end = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));

					cal.setTime(startTime);

					Date    start = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));

					iter = iter * everyTime;
					iter = startingAt.getTime() + (iter - (startingAt.getTime() % 1000));
					temp = new Date(iter);		// if(end.before(temp)) {

					cal.setTime(end);
					cal2.setTime(temp);
					if (!cal.after(cal2))
					{
						temp = null;

						break;
					}
				}

				rval = temp;
			}
		}
		else
		{			// if not try atTimes
			if ((atTimes != null) && (atTimes.size() > 0))
			{
				int     x = 1;

				cal2.setTime(temp);
				cal.setTime((Date) atTimes.elementAt(0));

				temp = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));		// while((x<atTimes.size())&&(temp.before(startingAt))) {

				while ((x < atTimes.size()) && (compare(temp, startingAt) != -1))
				{
					cal2.setTime(temp);
					cal.setTime((Date) atTimes.elementAt(x));

					temp = toDate(cal2.get(cal.YEAR), cal2.get(cal.MONTH), cal2.get(cal.DATE), cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), cal.get(cal.SECOND));
					x++;
				}		// if((temp.after(startingAt))||(temp.equals(startingAt))) {


				// if(temp.after(startingAt)) {

				if (compare(temp, startingAt) == -1)
				{
					rval = temp;
				}
			}
		}

		return (rval);
	}


	/**
	 * This returns an Enlgish description of the Schedule
   * @return This is a string of the description in english
	 */


	public String toString()
	{		// Paul I don't know how english you want this to be, so I'll leave it blank for now..
		return (getScheduleString());
	}


	/**
	 * This is the same as toString() except this is parsable and can be reused by another schedule
   * @return This is a string of the schedule description parsable by another schedule
	 */


	public String getScheduleString()
	{
		StringBuffer    rval = new StringBuffer();
		Calendar	cal = Calendar.getInstance(timezone);

		if (!isValid)
		{
			return ("");
		}

		if (startDate != null)
		{
			cal.setTime(startDate);
			rval.append("starting " + (cal.get(cal.MONTH) + 1) + "/" + cal.get(cal.DATE) + "/" + cal.get(cal.YEAR) % 100 + " ");
		}

		if (everyDate != 0)
		{
			rval.append("every " + everyDate);

			if (everyDateType == kDays)
			{
				rval.append("d ");
			}
			else if (everyDateType == kWeeks)
			{
				rval.append("w ");
			}
			else if (everyDateType == kMonths)
			{
				rval.append("m ");
			}
			else if (everyDateType == kYears)
			{
				rval.append("y ");
			}
		}
		else if ((dayMap[0]) || (dayMap[1]) || (dayMap[2]) || (dayMap[3]) || (dayMap[4]) || (dayMap[5]) || (dayMap[6]))
		{
			rval.append("on ");

			if (dayMap[0])
			{
				rval.append("SU");
			}

			if (dayMap[1])
			{
				rval.append("MO");
			}

			if (dayMap[2])
			{
				rval.append("TU");
			}

			if (dayMap[3])
			{
				rval.append("WE");
			}

			if (dayMap[4])
			{
				rval.append("TH");
			}

			if (dayMap[5])
			{
				rval.append("FR");
			}

			if (dayMap[6])
			{
				rval.append("SA");
			}

			rval.append(" ");
		}

		if (startTime != null)
		{
			cal.setTime(startTime);
			rval.append("between " + (cal.get(cal.HOUR_OF_DAY)) + ":" + cal.get(cal.MINUTE) + ":" + cal.get(cal.SECOND) + "-");
			cal.setTime(endTime);
			rval.append((cal.get(cal.HOUR_OF_DAY)) + ":" + cal.get(cal.MINUTE) + ":" + cal.get(cal.SECOND) + " ");
			rval.append("every " + everyTime);

			if (everyTimeType == kSeconds)
			{
				rval.append("s ");
			}
			else if (everyTimeType == kMinutes)
			{
				rval.append("m ");
			}
			else if (everyTimeType == kHours)
			{
				rval.append("h ");
			}
		}
		else if (atTimes != null)
		{
			Date    temp = (Date) atTimes.elementAt(0);

			cal.setTime(temp);
			rval.append("at " + (cal.get(cal.HOUR_OF_DAY)) + ":" + cal.get(cal.MINUTE) + ":" + cal.get(cal.SECOND) + " ");

			for (int x = 1; x < atTimes.size(); x++)
			{
				temp = (Date) atTimes.elementAt(x);

				cal.setTime(temp);
				rval.append(", " + (cal.get(cal.HOUR_OF_DAY)) + ":" + cal.get(cal.MINUTE) + ":" + cal.get(cal.SECOND) + " ");
			}

			rval.append(" ");
		}

		if (endDate != null)
		{
			cal.setTime(endDate);
			rval.append("until " + (cal.get(cal.MONTH) + 1) + "/" + cal.get(cal.DATE) + "/" + cal.get(cal.YEAR) % 100);
		}		// rval.append("\n");


		/* if(rval.toString().equals("on SUMOTUWETHFRSA ")) {
		  return(null);
		  } */

		return (rval.toString());
	} /* picture of tree:  (&-> is an AND pointer, +-> is an OR pointer), <> is empty node
     <head>&-> <pStartDate>
           &-> <>+-> <pEveryDate>&-><pEveryDateType>
                 +-> <pDayMap>
	   &-> <>+-> <pStartTime>&-><pEndTime>&-><pEveryTime>&-><pEveryTimeType>
	         +-> <pAtTimes>
	   &-> <pEndDate> */


	/**
	 * This is a precaution taken due to the lack of descrete inputs. The parser will assume certain
   * characteristics about the input String, this method makes sure that those assumptions are true.
   * It removes whitespace except for a single space between each word, the parser will tokenize using
   * the single space. It also changes everything to lowercase.
   * @param input this is the string to prepare
   * @return This is the prepared string
	 */


	private String prepInput(String input)
	{
		String  temp = input.toLowerCase();
		String  prepped = "";
		int     index = temp.indexOf('\n');
		int     index2 = 0;

		while (index != -1)
		{
			prepped = prepped + temp.substring(index2, index);

			if (!((temp.charAt(index - 1) == ' ') || (temp.charAt(index + 1) == ' ')))
			{
				prepped = prepped + " ";
			}

			index2 = index + 1;
			index = temp.indexOf('\n', index2);
		}

		index = temp.indexOf('\t');
		index2 = 0;

		while (index != -1)
		{
			prepped = prepped + temp.substring(index2, index);

			if (!((temp.charAt(index - 1) == ' ') || (temp.charAt(index + 1) == ' ')))
			{
				prepped = prepped + " ";
			}

			index2 = index + 1;
			index = temp.indexOf('\t', index2);
		}

		index = temp.indexOf("  ");
		index2 = 0;

		while (index != -1)
		{
			prepped = prepped + temp.substring(index2, index);
			index2 = index + 1;
			index = temp.indexOf('\n', index2);
		}

		prepped = prepped + temp.substring(index2);

		return (prepped);
	}


	/**
	 * This is the method that parses a string and builds a parse tree based on the syntax
   * of the schedule description. This passes the nodes to a method which updates the class fields
	 */

		// This gets ugly


	private synchronized void parse(String input)
	{
		pNode   head;
		pNode   pStartDate;
		pNode   pEveryDate;
		pNode   pEveryDateType;
		pNode   pDayMap;
		pNode   pStartTime;
		pNode   pEndTime;
		pNode   pEveryTime;
		pNode   pEveryTimeType;
		pNode   pAtTimes;
		pNode   pEndDate;		// make new nodes for all the fields

		head = new pNode();
		pStartDate = new pNode("starting ???");
		pEveryDate = new pNode("every ???");
		pEveryDateType = new pNode("???");
		pDayMap = new pNode("on ???");
		pStartTime = new pNode("between ???");
		pEndTime = new pNode("-???");
		pEveryTime = new pNode("every ???");
		pEveryTimeType = new pNode("???");
		pAtTimes = new pNode("at ???");
		pEndDate = new pNode("until ???");		// make an array of parse nodes for faster insertion

		pNode[] thead = new pNode[4];

		thead[0] = pStartDate;

		pNode[] ptemp = new pNode[2];

		ptemp[0] = pEveryDate;
		ptemp[1] = pDayMap;

		pEveryDate.addAndNode(pEveryDateType);

		thead[1] = new pNode();

		thead[1].addOrNodes(ptemp);

		ptemp[0] = pStartTime;
		ptemp[1] = pAtTimes;

		pStartTime.addAndNode(pEndTime);
		pEndTime.addAndNode(pEveryTime);
		pEveryTime.addAndNode(pEveryTimeType);

		thead[2] = new pNode();

		thead[2].addOrNodes(ptemp);

		thead[3] = pEndDate;

		head.addAndNodes(thead);	// point the pNode pointer to the head

		pNode		pointer = head;		// Tokenize the "words" from the input

		StringTokenizer st = new StringTokenizer(input, " ");		// Get the first token and start from the head
		String		token = st.nextToken();
		String		temp;		// This is here until I find a better way of doing this

		if (!(token.startsWith("starting") || token.startsWith("every") || token.startsWith("on") || token.startsWith("between") || token.startsWith("at") || token.startsWith("until")))
		{
			throw (new IllegalArgumentException("No Schedule to parse"));
		}		// The "word" to match will be the first Node of a depth-first-traversal

		temp = (String) (pointer.and[0].data);

		if (token.startsWith((temp.substring(0, temp.indexOf("???"))).trim()))
		{		// The first word is "starting", collect the rest of the tokens and store into pNode
			String  get = token;

			if (st.hasMoreTokens())
			{
				token = st.nextToken();
			}
			else
			{
				throw (new IllegalArgumentException("Expecting startDateTime"));
			}

			String  temp1 = (String) pointer.and[1].or[0].data;

			temp1 = temp1.substring(0, temp1.indexOf("???")).trim();

			String  temp2 = (String) pointer.and[1].or[1].data;

			temp2 = temp2.substring(0, temp2.indexOf("???")).trim();

			String  temp3 = (String) pointer.and[2].or[0].data;

			temp3 = temp3.substring(0, temp3.indexOf("???")).trim();

			String  temp4 = (String) pointer.and[2].or[1].data;

			temp4 = temp4.substring(0, temp4.indexOf("???")).trim();

			String  temp5 = (String) pointer.and[3].data;

			temp5 = temp5.substring(0, temp5.indexOf("???")).trim();

			while ((!(token.startsWith(temp1))) && (!(token.startsWith(temp2))) && (!(token.startsWith(temp3))) && (!(token.startsWith(temp4))) && (!(token.startsWith(temp5))))
			{
				get = get + token;

				if (st.hasMoreTokens())
				{
					token = st.nextToken();
				}
				else
				{		// No more tokens to read;
					break;
				}
			}

			pointer.and[0].auxData = get;

			if (!st.hasMoreTokens())
			{
				token = "STOPSTOP";
			}
		}		// The "word" to match will be the second Node of a DF traversal

		temp = (String) pointer.and[1].or[0].data;

		if (token.startsWith((temp.substring(0, temp.indexOf("???"))).trim()))
		{			// The word is "every"
			String  get = token;		// The next word has gotta be a number followed by a letter

			if (st.hasMoreTokens())
			{
				token = st.nextToken();
			}
			else
			{
				throw (new IllegalArgumentException("Expecting n[y|m|w|d]"));
			}

			int     i = token.indexOf('y');

			if (i == -1)
			{
				i = token.indexOf('m');
			}

			if (i == -1)
			{
				i = token.indexOf('w');
			}

			if (i == -1)
			{
				i = token.indexOf('d');
			}

			if (i == -1)
			{
				get = get + token;

				if (st.hasMoreTokens())
				{
					token = st.nextToken();
				}
				else
				{
					throw (new IllegalArgumentException("Expecting n[y|m|w|d]"));
				}

				i = token.indexOf('y');

				if (i == -1)
				{
					i = token.indexOf('m');
				}

				if (i == -1)
				{
					i = token.indexOf('w');
				}

				if (i == -1)
				{
					i = token.indexOf('d');
				}

				if (i == -1)
				{
					throw (new IllegalArgumentException("Expecting [y|m|w|d], inside word: " + token));
				}
			}		// Store the keyword and token

			pointer.and[1].or[0].auxData = get + token.substring(0, i);		// Store the Type
			pointer.and[1].or[0].and[0].auxData = token.substring(i, i + 1);	// Get the next word;

			if (st.hasMoreTokens())
			{
				token = st.nextToken();
			}
			else
			{		// No more tokens to read
				token = "STOPSTOP";
			}
		}
		else
		{
			temp = (String) pointer.and[1].or[1].data;

			if (token.startsWith((temp.substring(0, temp.indexOf("???"))).trim()))
			{
				String  get = token;

				if (st.hasMoreTokens())
				{
					token = st.nextToken();
				}
				else
				{
					throw (new IllegalArgumentException("Expecting Day of Week"));
				}

				String  temp1 = (String) pointer.and[2].or[0].data;

				temp1 = temp1.substring(0, temp1.indexOf("???")).trim();

				String  temp2 = (String) pointer.and[2].or[1].data;

				temp2 = temp2.substring(0, temp2.indexOf("???")).trim();

				String  temp3 = (String) pointer.and[3].data;

				temp3 = temp3.substring(0, temp3.indexOf("???")).trim();

				while ((!(token.startsWith(temp1))) && (!(token.startsWith(temp2))) && (!(token.startsWith(temp3))))
				{
					get = get + token;

					if (st.hasMoreTokens())
					{
						token = st.nextToken();
					}
					else
					{		// No more tokens to read;
						break;
					}
				}

				pointer.and[1].or[1].auxData = get;

				if (!st.hasMoreTokens())
				{
					token = "STOPSTOP";
				}
			}
		}

		temp = (String) pointer.and[2].or[0].data;

		if (token.startsWith((temp.substring(0, temp.indexOf("???"))).trim()))
		{
			String  get = token;

			if (st.hasMoreTokens())
			{
				token = st.nextToken();
			}
			else
			{
				throw (new IllegalArgumentException("Expecting startTime-"));
			}

			while (token.indexOf('-') == -1)
			{
				get = get + token;

				if (st.hasMoreTokens())
				{
					token = st.nextToken();
				}
				else
				{
					throw (new IllegalArgumentException("Expecting startTime-"));
				}
			}

			get = get + token.substring(0, token.indexOf('-'));
			pointer.and[2].or[0].auxData = get;
			get = token.substring(token.indexOf('-'));
			token = "";

			while (!(token.startsWith("every")))
			{
				get = get + token;

				if (st.hasMoreTokens())
				{
					token = st.nextToken();
				}
				else
				{
					throw (new IllegalArgumentException("Expecting every n[h|m|s]"));
				}
			}

			pointer.and[2].or[0].and[0].auxData = get;
			get = token;

			if (st.hasMoreTokens())
			{
				token = st.nextToken();
			}
			else
			{
				throw (new IllegalArgumentException("Expecting n[h|m|s]"));
			}

			int     i = token.indexOf('h');

			if (i == -1)
			{
				i = token.indexOf('m');
			}

			if (i == -1)
			{
				i = token.indexOf('s');
			}

			if (i == -1)
			{
				get = get + token;

				if (st.hasMoreTokens())
				{
					token = st.nextToken();
				}
				else
				{
					throw (new IllegalArgumentException("Expecting n[h|m|s]"));
				}

				i = token.indexOf('h');

				if (i == -1)
				{
					i = token.indexOf('m');
				}

				if (i == -1)
				{
					i = token.indexOf('s');
				}

				if (i == -1)
				{
					throw (new IllegalArgumentException("Expecting [h|m|s], inside word: " + token));
				}
			}

			get = get + token.substring(0, i);
			pointer.and[2].or[0].and[0].and[0].auxData = get.trim();
			get = token.substring(i).trim();
			pointer.and[2].or[0].and[0].and[0].and[0].auxData = get;

			if (st.hasMoreTokens())
			{
				token = st.nextToken();
			}
			else
			{
				token = "STOPSTOP";
			}
		}
		else
		{
			temp = (String) pointer.and[2].or[1].data;

			if (token.startsWith(temp.substring(0, temp.indexOf("???")).trim()))
			{
				String  get = token;

				if (st.hasMoreTokens())
				{
					token = st.nextToken();
				}
				else
				{
					throw (new IllegalArgumentException("Expecting time1,time2,..."));
				}

				while (!(token.startsWith("until")))
				{
					get = get + token;

					if (st.hasMoreTokens())
					{
						token = st.nextToken();
					}
					else
					{
						break;
					}
				}

				pointer.and[2].or[1].auxData = get;

				if (!st.hasMoreTokens())
				{
					token = "STOPSTOP";
				}
			}
		}

		temp = (String) pointer.and[3].data;

		if (token.startsWith(temp.substring(0, temp.indexOf("???")).trim()))
		{
			String  get = token;

			if (st.hasMoreTokens())
			{
				token = st.nextToken();
			}
			else
			{
				throw (new IllegalArgumentException("Expecting endDateTime"));
			}

			while (token != null)
			{
				get = get + token;

				if (st.hasMoreTokens())
				{
					token = st.nextToken();
				}
				else
				{
					break;
				}
			}

			pointer.and[3].auxData = get;
		}		// System.out.println(head.toString());

		update(pStartDate, pEveryDate, pEveryDateType, pDayMap, pStartTime, pEndTime, pEveryTime, pEveryTimeType, pAtTimes, pEndDate);
	}


	/**
	 * This function parses the data in each node and stores the information in the appropriate type 
   * in the appropriate field.
	 */


	public void update(pNode pStartDate, pNode pEveryDate, pNode pEveryDateType, pNode pDayMap, pNode pStartTime, pNode pEndTime, pNode pEveryTime, pNode pEveryTimeType, pNode pAtTimes, pNode pEndDate)
	{
		if ((pStartDate != null) && (pStartDate.auxData != null))
		{
			String  temp = (String) pStartDate.auxData;
			int     m = 0, d = 0, y = 1997, h = 0, n = 0;
			int     i = temp.indexOf('/');

			if (i != -1)
			{
				if ((temp.indexOf('/', i + 1) != -1) && ((temp.indexOf('/', i + 1) - i) <= 3))
				{
					int     j, k;

					j = i - 2;

					if (temp.charAt(j) != '1')
					{
						j++;
					}

					k = temp.indexOf('/', i + 1) + 2;

					if (Character.isDigit(temp.charAt(k)))
					{
						while (Character.isDigit(temp.charAt(k)))
						{
							k++;

							if (k == temp.length())
							{
								break;
							}
						}
					}

					try
					{
						m = Integer.parseInt(temp.substring(j, i)) - 1;
						d = Integer.parseInt(temp.substring(i + 1, temp.indexOf('/', i + 1)));
						y = Integer.parseInt(temp.substring(temp.indexOf('/', i + 1) + 1, k));

						if (y < 100)
						{
							y = y + 1900;
						}
					}
					catch (Exception e)
					{		// System.out.println("error");
						error("Invalid date format");

						return;
					}
				}
				else
				{		// System.out.println("[Schedule] Bad date format");
					error("Invalid date format, Date field must be in the form 'MM/DD/YY'.");

					return; /* int j,k;
					  j = i-2;
					  if(temp.charAt(j)!='1') {
					  j++;
					  }
					  k = i+3;
					  if(temp.charAt(k).isDigit()) {
					  while(temp.charAt(k).isDigit()) {
					  k++;
					  if(k==temp.length) {
					  break;
					  }
					  }
					  }
					  try {
					  m = Integer.parse.Int */
				}
			}

			startDate = toDate(y, m, d, 0, n, 0);
		}

		if ((pEveryDate != null) && (pEveryDate.auxData != null))
		{
			try
			{
				everyDate = Integer.parseInt(((String) pEveryDate.auxData).substring(5));
			}
			catch (Exception e)
			{		// System.out.println("error 2");
				error("Invalid 'everyDate' format.");

				return;
			}
		}

		if ((pEveryDateType != null) && (pEveryDateType.auxData != null))
		{
			String  temp = (String) pEveryDateType.auxData;

			switch (temp.charAt(0))
			{

				case 'y':
					everyDateType = kYears;

					break;

				case 'm':
					everyDateType = kMonths;

					break;

				case 'w':
					everyDateType = kWeeks;

					break;

				case 'd':
					everyDateType = kDays;

					break;
			}
		}

		if ((pDayMap != null) && (pDayMap.auxData != null))
		{
			for (int x = 0; x < 7; x++)
			{
				dayMap[x] = false;
			}

			String  map = (String) pDayMap.auxData;
			String  temp = " ";
			int     i = 2;

			while (i < map.length())
			{
				if (map.charAt(i) == '-')
				{
					String  pointer = map.substring(i - 2, i);
					String  end = map.substring(i + 1, i + 3);

					i = i + 3;

					while (!pointer.equals(end))
					{
						switch (pointer.charAt(0))
						{

							case 's':
								if (pointer.charAt(1) == 'u')
								{
									pointer = "mo";
									temp = temp + "mo ";

									break;		// if(pointer.equals(end)) {break;}
								}
								else
								{
									pointer = "su";
									temp = temp + "su ";

									break;		// if(pointer.equals(end)) {break;}
								}

							case 'm':
								pointer = "tu";
								temp = temp + "tu ";

								break;		// if(pointer.equals(end)) {break;}

							case 't':
								if (pointer.charAt(1) == 'u')
								{
									pointer = "we";
									temp = temp + "we ";

									break;		// if(pointer.equals(end)) {break;}
								}
								else
								{
									pointer = "fr";
									temp = temp + "fr ";

									break;		// if(pointer.equals(end)) {break;}
								}

							case 'w':
								pointer = "th";
								temp = temp + "th ";

								break;		// if(pointer.equals(end)) {break;}

							case 'f':
								pointer = "sa";
								temp = temp + "sa ";

								break;		// if(pointer.equals(end)) {break;}
						}
					}
				}

				if ((i + 2) >= map.length())
				{
					temp = temp + map.substring(i) + " ";

					break;
				}
				else
				{
					temp = temp + map.substring(i, i + 2) + " ";
					i = i + 2;
				}
			}

			StringTokenizer st = new StringTokenizer(temp, " ");
			String		token;

			while (st.hasMoreTokens())
			{
				token = st.nextToken();

				switch (token.charAt(0))
				{

					case 's':
						if (token.charAt(1) == 'u')
						{
							dayMap[0] = true;

							break;
						}
						else
						{
							dayMap[6] = true;

							break;
						}

					case 'm':
						dayMap[1] = true;

						break;

					case 't':
						if (token.charAt(1) == 'u')
						{
							dayMap[2] = true;

							break;
						}
						else
						{
							dayMap[4] = true;

							break;
						}

					case 'w':
						dayMap[3] = true;

						break;

					case 'f':
						dayMap[5] = true;

						break;
				}
			}
		}

		if ((pStartTime != null) && (pStartTime.auxData != null))
		{
			String  temp = (String) pStartTime.auxData; /* int h=0,m=0,s=0;
					  int i = temp.indexOf(':');
					  if(i!=-1) {
					  if((temp.indexOf(':',i+1)!=-1)&&((temp.indexOf(":",i+1)-i)<=2)) {
					  int j,k;
					  j = i-2;
					  i f(!Character.isDigit(temp.charAt(j))) {
					  j++;
					  } 
					  k = temp.indexOf(":",i+1)+2; 
					  if(Character.isDigit(temp.charAt(k))) { 
					  while(Character.isDigit(temp.charAt(k))) { 
					  k++; 
					  if(k==temp.length()) {
					  break;
					  } 
					  } 
					  } 
					  tr y {
					  h  = Integer.parseInt(temp.substring(j,i))-1;
					  m  = Integer.parseInt(temp.substring(i+1,temp.indexOf(":",i+1)));
					  s =  Integer.parseInt(temp.substring(temp.indexOf(":",i+1)+1,k));
					  } 
					  catch(Exception e) {
					  System.out.println("fucked up");
					  return;
					  }
					  }
					  } 
					  // May want to check over this and change it if so needs
 
					  if(startDate != null) {
					  startTime = startDate;
					  }
					  else {
					  startTime = new Date();
					  }
					  startTime.setHours(h);
					  startTime.setMinutes(m);
					  startTime.setSeconds(s);
					  startTime = new Date(97,0,0,h,m,s); */

			startTime = parseTime(temp);
		}

		if ((pEndTime != null) && (pEndTime.auxData != null))
		{
			String  temp = (String) pEndTime.auxData; /* int h=0,m=0,s=0;
					  int i = temp.indexOf(':');
					  if(i!=-1) {
					  if((temp.indexOf(":",i+1)!=-1)&&((temp.indexOf(":",i+1)-i)<=2)) {
					  int j,k;
					  j = i-2;
					  if(!Character.isDigit(temp.charAt(j))) {
					  j++;
					  }
					  k = temp.indexOf(":",i+1)+2;
					  if(Character.isDigit(temp.charAt(k))) {
					  while(Character.isDigit(temp.charAt(k))) {
					  k++;
					  if(k==temp.length()) {
					  break;
					  }
					  }
					  }
					  try {
					  h = Integer.parseInt(temp.substring(j,i));
					  m = Integer.parseInt(temp.substring(i+1,temp.indexOf(":",i+1)));
					  s = Integer.parseInt(temp.substring(temp.indexOf(":",i+1)+1,k));
					  }
					  catch(Exception e) {
					  System.out.println("fucked up");
					  return;
					  }
					  }
					  }
					  // May want to check over this and change it if so needs

					  if(startDate != null) {
					  endTime = startDate;
					  }
					  else {
					  endTime = new Date();
					  }
					  endTime.setHours(h);
					  endTime.setMinutes(m);
					  endTime.setSeconds(s);
					  endTime = new Date(97,0,0,h,m,s); */

			endTime = parseTime(temp);
		}

		if ((pEveryTime != null) && (pEveryTime.auxData != null))
		{
			try
			{
				everyTime = Integer.parseInt(((String) pEveryTime.auxData).substring(5));
			}
			catch (Exception e)
			{		// System.out.println("error 3");
				error("Invalid 'everyTime' field.");

				return;
			}
		}

		if ((pEveryTimeType != null) && (pEveryTimeType.auxData != null))
		{
			String  temp = (String) pEveryTimeType.auxData;

			switch (temp.charAt(0))
			{

				case 'h':
					everyTimeType = kHours;

					break;

				case 'm':
					everyTimeType = kMinutes;

					break;

				case 's':
					everyTimeType = kSeconds;

					break;
			}
		}

		if ((pAtTimes != null) && (pAtTimes.auxData != null))
		{
			String  temp = (String) pAtTimes.auxData;

			temp = temp.substring(2);

			StringTokenizer st = new StringTokenizer(temp, ",");
			String		token;

			atTimes = new Vector();

			while (st.hasMoreTokens())
			{
				token = st.nextToken();

				atTimes.addElement(parseTime(token));
			}
		}

		if ((pEndDate != null) && (pEndDate.auxData != null))
		{
			String  temp = (String) pEndDate.auxData;
			int     m = 0, d = 0, y = 1997, h = 0, n = 0;
			int     i = temp.indexOf('/');

			if (i != -1)
			{
				if ((temp.lastIndexOf('/') != i) && ((temp.lastIndexOf('/') - i) <= 3))
				{
					int     j, k;

					j = i - 2;

					if (temp.charAt(j) != '1')
					{
						j++;
					}

					k = temp.indexOf('/', i + 1) + 2;

					if (Character.isDigit(temp.charAt(k)))
					{
						while (Character.isDigit(temp.charAt(k)))
						{
							k++;

							if (k == temp.length())
							{
								break;
							}
						}
					}

					try
					{
						m = Integer.parseInt(temp.substring(j, i)) - 1;
						d = Integer.parseInt(temp.substring(i + 1, temp.indexOf('/', i + 1)));
						y = Integer.parseInt(temp.substring(temp.indexOf('/', i + 1) + 1, k));

						if (y < 100)
						{
							y = y + 1900;
						}
					}
					catch (Exception e)
					{		// System.out.println("error 4");
						error("Invalid 'endDate' field");

						return;
					}
				}
				else
				{		// System.out.println("[Schedule] Bad date format");
					error("Invalid date format, the date field must be in the form 'MM/DD/YY[YY]'.");

					return; /* int j,k;
					  j = i-2;
					  if(temp.charAt(j)!='1') {
					  j++;
					  }
					  k = i+3;
					  if(temp.charAt(k).isDigit()) {
					  while(temp.charAt(k).isDigit()) {
					  k++;
					  if(k==temp.length) {
					  break;
					  }
					  }
					  }
					  try {
					  m = Integer.parse.Int */
				}
			}

			endDate = toDate(y, m, d, 0, 0, 0);
		}

		if (!((dayMap[0]) || (dayMap[1]) || (dayMap[2]) || (dayMap[3]) || (dayMap[4]) || (dayMap[5]) || (dayMap[6])))
		{
			for (int x = 0; x < 7; x++)
			{
				dayMap[x] = true;
			}
		}
	}


	/**
	 * This method takes a string and parses it into a Date with time fields valid
	 */


	private Date parseTime(String token)
	{
		Date		rval = null;
		Calendar	cal = Calendar.getInstance(timezone);

		cal.setTime(new Date());

		int     year = cal.get(cal.YEAR);

		if (!Character.isDigit(token.charAt(0)))
		{
			while (!Character.isDigit(token.charAt(0)))
			{
				token = token.substring(1);
			}
		}

		if (!Character.isDigit(token.charAt(token.length() - 1)))
		{
			if (token.charAt(token.length() - 1) != 'm')
			{
				while ((!Character.isDigit(token.charAt(token.length() - 1))) || (token.charAt(token.length() - 1) != 'm'))
				{
					token = token.substring(0, token.length() - 2);
				}
			}
		}


		if (token.indexOf("pm") != -1)
		{
			try
			{
				int     h = Integer.parseInt(token.substring(0, token.indexOf(':')));

				if (h == 12)
				{
					h = 0;
				}

				int     m = 0;
				int     s = 0;

				if (token.lastIndexOf(':') == token.indexOf(':'))
				{
					m = Integer.parseInt(token.substring(token.indexOf(':') + 1, token.indexOf('p')));
				}
				else
				{
					m = Integer.parseInt(token.substring(token.indexOf(':') + 1, token.lastIndexOf(':')));
					s = Integer.parseInt(token.substring(token.lastIndexOf(':') + 1, token.indexOf('p')));
				}

				rval = toDate(year, 1, 1, h + 12, m, s);
			}
			catch (Exception e)
			{		// System.out.println("[Schedule] Exception caught: "+e.toString());
				error("Error parsing time.", e);
			}
		}
		else
		{
			boolean flag = false;

			if (token.indexOf("am") == -1)
			{
				flag = true;
			}

			try
			{
				int     h = Integer.parseInt(token.substring(0, token.indexOf(':')));

				if (h == 24)
				{
					h = 0;
				}

				if ((!flag) && (h == 12))
				{
					h = 0;
				}

				int     m = 0;
				int     s = 0;

				if (token.lastIndexOf(':') == token.indexOf(':'))
				{
					if (flag)
					{
						m = Integer.parseInt(token.substring(token.indexOf(':') + 1));
					}
					else
					{
						m = Integer.parseInt(token.substring(token.indexOf(':') + 1, token.indexOf('a')));
					}
				}
			else
				{
					m = Integer.parseInt(token.substring(token.indexOf(':') + 1, token.lastIndexOf(':')));

					if (flag)
					{
						s = Integer.parseInt(token.substring(token.lastIndexOf(':') + 1));
					}
					else
					{
						s = Integer.parseInt(token.substring(token.lastIndexOf(':') + 1, token.indexOf('a')));
					}
				}

				rval = toDate(year, 1, 1, h, m, s);
			}
			catch (Exception e)
			{		// System.out.println("[Schedule] Exception caught: "+e.toString());
				error("Error parsing time.", e);
			}
		}

		return (rval);
	}


	public static void main(String args[])
	{
		StringBuffer    input = new StringBuffer();

		for (int x = 0; x < args.length; x++)
		{
			input.append(args[x] + " ");
		}

		System.out.println(input);

		Schedule	sched = new Schedule(input.toString());

		System.out.println(sched.isValid());
		System.out.println("schedule=" + sched.getScheduleString());

		Date    s = new Date();

		while (true)
		{
			Date    nextDate = sched.getNextDate(s);

			if (nextDate == null)
			{
				System.out.println("startDate=" + sched.startDate);
				System.out.println("everyDate=" + sched.everyDate);
				System.out.println("everyDateType=" + sched.everyDateType);
				System.out.println("dayMap=SU:" + sched.dayMap[0] + ",MO:" + sched.dayMap[1] + ",TU:" + sched.dayMap[2] + ",WE:" + sched.dayMap[3] + ",TH:" + sched.dayMap[4] + ",FR:" + sched.dayMap[5] + ",SA:" + sched.dayMap[6]);
				System.out.println("startTime=" + sched.startTime);
				System.out.println("endTime=" + sched.endTime);
				System.out.println("everyTime=" + sched.everyTime);
				System.out.println("everyTimeType=" + sched.everyTimeType);
				System.out.println("atTimes=" + sched.atTimes);
				System.out.println("endDate=" + sched.endDate);

				break;
			}
			else
			{
				System.out.println("Date = " + s.toString() + ", next date = " + nextDate);
			}

			long    newDateMillis = s.getTime();

			s = new Date(newDateMillis + 1);
		}
	}


	public long getMaximumLatency()
	{
		return (maximumLatency);
	}


	public void setMaximumLatency(int milliseconds)
	{
		maximumLatency = milliseconds;
	}


	public int getScheduleType()
	{
		boolean b;		// Check to see if it's custom.

		if (startDate != null)
		{
			return (CUSTOM);
		}

		if (everyDate != 0)
		{
			return (CUSTOM);
		}

		if (everyDateType != 0)
		{
			return (CUSTOM);
		}

		if (atTimes != null)
		{
			return (CUSTOM);
		}

		if (endDate != null)
		{
			return (CUSTOM);	// Check to see if it's possibly every 1d or 1w
		}

		if (startTime.equals(endTime))
		{
			if ((everyTime == 1) && (everyTimeType == kMinutes))
			{
				int     count = 0;

				for (int i = 0; i < dayMap.length; i++)
				{
					if (dayMap[i])
					{
						count++;
					}
				}

				if (count == 7)
				{
					return (EVERY_DAY);
				}

				if (count == 1)
				{
					return (EVERY_WEEK);
				}
			}
		}
		else
		{		// Check to see if it's every 1m, 10m, 30m, 1h, 3h, 12h
			Calendar cal = Calendar.getInstance(timezone);
			cal.setTime(startTime);
			Calendar cal2 = Calendar.getInstance(timezone);
			cal.setTime(endTime);

			b = ((cal.get(Calendar.HOUR_OF_DAY) == 0) && (cal.get(Calendar.MINUTE) == 0) && (cal.get(Calendar.SECOND) == 1) &&
				  (cal2.get(Calendar.HOUR_OF_DAY) == 23) && (cal2.get(Calendar.MINUTE)==59) && (cal2.get(Calendar.SECOND)==59));
			//			b = ((startTime.getHours() == 0) && (startTime.getMinutes() == 0) && (startTime.getSeconds() == 1) && 
			//				  (endTime.getHours() == 23) && (endTime.getMinutes() == 59) && (endTime.getSeconds() == 59));

			if (b)
			{
				if (everyTimeType == kMinutes)
				{
					switch (everyTime)
					{

						case 1:
							return (EVERY_MINUTE);

						case 10:
							return (EVERY_TEN_MINUTES);

						case 30:
							return (EVERY_HALF_HOUR);
					}
				}
				else if (everyTimeType == kHours)
				{
					switch (everyTime)
					{

						case 1:
							return (EVERY_HOUR);

						case 3:
							return (EVERY_3_HOURS);

						case 12:
							return (EVERY_12_HOURS);
					}
				}
			}
		}

		return (CUSTOM);
	}		// Inner class: pNode


	class pNode
	{
		boolean valid = false;
		pNode[] and;
		pNode[] or;
		Object  data;
		Object  auxData;

		public pNode()
		{
			valid = true;
		}


		public pNode(Object inData)
		{
			valid = true;
			data = inData;
		}


		public pNode(Object inData, Object inAuxData)
		{
			valid = true;
			data = inData;
			auxData = inAuxData;
		}


		public pNode(Object inData, pNode[] inAnd, pNode[] inOr)
		{
			valid = true;
			data = inData;
			and = inAnd;
			or = inOr;
		}


		public pNode(Object inData, Object inAuxData, pNode[] inAnd, pNode[] inOr)
		{
			valid = true;
			data = inData;
			auxData = inAuxData;
			and = inAnd;
			or = inOr;
		}


		public void addAndNode(pNode addNode)
		{
			int     length;

			if (and == null)
			{
				length = 0;
			}
			else
			{
				length = and.length;
			}

			pNode[] temp = new pNode[length + 1];

			for (int x = 0; x < length; x++)
			{
				temp[x] = and[x];
			}

			temp[length] = addNode;
			and = temp;
		}


		public void addOrNode(pNode addNode)
		{
			int     length;

			if (or == null)
			{
				length = 0;
			}
			else
			{
				length = or.length;
			}

			pNode[] temp = new pNode[length + 1];

			for (int x = 0; x < length; x++)
			{
				temp[x] = or[x];
			}

			temp[length] = addNode;
			or = temp;
		}


		public void addAndNodes(pNode[] addNode)
		{
			int     length;

			if (and == null)
			{
				length = 0;
			}
			else
			{
				length = and.length;
			}

			int     tlength = length + addNode.length;
			pNode[] temp = new pNode[tlength];

			for (int x = 0; x < length; x++)
			{
				temp[x] = and[x];
			}

			for (int x = length; x < tlength; x++)
			{
				temp[x] = addNode[x - length];
			}

			and = temp;
		}


		public void addOrNodes(pNode[] addNode)
		{
			int     length;

			if (or == null)
			{
				length = 0;
			}
			else
			{
				length = or.length;
			}

			int     tlength = length + addNode.length;
			pNode[] temp = new pNode[tlength];

			for (int x = 0; x < length; x++)
			{
				temp[x] = or[x];
			}

			for (int x = length; x < tlength; x++)
			{
				temp[x] = addNode[x - length];
			}

			or = temp;
		} /* Don't even think about calling toString if you don't have strings stored as data */


		public String toString()
		{
			int     length;

			if (and == null)
			{
				length = 0;
			}
			else
			{
				length = and.length;
			}

			String  rval;
			String  text = "";

			if (data != null)
			{
				text = text + data + "!";
			}
			else
			{
				text = text + "ND!";
			}

			if (auxData != null)
			{
				text = text + auxData;
			}
			else
			{
				text = text + "NAD";
			}

			rval = " ->(" + text + ")[";

			if (length != 0)
			{
				rval = rval + "AND:";

				for (int x = 0; x < and.length; x++)
				{
					rval = rval + (String) and[x].toString();
				}
			}

			if (or == null)
			{
				length = 0;
			}
			else
			{
				length = or.length;
			}

			if (length != 0)
			{
				rval = rval + "][OR:";

				for (int x = 0; x < or.length; x++)
				{
					rval = rval + (String) or[x].toString();
				}
			}

			rval = rval + "]";

			return (rval);
		}


	}

	protected static void print(String s)
	{
		if (verbose)
		{
			System.out.println("Schedule: " + s);
		}
	}


	protected void error(String s)
	{
		if (errors)
		{
			System.out.println("[Schedule] Error: " + s);
		}
	}


	protected void error(String s, Exception e)
	{
		error(s + ": " + e);
		e.printStackTrace();
	}


	protected void debug(String s)
	{
		if (debug)
		{
			System.out.println("[Schedule] " + s);
		}
	}



}

