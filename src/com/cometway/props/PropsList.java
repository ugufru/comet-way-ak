
package com.cometway.props;


import com.cometway.util.FlushInterface;
import java.util.Collections;
import java.util.List;
import java.util.Vector;


/**
* This class represents a list of Props stored in memory.
* It can be extended to provide alternate storage layers,
* or used externally with associated Import and Export agents.
*/

public class PropsList implements PropsListInterface
{
	protected final Object[] synchObject = new Object[0];
	protected List propsList = new Vector();
	protected boolean dirty;
	protected IPropsChangeListener propsChangeListener = new PropsChangeListenerImpl();
	protected FlushInterface flushHandler;


	/**
	* Adds the specified Props to this list.
	* The current IPropsChangeListener is added to the Props.
	*/

	public void addProps(Props p)
	{
		synchronized (synchObject)
		{
			p.enableListeners();
			p.addListener(propsChangeListener);

			propsList.add(p);

			dirty = true;
		}
	}


	/**
	* Creates a new Props and adds it to this list.
	* The current IPropsChangeListener is added to the Props.
	*/

	public Props createProps()
	{
		Props p;

		synchronized (synchObject)
		{
			p = new Props();

			p.enableListeners();
			p.addListener(propsChangeListener);

			propsList.add(p);

			dirty = true;
		}

		return (p);
	}


	/**
	* Calls the FlushInterface implementation assigned to this object.
	* Use setFlushHandler() method to set the FlushInterface implementation.
	* This method is usually called in response to a change in the contents
	* of the PropsList so that the changes can be saved.
	*/

	public void flush()
	{
		if (flushHandler != null)
		{
			synchronized (synchObject)
			{
				flushHandler.flush();
			}
		}
	}


	/**
	* Returns the first matching Props in the list
	* based on specified key and value.
	*/

	public Props getProps(String key, Object value)
	{
		Props p = null;

		synchronized (synchObject)
		{
			int count = propsList.size();

			for (int i = 0; i < count; i++)
			{
				Props pp = (Props) propsList.get(i);
				Object ppValue = pp.getProperty(key);

				if ((pp != null) && (ppValue != null) && (ppValue.equals(value)))
				{
					p = pp;
					break;
				}
			}
		}

		return (p);
	}


	/**
	* Returns a List containing all of the Props contained by this List.
	* If the list is empty, a List with zero (0) elements is returned.
	*/

	public List listProps()
	{
		List v = new Vector();

		v.addAll(propsList);

		return (v);
	}


	/**
	* Returns a List containing a subset of the Props contained by this list
	* specified by a matching key and value.
	* If no matching Props are found, a List with zero (0) elements is returned.
	*/

	public List listProps(String key, Object value)
	{
		List v = new Vector();

		synchronized (synchObject)
		{
			int count = propsList.size();

			for (int i = 0; i < count; i++)
			{
				Props p = (Props) propsList.get(i);
				Object pValue = p.getProperty(key);

				if ((pValue != null) && (pValue.equals(value)))
				{
					v.add(p);
				}
			}
		}

		return (v);
	}


	/**
	* Returns a List containing a subset of the Props contained by this list
	* specified by a matching key and value.
	* The result set is alphabetically sorted using the specified key (sortBy).
	* If no matching Props are found, a List with zero (0) elements is returned.
	*/

	public List listProps(String key, Object value, String sortBy)
	{
		List v = listProps(key, value);

		Collections.sort(v, new PropsComparator(key));

		return (v);
	}


	/**
	* Returns a List containing a subset of the Props contained by this list
	* specified by a matching property containing a regular expression.
	* If no matching Props are found, a List with zero (0) elements is returned.
	*/

	public List listPropsRegExMatching(String key, String str)
	{
		List v = new Vector();

		synchronized (synchObject)
		{
			int count = propsList.size();

			for (int i = 0; i < count; i++)
			{
				Props p = (Props) propsList.get(i);

				if (p.regExPropertyMatches(key, str))
				{
					v.add(p);
				}
			}
		}

		return (v);
	}


	/**
	* Returns a List containing a subset of the Props contained by this list
	* specified by a matching property containing a regular expression.
	* If no matching Props are found, a List with zero (0) elements is returned.
	*/

	public List listPropsMatchingRegEx(String key, String regex)
	{
		List v = new Vector();

		synchronized (synchObject)
		{
			int count = propsList.size();

			for (int i = 0; i < count; i++)
			{
				Props p = (Props) propsList.get(i);

				if (p.propertyMatchesRegEx(key, regex))
				{
					v.add(p);
				}
			}
		}

		return (v);
	}


	/**
	* Returns true if any of the data managed by this list has been changed.
	*/

	public boolean needsToSave()
	{
		return (dirty);
	}


	/**
	* Resets the changed (dirty) state of this PropsList.
	*/

	public void resetNeedsToSave()
	{
		dirty = false;
	}


	/**
	* If the list contains the specified Props it is removed and true is return;
	* otherwise it returns false.
	*/

	public boolean removeProps(Props p)
	{
		boolean success = false;

		synchronized (synchObject)
		{
			success = propsList.remove(p);

			if (success)
			{
				dirty = true;
			}
		}

		return (success);
	}


	/**
	* Removes all Props from the list with matching key and value.
	* Returns true if Props were found and deleted; false otherwise.
	*/

	public boolean removeProps(String key, Object value)
	{
		boolean result = false;

		synchronized (synchObject)
		{
			int count = propsList.size();

			while (count > 0)
			{
				count--;

				Props p = (Props) propsList.get(count);
				Object pValue = p.getProperty(key);

				if ((pValue != null) && (pValue.equals(value)))
				{
					p.removeListener(propsChangeListener);

					propsList.remove(count);
					dirty = true;
					result = true;
				}
			}
		}

		return (result);
	}


	/**
	* Sets the FlushInterface implementation called by this class
	* when its flush() method is called.
	*/

	public void setFlushHandler(FlushInterface flushHandler)
	{
		this.flushHandler = flushHandler;
	}


	/**
	* Sets the PropsChangeListener implementation called by Props
	* added or created in this PropsList.
	*/

	protected void setPropsChangeListener(IPropsChangeListener propsChangeListener)
	{
		this.propsChangeListener = propsChangeListener;
	}


	/**
	* Static method for sorting a List containing Props alphabetically by the specified key.
	*/

	public static List sortPropsList(List propsList, String key)
	{
		Collections.sort(propsList, new PropsComparator(key));

		return (propsList);
	}


	/**
	* Updates all Props with matching key and value using the specificed Props.
	* Returns true if Props were found and updated; false otherwise.
	*/

	public boolean updateProps(Props p, String key, Object value)
	{
		boolean result = false;

		synchronized (synchObject)
		{
			int count = propsList.size();

			while (count > 0)
			{
				count--;

				Props pp = (Props) propsList.get(count);
				Object pValue = pp.getProperty(key);

				if ((pValue != null) && (pValue.equals(value)))
				{
					pp.copyFrom(p);
					dirty = true;
					result = true;
				}
			}
		}

		return (result);
	}


	/**
	* This receives propsChanged notifications from Props managed by this classs
	* and registers its status as having changed Props.
	*/

	class PropsChangeListenerImpl implements IPropsChangeListener
	{
		public void propsChanged(Props props, String changedKeys[])
		{
			dirty = true;
		}
	}
}

