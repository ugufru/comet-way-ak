
package com.cometway.ak;

import com.cometway.util.BinaryHeap;
import com.cometway.util.EmptyHeapException;
import com.cometway.util.IHeap;
import com.cometway.util.IHeapItem;
import com.cometway.util.ISchedule;
import com.cometway.util.ISchedulable;
import com.cometway.util.IScheduler;
import com.cometway.util.IScheduleChangeListener;
import com.cometway.util.KMethod;
import com.cometway.util.PooledThread;
import com.cometway.util.ThreadPool;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;


/**
* The Scheduler is a ServiceAgent which implements the <TT>com.cometway.util.IScheduler</TT> interface.
* It maintains a pool of threads that periodically wake up any <TT>com.cometway.util.IScheduleable</TT>
* that has been scheduled. 
*/

public class Scheduler extends ServiceAgent implements IScheduler, IScheduleChangeListener
{
	private final static int kMaxSchedulerThreads = 13;
	public Hashtable threadHash;
	private ThreadGroup schedulerThreadGroup;
	private SchedulerThread[] threads; // index for the pool of SchedulerThreads
	private static int threadIndex; // pool of threads available for running 'wakeups'
	private ThreadPool threadPool;

	/**
	* Initializes the Props for this agent:
	* <DL>
	* <DT>service_name
	* <DD>The service name this agent uses to register with the Service Manager.
	* <DT>number_of_threads
	* <DD>The number of threads reserved for waking up scheduled clients.
	* </DL>
	*/

	public void initProps()
	{
		setDefault("service_name", "scheduler");
		setDefault("number_of_threads", "5");
		setDefault("load_sleep_time", "1000");
	}


	/**
	* Starts up the scheduler and registers it with the Service Manager.
	*/

	public void start()
	{
		threadPool = new ThreadPool(getInteger("number_of_threads"));
		threadHash = new Hashtable();
		threadIndex = 0;
		threads = new SchedulerThread[kMaxSchedulerThreads];

		schedulerThreadGroup = new ThreadGroup(getString("name"));	// actual scheduling is managed by some number of SchedulerThreads

		for (int i = 0; i < kMaxSchedulerThreads; i++)
		{
			threads[i] = new SchedulerThread(i, threadPool, schedulerThreadGroup, this);
		}

		register();
	}


	/**
	* Set the debug state of Scheduler.
	*/

	public void setDebug(boolean debug)
	{
		for (int i = 0; i < kMaxSchedulerThreads; i++)
		{
			threads[i].setDebug(debug);
		}
	}


	/** cycles through indices up to kMaxSchedulerThreads */

	private int nextThreadIndex()
	{
		int retval = threadIndex++;

		if (threadIndex >= kMaxSchedulerThreads)
		{
			threadIndex = 0;
		}

		return retval;
	}


	/** cycles through SchedulerThreads */

	private SchedulerThread getSchedulerThread()
	{
		// a nice round robin

		int index = nextThreadIndex(); // XXXX check for bad ret?

		return threads[index];
	}


	/**
	* Method implements IScheduler.schedule().  Given an ISchedulable item,
	* method adds the item to the Scheduler.
	*
	*  @return true if schedulable is scheduled, false otherwise.
	*/

	public boolean schedule(ISchedulable schedulable)
	{
		SchedulerThread schedulerThread = (SchedulerThread) threadHash.get(schedulable);

		if (schedulerThread == null)
		{
			schedulerThread = getSchedulerThread();

			if (schedulerThread != null)
			{
				if (schedulerThread.schedule(schedulable))
				{
					threadHash.put(schedulable, schedulerThread);
				}
				else
				{
					schedulerThread = null;
				}
			}
		}

		return (schedulerThread != null);
	}


	/**
	* Method implements IScheduler.unschedule().  Given an ISchedulable item,
	*   method removes that item from the Scheduler.
	*
	* @return true if the schedulable exists in the Scheduler and was
	*   successfullly unscheduled, false otherwise.
	*/

	public boolean unschedule(ISchedulable schedulable)
	{
		boolean retval = false;		// if schedulable has been scheduled it will map to a thread
		SchedulerThread schedulerThread = (SchedulerThread) threadHash.remove(schedulable);

		if (schedulerThread != null)
		{
			retval = schedulerThread.unschedule(schedulable);
		}
		else
		{
			retval = false;
		}

		return retval;
	}


	/**
	* Method implements IScheduleChangeListener.scheduleChanged()
	*/

	public void scheduleChanged(ISchedulable schedulable)
	{
		// if schedulable has been scheduled it will map to a thread

		SchedulerThread schedulerThread = (SchedulerThread) threadHash.get(schedulable);

		if (schedulerThread != null)
		{
			schedulerThread.scheduleChanged(schedulable);
		}
	}


	/**
	* Get the size of the Scheduler.
	*/

	public int getSize()
	{
		int size = 0;

		for (int i = 0; i < kMaxSchedulerThreads; i++)
		{
			size = size + threads[i].getSize();
		}

		return size;
	}


	/* Static methods for accessing the scheduler instance. */


	/**
	* Returns a reference to the Scheduler registered with the Service Manager.
	*/

	public static IScheduler getScheduler()
	{
		return ((IScheduler) ServiceManager.getService("scheduler"));
	}


	/**
	* Returns a reference to the Scheduler registered as the passed name with
	* the Service Manager.
	*/

	public static IScheduler getScheduler(String serviceName)
	{
		return ((IScheduler) ServiceManager.getService(serviceName));
	}
}


/**
* A Scheduler Thread is a Thread which implements IScheduler and also
* IScheduleChangeListener in order to listen to its schedulables.
*/

class SchedulerThread extends PooledThread implements IScheduler, IScheduleChangeListener
{
	// the ThreadPool that we use for wakeups
	private ThreadPool threadPool;		// our heap of schedulables.  the heap contains SchedulerNodes which


	// represent schedulables.  A schedulable is only in the scheduleHeap if it
	// has been scheduled and not subsequently unscheduled, and also if it is
	// waiting to execute (run wakeup()).  schedulables which are in the
	// process of running wakeup() ARE NOT in the scheduleHeap and hence are
	// not waiting to be scheduled for their next wakeup().  schedulables
	// running wakeup() are re-scheduled when wakeup() is finished

	private IHeap schedulableHeap;	// a mapping from our schedulables to the node which represents them in the


	// heap.  any schedulablew which has been scheduled and not subsequently
	// unscheduled is in the snodeHash (though not always in the scheduleHeap)

	private Hashtable snodeHash;
	private int threadId;
	private boolean debug;
	private Scheduler scheduler;

	public SchedulerThread(int threadId, ThreadPool threadPool, ThreadGroup threadGroup, Scheduler scheduler)
	{
		super(threadPool);

		schedulableHeap = new BinaryHeap();
		snodeHash = new Hashtable();
		debug = false;
		this.threadPool = threadPool;
		this.threadId = threadId;
		this.scheduler = scheduler;

		start();
	}


	public SchedulerThread(int index, ThreadPool threadPool, ThreadGroup threadGroup, Scheduler scheduler, boolean debug)
	{
		this(index, threadPool, threadGroup, scheduler);

		setDebug(debug);
	}


	/**
	* schedule: method implements IScheduler.schedule().  given an
	* ISchedulable item, method adds the item to this SchedulerThread
	*/

	public boolean schedule(ISchedulable schedulable)
	{
		debug("scheduling: " + schedulable.toString() + ", " + schedulable.getSchedule());

		ISchedule schedule = schedulable.getSchedule();
		Date nextDate = schedule.getNextDate(new Date());

		if (schedule == null)
		{
			print("schedule(): getSchedule returned null schedule; unable to " + "schedule schedulable: " + schedulable.toString() + ".");
		}
		else
		{
			if (nextDate != null)
			{
				try
				{
					// we sync on ourself to gaurantee the consistency of
					// snodeHash/scheduleHeap state

					synchronized (this)
					{
						// if this schedulable has already been scheduled here then
						// we do nothing more.

						if (!snodeHash.containsKey(schedulable))
						{
							boolean notify = false;
							SchedulerNode newNode = new SchedulerNode(schedulable, nextDate);		// map the schedulable to its node in the heap and insert


							// the node into the heap

							snodeHash.put(schedulable, newNode);

							notify = schedulableHeap.insert(newNode);

							schedulable.addScheduleChangeListener(this);		// if the heap has a new min (indicated by a true return


							// from IHeap.insert(), then our accompanying thread
							// should wake up and check its heap

							if (notify)
							{
								notify();
							}
						}
					}

					return true;
				}
				catch (Exception e)
				{
					print("schedule(): caught exception: " + e);
				}
			}
			else
			{
				error("schedule(): A null next date was returned from a " + "schedulables schedule; perhaps the shcedule became " + "invalid before the schedulable could be scheduled.");
			}
		}

		return false;
	}
	

	/**
	* unschedule: method implements IScheduler.unschedule().  given an
	* ISchedulable item, method removes that item from the Scheduler
	*/

	public boolean unschedule(ISchedulable schedulable)
	{
		boolean retval = false;

		debug("unscheduling: " + schedulable.toString());		// we sync on ourself to gaurantee the consistency of


		// snodeHash/scheduleHeap state

		synchronized (this)
		{
			SchedulerNode   unNode = (SchedulerNode) snodeHash.remove(schedulable);		// if the schedulable is not in the snodeHash then it has not been


			// scheduled here and cannot be unscheduled here

			if (unNode != null)
			{
				schedulable.removeScheduleChangeListener(this);

				try
				{
					retval = schedulableHeap.delete(unNode);
				}
				catch (EmptyHeapException e)
				{
					// the heap could very well be empty.  if a schedulable were
					// running wakeup() it would be in the snode hash but not in
					// the heap

					retval = true;
				}
			}
			else
			{
				print("unschedule(): attempt to unschedule an unknown " + "schedulable: " + schedulable.toString() + ".");

				retval = false;
			}
		}

		return retval;
	}


	/*
	* implements IScheduleChangeListener.scheduleChanged()
	*/

	public void scheduleChanged(ISchedulable schedulable)
	{
		debug("scheduleChanged: " + schedulable.toString());

		Date newNextDate = schedulable.getSchedule().getNextDate(new Date());

		if (newNextDate != null)
		{
			// we sync on ourself to gaurantee the consistency of
			// snodeHash/scheduleHeap state

			synchronized (this)
			{
				SchedulerNode schedulableNode = (SchedulerNode) snodeHash.get(schedulable);
				boolean retval = false;		// if the schedulable is not in the snodeHash then it has not been


				// scheduled here and cannot be rescheduled here

				if (schedulableNode != null)
				{
					try
					{
						if (schedulableHeap.delete(schedulableNode))
						{
							schedulableNode.setDate(newNextDate);		// if the heap has a new min (indicated by a true return


							// from IHeap.insert(), then our accompanying thread
							// should wake up and check its heap

							if (schedulableHeap.insert(schedulableNode))
							{
								notify();
							}
						}		// else if the node is not in the heap then the schedulable

						// must be running wakeup(); hence the schedulable will be
						// re-scheduled with the changed schedule once wakeup() is
						// finished

					}
					catch (EmptyHeapException e)
					{
						// the heap could very well be empty.  if a schedulable were
						// running wakeup() it would be in the snode hash but not in
						// the heap.  see the above comment.
					}
				}
				else
				{
					print("scheduleChanged(): attempt to scheduleChanged() on an " + "unknown schedulable: " + schedulable.toString() + ".");		// we should not be listening to this one if it isn't in our

					// snodeHash

					schedulable.removeScheduleChangeListener(this);
				}
			}
		}
		else
		{
			error("scheduleChanged(): A null next date was returned from a " + "schedulables schedule; perhaps the shcedule became " + "invalid before the schedulable could be re-scheduled.");
		}
	}


	/**
	* wakeupSchedulable: method wakes the given schedulable by sending it to a
	* WakeupMethod which is given to the ThreadPool.
	*/

	private void wakeupSchedulable(ISchedulable schedulable)
	{
		debug("wakeupSchedulable: " + schedulable.toString());

		WakeupMethod m = new WakeupMethod(schedulable, this);

		threadPool.getThread(m);
	}

	/**
	* wakeupFinished: method is called by the WakeupMethod once the wakeup()
	* for a schedulable has completed.  wakeupFinished() re-schedules the
	* schedulable if it is in the snodeHash (meaning that it has not been
	* unscheduled since wakeup() started).
	*/

	protected void wakeupFinished(ISchedulable schedulable)
	{
		debug("wakeupFinished: " + schedulable.toString());

		Date currentDate = new Date();
		ISchedule schedule = schedulable.getSchedule();

		// If the schedule is null, no point in checking the next date
		if(schedule!=null) {
			Date newNextDate = schedule.getNextDate(currentDate);
			
			if (newNextDate != null)
			{
				// we sync on ourself to gaurantee the consistency of
				// snodeHash/scheduleHeap state
				
				synchronized (this)
				{
					SchedulerNode snode = (SchedulerNode) snodeHash.get(schedulable);
					
					if (snode != null)
					{
						long diffTime = newNextDate.getTime() - snode.getLastDate().getTime();
						
						if (false)		// (diffTime < 1000)
						{
							currentDate.setTime(currentDate.getTime() + diffTime);
							
							newNextDate = schedulable.getSchedule().getNextDate(currentDate);
						}
						
						snode.setDate(newNextDate);		// if the heap has a new min (indicated by a true return
						
						
						// from IHeap.insert(), then our accompanying thread
						// should wake up and check its heap
						
						if (schedulableHeap.insert(snode))
						{
							notify();
						}
					}
					else
					{
						print("wakeupFinished(): attempt to finish waking an unknown " + "schedulable: " + schedulable.toString() + ".");
						scheduler.unschedule(schedulable);
					}
				}
			}
			else
			{
				error("wakeupFinished(): A null next date was returned from a " + "schedulables schedule; perhaps the schedule became " + "invalid before the schedulable could be re-scheduled.");
				scheduler.unschedule(schedulable);
			}
		}
	}


	/**
	* run: implements Thread.run()
	*/

	public void run()
	{
		SchedulerNode node;
		Date nodeDate;
		Date currentDate;

		while (true)
		{
			try
			{
				synchronized (this)
				{
					// spin wait as long as nothing needs to run wakeup()

					while (schedulableHeap.size() == 0)
					{
						debug("nothing in the heap, waiting...");
						wait();
					}

					// pick the next node to wakeup()

					node = (SchedulerNode) schedulableHeap.findMin();
					currentDate = new Date();
					nodeDate = node.getDate();

					debug("currentDate:  " + currentDate + ":" + currentDate.getTime());

					if (nodeDate != null)
					{
						debug("node.getDate: " + nodeDate + ":" + nodeDate.getTime());
					}

					Calendar cal1 = Calendar.getInstance();
					Calendar cal2 = Calendar.getInstance();

					cal1.setTime(nodeDate);
					cal2.setTime(currentDate);

					if (nodeDate == null)
					{
						error("run(): A null next date was extracted from the " + "heap; this should never happen as ISchedulables " + "with null next dates should never be scheduled or " + "re-scheduled...");		// get rid of the offending node
						schedulableHeap.deleteMin();
					}
					else if (cal1.before(cal2) || cal1.equals(cal2))
					{
						long diffTime = diff(nodeDate, currentDate);

						debug("i have picked the next node: " + node);
						debug("Waking " + node.item + " " + diffTime + " milliseconds (" + diffTime / 1000 + " seconds) " + "after its scheduled wakeup.");
						node.setLastDate(nodeDate);		// the node comes out of the heap...
						schedulableHeap.deleteMin();		// and into wakeup()
						wakeupSchedulable(node.item);
					}
					else
					{
						// the next wakeup() time comes later.  we use a timeout on
						// the wait()

						long sleepTime = diff(nodeDate, currentDate);

						wait(sleepTime);

						long d = diff(currentDate, new Date());

						if (d < sleepTime)
						{
							debug("didn't sleep long enough, diff was " + d);
						}
					}
				}
			}
			catch (EmptyHeapException e)
			{
				print("run() saw an empty schedulable heap: " + e);
			}
			catch (Exception e)
			{
				error("caught exception in run(): " + e);
			}
		}
	}
	

	/**
	* returns the number of milliseconds between given dates date1 and date2
	*/

	private long diff(Date date1, Date date2)
	{
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();

		cal1.setTime(date1);
		cal2.setTime(date2);

		if (cal1.after(cal2))
		{
			Date temp = date1;

			date1 = date2;
			date2 = temp;
		}

		long diff = date2.getTime() - date1.getTime();

		return diff;
	}


	/**
	* Returns the number of currently scheduled clients.
	*/

	public int getSize()
	{
		return schedulableHeap.size();
	}


	/**
	* Turns on debugging mode for this agent.
	*/

	protected void setDebug(boolean debug)
	{
		this.debug = debug;
	}


	public void print(String s)
	{
		System.out.println("Scheduler Thread #" + threadId + ": " + s);
	}


	public void debug(String s)
	{
		if (debug)
		{
			System.out.println("Scheduler Thread #" + threadId + ": " + s);
		}
	}


	public void error(String s)
	{
		System.out.println("Scheduler Thread #" + threadId + " ERROR: " + s);
	}
}


/*
* SchedulerNode is a heap node that contains a schedulable.  the
* ISchedulable.getNextDate determines the nodes ordering in the heap.
*/

class SchedulerNode implements IHeapItem
{
	public ISchedulable item;
	public Date date;
	public Date lastDate;
	private boolean dead = false;


	public SchedulerNode(ISchedulable s)
	{
		item = s;

		reset();
	}


	public SchedulerNode(ISchedulable s, Date initialDate)
	{
		item = s;
		date = initialDate;
		lastDate = null;
	}


	public Date getDate()
	{
		return date;
	}


	public void setDate(Date newDate)
	{
		this.date = newDate;
	}


	public void setLastDate(Date lastDate)
	{
		this.lastDate = lastDate;
	}


	public Date getLastDate()
	{
		return this.lastDate;
	}


	public void reset()
	{
		date = item.getSchedule().getNextDate(new Date());
		lastDate = null;
	}


	/** a SchedulerNode is valid if it has a valid (non-null) date */

	public boolean isValid()
	{
		return (date != null);
	}


	/**
	* a SchedulerNode is greater then the given item if its date comes after the given items date
	*/
	
	public boolean greaterThan(IHeapItem item)
	{
		if (item instanceof SchedulerNode)
		{
			SchedulerNode s = (SchedulerNode) item;

			// with this test, invalid nodes should fall to the bottom of the heap
			// since they will always be greaterThan() any other heap node

			if (s.getDate() != null)
			{
				Calendar cal1 = Calendar.getInstance();
				Calendar cal2 = Calendar.getInstance();

				cal1.setTime(s.getDate());
				cal2.setTime(date);

				return cal2.after(cal1);
			}
		}

		return false;
	}


	protected ISchedulable getSchedulable()
	{
		return this.item;
	}


	public String toString()
	{
		try
		{
			String  s = item.getSchedule().getScheduleString();

			s = s.substring(s.indexOf("between") + 7, s.length());

			return ("SNODE: " + s + " : " + item.getSchedule().getNextDate(new Date()));
		}
		catch (Exception e)
		{
			return ("SNODE: " + item.getSchedule().getScheduleString());
		}
	}


}


/* WakeupMethod executes wakeup() for a given schedulable on a new Thread */

class WakeupMethod extends KMethod
{
	ISchedulable schedulable;
	SchedulerThread thread;

	public WakeupMethod(ISchedulable schedulable, SchedulerThread thread)
	{
		this.schedulable = schedulable;
		this.thread = thread;
	}


	public void execute()
	{
		try
		{
			schedulable.wakeup();
			thread.wakeupFinished(schedulable);

			schedulable = null;
			thread = null;
		}
		catch (Exception exn)
		{
			error("WakeupMethod.wakeup() (Scheduler): ", exn);
		}
	}


	public String toString()
	{
		return ("Wakeup: " + schedulable);
	}
}

