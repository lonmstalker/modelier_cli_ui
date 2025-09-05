# Руководство по настройке

## Установка и первоначальная настройка

### Требования

**Системные требования:**
- IntelliJ IDEA 2024.3.6+ (Community или Ultimate)
- Java 21+
- Git (для интеграции с системой контроля версий)

**AI инструменты (необходимо установить отдельно):**
- Claude Code CLI
- Codex CLI (опционально)
- GitHub Copilot CLI (опционально)

### Установка AI инструментов

#### Claude Code CLI

```bash
# macOS/Linux
curl -fsSL https://claude.ai/install.sh | sh

# Windows (PowerShell)
Invoke-RestMethod https://claude.ai/install.ps1 | Invoke-Expression

# Проверка установки
claude-code --version
```

#### Codex CLI (если планируется использование)

```bash
# npm install
npm install -g @openai/codex-cli

# Проверка установки
codex --version
```

#### GitHub Copilot CLI

```bash
# GitHub CLI extension
gh extension install github/gh-copilot

# Проверка установки
gh copilot --version
```

## Настройка плагина

### Базовая конфигурация

1. **Открыть настройки IDE:**
   - `File → Settings` (Windows/Linux)
   - `IntelliJ IDEA → Preferences` (macOS)

2. **Перейти к настройкам плагина:**
   - `Tools → AI CLI Integration`

3. **Настроить провайдеры AI:**

### Конфигурация Claude Code

```yaml
# Настройки провайдера
Provider: Claude Code
Status: ✓ Available
Executable Path: /usr/local/bin/claude-code
API Key: [Настроить в Credentials]

# Расширенные параметры
Default Model: claude-3-sonnet
Timeout: 300 seconds
Max Context Size: 100000 tokens
Temperature: 0.1
```

**Настройка API ключа:**

1. Получите API ключ на [claude.ai](https://claude.ai)
2. В настройках плагина нажмите "Configure API Key"
3. Введите ключ - он будет сохранен в безопасном хранилище IDE

### Конфигурация дополнительных провайдеров

#### Codex Provider

```yaml
Provider: OpenAI Codex
Status: ✓ Available
Executable Path: /usr/local/bin/codex
API Key: [OpenAI API Key]

# Специфичные параметры Codex
Model: code-davinci-002
Max Tokens: 2048
Temperature: 0.0
Top P: 1.0
```

#### GitHub Copilot

```yaml
Provider: GitHub Copilot
Status: ✓ Available
Executable Path: gh copilot
Authentication: GitHub CLI

# Настройки
Suggestions Mode: inline
Auto-trigger: true
Language Filter: all
```

## Настройка рабочего пространства

### Tool Window Configuration

**Расположение окна:**
- По умолчанию: правая панель
- Альтернативы: левая панель, нижняя панель, floating window

**Вкладки и панели:**
```
┌─ AI CLI Integration ─────────────────┐
│ [Claude Code] [Codex] [Copilot] [+] │
├─────────────────────────────────────┤
│ Input Panel                         │
│ ┌─────────────────────────────────┐ │
│ │ Enter your request...           │ │
│ └─────────────────────────────────┘ │
│ [Send] [Clear] [History] [Settings] │
├─────────────────────────────────────┤
│ Output Panel                        │
│ ┌─────────────────────────────────┐ │
│ │ AI response appears here...     │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Status: Ready | Model: Claude-3.5   │
└─────────────────────────────────────┘
```

### Editor Integration Settings

**Контекстные действия:**
- `Right-click → AI Actions → Explain Code`
- `Right-click → AI Actions → Generate Code`
- `Right-click → AI Actions → Refactor with AI`
- `Right-click → AI Actions → Generate Tests`

**Горячие клавиши (по умолчанию):**
```
Ctrl+Alt+E  - Explain selected code
Ctrl+Alt+G  - Generate code
Ctrl+Alt+R  - Refactor with AI
Ctrl+Alt+T  - Generate tests
Ctrl+Alt+H  - Show AI tool window
```

## Расширенная конфигурация

### Custom Prompts

Создание собственных шаблонов запросов:

```yaml
Custom Prompts:
  - name: "Code Review"
    template: "Review this code for potential issues: {selected_code}"
    provider: "Claude Code"
    
  - name: "Performance Analysis"  
    template: "Analyze performance and suggest optimizations: {selected_code}"
    provider: "Codex"
    
  - name: "Security Check"
    template: "Check for security vulnerabilities: {selected_code}"
    provider: "Claude Code"
```

### Project-Specific Settings

**.idea/ai-cli-integration.xml** (автоматически создается):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="AICliIntegrationSettings">
    <option name="defaultProvider" value="Claude Code" />
    <option name="enableAutoContext" value="true" />
    <option name="includeGitStatus" value="true" />
    <option name="maxHistorySize" value="1000" />
    <option name="customPrompts">
      <list>
        <option name="name" value="Project Specific Review" />
        <option name="template" value="Review for our Java Spring project: {code}" />
      </list>
    </option>
  </component>
</project>
```

### Context Configuration

**Автоматическая передача контекста:**

```yaml
Context Settings:
  Include Current File: true
  Include Selected Text: true
  Include Git Status: true
  Include Project Structure: false  # может быть медленно
  Max Context Size: 50KB
  
Git Integration:
  Include Current Branch: true
  Include Modified Files: true
  Include Commit History: false
  Max Diff Size: 10KB
```

## Настройка безопасности

### Управление учетными данными

**API ключи хранятся в:**
- Windows: Windows Credential Store
- macOS: Keychain
- Linux: Secret Service (GNOME Keyring/KWallet)

**Настройки конфиденциальности:**

```yaml
Privacy Settings:
  Log Requests: false  # не логировать чувствительные данные
  Cache Responses: true
  Include File Paths: false  # скрывать пути файлов
  Filter Sensitive Data: true
  
Data Filtering:
  - API Keys
  - Passwords  
  - Personal Information
  - Internal URLs
  - Database Credentials
```

### Network Configuration

**Proxy Settings:**

```yaml
Network:
  Use System Proxy: true
  Custom Proxy:
    HTTP Proxy: proxy.company.com:8080
    HTTPS Proxy: proxy.company.com:8080
    Auth Required: true
    Username: [encrypted]
    Password: [encrypted]
  
SSL/TLS:
  Verify Certificates: true
  Custom CA Bundle: /path/to/ca-bundle.crt
```

## Производительность и оптимизация

### Настройки кеширования

```yaml
Performance:
  Enable Caching: true
  Cache Duration: 24h
  Max Cache Size: 100MB
  Cache Location: ~/.ai-cli-cache
  
Request Optimization:
  Batch Similar Requests: true
  Compress Large Payloads: true
  Request Timeout: 30s
  Max Concurrent Requests: 3
```

### Resource Management

```yaml
Resources:
  Max Memory Usage: 512MB
  Background Thread Pool: 4
  UI Update Frequency: 100ms
  History Cleanup: weekly
  
Logging:
  Level: INFO
  File Location: logs/ai-cli-integration.log
  Max File Size: 10MB
  Rotation: daily
```

## Диагностика и устранение неисправностей

### Проверка конфигурации

**Diagnostic Panel** (доступна в настройках):

```
✓ Claude Code CLI found at /usr/local/bin/claude-code
✓ API key configured and valid
✓ Network connectivity OK
✗ Codex CLI not found (optional)
⚠ Git integration partially available

Health Check Results:
- Claude Code: Response time 1.2s ✓
- Codex: Not configured ⚠
- Network: Latency 45ms ✓
- Permissions: Read/Write OK ✓
```

### Общие проблемы и решения

**Claude Code не найден:**
```bash
# Проверить установку
which claude-code
claude-code --version

# Переустановка
curl -fsSL https://claude.ai/install.sh | sh
```

**Проблемы с API ключом:**
1. Проверить валидность ключа на claude.ai
2. Пересоздать ключ
3. Очистить кеш учетных данных: `Settings → Passwords → Clear AI CLI credentials`

**Медленная работа:**
1. Увеличить timeout в настройках
2. Уменьшить размер контекста
3. Отключить автоматическую передачу git статуса

**Проблемы с прокси:**
1. Проверить настройки прокси в IDE
2. Добавить исключения для AI API endpoints
3. Настроить аутентификацию прокси

### Логирование и отладка

**Включение debug логов:**

```yaml
Logging Configuration:
  com.github.lonmstalker.ai-cli: DEBUG
  com.intellij.openapi.diagnostic: INFO
  
Log Locations:
  IDE Log: ~/Library/Logs/JetBrains/IntelliJIdea2024.3/idea.log
  Plugin Log: ~/.ai-cli-integration/debug.log
```

**Экспорт конфигурации:**

Для поддержки можно экспортировать настройки:
`Settings → AI CLI Integration → Export Configuration`

## Обновления и миграция

### Автоматические обновления

```yaml
Updates:
  Check for Updates: automatically
  Update Channel: stable  # или beta
  Backup Settings: true
  
Migration:
  Preserve Settings: true
  Import Previous Config: true
  Validate After Update: true
```

### Backup конфигурации

Важные файлы для резервного копирования:
- `.idea/ai-cli-integration.xml` - настройки проекта
- Настройки IDE (включая credentials)
- Custom prompts и шаблоны