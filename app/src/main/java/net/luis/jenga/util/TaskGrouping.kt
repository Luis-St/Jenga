package net.luis.jenga.util

import net.luis.jenga.domain.model.Category
import net.luis.jenga.domain.model.Task

/**
 * Groups tasks into category "folders". A task with multiple categories appears under each of
 * them; tasks with none fall under the null key (uncategorized). Folders are ordered naturally
 * by name with uncategorized last; the incoming task order is preserved within each folder.
 */
fun List<Task>.groupedByCategory(): Map<Category?, List<Task>> =
    flatMap { task ->
        if (task.categories.isEmpty()) listOf<Pair<Category?, Task>>(null to task)
        else task.categories.map { cat -> cat to task }
    }.groupBy({ it.first }, { it.second })
        .entries
        .sortedWith(compareBy(naturalOrder) { it.key?.name ?: "￿" })
        .associate { it.key to it.value }
