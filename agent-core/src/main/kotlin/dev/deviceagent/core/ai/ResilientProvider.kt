package dev.deviceagent.core.ai

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

data class RetryPolicy(
    val maxRetries: Int = 3,
    val baseDelayMs: Long = 1_000,
    val maxDelayMs: Long = 30_000,
    /** Fallback is opt-in and must respect the user's privacy choices. */
    val allowFallback: Boolean = true,
)

data class UsageRecord(
    val provider: String, val model: String, val inputTokens: Int, val outputTokens: Int,
    val latencyMs: Long, val error: String?,
)

class UsageTracker {
    private val records = java.util.concurrent.CopyOnWriteArrayList<UsageRecord>()
    private val requests = AtomicInteger()
    fun record(r: UsageRecord) { records += r; requests.incrementAndGet() }
    fun all(): List<UsageRecord> = records.toList()
    val requestCount: Int get() = requests.get()
    val inputTokens: Long get() = records.sumOf { it.inputTokens.toLong() }
    val outputTokens: Long get() = records.sumOf { it.outputTokens.toLong() }
    val errorCount: Int get() = records.count { it.error != null }
}

/**
 * Wraps a provider with the AI-API recovery rules from the spec:
 *  - rate limit / network  → exponential backoff, honouring Retry-After
 *  - model unavailable     → retry, then the configured fallback model/provider (if permitted)
 *  - context too large     → shrink the history once and retry
 *  - invalid credentials / bad request / safety block → surface immediately (retrying cannot help)
 */
class ResilientProvider(
    private val primary: AIProvider,
    private val fallback: AIProvider? = null,
    private val fallbackModel: String? = null,
    private val policy: RetryPolicy = RetryPolicy(),
    private val tracker: UsageTracker? = null,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    private val clock: () -> Long = System::currentTimeMillis,
    private val reducer: (GenerateRequest) -> GenerateRequest = ContextReducer::reduce,
) : AIProvider {
    override val providerId: String get() = primary.providerId
    override suspend fun listModels() = primary.listModels()
    override suspend fun validateCredentials() = primary.validateCredentials()
    override fun capabilities(model: String) = primary.capabilities(model)

    override suspend fun generate(request: GenerateRequest): GenerateResponse {
        try {
            return attempt(primary, request)
        } catch (e: ProviderException) {
            if (e.kind == ProviderErrorKind.MODEL_UNAVAILABLE && policy.allowFallback && fallback != null) {
                return attempt(fallback, request.copy(model = fallbackModel ?: request.model))
            }
            throw e
        }
    }

    private suspend fun attempt(provider: AIProvider, original: GenerateRequest): GenerateResponse {
        var request = original
        var retries = 0
        var reduced = false
        while (true) {
            val started = clock()
            try {
                val response = provider.generate(request)
                tracker?.record(
                    UsageRecord(provider.providerId, request.model, response.usage?.inputTokens ?: 0,
                        response.usage?.outputTokens ?: 0, clock() - started, null),
                )
                return response
            } catch (e: ProviderException) {
                tracker?.record(UsageRecord(provider.providerId, request.model, 0, 0, clock() - started, "${e.kind}: ${e.message}"))
                when (e.kind) {
                    ProviderErrorKind.RATE_LIMITED, ProviderErrorKind.NETWORK, ProviderErrorKind.MODEL_UNAVAILABLE -> {
                        if (retries >= policy.maxRetries) throw e
                        val backoff = minOf(policy.maxDelayMs, policy.baseDelayMs shl retries)
                        sleep(maxOf(e.retryAfterMs ?: 0L, backoff).coerceAtMost(policy.maxDelayMs))
                        retries++
                    }
                    ProviderErrorKind.CONTEXT_TOO_LARGE -> {
                        if (reduced) throw e
                        val smaller = reducer(request)
                        if (smaller == request) throw e
                        request = smaller
                        reduced = true
                    }
                    else -> throw e
                }
            }
        }
    }
}

/** Shrinks a conversation without breaking tool-call / tool-result pairing. */
object ContextReducer {
    private const val KEEP_TAIL = 8

    fun reduce(request: GenerateRequest): GenerateRequest {
        val msgs = request.messages
        if (msgs.size <= KEEP_TAIL + 1) return request
        var start = msgs.size - KEEP_TAIL
        // A tool-results message must follow the assistant message that made the calls.
        while (start < msgs.size && msgs[start] is Message.ToolResults) start++
        if (start >= msgs.size) start = msgs.size - 1
        if (start <= 1) return request
        val dropped = start - 1
        val note = Message.User("[$dropped earlier message(s) were omitted to fit the context window. Re-inspect the device state with read-only tools if you need details from them.]")
        return request.copy(messages = listOf(msgs.first(), note) + msgs.subList(start, msgs.size))
    }
}
