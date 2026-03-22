package com.taskify.app.domain.model

/** Represents the active tab/filter selection in the task list screen. */
enum class TaskFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    HIGH_PRIORITY("High Priority"),
    COMPLETED("Completed")
}

/** Sort order for the task list. */
enum class SortOrder(val label: String) {
    DUE_DATE_ASC("Earliest First"),
    DUE_DATE_DESC("Latest First"),
    PRIORITY_DESC("High → Low"),
    PRIORITY_ASC("Low → High"),
    CREATED_DATE_DESC("Newest First"),
    CREATED_DATE_ASC("Oldest First"),
    TITLE_ASC("A → Z"),
    TITLE_DESC("Z → A")
}
