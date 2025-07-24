
package com.cometway.jdbc;


import com.cometway.ak.Agent;
import com.cometway.props.Props;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


/**
* The JDBCAgentInterface manages a JDBC connection and performs simple database
* tasks such as creating and dropping tables, and executing queries,
* inserts and updates.
*/

public interface JDBCAgentInterface
{
	/**
	* Creates the specified table using the specified parameters.
	* Elements in fieldList and typeList are Strings.
	* The type is a string similar to "varchar (5) NOT NULL" part
	* of a sql CREATE statement. Remember to quote table and field
	* names appropriately for your SQL server.
	*/
	
	public boolean createTable(String tableName, List fieldList, List typeList);


	/**
	* Drops the specified table from the databsae.
	*/
	
	public boolean dropTable(String tableName);


	/**
	* Inserts the specified Props values into a table.
	* Returns true if successful; false otherwise.
	*/
	
	public boolean executeInsert(String tableName, Props p);


	/**
	* Executes the specified SQL query, returning the result as a Vector of Props.
	*/

	public List executeQuery(String sql);


	/**
	* Executes a search query based on a unique key and value, returning the result as a Vector of Props.
	*/

	public List executeQuery(String tableName, String uniqueKey, String uniqueValue);


	/**
	* Executes the specified update SQL, returning true if the update succeeded; false otherwise.
	*/
	
	public boolean executeUpdate(String sql);


	/**
	* Updates the specified Props into the record specified by uniqueKey and uniqueValue.
	* Returns true if successful; false otherwise.
	*/
	
	public boolean executeUpdate(String tableName, Props p, String uniqueKey, String uniqueValue);


	/**
	* Generates appropriate SQL for inserting the specified Props into a table.
	*/
	
	public String getInsertPropsSQL(String tableName, Props p);


	/**
	* Creates a new connection to the database using the current JDBC related settings.
	*/
	
	public Connection getConnection() throws SQLException;


	/**
	 * Properly closes the specified Connection. All Connections issued by getConnection should be
	 * closed using this method with they are no longer needed.
	 */
	
	public void closeConnection(Connection connection);
}


