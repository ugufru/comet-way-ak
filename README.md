# Comet Way Agent Kernel

**Version 3.4 Final (Java 21 Modernized) - July 23, 2025**

A **modernized and actively maintained** open-source agent-based Java framework originally created in 2008. This framework has been fully upgraded to Java 21 LTS and is ready for modern development and production use.

## Overview

The Comet Way Agent Kernel is a lightweight, extensible framework for building agent-based internet applications. With a small amount of code, developers can create sophisticated distributed systems using the agent programming model.

## Key Features

- **Agent-Based Architecture**: Build applications using autonomous agents that communicate via message passing
- **Service Management**: Built-in service discovery and management system
- **HTTP Server**: Integrated web server with support for dynamic content and extensions
- **Database Integration**: JDBC support with connection pooling
- **Email Capabilities**: Send and receive email through agent-based interfaces
- **Session Management**: Built-in session handling for web applications
- **Extensible**: Plugin architecture for custom functionality

## Java 21 Modernization

This version has been fully modernized for Java 21 LTS, including:

- **Dependency Removal**: Replaced Jakarta-ORO with native java.util.regex
- **JDBC Compatibility**: Updated JDBC interfaces for Java 21
- **Constructor Updates**: Fixed deprecated wrapper constructor warnings
- **Build System**: Updated for OpenJDK 21

## Quick Start

### Prerequisites

- Java 21 LTS (OpenJDK recommended)
- Basic understanding of Java programming

### Building

```bash
cd bin
./compile.sh
```

### Running Examples

```bash
# Run the sample agent
java -cp ../ak.jar:../classes SampleAgent

# Start the web server demo
java -cp ../ak.jar:../classes com.cometway.ak.AK webserver/ak.xstartup
```

## Project Structure

```
├── src/          # Java source code
├── demos/        # Example applications and demos
├── docs/         # Documentation
├── bin/          # Build scripts
└── import/       # External dependencies (legacy)
```

## Version History

| Version | Date | Java Version | Key Features |
|---------|------|--------------|--------------|
| 3.4 | July 2025 | Java 21 | Modernized for Java 21 LTS |
| 3.3 | October 2011 | Java 1.4+ | Final legacy version |
| 3.0 | April 2008 | Java 1.4+ | Initial open source release |

## Architecture

The framework is built around several core components:

- **Agent Kernel**: Central runtime and message routing
- **Service Manager**: Service discovery and lifecycle management
- **Scheduler**: Time-based task execution
- **Web Server**: HTTP request handling and response generation
- **Object Manager**: Persistent object storage and retrieval

## License

This project maintains its original open-source license. See LICENSE.html for details.

## Contributing

This is an active open-source project welcoming contributions! 

### Getting Started
- Check out the `demos/` directory for examples
- Read `CLAUDE.md` for development guidelines
- See `docs/` for architectural details
- Review `RELEASES.md` for recent changes

### Development Areas
- New agent types and extensions
- Performance optimizations  
- Modern Java pattern adoption
- Additional protocol support
- Documentation improvements

## Project History

While this framework has been modernized for Java 21, it maintains its proven architecture from 2008-2011:

- **2008**: Original release with Java 1.4+ compatibility
- **2011**: Version 3.3 with mature agent-based patterns
- **2025**: Version 3.4 - Full Java 21 modernization with maintained API compatibility

This evolution demonstrates the framework's robust design - the core agent-based architecture has proven timeless while the implementation has been updated for modern Java development.