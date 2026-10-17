package com.nendo.argosy.data.social

import com.nendo.argosy.data.social.uploader.PlaySessionBackfill
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class SocialRepository @Inject constructor() {
    actual data class FriendCode(actual val code: String, actual val url: String)

    actual companion object {}

    actual val connectionState: StateFlow<SocialConnectionState> = TODO("desktop: connectionState")

    actual val friendCode: StateFlow<FriendCode?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val friends: StateFlow<List<Friend>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val friendsActivity: StateFlow<Map<Int, List<FriendActivity>>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())

    actual val quayPassCheckins: StateFlow<List<QuayPassCheckin>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val sharedCollections: StateFlow<List<CollectionSummary>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val savedCollections: StateFlow<List<CollectionSummary>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val feedEvents: StateFlow<List<FeedEventDto>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val reviewSummaries: StateFlow<Map<Int, ReviewSummary>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())

    actual val gameReviews: StateFlow<Map<Int, GameReviewsPage>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())

    actual val reviewWriteEvents: SharedFlow<ReviewWriteEvent> = kotlinx.coroutines.flow.MutableSharedFlow()

    actual val discordLinked: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val discordUsername: StateFlow<String?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val feedHasMore: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val isLoadingFeed: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val eventComments: StateFlow<Map<String, List<FeedComment>>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())

    actual val unreadCount: StateFlow<Int> = kotlinx.coroutines.flow.MutableStateFlow(0)

    actual val notifications: StateFlow<List<SocialNotification>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val notificationsHasMore: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val isLoadingNotifications: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val communityFollows: StateFlow<List<CommunityFollow>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val communityFeed: StateFlow<List<FeedEventDto>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val communityFeedHasMore: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val isLoadingCommunityFeed: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val userSettings: StateFlow<UserSettings> = kotlinx.coroutines.flow.MutableStateFlow(UserSettings())

    actual val hiddenGameIds: StateFlow<Set<Int>> = kotlinx.coroutines.flow.MutableStateFlow(emptySet())

    actual val userProfile: StateFlow<UserProfileData?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val isLoadingProfile: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val netplayInvites: SharedFlow<NetplayInvitePayload> = kotlinx.coroutines.flow.MutableSharedFlow()

    actual val authState: StateFlow<SocialAuthManager.AuthState> = TODO("desktop: authState")

    actual val serviceConnectionState: StateFlow<ArgosSocialService.ConnectionState> = TODO("desktop: serviceConnectionState")

    actual suspend fun startAuth(): SocialAuthManager.AuthResult = TODO("desktop: startAuth")

    actual fun cancelAuth() {
    }

    actual suspend fun suspendForAccountSwitch() {
    }

    actual suspend fun resumeAfterAccountSwitch() {
    }

    actual suspend fun logout() {
    }

    actual fun sendPresence(status: PresenceStatus, gameIgdbId: Int?, gameTitle: String?, media: MediaPresence?): Boolean = false

    actual fun isConnected(): Boolean = false

    actual fun reconnectIfNeeded() {
    }

    actual fun requestUserProfile(userId: String?) {
    }

    actual fun sendFriendRequest(userId: String) {
    }

    actual fun acceptFriend(userId: String) {
    }

    actual fun removeFriend(userId: String) {
    }

    actual fun requestQuayPassCheckins() {
    }

    actual fun requestDiscordTokens() {
    }

    actual fun toggleFavoriteFriend(friendId: String) {
    }

    actual fun toggleGameVisibility(igdbGameId: Int?, steamAppId: Int?, isCurrentlyHidden: Boolean) {
    }

    actual fun requestFriendCode() {
    }

    actual fun regenerateFriendCode() {
    }

    actual fun addFriendByCode(code: String) {
    }

    actual suspend fun backfillRomMPlaySessions(): PlaySessionBackfill.Summary = TODO("desktop: backfillRomMPlaySessions")

    actual suspend fun syncUnsharedAchievements() {
    }

    actual fun requestReviewSummary(igdbId: Int) {
    }

    actual fun requestGameReviews(igdbId: Int) {
    }

    actual fun loadMoreGameReviews(igdbId: Int) {
    }

    actual fun upsertReview(igdbId: Int, recommended: Boolean, body: String?, visibility: String): Boolean = false

    actual fun deleteReview(igdbId: Int): Boolean = false

    actual fun requestFeed(limit: Int, beforeId: String?, userId: String?) {
    }

    actual fun loadMoreFeed() {
    }

    actual fun likeEvent(eventId: String) {
    }

    actual fun hideEvent(eventId: String) {
    }

    actual fun reportEvent(eventId: String, reason: String?) {
    }

    actual fun requestNotifications(limit: Int) {
    }

    actual fun loadMoreNotifications() {
    }

    actual fun markNotificationRead(notificationId: String) {
    }

    actual fun markAllNotificationsRead() {
    }

    actual fun getEvent(eventId: String) {
    }

    actual fun requestEventComments(eventId: String) {
    }

    actual fun commentEvent(eventId: String, content: String) {
    }

    actual fun createPost(body: String, canvasSize: Int?, doodleData: String?, igdbId: Int?, gameTitle: String?, public: Boolean) {
    }

    actual fun requestCommunityFeed(igdbGameId: Int?, limit: Int) {
    }

    actual fun loadMoreCommunityFeed(igdbGameId: Int?) {
    }

    actual fun requestCommunityFollows() {
    }

    actual fun followCommunity(igdbGameId: Int, isPrivate: Boolean, anonymous: Boolean) {
    }

    actual fun unfollowCommunity(igdbGameId: Int) {
    }

    actual fun updateCommunityFollow(igdbGameId: Int, isPrivate: Boolean, anonymous: Boolean) {
    }

    actual fun requestUserSettings() {
    }

    actual fun updateUserSettings(communityAutoShare: Boolean?) {
    }

    actual fun updateEventVisibility(eventId: String, public: Boolean) {
    }

    actual fun emitAchievementUnlocked(igdbId: Long?, raGameId: Long?, gameTitle: String, achievementRaId: Long, achievementName: String, achievementDescription: String?, points: Int, badgeName: String?, isHardcore: Boolean, earnedCount: Int, totalCount: Int, unlockedAt: Long): Boolean = false

    actual fun emitPerfectGame(igdbId: Long?, gameTitle: String, isHardcore: Boolean, earnedCount: Int, totalCount: Int): Boolean = false

    actual suspend fun emitStartedPlaying(igdbId: Long?, gameTitle: String, platformSlug: String) {
    }

    actual suspend fun emitMarathonSession(igdbId: Long?, gameTitle: String, durationMins: Int, platformSlug: String) {
    }

    actual fun createDoodle(canvasSize: Int, data: String, caption: String?, igdbId: Int?, gameTitle: String?) {
    }

    actual suspend fun clearPresenceCovers() {
    }
}
