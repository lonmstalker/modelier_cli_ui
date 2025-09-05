package com.github.lonmstalker.aiintegration.core.validation

import com.github.lonmstalker.aiintegration.core.models.*

/**
 * Сервис валидации для AI запросов и ответов
 */
interface ValidationService {
    
    /**
     * Валидирует AI запрос перед отправкой
     */
    fun validateRequest(request: AIRequest): ValidationResult
    
    /**
     * Валидирует ответ от AI провайдера
     */
    fun validateResponse(response: AIResponse): ValidationResult
    
    /**
     * Валидирует конфигурацию провайдера
     */
    fun validateProviderConfiguration(provider: AIProvider): ValidationResult
    
    /**
     * Валидирует сгенерированный код
     */
    fun validateGeneratedCode(code: String, language: String): CodeValidationResult
}

/**
 * Результат валидации
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<ValidationWarning> = emptyList()
) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    
    companion object {
        fun valid() = ValidationResult(true)
        
        fun invalid(vararg errors: ValidationError) = ValidationResult(
            isValid = false,
            errors = errors.toList()
        )
        
        fun withWarnings(vararg warnings: ValidationWarning) = ValidationResult(
            isValid = true,
            warnings = warnings.toList()
        )
    }
}

/**
 * Результат валидации кода
 */
data class CodeValidationResult(
    val isValid: Boolean,
    val syntaxErrors: List<String> = emptyList(),
    val securityIssues: List<SecurityIssue> = emptyList(),
    val qualityIssues: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
) {
    fun hasIssues(): Boolean = syntaxErrors.isNotEmpty() || securityIssues.isNotEmpty()
    fun hasCriticalIssues(): Boolean = securityIssues.any { it.severity == SecuritySeverity.CRITICAL }
}

/**
 * Ошибка валидации
 */
data class ValidationError(
    val code: String,
    val message: String,
    val field: String? = null,
    val value: Any? = null
) {
    companion object {
        // Предустановленные ошибки валидации
        fun required(field: String) = ValidationError("REQUIRED", "Field '$field' is required", field)
        fun invalidFormat(field: String, value: Any?) = ValidationError("INVALID_FORMAT", "Invalid format for field '$field'", field, value)
        fun tooLong(field: String, maxLength: Int) = ValidationError("TOO_LONG", "Field '$field' exceeds maximum length of $maxLength", field)
        fun tooShort(field: String, minLength: Int) = ValidationError("TOO_SHORT", "Field '$field' is shorter than minimum length of $minLength", field)
        fun invalidValue(field: String, value: Any?) = ValidationError("INVALID_VALUE", "Invalid value for field '$field': $value", field, value)
    }
}

/**
 * Предупреждение валидации
 */
data class ValidationWarning(
    val code: String,
    val message: String,
    val field: String? = null
)

/**
 * Проблема безопасности в коде
 */
data class SecurityIssue(
    val type: SecurityIssueType,
    val severity: SecuritySeverity,
    val description: String,
    val line: Int? = null,
    val suggestion: String? = null
)

/**
 * Тип проблемы безопасности
 */
enum class SecurityIssueType {
    COMMAND_INJECTION,
    SQL_INJECTION,
    XSS,
    INSECURE_RANDOM,
    HARDCODED_SECRET,
    UNSAFE_DESERIALIZATION,
    PATH_TRAVERSAL,
    WEAK_CRYPTO,
    DANGEROUS_FUNCTION
}

/**
 * Серьезность проблемы безопасности
 */
enum class SecuritySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Реализация сервиса валидации по умолчанию
 */
class DefaultValidationService : ValidationService {
    
    override fun validateRequest(request: AIRequest): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()
        
        // Валидация команды
        if (request.command.toString().isBlank()) {
            errors.add(ValidationError.required("command"))
        }
        
        // Валидация ID запроса
        if (request.requestId.isBlank()) {
            errors.add(ValidationError.required("requestId"))
        }
        
        // Валидация контекста
        val contextSize = request.context.selectedText?.length ?: 0
        if (contextSize > 50000) {
            warnings.add(ValidationWarning("LARGE_CONTEXT", "Large context size may affect performance: $contextSize characters", "context"))
        }
        
        // Валидация параметров
        request.parameters.forEach { (key, value) ->
            when (key) {
                "maxTokens" -> {
                    val maxTokens = value.toString().toIntOrNull()
                    if (maxTokens == null || maxTokens <= 0) {
                        errors.add(ValidationError.invalidValue("maxTokens", value))
                    }
                }
                "temperature" -> {
                    val temperature = value.toString().toDoubleOrNull()
                    if (temperature == null || temperature < 0.0 || temperature > 2.0) {
                        errors.add(ValidationError.invalidValue("temperature", value))
                    }
                }
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }
    
    override fun validateResponse(response: AIResponse): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()
        
        // Валидация успешного ответа
        if (response.success && response.content.isBlank()) {
            errors.add(ValidationError("EMPTY_CONTENT", "Successful response should not have empty content"))
        }
        
        // Валидация неуспешного ответа
        if (!response.success && response.error == null) {
            errors.add(ValidationError("MISSING_ERROR", "Failed response should have error information"))
        }
        
        // Валидация размера контента
        if (response.content.length > 100000) {
            warnings.add(ValidationWarning("LARGE_CONTENT", "Response content is very large: ${response.content.length} characters"))
        }
        
        // Валидация времени выполнения
        if (response.executionTimeMs < 0) {
            errors.add(ValidationError("INVALID_EXECUTION_TIME", "Execution time cannot be negative"))
        }
        
        if (response.executionTimeMs > 300000) { // 5 minutes
            warnings.add(ValidationWarning("LONG_EXECUTION", "Response took a very long time: ${response.executionTimeMs}ms"))
        }
        
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }
    
    override fun validateProviderConfiguration(provider: AIProvider): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()
        
        // Валидация имени провайдера
        if (provider.name.isBlank()) {
            errors.add(ValidationError.required("name"))
        }
        
        // Валидация доступности
        try {
            if (!provider.isAvailable()) {
                warnings.add(ValidationWarning("PROVIDER_UNAVAILABLE", "Provider is not currently available"))
            }
        } catch (e: Exception) {
            warnings.add(ValidationWarning("AVAILABILITY_CHECK_FAILED", "Could not check provider availability: ${e.message}"))
        }
        
        // Валидация возможностей
        if (provider.capabilities.isEmpty()) {
            warnings.add(ValidationWarning("NO_CAPABILITIES", "Provider has no declared capabilities"))
        }
        
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }
    
    override fun validateGeneratedCode(code: String, language: String): CodeValidationResult {
        val syntaxErrors = mutableListOf<String>()
        val securityIssues = mutableListOf<SecurityIssue>()
        val qualityIssues = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        
        // Базовая валидация синтаксиса
        when (language.lowercase()) {
            "kotlin" -> validateKotlinCode(code, syntaxErrors, qualityIssues, suggestions)
            "java" -> validateJavaCode(code, syntaxErrors, qualityIssues, suggestions)
            "python" -> validatePythonCode(code, syntaxErrors, qualityIssues, suggestions)
        }
        
        // Проверка безопасности для всех языков
        validateCodeSecurity(code, language, securityIssues)
        
        return CodeValidationResult(
            isValid = syntaxErrors.isEmpty() && securityIssues.none { it.severity == SecuritySeverity.CRITICAL },
            syntaxErrors = syntaxErrors,
            securityIssues = securityIssues,
            qualityIssues = qualityIssues,
            suggestions = suggestions
        )
    }
    
    private fun validateKotlinCode(
        code: String, 
        syntaxErrors: MutableList<String>,
        qualityIssues: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        // Простая проверка парности скобок
        var braceCount = 0
        var parenCount = 0
        
        code.forEach { char ->
            when (char) {
                '{' -> braceCount++
                '}' -> braceCount--
                '(' -> parenCount++
                ')' -> parenCount--
            }
        }
        
        if (braceCount != 0) syntaxErrors.add("Unmatched braces")
        if (parenCount != 0) syntaxErrors.add("Unmatched parentheses")
        
        // Проверка качества кода
        if (!code.contains("fun ") && code.length > 50) {
            qualityIssues.add("Consider extracting code into functions")
        }
        
        if (code.contains("TODO") || code.contains("FIXME")) {
            qualityIssues.add("Code contains TODO or FIXME comments")
        }
        
        // Рекомендации
        if (!code.contains("//") && code.lines().size > 10) {
            suggestions.add("Consider adding comments for better readability")
        }
    }
    
    private fun validateJavaCode(
        code: String,
        syntaxErrors: MutableList<String>,
        qualityIssues: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        // Базовая валидация Java
        if (code.contains("class ") && !code.contains("{")) {
            syntaxErrors.add("Class declaration without opening brace")
        }
        
        if (code.contains("public static void main") && !code.contains("String[] args")) {
            qualityIssues.add("Main method should accept String[] args")
        }
    }
    
    private fun validatePythonCode(
        code: String,
        syntaxErrors: MutableList<String>,
        qualityIssues: MutableList<String>,
        suggestions: MutableList<String>
    ) {
        // Базовая валидация Python
        val lines = code.lines()
        
        lines.forEachIndexed { index, line ->
            if (line.trimStart().startsWith("def ") && !line.endsWith(":")) {
                syntaxErrors.add("Function definition on line ${index + 1} should end with colon")
            }
        }
        
        if (code.contains("import os") && code.contains("os.system")) {
            qualityIssues.add("Consider using subprocess instead of os.system")
        }
    }
    
    private fun validateCodeSecurity(
        code: String, 
        language: String,
        securityIssues: MutableList<SecurityIssue>
    ) {
        val lowerCode = code.lowercase()
        
        // Проверка на опасные команды
        val dangerousCommands = mapOf(
            "rm -rf" to SecurityIssue(SecurityIssueType.DANGEROUS_FUNCTION, SecuritySeverity.CRITICAL, "Dangerous file deletion command"),
            "system(" to SecurityIssue(SecurityIssueType.COMMAND_INJECTION, SecuritySeverity.HIGH, "Potential command injection vulnerability"),
            "exec(" to SecurityIssue(SecurityIssueType.COMMAND_INJECTION, SecuritySeverity.HIGH, "Potential command injection vulnerability"),
            "eval(" to SecurityIssue(SecurityIssueType.COMMAND_INJECTION, SecuritySeverity.HIGH, "Code evaluation can be dangerous"),
            "drop table" to SecurityIssue(SecurityIssueType.SQL_INJECTION, SecuritySeverity.HIGH, "Potential SQL injection"),
            "delete from" to SecurityIssue(SecurityIssueType.SQL_INJECTION, SecuritySeverity.MEDIUM, "Potential SQL injection"),
            "password" to SecurityIssue(SecurityIssueType.HARDCODED_SECRET, SecuritySeverity.MEDIUM, "Possible hardcoded password"),
            "secret_key" to SecurityIssue(SecurityIssueType.HARDCODED_SECRET, SecuritySeverity.MEDIUM, "Possible hardcoded secret"),
            "api_key" to SecurityIssue(SecurityIssueType.HARDCODED_SECRET, SecuritySeverity.MEDIUM, "Possible hardcoded API key")
        )
        
        dangerousCommands.forEach { (pattern, issue) ->
            if (lowerCode.contains(pattern)) {
                securityIssues.add(issue.copy(suggestion = "Review and secure this code pattern"))
            }
        }
        
        // Проверка на слабую криптографию
        if (lowerCode.contains("md5") || lowerCode.contains("sha1")) {
            securityIssues.add(SecurityIssue(
                SecurityIssueType.WEAK_CRYPTO,
                SecuritySeverity.MEDIUM,
                "Weak cryptographic algorithm detected",
                suggestion = "Use SHA-256 or stronger algorithms"
            ))
        }
    }
}

/**
 * Фабрика для создания сервисов валидации
 */
object ValidationServiceFactory {
    
    private var defaultService: ValidationService = DefaultValidationService()
    
    fun getDefault(): ValidationService = defaultService
    
    fun setDefault(service: ValidationService) {
        defaultService = service
    }
    
    fun create(): ValidationService = DefaultValidationService()
}