# Мульти-модельный оркестратор AI CLI Integration

## 🎯 Обзор

Мульти-модельный оркестратор позволяет одновременно использовать несколько AI провайдеров для получения более качественных и надежных результатов. Система автоматически сравнивает ответы разных моделей и выбирает лучший результат.

## 🏗 Архитектура

### Компоненты системы

```
┌─────────────────┐    ┌──────────────────────┐    ┌─────────────────┐
│   AI Service    │ ──▶│ MultiModelOrchestra  │ ──▶│ Comparison      │
└─────────────────┘    └──────────────────────┘    │ & Validation    │
                                │                   └─────────────────┘
                                ▼
           ┌────────────────────────────────────────┐
           │          Parallel Execution            │
           │  ┌──────┐  ┌──────┐  ┌──────┐         │
           │  │ GPT  │  │Claude│  │Gemini│  ...    │
           │  └──────┘  └──────┘  └──────┘         │
           └────────────────────────────────────────┘
```

### Основные классы

1. **MultiModelOrchestrator** - интерфейс оркестратора
2. **IntellijMultiModelOrchestrator** - реализация для IntelliJ
3. **ComparisonResult** - результат сравнения моделей
4. **MultiModelResponse** - объединенный ответ

## 🚀 Использование

### 1. Базовое использование через AIService

```kotlin
// Получаем сервис
val aiService = AIService.getInstance()

// Создаем запрос
val request = AIRequest(
    command = AICommand.ExplainCode("fun fibonacci(n: Int): Int", "kotlin"),
    context = projectContext,
    parameters = mapOf("maxTokens" to "1000"),
    requestId = UUID.randomUUID().toString()
)

// Выполняем с несколькими провайдерами
val multiResponse = aiService.executeMultiModelRequest(
    request = request,
    providerNames = listOf("claude", "gpt-4", "gemini") // или пустой список для всех
)

multiResponse?.let { response ->
    println("Лучший ответ от: ${response.recommendedProvider.name}")
    println("Содержание: ${response.recommendedResponse.content}")
    
    // Консенсус-ответ (объединяет лучшие части)
    response.consensusResponse?.let { consensus ->
        println("Консенсус: ${consensus.content}")
    }
    
    // Статистика
    val stats = response.executionStats
    println("Время выполнения: ${stats.totalExecutionTimeMs}ms")
    println("Успешных провайдеров: ${stats.successfulProviders}/${stats.providersUsed}")
}
```

### 2. Продвинутое использование с кастомным сравнением

```kotlin
val orchestrator = MultiModelOrchestratorFactory.getInstance()

// Кастомные критерии для генерации кода
val criteria = ComparisonCriteria(
    weights = ComparisonWeights.forCodeGeneration(),
    taskType = TaskType.CODE_GENERATION
)

// Параллельное выполнение
val multiResponse = orchestrator.executeParallel(request, providers)

// Детальный анализ результатов
val comparison = multiResponse.comparison
comparison?.let { comp ->
    println("Победитель: ${comp.winner.name} (уверенность: ${comp.confidence})")
    
    // Разбивка по критериям
    comp.scores.forEach { (provider, score) ->
        println("${provider.name}:")
        println("  - Точность: ${score.accuracy}")
        println("  - Полнота: ${score.completeness}")
        println("  - Качество кода: ${score.codeQuality}")
        println("  - Объяснение: ${score.explanation}")
    }
    
    // Рекомендации
    comp.recommendations.forEach { recommendation ->
        println("💡 $recommendation")
    }
}
```

## ⚙️ Конфигурация

### Настройка весов сравнения

```kotlin
// Предустановленные веса
val defaultWeights = ComparisonWeights.default()
val codeGenWeights = ComparisonWeights.forCodeGeneration()  
val reviewWeights = ComparisonWeights.forCodeReview()

// Кастомные веса
val customWeights = ComparisonWeights(
    accuracy = 0.40,      // Приоритет точности
    completeness = 0.30,  // Важна полнота
    codeQuality = 0.20,   
    explanation = 0.05,   // Объяснения менее важны
    relevance = 0.03,
    safety = 0.02
)
```

### Типы задач и их оптимизация

| TaskType | Описание | Оптимизация |
|----------|----------|-------------|
| CODE_GENERATION | Генерация кода | Приоритет качеству кода и точности |
| CODE_EXPLANATION | Объяснение кода | Приоритет объяснению и полноте |
| CODE_REVIEW | Ревью кода | Приоритет полноте и объяснению |
| BUG_FIXING | Исправление багов | Приоритет точности и безопасности |
| REFACTORING | Рефакторинг | Приоритет качеству кода |

## 📊 Система оценки

### Критерии сравнения

1. **Accuracy (Точность)** - корректность ответа
2. **Completeness (Полнота)** - всесторонность рассмотрения
3. **Code Quality (Качество кода)** - соответствие best practices
4. **Explanation (Объяснение)** - качество пояснений
5. **Relevance (Релевантность)** - соответствие запросу
6. **Safety (Безопасность)** - отсутствие опасного кода

### Алгоритм оценки

```kotlin
// Каждый критерий оценивается от 0.0 до 1.0
val overallScore = (accuracy * weights.accuracy +
                   completeness * weights.completeness +
                   codeQuality * weights.codeQuality +
                   explanation * weights.explanation +
                   relevance * weights.relevance +
                   safety * weights.safety) / weights.total()
```

## 🔍 Примеры сценариев использования

### Сценарий 1: Генерация критически важного кода

```kotlin
// Используем несколько провайдеров для важного кода
val request = AIRequest(
    command = AICommand.GenerateCode(
        description = "Secure password validation function",
        language = "kotlin"
    ),
    context = context,
    requestId = UUID.randomUUID().toString()
)

val multiResponse = aiService.executeMultiModelRequest(
    request = request,
    providerNames = listOf("claude-3", "gpt-4", "gemini-pro")
)

// Проверяем консенсус между моделями
multiResponse?.consensusResponse?.let { consensus ->
    // Используем консенсус-ответ для критически важного кода
    applyCodeSuggestion(consensus.content)
}
```

### Сценарий 2: Сравнение подходов к решению

```kotlin
// Получаем разные подходы к одной задаче
val comparison = orchestrator.compareResponses(
    responses = mapOf(
        claudeProvider to claudeResponse,
        gptProvider to gptResponse,
        geminiProvider to geminiResponse
    ),
    originalQuery = "Optimize this database query",
    criteria = ComparisonCriteria(
        weights = ComparisonWeights(
            accuracy = 0.30,
            completeness = 0.25,
            codeQuality = 0.25,
            explanation = 0.20
        )
    )
)

// Анализируем разные подходы
comparison.scores.forEach { (provider, score) ->
    println("Подход ${provider.name}: ${score.overall * 10}/10")
}
```

## 🛠 Расширение системы

### Добавление нового провайдера

```kotlin
class CustomAIProvider : AIProvider {
    override val name = "Custom Provider"
    override val capabilities = setOf(AICapability.CODE_GENERATION)
    
    override suspend fun execute(request: AIRequest): AIResponse {
        // Ваша реализация
    }
    
    // ... остальные методы
}

// Регистрация
aiService.registerProvider(CustomAIProvider())
```

### Кастомные критерии сравнения

```kotlin
class CustomOrchestrator : MultiModelOrchestrator {
    
    override suspend fun compareResponses(
        responses: Map<AIProvider, AIResponse>,
        originalQuery: String,
        criteria: ComparisonCriteria?
    ): ComparisonResult {
        // Ваш алгоритм сравнения
        // Можно использовать ML модели, внешние сервисы оценки и т.д.
    }
}
```

## 📈 Производительность и оптимизация

### Стратегии выполнения

1. **Параллельное выполнение** - все провайдеры запускаются одновременно
2. **Таймауты** - защита от зависших запросов
3. **Graceful degradation** - работа даже если часть провайдеров недоступна
4. **Кэширование** - сохранение результатов сравнений

### Настройка производительности

```kotlin
// В конфигурации приложения
val coreConfig = CoreConfiguration(
    enableMultiModel = true,
    maxContextTokens = 50000,
    cacheEnabled = true,
    cacheExpirationHours = 24
)
```

## 🔐 Безопасность

### Валидация ответов

- Проверка на наличие потенциально опасного кода
- Фильтрация конфиденциальной информации
- Валидация синтаксиса сгенерированного кода

### Приватность данных

- Запросы к разным провайдерам изолированы
- Логирование сравнений без сохранения чувствительных данных
- Возможность отключения определенных провайдеров

## 🎛 Интеграция с UI

### Отображение результатов мульти-модели

```kotlin
// В AIChatPanel можно показать:
// 1. Основной рекомендованный ответ
// 2. Альтернативные варианты от других моделей  
// 3. Консенсус-ответ
// 4. Статистику сравнения

when (val response = aiService.executeMultiModelRequest(request, emptyList())) {
    null -> showError("Все провайдеры недоступны")
    else -> {
        showRecommendedAnswer(response.recommendedResponse)
        
        if (response.responses.size > 1) {
            showAlternativeAnswers(response.responses)
            showComparisonStats(response.comparison)
        }
        
        response.consensusResponse?.let { 
            showConsensusAnswer(it) 
        }
    }
}
```

## 🧪 Тестирование

### Unit тесты

```kotlin
@Test
fun `should select best response based on criteria`() {
    val orchestrator = IntellijMultiModelOrchestrator()
    val responses = mapOf(
        mockProvider1 to mockResponse1,
        mockProvider2 to mockResponse2
    )
    
    val comparison = orchestrator.compareResponses(
        responses, 
        "test query",
        ComparisonCriteria()
    )
    
    assertThat(comparison.winner).isEqualTo(expectedWinner)
    assertThat(comparison.confidence).isGreaterThan(0.5)
}
```

## 📚 FAQ

**Q: Как выбираются провайдеры для сравнения?**
A: По умолчанию используются все доступные провайдеры. Можно явно указать список через параметр `providerNames`.

**Q: Что происходит если все провайдеры вернули ошибку?**
A: Система выбрасывает `IllegalStateException` с соответствующим сообщением.

**Q: Можно ли настроить веса критериев для разных типов задач?**
A: Да, используйте `ComparisonWeights.forCodeGeneration()`, `forCodeReview()` или создайте кастомные веса.

**Q: Как работает консенсус-ответ?**
A: Система берет лучший ответ как основу и добавляет альтернативные предложения от других провайдеров.

**Q: Можно ли отключить мульти-модельный режим?**
A: Да, просто используйте обычный `executeRequest()` вместо `executeMultiModelRequest()`.