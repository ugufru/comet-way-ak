# Demo Guide - Comet Way Agent Kernel

This guide provides instructions for running the various demos included with the Comet Way Agent Kernel.

## Prerequisites

1. **Java 21 LTS** or higher installed
2. **Compiled framework**: Run `cd bin && ./compile.sh` to build the project
3. **Working directory**: Run demos from the project root directory

## Basic Demo Execution

The Agent Kernel can run demos in several ways:

### Method 1: Direct Classname (Simplest)
```bash
java -cp ak.jar:classes com.cometway.ak.AK <ClassName>
```

### Method 2: Startup File (For Complex Demos)
```bash
java -cp ak.jar:classes com.cometway.ak.AK <path/to/ak.xstartup>
```

---

## Available Demos

### ✅ Hello World
**Location**: `demos/hello_world/`
**Description**: Classic "Hello, world!" example demonstrating basic agent structure.

**How to run**:
```bash
java -cp ak.jar:classes com.cometway.ak.AK HelloWorldAgent
```

**Expected output**:
```
[AK] Comet Way Agent Kernel 3.4 Final 07-23-2025 - Java 21.
[000_AgentKernel] Starting on 2025/07/23 22:25:08.609 EDT
[000_AgentKernel] Creating agent HelloWorldAgent
[HelloWorldAgent] Hello, world!
```

---

## Demo Categories

### 🏗️ Core Framework Demos
- **hello_world** - Basic agent example
- **sample_agent** - Comprehensive agent features demonstration
- **system_properties** - System information agent

### 🌐 Web Server Demos  
- **webserver** - HTTP server with dynamic content
- **sessions** - Web session management

### 📧 Email Demos
- **email** - Email sending and receiving agents

### 🗄️ Database Demos
- **jdbc** - Database connectivity examples

### 🔄 Advanced Features
- **biolife** - Complex multi-agent simulation
- **monitors** - System monitoring agents
- **scheduler** - Time-based agent execution
- **replicator** - Agent replication patterns

---

## Demo Status

| Demo | Status | Notes |
|------|--------|-------|
| hello_world | ✅ Tested | Working with Java 21 |
| sample_agent | ⏳ Pending | Needs testing |
| webserver | ⏳ Pending | Needs testing |
| email | ⏳ Pending | Needs testing |
| jdbc | ⏳ Pending | Needs testing |
| biolife | ⏳ Pending | Needs testing |
| monitors | ⏳ Pending | Needs testing |
| scheduler | ⏳ Pending | Needs testing |
| sessions | ⏳ Pending | Needs testing |
| replicator | ⏳ Pending | Needs testing |

---

## Troubleshooting

### Common Issues

1. **ClassNotFoundException**: Ensure you're running from the project root directory
2. **Compilation errors**: Run `cd bin && ./compile.sh` to rebuild
3. **Java version**: Verify you're using Java 21+ with `java -version`

### Classpath Requirements
Always include both `ak.jar` and `classes` in your classpath:
```bash
-cp ak.jar:classes
```

### Getting Help
- Check `CLAUDE.md` for development guidelines
- Review original documentation in `docs/` directory
- Look at demo source code for implementation details

---

## Writing Your Own Demos

### Basic Agent Structure
```java
import com.cometway.ak.*;

public class MyAgent extends Agent {
    public void start() {
        println("My agent is starting!");
        // Your agent logic here
    }
}
```

### Compilation
```bash
cd bin && ./compile.sh
```

### Execution
```bash
java -cp ak.jar:classes com.cometway.ak.AK MyAgent
```

---

*For more detailed documentation, see the [tutorial](docs/tutorial/) directory and [README.md](README.md)*