
package com.cometway.props;


import com.cometway.util.FlushInterface;
import java.util.List;


/**
* This interface represents a list of Props.
*/

public interface PropsListInterface
{
	/**
	* Adds the specified Props to this list.
	* The current IPropsChangeListener is added to the Props.
	*/

	public void addProps(Props p);


	/**
	* Call the flush implementation of this object.
	* It is an explicit signal sent to indicate that any unsaved objects should now be saved.
	* This method is usually called in response to a change in the contents
	* of the PropsList so that the changes can be saved.
	*/

	public void flush();

	/**
	* Returns the first matching Props in the list
	* based on specified key and value.
	*/

	public Props getProps(String key, Object value);

	/**
	* Returns a List containing all of the Props contained by this List.
	* If the list is empty, a List with zero (0) elements is returned.
	*/

	public List listProps();

	/**
	* Returns a List containing a subset of the Props contained by this list
	* specified by a matching key and value.
	* If no matching Props are found, a List with zero (0) elements is returned.
	*/

	public List listProps(String key, Object value);

	/**
	* Returns a List containing a subset of the Props contained by this list
	* specified by a matching key and value.
	* The result set is alphabetically sorted using the specified key (sortBy).
	* If no matching Props are found, a List with zero (0) elements is returned.
	*/

	public List listProps(String key, Object value, String sortBy);

	/**
	* Returns a List containing a subset of the Props contained by this list
	* specified by a matching property containing a regular expression.
	* If no matching Props are found, a List with zero (0) elements is returned.
	*/

	public List listPropsRegExMatching(String key, String str);

	/**
	* Returns a List containing a subset of the Props contained by this list
	* specified by a matching property containing a regular expression.
	* If no matching Props are found, a List with zero (0) elements is returned.
	*/

	public List listPropsMatchingRegEx(String key, String regex);

	/**
	* If the list contains the specified Props it is removed and true is return;
	* otherwise it returns false.
	*/

	public boolean removeProps(Props p);

	/**
	* Removes all Props from the list with matching key and value.
	* Returns true if Props were found and deleted; false otherwise.
	*/

	public boolean removeProps(String key, Object value);


	/**
	* Updates all Props with matching key and value using the specificed Props.
	* Returns true if Props were found and updated; false otherwise.
	*/

	public boolean updateProps(Props p, String key, Object value);
}


