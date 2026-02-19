# Wheel3

A Java automation framework for browser, desktop, and API testing — powered by Chrome DevTools Protocol (CDP), Windows UI Automation, and SikuliX. Includes an MCP server for AI-driven browser control.

[![CI & Publish](https://github.com/aji4032/Wheel3/actions/workflows/ci.yml/badge.svg)](https://github.com/aji4032/Wheel3/actions/workflows/ci.yml)

## Features

- **Browser Automation** — Direct CDP (Chrome DevTools Protocol) integration, no WebDriver required
- **MCP Server** — JSON-RPC 2.0 stdio server for AI tools (Claude Desktop, Cursor) to control browsers
- **Desktop Automation** — Windows UI Automation via MS UIAutomation API
- **Image-Based Automation** — SikuliX pattern matching for visual element interaction
- **REST API Client** — Apache HttpClient wrapper for API testing
- **Utilities** — JSON parsing (Jackson), Excel, PDF, file operations, logging, and ExtentReports integration

## Requirements

- **Java 21+**
- **Maven 3.8+**
- **Windows** (required for desktop automation features)
- **Chromium-based browser** (for CDP features)

## Installation

### From GitHub Packages

Add the repository and dependency to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/aji4032/Wheel3</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>io.github.aji4032</groupId>
    <artifactId>Wheel3</artifactId>
    <version>LATEST</version>
  </dependency>
</dependencies>
```

### Build from Source

```bash
git clone https://github.com/aji4032/Wheel3.git
cd Wheel3
mvn clean package
```

## Project Structure

```
src/main/java/
├── cdphandler/        # CDP browser automation engine
│   ├── CdpDriver      # Browser driver (navigation, windows, tabs, screenshots)
│   ├── CdpElement      # Element interactions (click, type, drag, scroll)
│   ├── CdpClient       # WebSocket client for CDP communication
│   ├── CdpUtility      # Low-level CDP command execution
│   ├── OllamaUtility   # AI-powered action planning via Ollama LLM
│   └── ApiInterceptor   # Network request/response interception
├── mcp/               # MCP server for AI tool integration
│   ├── BrowserMcpServer    # JSON-RPC 2.0 stdio entry point
│   ├── McpToolDispatcher   # Routes tool calls to CdpDriver/CdpElement
│   └── McpResponse         # Response formatting
├── automationTools/   # Windows desktop automation
│   └── desktop/
│       ├── DesktopBy       # Locator builder (automationId, name, className, controlType)
│       └── DesktopDriver   # Desktop element interaction
├── sikuli/            # Image-based automation
│   ├── SikuliActions   # Click, type, wait, drag via image patterns
│   └── SikuliFactory   # Screen/region factory
├── apachehttpclient/  # REST API testing
│   ├── APIExecutor     # GET/POST/PUT/DELETE executor
│   └── ResponseObject  # Response wrapper
└── tools/             # Shared utilities
    ├── JSONParser           # JSON parsing (Jackson)
    ├── FileUtilities        # File, ZIP, PDF, Base64 operations
    ├── ExcelUtilities       # Excel read/write
    ├── Log                  # Log4j + ExtentReports logging
    ├── ExtentManager        # ExtentReports singleton
    ├── ExtentTestNGListener # TestNG lifecycle listener
    ├── CommandLineExecutor  # OS command execution
    ├── PropertyFileHandler  # Properties file reader
    ├── HTMLParser           # HTML parsing with dom4j
    ├── Checksum             # MD5/SHA checksums
    └── Utilities            # Wait/retry helpers
```

## Quick Start

### Browser Automation

```java
// Launch browser and navigate
CdpDriver driver = new CdpDriver("ws://localhost:9222/devtools/page/...");
driver.get("https://google.com");

// Find and interact with elements
ICdpElement searchBox = driver.findElement(CdpBy.cssSelector("input[name='q']"));
searchBox.sendKeys("Wheel3 automation");
searchBox.findElement(CdpBy.xpath("//button[@type='submit']")).click();

// Screenshots
String base64Screenshot = driver.captureScreenshot();

driver.close();
```

### TestNG Execution

```bash
# Execute testng.xml
java -cp desktop-ui-automation-1.1.0.18.1-jar-with-dependencies.jar org.testng.TestNG src/test/resources/testng.xml
```

### MCP Server (AI Integration)
```bash
# Start the MCP stdio server
java -jar desktop-ui-automation-1.1.0.18.1-jar-with-dependencies.jar ws://localhost:9222/devtools/page/...
```
Configure in Claude Desktop or Cursor's MCP settings to enable AI-driven browser control.

### Desktop Automation
```java
DesktopBy notepad = new DesktopBy();
notepad.setName("Untitled - Notepad");
notepad.setControlType(DesktopBy.ControlType.Window);
```

### API Testing
```java
ResponseObject response = APIExecutor.get("https://api.example.com/users");
String body = response.getResponseBody();
```

## CI/CD
The project uses GitHub Actions (`.github/workflows/ci.yml`) with:

- **Build** — Runs on every push and PR to `main`
- **Publish** — Deploys to GitHub Packages on push to `main` or manual trigger
- **Manual dispatch** — Trigger a publish anytime from the Actions tab

## Tech Stack

| Category | Library | Version |
|----------|---------|---------|
| Language | Java | 21 |
| Browser Automation | Chrome DevTools Protocol | Direct WebSocket |
| Desktop Automation | MS UI Automation (mmarquee) | 0.7.0 |
| Image Automation | SikuliX | 2.0.5 |
| HTTP Client | OkHttp | 5.3.2 |
| REST Client | Apache HttpClient | 4.5.14 |
| JSON | Jackson | 2.21.0 |
| XML/HTML | dom4j | 2.2.0 |
| Logging | Log4j 2 | 2.25.3 |
| Reporting | ExtentReports | 5.1.2 |
| Testing | TestNG | 7.12.0 |

## License

See [LICENSE](LICENSE) for details.