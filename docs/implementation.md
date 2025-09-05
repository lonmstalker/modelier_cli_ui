# Техническая документация по реализации

## Архитектура плагина

### Основные компоненты

```
src/main/kotlin/com/github/lonmstalker/modeliercliui/
├── services/
│   ├── AICliService.kt              # Основной сервис для AI интеграции
│   ├── ClaudeCodeService.kt         # Специфичный сервис для Claude Code
│   ├── CodexService.kt              # Сервис для Codex CLI
│   ├── ExecutionService.kt          # Выполнение CLI команд
│   ├── ConfigurationService.kt      # Управление конфигурацией
│   └── HistoryService.kt            # История взаимодействий
├── toolWindow/
│   ├── AIToolWindow.kt              # Главное окно инструмента
│   ├── AIToolWindowFactory.kt       # Factory для tool window
│   ├── components/                  # UI компоненты
│   └── panels/                      # Панели интерфейса
├── actions/
│   ├── AIActionGroup.kt             # Группа действий для меню
│   ├── ExplainCodeAction.kt         # Объяснение кода
│   ├── GenerateCodeAction.kt        # Генерация кода
│   └── RefactorWithAIAction.kt      # Рефакторинг с AI
├── settings/
│   ├── AIPluginConfigurable.kt      # Настройки плагина
│   ├── AIPluginSettings.kt          # State объект настроек
│   └── AIProviderConfiguration.kt   # Конфигурация AI провайдеров
├── models/
│   ├── AIProvider.kt                # Абстракция AI провайдера
│   ├── AIRequest.kt                 # Модель запроса
│   ├── AIResponse.kt                # Модель ответа
│   └── ExecutionResult.kt           # Результат выполнения
└── utils/
    ├── CommandExecutor.kt           # Утилиты выполнения команд
    ├── FileContextExtractor.kt      # Извлечение контекста файлов
    └── GitIntegrationHelper.kt      # Помощник Git интеграции
```

## Ключевые сервисы

### AICliService

Основной сервис, предоставляющий единый интерфейс для работы с различными AI инструментами.

```kotlin
@Service
class AICliService {
    private val providers = mutableMapOf<String, AIProvider>()
    
    fun registerProvider(name: String, provider: AIProvider)
    fun executeRequest(providerName: String, request: AIRequest): AIResponse
    fun getAvailableProviders(): List<String>
    fun getProviderCapabilities(providerName: String): Set<AICapability>
}
```

### Основные интерфейсы

```kotlin
interface AIProvider {
    fun isAvailable(): Boolean
    fun execute(request: AIRequest): AIResponse
    fun getCapabilities(): Set<AICapability>
    fun validateConfiguration(): ValidationResult
}

enum class AICapability {
    CODE_GENERATION,
    CODE_EXPLANATION,
    CODE_REFACTORING,
    ERROR_FIXING,
    TEST_GENERATION,
    DOCUMENTATION_GENERATION,
    CODE_REVIEW,
    GIT_INTEGRATION
}

data class AIRequest(
    val command: String,
    val context: ProjectContext,
    val parameters: Map<String, Any> = emptyMap()
)

data class AIResponse(
    val success: Boolean,
    val content: String,
    val metadata: Map<String, Any> = emptyMap(),
    val error: String? = null
)
```

## Реализация провайдеров

### Claude Code Provider

```kotlin
class ClaudeCodeProvider : AIProvider {
    companion object {
        private const val CLAUDE_CODE_COMMAND = "claude-code"
    }
    
    override fun isAvailable(): Boolean {
        return CommandExecutor.isCommandAvailable(CLAUDE_CODE_COMMAND)
    }
    
    override fun execute(request: AIRequest): AIResponse {
        val command = buildClaudeCommand(request)
        val result = CommandExecutor.execute(command, request.context.workingDirectory)
        return parseClaudeResponse(result)
    }
    
    private fun buildClaudeCommand(request: AIRequest): List<String> {
        // Построение команды на основе запроса
        val command = mutableListOf(CLAUDE_CODE_COMMAND)
        
        when (request.command) {
            "explain" -> command.addAll(buildExplainCommand(request))
            "generate" -> command.addAll(buildGenerateCommand(request))
            "refactor" -> command.addAll(buildRefactorCommand(request))
            else -> throw UnsupportedOperationException("Unknown command: ${request.command}")
        }
        
        return command
    }
}
```

## Tool Window реализация

### Основной UI компонент

```kotlin
class AIToolWindow(private val project: Project) {
    private val tabbedPane = JBTabbedPane()
    private val historyPanel = HistoryPanel(project)
    private val settingsPanel = QuickSettingsPanel(project)
    
    fun createContent(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // Создание панелей для каждого провайдера
        val providers = AICliService.getInstance().getAvailableProviders()
        providers.forEach { providerName ->
            val panel = createProviderPanel(providerName)
            tabbedPane.addTab(providerName, panel)
        }
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
        mainPanel.add(createStatusPanel(), BorderLayout.SOUTH)
        
        return mainPanel
    }
    
    private fun createProviderPanel(providerName: String): JComponent {
        return when (providerName) {
            "Claude Code" -> ClaudeCodePanel(project)
            "Codex" -> CodexPanel(project)
            else -> GenericAIPanel(project, providerName)
        }
    }
}
```

### Интеграция с редактором

```kotlin
class ExplainCodeAction : AnAction("Explain Code with AI") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return
        
        val context = ProjectContext.fromEditor(editor, project)
        val request = AIRequest(
            command = "explain",
            context = context,
            parameters = mapOf("code" to selectedText)
        )
        
        // Асинхронное выполнение
        ApplicationManager.getApplication().executeOnPooledThread {
            val response = AICliService.getInstance()
                .executeRequest("Claude Code", request)
            
            // Обновление UI в EDT
            ApplicationManager.getApplication().invokeLater {
                showExplanationDialog(response.content)
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabled = hasSelection
    }
}
```

## Система конфигурации

### Settings State

```kotlin
@State(
    name = "AIPluginSettings",
    storages = [Storage("ai-plugin-settings.xml")]
)
class AIPluginSettings : PersistentStateComponent<AIPluginSettings.State> {
    
    data class State(
        var providers: MutableMap<String, ProviderConfig> = mutableMapOf(),
        var defaultProvider: String = "Claude Code",
        var enableHistoryLogging: Boolean = true,
        var maxHistorySize: Int = 1000
    )
    
    data class ProviderConfig(
        var enabled: Boolean = true,
        var apiKey: String = "",
        var customParameters: Map<String, String> = emptyMap(),
        var executable: String = ""
    )
    
    private var myState = State()
    
    override fun getState(): State = myState
    override fun loadState(state: State) {
        myState = state
    }
    
    companion object {
        fun getInstance(): AIPluginSettings = service()
    }
}
```

### Configurable UI

```kotlin
class AIPluginConfigurable : Configurable {
    private var settingsPanel: AISettingsPanel? = null
    
    override fun createComponent(): JComponent {
        settingsPanel = AISettingsPanel()
        return settingsPanel!!.createPanel()
    }
    
    override fun isModified(): Boolean {
        return settingsPanel?.isModified() ?: false
    }
    
    override fun apply() {
        settingsPanel?.apply()
    }
    
    override fun reset() {
        settingsPanel?.reset()
    }
    
    override fun getDisplayName(): String = "AI CLI Integration"
}
```

## Выполнение команд

### Command Executor

```kotlin
object CommandExecutor {
    fun execute(
        command: List<String>, 
        workingDirectory: String? = null,
        timeout: Duration = Duration.ofMinutes(5)
    ): ExecutionResult {
        
        return try {
            val processBuilder = ProcessBuilder(command)
                .directory(workingDirectory?.let { File(it) })
                .redirectErrorStream(true)
            
            val process = processBuilder.start()
            
            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            // Чтение вывода с таймаутом
            val completed = process.waitFor(timeout.seconds, TimeUnit.SECONDS)
            
            reader.useLines { lines ->
                lines.forEach { output.appendLine(it) }
            }
            
            ExecutionResult(
                success = completed && process.exitValue() == 0,
                output = output.toString(),
                exitCode = if (completed) process.exitValue() else -1,
                timeout = !completed
            )
        } catch (e: Exception) {
            ExecutionResult(
                success = false,
                output = "",
                exitCode = -1,
                error = e.message
            )
        }
    }
    
    fun isCommandAvailable(command: String): Boolean {
        return try {
            val result = execute(listOf(command, "--version"))
            result.success
        } catch (e: Exception) {
            false
        }
    }
}

data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int,
    val error: String? = null,
    val timeout: Boolean = false
)
```

## Контекст проекта

### Project Context Extractor

```kotlin
data class ProjectContext(
    val workingDirectory: String,
    val selectedFiles: List<VirtualFile>,
    val currentFile: VirtualFile?,
    val selectedText: String?,
    val gitStatus: GitStatus?,
    val projectStructure: ProjectStructure?
) {
    companion object {
        fun fromEditor(editor: Editor, project: Project): ProjectContext {
            val currentFile = FileDocumentManager.getInstance()
                .getFile(editor.document)
            
            return ProjectContext(
                workingDirectory = project.basePath ?: "",
                selectedFiles = arrayOf(currentFile).filterNotNull(),
                currentFile = currentFile,
                selectedText = editor.selectionModel.selectedText,
                gitStatus = extractGitStatus(project),
                projectStructure = extractProjectStructure(project)
            )
        }
        
        private fun extractGitStatus(project: Project): GitStatus? {
            // Извлечение git статуса проекта
            val gitRepository = GitRepositoryManager.getInstance(project)
                .repositories.firstOrNull()
            
            return gitRepository?.let {
                GitStatus(
                    currentBranch = it.currentBranch?.name,
                    hasUncommittedChanges = !it.isDirty,
                    modifiedFiles = getModifiedFiles(it)
                )
            }
        }
    }
}
```

## Тестирование

### Unit Tests Structure

```kotlin
class AICliServiceTest : LightPlatformTestCase() {
    
    private lateinit var service: AICliService
    private lateinit var mockProvider: AIProvider
    
    override fun setUp() {
        super.setUp()
        service = AICliService()
        mockProvider = mockk<AIProvider>()
    }
    
    @Test
    fun `should register provider successfully`() {
        service.registerProvider("test", mockProvider)
        assertTrue(service.getAvailableProviders().contains("test"))
    }
    
    @Test
    fun `should execute request with correct provider`() {
        every { mockProvider.execute(any()) } returns AIResponse(
            success = true,
            content = "Test response"
        )
        
        service.registerProvider("test", mockProvider)
        val request = AIRequest("test_command", mockk())
        val response = service.executeRequest("test", request)
        
        assertTrue(response.success)
        assertEquals("Test response", response.content)
        verify { mockProvider.execute(request) }
    }
}
```

## Deployment и Distribution

### plugin.xml Configuration

```xml
<idea-plugin>
    <id>com.github.lonmstalker.ai-cli-integration</id>
    <name>AI CLI Integration</name>
    <vendor>LongStalker</vendor>
    
    <description><![CDATA[
        Интеграция с популярными AI инструментами командной строки
    ]]></description>
    
    <depends>com.intellij.modules.platform</depends>
    <depends>Git4Idea</depends>
    
    <extensions defaultExtensionNs="com.intellij">
        <toolWindow 
            id="AI CLI" 
            secondary="false" 
            icon="/icons/ai-tool.svg" 
            anchor="right" 
            factoryClass="com.github.lonmstalker.modeliercliui.toolWindow.AIToolWindowFactory"/>
        
        <applicationConfigurable 
            parentId="tools" 
            instance="com.github.lonmstalker.modeliercliui.settings.AIPluginConfigurable"/>
        
        <applicationService 
            serviceImplementation="com.github.lonmstalker.modeliercliui.services.AICliService"/>
    </extensions>
    
    <actions>
        <group id="AIActions" text="AI Actions" popup="true">
            <action id="ExplainCode" 
                    class="com.github.lonmstalker.modeliercliui.actions.ExplainCodeAction"/>
            <action id="GenerateCode" 
                    class="com.github.lonmstalker.modeliercliui.actions.GenerateCodeAction"/>
            <add-to-group group-id="EditorPopupMenu" anchor="last"/>
        </group>
    </actions>
</idea-plugin>
```

### Build Configuration

Обновления для `build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

intellij {
    plugins.set(listOf("Git4Idea"))
}
```

## Безопасность и лучшие практики

### API Keys Management

```kotlin
class SecureCredentialStorage {
    companion object {
        private const val CREDENTIAL_SUBSYSTEM = "AI_CLI_PLUGIN"
        
        fun storeApiKey(providerName: String, apiKey: String) {
            val credentialAttributes = CredentialAttributes(
                generateServiceName(CREDENTIAL_SUBSYSTEM, providerName)
            )
            PasswordSafe.instance.setPassword(credentialAttributes, apiKey)
        }
        
        fun getApiKey(providerName: String): String? {
            val credentialAttributes = CredentialAttributes(
                generateServiceName(CREDENTIAL_SUBSYSTEM, providerName)
            )
            return PasswordSafe.instance.getPassword(credentialAttributes)
        }
    }
}
```

### Error Handling

```kotlin
class AIOperationHandler {
    fun executeWithErrorHandling(operation: () -> AIResponse): AIResponse {
        return try {
            operation()
        } catch (e: Exception) {
            logger.error("AI operation failed", e)
            
            when (e) {
                is CommandNotFoundException -> AIResponse(
                    success = false,
                    content = "",
                    error = "AI tool not found. Please install and configure the tool."
                )
                is TimeoutException -> AIResponse(
                    success = false,
                    content = "",
                    error = "Operation timed out. Try with a smaller request."
                )
                else -> AIResponse(
                    success = false,
                    content = "",
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }
}
```