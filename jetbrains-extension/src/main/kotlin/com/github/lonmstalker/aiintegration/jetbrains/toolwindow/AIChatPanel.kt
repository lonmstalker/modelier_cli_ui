package com.github.lonmstalker.aiintegration.jetbrains.toolwindow

import com.github.lonmstalker.aiintegration.core.ui.*
import com.github.lonmstalker.aiintegration.jetbrains.localization.IntellijChatLocalization
import com.github.lonmstalker.aiintegration.jetbrains.services.AIService
import com.github.lonmstalker.aiintegration.jetbrains.services.ProjectContextService
import com.github.lonmstalker.aiintegration.core.models.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.text.DefaultCaret

/**
 * Основная панель чата для взаимодействия с AI ассистентом в IntelliJ IDEA.
 * 
 * Эта панель предоставляет полноценный интерфейс чата, включающий:
 * - Область отображения сообщений с автопрокруткой
 * - Поле ввода с поддержкой многострочного текста
 * - Кнопки управления (отправка, очистка)
 * - Индикатор печати ассистента
 * - Полную локализацию интерфейса
 * 
 * Панель интегрируется с core AI сервисами через {@link AIService} и 
 * {@link ProjectContextService} для получения контекста проекта.
 * 
 * @author AI CLI Integration Team
 * @version 0.0.1
 * @since 0.0.1
 * 
 * @param project текущий проект IntelliJ IDEA
 */
class AIChatPanel(private val project: Project) : JPanel(BorderLayout()) {
    /** Локализация интерфейса чата */
    private val localization: ChatLocalization = IntellijChatLocalization()
    
    private val chatArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        background = UIManager.getColor("TextArea.background")
        foreground = UIManager.getColor("TextArea.foreground")
        caret = DefaultCaret().apply { updatePolicy = DefaultCaret.ALWAYS_UPDATE }
    }
    
    private val inputArea = JBTextArea(3, 0).apply {
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        border = JBUI.Borders.empty(5)
        background = UIManager.getColor("TextArea.background")
        foreground = UIManager.getColor("TextArea.foreground")
        toolTipText = localization.getMessage(ChatLocalizationKeys.PLACEHOLDER_INPUT)
    }
    
    /** Кнопка отправки сообщения */
    private val sendButton = JButton().apply {
        preferredSize = Dimension(80, 30)
        text = localization.getMessage(ChatLocalizationKeys.BUTTON_SEND)
    }
    
    /** Кнопка очистки истории чата */
    private val clearButton = JButton().apply {
        preferredSize = Dimension(60, 30)
        text = localization.getMessage(ChatLocalizationKeys.BUTTON_CLEAR)
    }
    
    /** Сервис для работы с AI */
    private val aiService = AIService.getInstance()
    
    /** Сервис для получения контекста проекта */
    private val contextService = project.service<ProjectContextService>()
    
    /** Область видимости для корутин */
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /** Формат времени для сообщений */
    private val timeFormat = SimpleDateFormat(
        localization.getMessage(ChatLocalizationKeys.TIME_FORMAT_SHORT)
    )
    
    init {
        setupUI()
        setupEventHandlers()
        addWelcomeMessage()
    }
    
    /**
     * Настраивает пользовательский интерфейс панели чата.
     * 
     * Создает и размещает все UI компоненты:
     * - Область отображения сообщений с прокруткой
     * - Поле ввода сообщений с прокруткой
     * - Панель кнопок (отправка, очистка)
     * - Настраивает границы и отступы согласно IntelliJ IDEA стандартам
     * 
     * Использует {@link BorderLayout} для основной компоновки и {@link FlowLayout}
     * для панели кнопок.
     */
    private fun setupUI() {
        // Chat messages area
        val chatScrollPane = JBScrollPane(chatArea).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(UIManager.getColor("Component.borderColor"), 1),
                JBUI.Borders.empty(5)
            )
        }
        
        // Input area
        val inputScrollPane = JBScrollPane(inputArea).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            preferredSize = Dimension(0, 80)
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(UIManager.getColor("Component.borderColor"), 1),
                JBUI.Borders.empty(2)
            )
        }
        
        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(clearButton)
            add(sendButton)
            border = JBUI.Borders.empty(5)
        }
        
        // Input panel (input + buttons)
        val inputPanel = JPanel(BorderLayout()).apply {
            add(inputScrollPane, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
            border = JBUI.Borders.empty(5, 0)
        }
        
        // Main layout
        add(chatScrollPane, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)
        
        border = JBUI.Borders.empty(5)
    }
    
    /**
     * Настраивает обработчики событий для элементов интерфейса.
     * 
     * Устанавливает следующие обработчики:
     * - Клик по кнопке "Отправить" → отправка сообщения
     * - Клик по кнопке "Очистить" → очистка чата
     * - Enter в поле ввода → отправка сообщения
     * - Ctrl+Enter в поле ввода → добавление новой строки
     * 
     * Все действия используют локализованные строки и следуют UX паттернам IntelliJ IDEA.
     */
    private fun setupEventHandlers() {
        // Send button action
        sendButton.addActionListener { sendMessage() }
        
        // Clear button action
        clearButton.addActionListener { clearChat() }
        
        // Enter to send (Ctrl+Enter for new line)
        inputArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    if (e.isControlDown) {
                        // Ctrl+Enter adds new line
                        inputArea.insert("\\n", inputArea.caretPosition)
                    } else {
                        // Enter sends message
                        e.consume()
                        sendMessage()
                    }
                }
            }
        })
    }
    
    /**
     * Добавляет приветственное сообщение от AI ассистента.
     * Использует локализованные строки для отображения возможностей ассистента.
     */
    private fun addWelcomeMessage() {
        val assistantName = localization.getMessage(ChatLocalizationKeys.SENDER_ASSISTANT)
        
        appendMessage(assistantName, localization.getWelcomeMessage())
    }
    
    /**
     * Отправляет сообщение пользователя в чат и инициирует обработку AI ассистентом.
     * 
     * Метод выполняет следующие действия:
     * 1. Добавляет сообщение пользователя в историю чата
     * 2. Очищает поле ввода
     * 3. Показывает индикатор "печатает..."
     * 4. Асинхронно отправляет запрос AI сервису
     * 5. Отображает ответ или ошибку
     * 
     * Все строки используют локализацию через {@link ChatLocalization}.
     */
    private fun sendMessage() {
        val message = inputArea.text.trim()
        if (message.isEmpty()) return
        
        // Add user message
        appendMessage(localization.getMessage(ChatLocalizationKeys.SENDER_YOU), message)
        inputArea.text = ""
        
        // Show typing indicator
        appendMessage(
            localization.getMessage(ChatLocalizationKeys.SENDER_ASSISTANT), 
            localization.getMessage(ChatLocalizationKeys.TYPING_INDICATOR)
        )
        
        // Process message asynchronously
        scope.launch {
            try {
                val response = processUserMessage(message)
                
                // Remove typing indicator and add response
                SwingUtilities.invokeLater {
                    removeLastMessage() // Remove typing indicator
                    appendMessage(
                        localization.getMessage(ChatLocalizationKeys.SENDER_ASSISTANT), 
                        response
                    )
                }
                
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    removeLastMessage() // Remove typing indicator
                    appendMessage(
                        localization.getMessage(ChatLocalizationKeys.SENDER_ASSISTANT),
                        localization.getErrorMessage(e.message ?: "Unknown error")
                    )
                }
            }
        }
    }
    
    /**
     * Обрабатывает сообщение пользователя через AI сервис.
     * 
     * Метод создает контекст проекта, формирует AI запрос и отправляет его
     * в сервис для обработки. Выполняется асинхронно в IO потоке.
     * 
     * @param message текст сообщения от пользователя
     * @return ответ от AI ассистента или сообщение об ошибке
     * @throws Exception при ошибках взаимодействия с AI сервисом
     */
    private suspend fun processUserMessage(message: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Build context from current IDE state
                val context = contextService.buildContextFromIde(null)
                
                // Create AI request
                val request = AIRequest(
                    command = AICommand.CustomPrompt(message, null),
                    context = context,
                    parameters = mapOf(
                        "maxTokens" to "2000",
                        "temperature" to "0.7"
                    ),
                    targetProvider = null, // Let service choose
                    requestId = UUID.randomUUID().toString()
                )
                
                // Execute request
                val response = aiService.executeRequest(request)
                
                if (response.success) {
                    response.content
                } else {
                    localization.getErrorMessage(response.error?.message ?: "Unknown error occurred")
                }
                
            } catch (e: Exception) {
                localization.getDefaultMessage(message)
            }
        }
    }
    
    /**
     * Добавляет новое сообщение в область чата.
     * 
     * Форматирует сообщение с отметкой времени и отправителем,
     * добавляет в текстовую область и автоматически прокручивает к концу.
     * 
     * @param sender имя отправителя (локализованное)
     * @param message текст сообщения
     */
    private fun appendMessage(sender: String, message: String) {
        SwingUtilities.invokeLater {
            val timestamp = timeFormat.format(Date())
            val formattedMessage = "[$timestamp] $sender: $message\\n\\n"
            chatArea.append(formattedMessage)
            chatArea.caretPosition = chatArea.document.length
        }
    }
    
    /**
     * Удаляет последнее сообщение из чата.
     * 
     * Используется для удаления индикатора "печатает..." перед добавлением
     * реального ответа от AI ассистента.
     */
    private fun removeLastMessage() {
        SwingUtilities.invokeLater {
            val text = chatArea.text
            val lastIndex = text.lastIndexOf("\\n\\n")
            if (lastIndex > 0) {
                val previousIndex = text.lastIndexOf("\\n\\n", lastIndex - 1)
                if (previousIndex > 0) {
                    chatArea.text = text.substring(0, previousIndex + 2)
                    chatArea.caretPosition = chatArea.document.length
                }
            }
        }
    }
    
    /**
     * Очищает историю чата и добавляет приветственное сообщение.
     * 
     * Полностью удаляет весь текст из области чата и показывает
     * новое приветственное сообщение от AI ассистента.
     */
    private fun clearChat() {
        chatArea.text = ""
        addWelcomeMessage()
    }
    
    /**
     * Освобождает ресурсы панели чата.
     * 
     * Отменяет все активные корутины для предотвращения утечек памяти.
     * Должен быть вызван при закрытии панели или tool window.
     */
    fun dispose() {
        scope.cancel()
    }
}