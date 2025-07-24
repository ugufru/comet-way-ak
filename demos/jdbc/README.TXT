JDBC AGENTS
===========

The com.cometway.jdbc.JDBCAgent manages database connections and queries. 
It also contains utility methods for storing props in a relational database. 
The com.cometway.jdbc.JDBCToXMLAgent parses tables from a relational database 
into XML and outputs the data to the System.out stream.

Running the AK in this directory will start a JDBCAgent to connect to a 
SQL database, and a JDBCToXMLAgent that outputs Table1, Table2 and Table3 
from the database as XML in the console. To run the demonstration, you will need
to configure the props files to refer to tables and data sources that exist in your
network environment.

With the exception of the ODBC_JDBC_Agent, which uses the JDBC-ODBC driver included 
with the JDK, all of the agents in /db_agents require that you have a JDBC driver in
your classpath and that you edit the agent property file to include your database and
authentication information.
