package com.github.lonmstalker.aiintegration.core.ui

/**
 * Абстрактный интерфейс для реализации чата на разных платформах.
 * Обеспечивает единообразное API для всех UI реализаций
 * (IntelliJ IDEA, VS Code, Web, Desktop).
 * 
 * @author AI CLI Integration Team
 * @since 0.0.1
 */
interface ChatUIProvider {
    
    /**
     * Инициализирует UI чата с заданной конфигурацией.
     * Должен быть вызван перед использованием любых других методов.
     * 
     * @param config конфигурация пользовательского интерфейса
     * @throws IllegalStateException если провайдер уже инициализирован
     */
    fun initialize(config: ChatUIConfig)
    
    /**
     * Добавляет новое сообщение в чат.
     * Автоматически обновляет UI и, при необходимости, прокручивает к концу.
     * 
     * @param message сообщение для добавления
     */
    fun addMessage(message: ChatMessage)
    
    /**
     * Добавляет несколько сообщений за одну операцию.
     * Более эффективно чем множественные вызовы addMessage.
     * 
     * @param messages список сообщений для добавления
     */
    fun addMessages(messages: List<ChatMessage>) {
        messages.forEach { addMessage(it) }
    }
    
    /**
     * Очищает всю историю сообщений чата.
     * После очистки может быть показано приветственное сообщение.
     */
    fun clearMessages()
    
    /**
     * Управляет отображением индикатора печати ассистента.
     * 
     * @param isVisible true для показа индикатора, false для скрытия
     */
    fun setTypingIndicator(isVisible: Boolean)
    
    /**
     * Устанавливает текст в поле ввода пользователя.
     * 
     * @param text новый текст для поля ввода
     */
    fun setInputText(text: String)
    
    /**
     * Получает текущий текст из поля ввода пользователя.
     * 
     * @return текущий текст в поле ввода
     */
    fun getInputText(): String
    
    /**
     * Активирует или деактивирует весь интерфейс чата.
     * В деактивированном состоянии пользователь не может отправлять сообщения.
     * 
     * @param enabled true для активации, false для деактивации
     */
    fun setEnabled(enabled: Boolean)
    
    /**
     * Устанавливает обработчик событий чата.
     * Все пользовательские действия будут переданы в этот обработчик.
     * 
     * @param handler функция для обработки событий чата
     */
    fun setEventHandler(handler: (ChatEvent) -> Unit)
    
    /**
     * Прокручивает область сообщений к самому последнему сообщению.
     */
    fun scrollToBottom()
    
    /**
     * Прокручивает к определенному сообщению по его ID.
     * 
     * @param messageId идентификатор сообщения
     * @return true если сообщение найдено и прокрутка выполнена
     */
    fun scrollToMessage(messageId: String): Boolean
    
    /**
     * Обновляет конфигурацию UI без перезапуска компонента.
     * Изменения применяются немедленно.
     * 
     * @param config новая конфигурация
     */
    fun updateConfig(config: ChatUIConfig)
    
    /**
     * Получает текущую конфигурацию UI.
     * 
     * @return текущая конфигурация
     */
    fun getCurrentConfig(): ChatUIConfig
    
    /**
     * Получает текущее состояние чата.
     * Полезно для сохранения состояния между сессиями.
     * 
     * @return текущее состояние чата
     */
    fun getCurrentState(): ChatState
    
    /**
     * Восстанавливает состояние чата из сохраненных данных.
     * 
     * @param state состояние для восстановления
     */
    fun restoreState(state: ChatState)
    
    /**
     * Устанавливает фокус на поле ввода.
     */
    fun focusInput()
    
    /**
     * Проверяет, инициализирован ли провайдер.
     * 
     * @return true если провайдер инициализирован и готов к использованию
     */
    fun isInitialized(): Boolean
    
    /**
     * Экспортирует историю чата в указанном формате.
     * 
     * @param format формат экспорта ("markdown", "json", "txt")
     * @return содержимое чата в выбранном формате
     */
    fun exportChat(format: String = "markdown"): String
    
    /**
     * Освобождает все ресурсы, используемые провайдером.
     * После вызова dispose() провайдер становится непригодным для использования.
     */
    fun dispose()
}

/**
 * Базовая реализация ChatUIProvider с общей функциональностью.
 * Платформо-специфичные реализации могут наследоваться от этого класса.
 */
abstract class BaseChatUIProvider : ChatUIProvider {
    
    protected var config: ChatUIConfig = ChatUIConfig()
    protected var chatEventHandler: ((ChatEvent) -> Unit)? = null
    protected var initialized: Boolean = false
    protected val messages: MutableList<ChatMessage> = mutableListOf()
    
    override fun initialize(config: ChatUIConfig) {
        check(!initialized) { "ChatUIProvider is already initialized" }
        this.config = config.constrain()
        this.initialized = true
        initializePlatformSpecific()
    }
    
    override fun getCurrentConfig(): ChatUIConfig = config
    
    override fun getCurrentState(): ChatState {
        return ChatState(
            messages = messages.toList(),
            isTyping = false, // Должно быть переопределено в реализации
            isEnabled = true,  // Должно быть переопределено в реализации
            currentInput = getInputText(),
            selectedProvider = null // Должно быть переопределено в реализации
        )
    }
    
    override fun isInitialized(): Boolean = initialized
    
    override fun exportChat(format: String): String {
        return when (format.lowercase()) {
            "markdown" -> exportAsMarkdown()
            "json" -> exportAsJson()
            "txt" -> exportAsText()
            else -> throw IllegalArgumentException("Unsupported export format: $format")
        }
    }
    
    /**
     * Платформо-специфичная инициализация.
     * Должна быть переопределена в конкретных реализациях.
     */
    protected abstract fun initializePlatformSpecific()
    
    override fun setEventHandler(handler: (ChatEvent) -> Unit) {
        this.chatEventHandler = handler
    }
    
    /**
     * Экспорт чата в формате Markdown.
     */
    protected open fun exportAsMarkdown(): String {
        return buildString {
            appendLine("# AI Chat History")
            appendLine()
            messages.forEach { message ->
                val sender = when (message.sender) {
                    MessageSender.USER -> "**You**"
                    MessageSender.ASSISTANT -> "*AI Assistant*"
                    MessageSender.SYSTEM -> "~~System~~"
                }
                appendLine("$sender: ${message.content}")
                appendLine()
            }
        }
    }
    
    /**
     * Экспорт чата в формате JSON.
     */
    protected open fun exportAsJson(): String {
        // В реальной реализации использовать kotlinx.serialization
        return """{"messages": ${messages.joinToString(",", "[", "]") { 
            """{
                "id": "${it.id}",
                "sender": "${it.sender}",
                "content": "${it.content.replace("\"", "\\\"")}",
                "timestamp": ${it.timestamp},
                "type": "${it.type}"
            }""".trimIndent()
        }}}"""
    }
    
    /**
     * Экспорт чата в обычном текстовом формате.
     */
    protected open fun exportAsText(): String {
        return buildString {
            appendLine("AI Chat History")
            appendLine("=".repeat(50))
            messages.forEach { message ->
                val sender = when (message.sender) {
                    MessageSender.USER -> "You"
                    MessageSender.ASSISTANT -> "AI"
                    MessageSender.SYSTEM -> "System"
                }
                appendLine("[$sender]: ${message.content}")
                appendLine()
            }
        }
    }
    
    protected fun notifyEventHandler(event: ChatEvent) {
        chatEventHandler?.invoke(event)
    }
}
