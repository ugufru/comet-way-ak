
package com.cometway.props;


/**
* Implements a Props parser that returns one or more Props.
*/

public interface PropsReaderInterface
{
	/**
	* Returns the next available Props, or null if one is not available.
	*/

	public Props nextProps() throws PropsException; 
}

