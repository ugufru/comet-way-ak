package com.cometway.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.io.InputStreamReader;
import java.io.BufferedReader;


/**
 * This is a utility class that is used on the command line to interface with an SQL database.
 * It is invoked as: java com.cometway.jdbc.JDBCShell [driver] [database_url] [username] [password]
 *
 * SQL Queries can be entered at the # prompt. Aside from sql queries, the shell supports the
 * following additional commands:
 *   quit - closes the JDBC connection to the database and exits
 *   info [tablename] - prints out available info about the table including index, column names and types
 */
public class JDBCShell extends Thread
{
	Connection connection = null;
	BufferedReader in = null;

	public JDBCShell(String driver, String url, String username, String password)
	{
		Driver d = null;
		try {
			in = new BufferedReader(new InputStreamReader(System.in));
		}
		catch(Exception e) {
			System.err.println("Fatal error, could not read from stdin.");
			System.exit(-1);
		}
		try {
			d = (Driver)Class.forName(driver).newInstance();
		}
		catch(Exception e) {e.printStackTrace();}
		if(d==null) {
			System.err.println("Could not load driver: "+driver);
			System.exit(-1);
		}
		try {
			DriverManager.registerDriver(d);
		}
		catch(Exception e) {
			System.err.println("Could not register driver: "+driver);
		}
		try {
			connection = DriverManager.getConnection(url,username,password);
		}
		catch(Exception e) {e.printStackTrace();}
		if(connection==null) {
			System.err.println("Could not connect to database: "+url);
			System.exit(-1);
		}

		System.out.println("Connected to: "+url);
		start();
	}

	public void run()
	{
		String input = null;
		Statement statement = null;
		while(true) {
			try {
				System.out.print("# ");
				input = in.readLine();
				if(input.trim().equalsIgnoreCase("quit")) {
					System.out.println("Exiting...");
					break;
				}
				else if(input.trim().startsWith("info ")) {
					DatabaseMetaData metadata = connection.getMetaData();
					String table = input.substring(5).trim();
					ResultSet metaResults = metadata.getIndexInfo(null,null,table,true,false);
					while(metaResults.next()) {
						String columnName = metaResults.getString(9);
						if(columnName!=null) {
							System.out.print(metaResults.getString(3));
							System.out.print(" - index = ");
							System.out.print(columnName);
							System.out.println();
						}
					}
					metaResults = metadata.getColumns(null,null,table,"%");
					while(metaResults.next()) {
						System.out.print(metaResults.getString(4));
						System.out.print(" (");
						System.out.print(metaResults.getString(6).toLowerCase());
						System.out.print(")\t");
					}
					System.out.println();
				}
				else {
					try {
						statement = connection.createStatement();
						if(statement.execute(input)) {
							ResultSet set = statement.getResultSet();
							ResultSetMetaData meta = set.getMetaData();

							for(int x=1;x<=meta.getColumnCount();x++) {
								System.out.print(meta.getColumnName(x)+"\t");
							}
							System.out.println();
							while(set.next()) {
								for(int x=1;x<=meta.getColumnCount();x++) {
									System.out.print(set.getString(x)+"\t");
								}
								System.out.println();
							}
						}
						else {
							System.err.println("Could not execute statement");
						}
					}
					catch(Exception e) {
						System.err.println("Could not execute statement");
						e.printStackTrace();
					}
					finally {
						try {statement.close();}catch(Exception e) {;}
					}
				}				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		try {
			connection.close();
		}
		catch(Exception e) {;}
	}


	public static void main(String[] args)
	{
		if(args.length==4) {
			new JDBCShell(args[0], args[1], args[2], args[3]);
		}
		else if(args.length==2) {
			new JDBCShell(args[0], args[1], "", "");
		}
		else {
			System.err.println("JDBCShell <driver> <url> [username] [password]");
		}
	}
}
