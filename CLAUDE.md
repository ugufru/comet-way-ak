# Claude Developer Guide - Comet Way Agent Kernel

This file contains information for developers working on this codebase with Claude Code.

## Build Commands

### Compilation
```bash
cd bin && ./compile.sh
```

### Testing Compilation
```bash
export PATH="/usr/local/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/usr/local/opt/openjdk@21"
cd bin && ./compile.sh
```

### Check Warnings
```bash
cd bin && ./compile.sh 2>&1 | grep "warning"
```

## Architecture Overview

### Core Components

1. **Agent Kernel (com.cometway.ak)**
   - `AK.java` - Main entry point and version info
   - `AgentKernel.java` - Core runtime engine
   - `ServiceManager.java` - Service discovery and management
   - `Scheduler.java` - Time-based task execution

2. **HTTP Server (com.cometway.httpd)**
   - `WebServer.java` - Main HTTP server implementation
   - `HTTPAgentRequest.java` - Request wrapper
   - Extension system for custom functionality

3. **Database (com.cometway.jdbc)**
   - `JDBCConnection.java` - Pooled database connections
   - `JDBCAgent.java` - Database interaction agents

4. **Utilities (com.cometway.util)**
   - `jGrep.java` - Regular expression utilities (modernized)
   - `StringTools.java` - String manipulation helpers
   - Reporter interfaces for logging

### Java 21 Modernization Changes

#### Jakarta-ORO Removal (Complete)
- **Before**: Used `org.apache.oro.text.perl.Perl5Util` + external JAR dependency
- **After**: Uses native `java.util.regex.Pattern` and `Matcher`
- **Dependency**: Completely removed from build scripts and documentation
- **Files affected**: 
  - `jGrep.java` - Core regex utilities
  - `RegExpTextFinder.java` - Text search functionality
  - HTTP extension classes - Request processing
  - Build scripts - Removed Jakarta-ORO compilation steps

#### JDBC Interface Updates
- **Added methods for Java 21 compatibility**:
  - `getNetworkTimeout()`, `setNetworkTimeout()`
  - `abort()`, `getSchema()`, `setSchema()`
  - `createSQLXML()`, `createNClob()`, etc.
  - `getParentLogger()` for Driver interface

#### Deprecated Constructor Fixes
- **Before**: `new Integer(i)`, `new Boolean(b)`, etc.
- **After**: `Integer.valueOf(i)`, `Boolean.valueOf(b)`, etc.

## Common Development Tasks

### Adding New Agents
1. Extend `Agent` or implement `AgentInterface`
2. Override `start()`, `stop()`, and `handleMessage()` methods
3. Register with ServiceManager if needed

### Creating Web Extensions
1. Extend `WebServerExtension`
2. Implement `handleRequest(HTTPAgentRequest)`
3. Configure in agent startup files

### Database Integration
1. Use `JDBCAgent` for database operations
2. Configure connection pools via properties
3. Handle transactions appropriately

## Testing

### Unit Testing
- No formal test framework in original codebase
- Test via demo applications in `demos/` directory

### Integration Testing
- Start full applications via `.xstartup` files
- Monitor via built-in reporter system

## Known Patterns

### Properties-Based Configuration
- All agents use `Props` objects for configuration
- Standard patterns: `getString()`, `getInteger()`, `getBoolean()`
- Schema validation available via `PropsSchema`

### Message Passing
- Agents communicate via `AgentMessage` objects
- Asynchronous by default
- Built-in request/response pattern

### Service Discovery
- All services register with `ServiceManager`
- Lookup by service name string
- Lifecycle management built-in

## Debugging

### Reporter System
- Use `debug()`, `warning()`, `error()` methods
- Output controlled by reporter configuration
- Multiple reporter types available

### Common Issues
- Check service registration in ServiceManager
- Verify property file syntax
- Monitor agent lifecycle (start/stop)

## Legacy Considerations

This codebase was originally written for Java 1.4+ and shows patterns from that era:
- Vector instead of ArrayList (historical reasons)
- String concatenation instead of StringBuilder (in some places)
- Manual resource management (pre-try-with-resources)

These patterns have been preserved for compatibility and stability.