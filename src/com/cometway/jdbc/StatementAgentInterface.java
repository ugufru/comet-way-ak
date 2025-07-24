
package com.cometway.jdbc;


import com.cometway.props.Props;
import java.util.List;


/**
* This interface facilitates the execution of stored JDBC statements.
*/

public interface StatementAgentInterface
{
	public boolean executeInsert(String statementName, Props p);

	public boolean executeInsert(String statementName, Props params, Props p);

	public List executeQuery(String statementName);

	public List executeQuery(String statementName, Props params);

	public boolean executeUpdate(String statementName);

	public boolean executeUpdate(String statementName, Props params);
}


