
DemoService.java

This agent is a demonstration of creating a service by subclassing
ServiceAgent. This approach makes things simpler since the ServiceAgent
automatically registers with the service manager using the service_name
property when the agent starts. It automatically unregisters when a stop
is requested. Other agents using this service (such as DemoServiceClient)
can use the service manager to get a reference to it.

DemoServiceClient.java

This agent is a demonstration of accessing another agent that is registered
with the service manager. It looks up the service as specified by the
demo_service property (demo_service by default), taking care
to cast the returned object as DemoService, and calling its run method.

The configuration for these agents are stored in the ak.xstartup file.

To run this demo type:
ak


