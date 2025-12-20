package org.vitbuk.model

data class DrawResult(
    val assignments: Map<Long, Long>,
    val warnings: List<String> = emptyList()
)