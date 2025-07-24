
package com.cometway.props;


import java.util.List;


/*
* Implement this interface to implement a class capable of writing Props.
*/

public interface PropsWriterInterface
{
	public void writeProperty(String key, Object o) throws PropsException;

	public void writeProps(Props p) throws PropsException;

	public void writePropsList(List p) throws PropsException;
}

