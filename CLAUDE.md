# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-module AI CLI Integration plugin project. The main goal is to provide integration with popular AI command-line tools (Claude Code, GitHub Copilot CLI, etc.) directly within IntelliJ IDEA and other IDEs. The project is built using Kotlin with Java 21 and utilizes Gradle's multi-project structure.

**Root project name**: `ai-cli-integration`

## Multi-Module Architecture

The project consists of several modules:
- **`:core`** - Core interfaces and models for AI integration (`com.github.lonmstalker.aiintegration.core`)
- **`:jetbrains-extension`** - IntelliJ Platform plugin implementation
- **`:vscode-extension`** - VS Code extension (planned)
- **`:web-ui`** - Web-based interface (planned)
- **`:desktop-ui`** - Desktop UI application (planned)

## Development Commands

### Build and Development
- `./gradlew buildPlugin` - Builds the IntelliJ plugin and creates a ZIP distribution
- `./gradlew runIde` - Runs IntelliJ IDEA with the plugin loaded for testing
- `./gradlew runIdeForUiTests` - Runs IDE with UI testing configuration and robot server
- `./gradlew build` - Builds all modules
- `./gradlew :core:build` - Builds only the core module
- `./gradlew :jetbrains-extension:build` - Builds only the JetBrains extension

### Testing
- `./gradlew test` - Runs unit tests for all modules
- `./gradlew :core:test` - Runs tests for core module only
- `./gradlew :jetbrains-extension:test` - Runs tests for JetBrains extension only
- `./gradlew check` - Runs tests and verification tasks (includes linting and code quality checks)

### Code Quality
- `./gradlew verifyPlugin` - Verifies plugin structure and compatibility
- Qodana inspections are configured and run via GitHub Actions
- Kover is configured for code coverage reporting

### Plugin Management
- `./gradlew publishPlugin` - Publishes plugin to JetBrains Marketplace (requires tokens)
- `./gradlew patchChangelog` - Updates changelog for release

## Core Architecture

The core module defines the main abstractions:

### Key Interfaces
- **`AIProvider`** - Main interface for all AI providers (Claude Code, GitHub Copilot, etc.)
- **`SystemIntegration`** - Platform-specific system integration
- **`ProjectContextProvider`** - Provides project context for AI requests
- **`VectorDatabase`** - Interface for vector storage and retrieval
- **`MultiModelOrchestrator`** - Coordinates multiple AI models

### Core Models
- **`AIRequest`** / **`AIResponse`** - Request/response models for AI communication
- **`ProjectContext`** - Project metadata and code context
- **`AICapability`** - Enum of AI provider capabilities
- **`Configuration`** - Plugin configuration models

## JetBrains Extension Structure

Located in `jetbrains-extension/src/main/kotlin/com/github/lonmstalker/aiintegration/jetbrains/`:

```
├── actions/
│   ├── ExplainCodeAction.kt
│   ├── GenerateCodeAction.kt
│   ├── RefactorCodeAction.kt
│   ├── ReviewCodeAction.kt
│   └── ShowAIToolWindowAction.kt
├── settings/
│   └── AIPluginConfigurable.kt
├── startup/
│   └── AIPluginStartupActivity.kt
└── toolwindow/
    └── AIToolWindowFactory.kt
```

### Plugin Configuration
- **Plugin ID**: `com.github.lonmstalker.aiintegration.jetbrains`
- **Dependencies**: `com.intellij.modules.platform`, `Git4Idea`
- **Target Platform**: IntelliJ Community (IC) 2024.3.6+
- **Since Build**: 243

### Action System Integration
- **Keyboard Shortcuts**: 
  - `Ctrl+Alt+E` - Explain Code
  - `Ctrl+Alt+G` - Generate Code
  - `Ctrl+Alt+R` - Refactor Code
  - `Ctrl+Alt+H` - Show AI Tool Window
- **Context Menus**: Available in Editor, Project View, and Tools menu

## Key Configuration Files

- **Root `build.gradle.kts`** - Multi-module build configuration
- **`settings.gradle.kts`** - Module declarations and project structure
- **`gradle.properties`** - Plugin metadata, versions, and Gradle optimization settings
- **`jetbrains-extension/src/main/resources/META-INF/plugin.xml`** - Plugin descriptor
- **`gradle/libs.versions.toml`** - Version catalog for dependencies

## AI Provider Integration Pattern

When working with AI providers, follow this pattern:
1. Implement the `AIProvider` interface in the core module
2. Use `AIRequest` and `AIResponse` models for communication
3. Handle streaming responses via `Flow<AIResponseChunk>`
4. Validate configurations using `ValidationResult`
5. Provide usage statistics through `UsageStats`

## Development Notes

- **Multi-module setup**: Use Gradle's composite build features
- **Kotlin JVM toolchain**: Java 21 is required
- **Configuration cache**: Enabled for build performance
- **Gradle build cache**: Enabled for faster builds
- **Kotlin stdlib**: Explicitly excluded from plugin bundling
- **Internationalization**: Russian and English locales supported via resource bundles
- **Code coverage**: Configured with Kover and CodeCov integration