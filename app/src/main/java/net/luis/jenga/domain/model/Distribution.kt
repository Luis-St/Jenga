package net.luis.jenga.domain.model

data class Distribution(
    val id: Long = 0,
    val name: String,
    val groups: List<DistributionGroup>
) {
    val totalBlocks: Int get() = groups.sumOf { it.groupCount * it.blockCount }
    val tasksNeeded: Int get() = groups.sumOf { it.groupCount }
}
