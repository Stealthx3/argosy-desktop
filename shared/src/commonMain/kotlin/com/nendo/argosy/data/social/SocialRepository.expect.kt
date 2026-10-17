package com.nendo.argosy.data.social

import com.nendo.argosy.data.social.uploader.PlaySessionBackfill
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

expect class SocialRepository {
    class FriendCode {
        val code: String
        val url: String
    }

    companion object {}

    val connectionState: StateFlow<SocialConnectionState>

    val friendCode: StateFlow<FriendCode?>

    val friends: StateFlow<List<Friend>>

    val friendsActivity: StateFlow<Map<Int, List<FriendActivity>>>

    val quayPassCheckins: StateFlow<List<QuayPassCheckin>>

    val sharedCollections: StateFlow<List<CollectionSummary>>

    val savedCollections: StateFlow<List<CollectionSummary>>

    val feedEvents: StateFlow<List<FeedEventDto>>

    val reviewSummaries: StateFlow<Map<Int, ReviewSummary>>

    val gameReviews: StateFlow<Map<Int, GameReviewsPage>>

    val reviewWriteEvents: SharedFlow<ReviewWriteEvent>

    val discordLinked: StateFlow<Boolean>

    val discordUsername: StateFlow<String?>

    val feedHasMore: StateFlow<Boolean>

    val isLoadingFeed: StateFlow<Boolean>

    val eventComments: StateFlow<Map<String, List<FeedComment>>>

    val unreadCount: StateFlow<Int>

    val notifications: StateFlow<List<SocialNotification>>

    val notificationsHasMore: StateFlow<Boolean>

    val isLoadingNotifications: StateFlow<Boolean>

    val communityFollows: StateFlow<List<CommunityFollow>>

    val communityFeed: StateFlow<List<FeedEventDto>>

    val communityFeedHasMore: StateFlow<Boolean>

    val isLoadingCommunityFeed: StateFlow<Boolean>

    val userSettings: StateFlow<UserSettings>

    val hiddenGameIds: StateFlow<Set<Int>>

    val userProfile: StateFlow<UserProfileData?>

    val isLoadingProfile: StateFlow<Boolean>

    val netplayInvites: SharedFlow<NetplayInvitePayload>

    val authState: StateFlow<SocialAuthManager.AuthState>

    val serviceConnectionState: StateFlow<ArgosSocialService.ConnectionState>

    suspend fun startAuth(): SocialAuthManager.AuthResult

    fun cancelAuth()

    suspend fun suspendForAccountSwitch()

    suspend fun resumeAfterAccountSwitch()

    suspend fun logout()

    fun sendPresence(status: PresenceStatus, gameIgdbId: Int? = null, gameTitle: String? = null, media: MediaPresence? = null): Boolean

    fun isConnected(): Boolean

    fun reconnectIfNeeded()

    fun requestUserProfile(userId: String? = null)

    fun sendFriendRequest(userId: String)

    fun acceptFriend(userId: String)

    fun removeFriend(userId: String)

    fun requestQuayPassCheckins()

    fun requestDiscordTokens()

    fun toggleFavoriteFriend(friendId: String)

    fun toggleGameVisibility(igdbGameId: Int?, steamAppId: Int? = null, isCurrentlyHidden: Boolean = false)

    fun requestFriendCode()

    fun regenerateFriendCode()

    fun addFriendByCode(code: String)

    suspend fun backfillRomMPlaySessions(): PlaySessionBackfill.Summary

    suspend fun syncUnsharedAchievements()

    fun requestReviewSummary(igdbId: Int)

    fun requestGameReviews(igdbId: Int)

    fun loadMoreGameReviews(igdbId: Int)

    fun upsertReview(igdbId: Int, recommended: Boolean, body: String?, visibility: String): Boolean

    fun deleteReview(igdbId: Int): Boolean

    fun requestFeed(limit: Int = 10, beforeId: String? = null, userId: String? = null)

    fun loadMoreFeed()

    fun likeEvent(eventId: String)

    fun hideEvent(eventId: String)

    fun reportEvent(eventId: String, reason: String? = null)

    fun requestNotifications(limit: Int = 20)

    fun loadMoreNotifications()

    fun markNotificationRead(notificationId: String)

    fun markAllNotificationsRead()

    fun getEvent(eventId: String)

    fun requestEventComments(eventId: String)

    fun commentEvent(eventId: String, content: String)

    fun createPost(body: String, canvasSize: Int? = null, doodleData: String? = null, igdbId: Int? = null, gameTitle: String? = null, public: Boolean = false)

    fun requestCommunityFeed(igdbGameId: Int? = null, limit: Int = 10)

    fun loadMoreCommunityFeed(igdbGameId: Int? = null)

    fun requestCommunityFollows()

    fun followCommunity(igdbGameId: Int, isPrivate: Boolean = false, anonymous: Boolean = false)

    fun unfollowCommunity(igdbGameId: Int)

    fun updateCommunityFollow(igdbGameId: Int, isPrivate: Boolean, anonymous: Boolean)

    fun requestUserSettings()

    fun updateUserSettings(communityAutoShare: Boolean? = null)

    fun updateEventVisibility(eventId: String, public: Boolean)

    fun emitAchievementUnlocked(igdbId: Long?, raGameId: Long?, gameTitle: String, achievementRaId: Long, achievementName: String, achievementDescription: String?, points: Int, badgeName: String?, isHardcore: Boolean, earnedCount: Int, totalCount: Int, unlockedAt: Long): Boolean

    fun emitPerfectGame(igdbId: Long?, gameTitle: String, isHardcore: Boolean, earnedCount: Int, totalCount: Int): Boolean

    suspend fun emitStartedPlaying(igdbId: Long?, gameTitle: String, platformSlug: String)

    suspend fun emitMarathonSession(igdbId: Long?, gameTitle: String, durationMins: Int, platformSlug: String)

    fun createDoodle(canvasSize: Int, data: String, caption: String?, igdbId: Int?, gameTitle: String?)

    suspend fun clearPresenceCovers()
}
