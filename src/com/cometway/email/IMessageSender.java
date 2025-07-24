
package com.cometway.email;


/**
* IMessageSender is an interface for those classes that support a sendMessage()
*   method for sending an IMessage
*/

public interface IMessageSender
{


	/**
	* sendMessage is a method that sends a given IMessage
	* @return true if the IMessage was successfully 'sent', false otherwise
	*/

	public boolean sendMessage(IMessage m);
}

