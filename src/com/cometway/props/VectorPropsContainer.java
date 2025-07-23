
package com.cometway.props;

import java.util.Enumeration;
import java.util.Vector;


/**
* This props container uses a Vector for indexable storage as a Props.
*/

public class VectorPropsContainer extends AbstractPropsContainer
{
	/** The Vector used for Props storage. */

	private Vector v;


	/** Creates a new instance with its own Vector for storage. */

	public VectorPropsContainer()
	{
		v = new Vector();
	}


	/**
	* Creates a new instance using the specified Vector for storage.
	*/

	public VectorPropsContainer(Vector v)
	{
		this.v = v;
	}


	/** Returns the Vector used for storage. */

	public Vector getVector()
	{
		return (v);
	}


	/**
	* <PRE>Returns the Object specified by key.
	* If the key is Vector, the Vector used for storage is returned.
	* If the key is size, the result of Vector.size() is returned.
	* If the key is isEmpty, the result of Vector.isEmpty() is returned.
	* If the key is capacity, the result of Vector.capacity() is returned.
	* If the key is elements, the result of Vector.elements() is returned.
	* If the key is firstElement, the result of Vector.firstElement() is returned.
	* If the key is lastElement, the result of Vector.lastElement() is returned.
	* If the key is an integer, the result of Vector.get(integer) is returned.
	* Otherwise, null is returned.</PRE>
	*/

	public Object getProperty(String key)
	{
		Object o = null;
		int index = key.indexOf(':');

		if (index >= 0)
		{
			Object node = get(key.substring(0, index));

			if (node instanceof IPropsContainer)
			{
				IPropsContainer c = (IPropsContainer) node;

				o = c.getProperty(key.substring(index + 1));
			}
		}
		else
		{
			o = get(key);
		}

		return (o);
	}


	private Object get(String key)
	{
		Object o = null;

		if (key.equals("Vector"))
		{
			o = v;
		}
		else if (key.equals("size"))
		{
			o = Integer.valueOf(v.size());
		}
		else if (key.equals("isEmpty"))
		{
			o = Boolean.valueOf(v.isEmpty());
		}
		else if (key.equals("capacity"))
		{
			o = Integer.valueOf(v.capacity());
		}
		else if (key.equals("elements"))
		{
			o = v.elements();
		}
		else if (key.equals("firstElement"))
		{
			o = v.firstElement();
		}
		else if (key.equals("lastElement"))
		{
			o = v.lastElement();
		}
		else
		{
			try
			{
				o = v.get(Integer.parseInt(key));
			}
			catch (Exception e)
			{
				throw new RuntimeException("VectorPropsContainer.getProperty: Unsupported key \"" + key + "\"");
			}
		}

		return (o);
	}


	/**
	* Removes the specified Object at the Vector index specified by key.
	* Returns true if the key is a valid integer within the range of the Vectors size;
	* false otherwise.
	*/

	public boolean removeProperty(String key)
	{
		boolean result = false;

		try
		{
			v.removeElementAt(Integer.parseInt(key));

			result = true;
		}
		catch (Exception e)
		{
			throw new RuntimeException("VectorPropsContainer.removeProperty: Unsupported key \"" + key + "\"");
		}

		return (result);
	}


	/**
	* <PRE>Sets the specified Vector property.
	* If the key is Vector; the storage Vector is replaced by the value.
	* If the key is size; the storage Vector is set to the specified size using Vector.setSize().
	* If the key is capacity; the storage Vector capacity is incresed using Vector.ensureCapacity().
	* If the key is add; the value is appended to the storage Vector using Vector.add().
	* If the key is an integer within the valid range of the Vector, the specified element is replaced by the value.</PRE>
	*/

	public void setProperty(String key, Object value)
	{
		boolean result = false;
		int index = key.indexOf(':');

		if (index >= 0)
		{
			Object node = get(key.substring(0, index));

			if (node instanceof IPropsContainer)
			{
				IPropsContainer c = (IPropsContainer) node;

				c.setProperty(key.substring(index + 1), value);
			}
		}
		else
		{
			if (key.equals("Vector"))
			{
				v = (Vector) value;
			}
			else if (key.equals("size"))
			{
				v.setSize(((Integer) value).intValue());
			}
			else if (key.equals("capacity"))
			{
				v.ensureCapacity(((Integer) value).intValue());
			}
			else if (key.equals("add"))
			{
				v.add(value);
			}
			else
			{
				try
				{
					v.setElementAt(value, Integer.parseInt(key));
				}
				catch (Exception e)
				{
					throw new RuntimeException("VectorPropsContainer.setProperty: Unsupported key \"" + key + "\"");
				}
			}
		}
	}


	/**
	* Sets the Vector used for Props storage.
	*/

	public void setVector(Vector v)
	{
		this.v = v;
	}


	/**
	* Copys the elements specified by enumerateProps() into the Props.
	*/

	public void copy(IPropsContainer ipc)
	{
		String key;
		Enumeration e = enumerateProps();

		while (e.hasMoreElements())
		{
			key = (String) e.nextElement();

			ipc.setProperty(key, getProperty(key));
		}
	}


	/**
	* Returns a range of integer keys represented by the storage Vector.
	*/

	public Enumeration enumerateProps()
	{
		int size = v.size();
		Vector keys = new Vector();

		for (int i = 0; i < size; i++)
		{
			keys.addElement(Integer.toString(i));
		}

		return (keys.elements());
	}
}


