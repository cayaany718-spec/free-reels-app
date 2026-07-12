package com.example.data

data class Drama(
    val id: Int,
    val title: String,
    val coverUrl: String, // e.g. "file:///android_asset/cover_ceo_wife.jpg"
    val description: String,
    val category: String,
    val rating: Double,
    val views: String,
    val episodesCount: Int,
    val isHot: Boolean = false,
    val isTrending: Boolean = false,
    val author: String = "FreeReels Studio",
    val isDubbed: Boolean = false,
    val subtag: String = "Báo thù",
    val overlayBadge: String? = null
)

data class Episode(
    val id: Int,
    val dramaId: Int,
    val episodeNumber: Int,
    val title: String,
    val videoUrl: String,
    val duration: String,
    val isLocked: Boolean = false
)

data class UserProfile(
    val id: String = "FR_948102",
    val nickname: String = "Thành viên FreeReels 🦊",
    val avatarEmoji: String = "🦊",
    val phoneNumber: String = "",
    val vipLevel: String = "THÀNH VIÊN VIP PREMIUM",
    val isVip: Boolean = true
)

