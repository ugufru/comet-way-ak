# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

The Comet Way Agent Kernel (AK) is a mature Java framework for building agent-based applications. Version 3.0 implements a dynamic component software model for deploying "Agent Applications" - autonomous server applications, web services, email handlers, database interfaces, and GUI applications built from interchangeable agent components.

This codebase has been modernized from Java 1.4+ to **Java 21**, with Jakarta-ORO replaced by `java.util.regex` and JDBC interfaces updated for compatibility.

## Build Commands

### Core Build Process
```bash
# Full build (compile + finalize + documentation)
cd bin && ./build.sh

# Compile only (fastest for development)
cd bin && ./compile.sh

# Create distribution package
cd bin && ./finalize.sh

# Generate Javadocs
cd bin && ./document.sh
```

### Java Environment
The build system is configured for Java 21. The compile script automatically sets:
```bash
export PATH="/usr/local/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/usr/local/opt/openjdk@21"
```

## Architecture Overview

### Agent-Based Design Pattern
The framework centers around the **Agent** abstraction - self-contained components that can be dynamically created, configured, started, stopped, and destroyed. All functionality is implemented as agents that communicate through:

1. **Properties (Props)** - Configuration and state data
2. **Message passing** - Inter-agent communication  
3. **Service registration** - Agent discovery and lookup
4. **State management** - Lifecycle control

### Core Package Structure

- **`com.cometway.ak`** - Agent Kernel core (Agent, AgentKernel, ServiceManager)
- **`com.cometway.props`** - Property management system (Props, PropsList, PropsContainer)
- **`com.cometway.httpd`** - HTTP server agents (WebServer, WebServerExtension)
- **`com.cometway.jdbc`** - Database connectivity agents (JDBCAgent, JDBCConnection)
- **`com.cometway.email`** - Email handling agents (SMTP/POP3)
- **`com.cometway.util`** - Utilities (ThreadPool, StringTools, jGrep)
- **`com.cometway.om`** - Object Management (persistence, sessions)

### Agent Lifecycle States
```
CREATING → STOPPED → STARTING → RUNNING → STOPPING → DESTROYING → DESTROYED
                     ↑              ↓
                   (restart)    (FAILED)
```

### Key Interfaces
- **`AgentInterface`** - Basic agent contract (start/stop/destroy)
- **`AgentKernelInterface`** - Agent creation and management
- **`ServiceManagerInterface`** - Service registration and lookup
- **`ReporterInterface`** - Logging and output handling

### Web Server Architecture
The HTTP server (`WebServer`) uses a modular extension system:
- **`WebServerExtension`** - Base class for HTTP request processors
- **Request pipeline** - Extensions process requests in sequence
- **Built-in extensions** - File serving, access control, path rewriting, CGI
- **Thread pool** - Configurable concurrent request handling

### Property System
Central configuration mechanism using key-value pairs:
- **`Props`** - Basic property container
- **`PropsList`** - Ordered collections of Props
- **Type conversion** - Automatic string→primitive conversion
- **Hierarchical lookup** - Property inheritance and defaults

## Development Notes

### Recent Modernization (Java 21 Upgrade)
- Jakarta-ORO regex library removed, replaced with `java.util.regex`
- JDBC interfaces updated with missing Java 21 methods
- Build scripts updated for modern Java toolchain
- Compilation produces 41 deprecation warnings (non-breaking)

### Agent Development Pattern
1. Extend `Agent` class
2. Override `initProps()` for configuration defaults
3. Implement `start()` and `stop()` for lifecycle management
4. Use `getString()`, `getInteger()`, etc. for configuration
5. Use `println()`, `debug()`, `warning()` for output

### HTTP Extension Development
1. Extend `WebServerExtension`
2. Override `handleRequest(HTTPAgentRequest)` 
3. Return `true` to halt processing, `false` to continue pipeline
4. Configure via `service_name` property for URL matching

### Service Registration
```java
// Register a service
serviceManager.registerService(serviceName, agentInterface);

// Lookup a service  
AgentInterface service = serviceManager.getService(serviceName);
```

### Threading Considerations
- Agents should be thread-safe if accessed concurrently
- `ThreadPool` utility available for managed threading
- HTTP requests processed in thread pool by default
- Use `synchronized` blocks or concurrent collections as needed

## File Structure Notes

- **`bin/`** - Build scripts and utilities
- **`src/com/cometway/`** - Main source tree
- **`import/`** - Third-party dependencies (legacy jakarta-oro, unused post-upgrade)
- **`ak.jar`** - Compiled output
- **Generated**: `temp_classes/`, `javadocs/`, distribution tarballs