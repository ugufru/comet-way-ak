# Comet Way Agent Kernel

**Version 3.4 - Java 21 Edition**  
**Build 07-23-2025**

## Overview

Thank you for downloading this release of the Comet Way Agent Kernel.

We have overwhelmingly found that agent-based application design is an effective pattern for the creation of any kind of software. This includes autonomous server applications, web-based applications, embedded software, and interactive GUIs. Virtually any type of application can be written using the Comet Way Agent Kernel.

## Key Features

- **Dynamic Component Software Model** - Implements an agent-based architecture specifically designed for deploying *Agent Applications*
- **Interchangeable Components** - Link up agent-based components to create robust applications
- **Multi-Protocol Support** - Includes agents for web (HTTP/HTTPS), email (SMTP/POP3), database (JDBC/XML), and GUI (Swing) services
- **Command Line Friendly** - Designed for the command line; easily enhanced by your favorite IDE
- **Mature Framework** - A proven framework for *Agent Programming*
- **Java-Based** - Write customized agents using the Java Programming Language
- **Modern Java Support** - Fully modernized for Java 21 LTS with excellent compatibility

## Java Version Compatibility

### Minimum Requirements
- **Minimum JDK**: Java 11+ (LTS)
- **Recommended**: Java 17+ (LTS) 
- **Fully Tested**: Java 21+ (LTS)

### Supported JVMs
- OpenJDK 11+ (recommended)
- Oracle JDK 11+
- Any Java 11+ compliant JVM

> **Note**: While the codebase may work with Java 8, it has not been tested and Java 11+ is strongly recommended for full compatibility with all modern JDBC features and performance optimizations.

## Version 3.4 - Java 21 Modernization (Complete)

This major release (following version 3.3 from 2011) includes comprehensive modernization improvements:

- ✅ **Java 21 Support** - Upgraded from Java 1.4+ to Java 21 LTS
- ✅ **Modern Regex** - Replaced deprecated Jakarta-ORO with java.util.regex
- ✅ **JDBC Compatibility** - Updated JDBC interfaces for modern Java versions
- ✅ **Build System** - Updated compilation scripts for Java 21 toolchain
- ✅ **Code Modernization** - Fixed deprecated APIs and constructor patterns
- ✅ **Reduced Warnings** - Compilation warnings reduced from 41 to 6 (85% reduction)
- ✅ **Performance** - Benefits from 20+ years of JVM improvements and modern caching

## Build and Development

### Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd comet-way-ak

# Compile the project
cd bin && ./compile.sh

# Full build with documentation
cd bin && ./build.sh
```

### Build Commands

| Command | Description |
|---------|-------------|
| `cd bin && ./compile.sh` | **Compile** - Fastest option for development |
| `cd bin && ./build.sh` | **Full Build** - Compile + finalize + documentation |
| `cd bin && ./finalize.sh` | **Package** - Create distribution package |
| `cd bin && ./document.sh` | **Documentation** - Generate Javadocs |

### Build Requirements

The build system automatically configures Java 21:
```bash
export PATH="/usr/local/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/usr/local/opt/openjdk@21"
```

## Modernization Achievements

The comprehensive modernization effort included:

- ✅ **Complete removal of Jakarta-ORO dependency**
- ✅ **Migration to java.util.regex Pattern/Matcher API**
- ✅ **Updated JDBC interface implementations** 
- ✅ **Fixed deprecated wrapper constructors** (Integer, Boolean, etc.)
- ✅ **Added proper @Deprecated annotations**
- ✅ **Comprehensive warning reduction** (41 → 6 warnings)
- ✅ **Removed deprecated ThreadGroup.destroy() usage**
- ✅ **Enhanced build scripts for modern Java**

## Version History

| Version | Release Date | Java Version | Key Features |
|---------|--------------|--------------|--------------|
| **3.4** | **July 2025** | **Java 21** | **Complete Java 21 modernization, Jakarta-ORO removal, warning reduction** |
| 3.3 | October 2011 | Java 1.4+ | Feature updates and bug fixes |
| 3.0 | April 2008 | Java 1.4+ | Initial open source release |

> **Version 3.4** represents a **14-year leap forward** from version 3.3, bringing the codebase from 2011-era Java 1.4+ to modern Java 21 standards.

## Architecture Overview

The framework centers around the **Agent** abstraction - self-contained components that can be dynamically created, configured, started, stopped, and destroyed. 

### Core Components

- **`com.cometway.ak`** - Agent Kernel core (Agent, AgentKernel, ServiceManager)
- **`com.cometway.props`** - Property management system (Props, PropsList, PropsContainer)
- **`com.cometway.httpd`** - HTTP server agents (WebServer, WebServerExtension)
- **`com.cometway.jdbc`** - Database connectivity agents (JDBCAgent, JDBCConnection)
- **`com.cometway.email`** - Email handling agents (SMTP/POP3)
- **`com.cometway.util`** - Utilities (ThreadPool, StringTools, jGrep)
- **`com.cometway.om`** - Object Management (persistence, sessions)

### Agent Lifecycle

```
CREATING → STOPPED → STARTING → RUNNING → STOPPING → DESTROYING → DESTROYED
                     ↑              ↓
                   (restart)    (FAILED)
```

## Documentation

Complete details and documentation available at [www.agentkernel.com](http://www.agentkernel.com)

## License

The Comet Way Agent Kernel is released under our own open source license. See [LICENSE.html](LICENSE.html) for details.

## Contact

**Comet Way, Inc**  
4551 Forbes Ave  
Pittsburgh, Pennsylvania 15213  
USA  

📞 412-682-5282  
📧 [support@cometway.com](mailto:support@cometway.com)

---

*Copyright © 1999-2008, Comet Way, Inc.*  
*Java 21 modernization 2025.*  
*All rights reserved.*