Reporter Agents Example

=======================



There are variety of different Reporters that can be used with the Agent Kernel.


Some of the default reporter agents may be specified as a command line option for the AK command; others must be started normally as Agents.

Using the SampleAgent from the sample_agent demo, we can demonstrate the effects of using 
different Reporter Agents:



1. 
Disable all output from agents:
  ak -reporter null 200_SampleAgent.agent

2. 

If you are viewing output in a VT100 console, the VT100ConsoleReporter
highlights debug messages in bold. Warnings and errors are inversed:

  ak -reporter com.cometway.io.VT100ConsoleReporter 200_SampleAgent.agent

3. 

Send output to log files using the FileReporter:
  

ak 100_FileReporter.agent 200_SampleAgent.agent



Reporters can be written to manage and route kernel output in any number 
of ways; developers may write 
Reporter Agents that log activities to a database or TCP/IP connections in addition to using the reporter agents provided with the Agent Kernel.


Updated: 
2002-03-22 PC
2003-10-19 DM

