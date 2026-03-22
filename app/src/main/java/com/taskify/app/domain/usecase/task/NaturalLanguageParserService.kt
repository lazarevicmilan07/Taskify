package com.taskify.app.domain.usecase.task

import java.time.LocalDateTime

/**
 * Stub interface for future natural-language task parsing.
 * e.g. "Meeting with John tomorrow at 3pm" → Task(title="Meeting with John", dueDate=...)
 *
 * A future implementation could call a remote LLM API or use a local NLP library.
 * By keeping this as an interface in the domain layer, the UI just calls `parse()`
 * without knowing how it works.
 */
interface NaturalLanguageParserService {
    data class ParseResult(
        val title: String,
        val dueDate: LocalDateTime? = null,
        val priority: String? = null
    )

    suspend fun parse(input: String): ParseResult
}

/** Default no-op implementation — returns the raw input as the title. */
class DefaultNaturalLanguageParser : NaturalLanguageParserService {
    override suspend fun parse(input: String) =
        NaturalLanguageParserService.ParseResult(title = input.trim())
}
