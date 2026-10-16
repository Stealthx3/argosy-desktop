// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/data/social/SocialModels.kt: FriendActivity
package com.nendo.argosy.data.social



/**
 * A friend's tie to one game: playing it right now, or [lastPlayedAtMillis] when they last did.
 */
data class FriendActivity(
    val friendId: String,
    val displayName: String,
    val avatarColor: String,
    val quayPassAvatar: String?,
    val playingNow: Boolean,
    val lastPlayedAtMillis: Long? = null
)
