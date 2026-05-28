package net.luis.jenga.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DistributionGroup(
    val groupCount: Int,
    val blockCount: Int
)
