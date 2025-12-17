package com.pawansingh.vp.update

data class ShortsModel(
    val id: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val title: String = "",
    val soundName: String = "",
    val soundUrl: String = "",
    val uploaderId: String = "",
    val uploaderName: String = "",
    val uploaderImage: String = "",
    var views: Long = 0,
    var likes: Long = 0,
    var comments: Long = 0,
    val shares: Long = 0,
    val uploadedAt: Long = 0L,
    val tags: List<String> = emptyList(),
    var liked: Boolean = false
)

