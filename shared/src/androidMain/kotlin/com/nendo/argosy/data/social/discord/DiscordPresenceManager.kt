package com.nendo.argosy.data.social.discord

import android.app.Activity
import android.util.Log
import com.nendo.argosy.config.AppBuildConfig as BuildConfig
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nendo.argosy.data.emulator.PlaySessionTracker
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import com.nendo.argosy.data.social.DiscordTokenHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class DiscordPresenceManager @Inject constructor(
    private val tokenHolder: DiscordTokenHolder,
    private val playSessionTracker: PlaySessionTracker,
    private val preferencesRepository: UserPreferencesRepository,
    private val gameDao: GameDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow<DiscordPresenceState>(DiscordPresenceState.Disconnected)
    actual val state: StateFlow<DiscordPresenceState> = _state.asStateFlow()

    private var sdkAvailable = false
    private var sdkClient: Any? = null
    private var callbackJob: Job? = null
    private var isInForeground = true

    fun init(activity: Activity) {
        if (!BuildConfig.DISCORD_SDK_ENABLED) {
            Log.d(TAG, "Discord SDK disabled via build config")
            _state.value = DiscordPresenceState.Unavailable
            return
        }
        if (BuildConfig.DISCORD_APP_ID.isBlank()) {
            Log.w(TAG, "Discord Application ID not configured")
            _state.value = DiscordPresenceState.Unavailable
            return
        }
        if (!initSdk(activity)) return
        observeLifecycle()
        observeTokenChanges()
        observePresenceUpdates()
    }

    private fun initSdk(activity: Activity): Boolean {
        return try {
            val clazz = Class.forName("com.discord.socialsdk.DiscordSocialSdkInit")
            val method = clazz.getMethod("setEngineActivity", Activity::class.java)
            method.invoke(null, activity)
            sdkAvailable = true
            Log.d(TAG, "Discord Social SDK engine initialized (appId=${BuildConfig.DISCORD_APP_ID})")
            true
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Discord Social SDK not available (AAR not bundled)")
            _state.value = DiscordPresenceState.Unavailable
            false
        } catch (e: Exception) {
            Log.e(TAG, "Discord Social SDK init failed", e)
            _state.value = DiscordPresenceState.Unavailable
            false
        }
    }

    private fun observeLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isInForeground = true
                if (sdkClient != null) startCallbackLoop()
            }

            override fun onStop(owner: LifecycleOwner) {
                isInForeground = false
                callbackJob?.cancel()
                callbackJob = null
            }
        })
    }

    private fun observeTokenChanges() {
        scope.launch {
            tokenHolder.token.collect { token ->
                if (token != null && tokenHolder.isValid()) {
                    connectSdk(token.accessToken, token.tokenType)
                } else {
                    disconnectSdk()
                }
            }
        }
    }

    private fun observePresenceUpdates() {
        scope.launch {
            combine(
                playSessionTracker.activeSession,
                tokenHolder.token,
                preferencesRepository.userPreferences.map { it.discordRichPresenceEnabled }.distinctUntilChanged()
            ) { session, token, enabled ->
                Triple(session, token, enabled)
            }.collect { (session, token, enabled) ->
                if (!sdkAvailable || sdkClient == null || token == null) return@collect

                if (!enabled) {
                    clearPresence()
                    return@collect
                }

                if (session != null) {
                    val game = gameDao.getById(session.gameId)
                    val title = game?.title ?: "Unknown Game"
                    val platform = game?.platformSlug
                    updatePresence(title, platform, session.startTime.toEpochMilli())
                } else {
                    clearPresence()
                }
            }
        }
    }

    private fun connectSdk(accessToken: String, tokenType: String) {
        if (!sdkAvailable) return
        try {
            // SDK integration point:
            // val client = DiscordClient(BuildConfig.DISCORD_APP_ID.toLong())
            // client.UpdateToken(tokenType, accessToken)
            // client.Connect()
            // sdkClient = client
            Log.d(TAG, "Discord SDK connect: appId=${BuildConfig.DISCORD_APP_ID}, tokenType=$tokenType")
            _state.value = DiscordPresenceState.Connected
            startCallbackLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Discord SDK connect failed", e)
            _state.value = DiscordPresenceState.Error(e.message ?: "Connect failed")
        }
    }

    private fun disconnectSdk() {
        try {
            callbackJob?.cancel()
            callbackJob = null
            if (sdkClient != null) {
                // client.Disconnect()
                sdkClient = null
                Log.d(TAG, "Discord SDK disconnected")
            }
            if (_state.value != DiscordPresenceState.Unavailable) {
                _state.value = DiscordPresenceState.Disconnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Discord SDK disconnect failed", e)
        }
    }

    private fun updatePresence(title: String, platform: String?, startTimestamp: Long) {
        if (!sdkAvailable || sdkClient == null) return
        try {
            // SDK integration point:
            // val activity = DiscordActivity()
            // activity.setDetails("Playing $title")
            // if (platform != null) activity.setState(platform)
            // activity.timestamps.start = startTimestamp / 1000
            // client.UpdateRichPresence(activity)
            Log.d(TAG, "Discord presence update: $title ($platform)")
            _state.value = DiscordPresenceState.Active(title)
        } catch (e: Exception) {
            Log.e(TAG, "Discord presence update failed", e)
            _state.value = DiscordPresenceState.Error(e.message ?: "Presence update failed")
        }
    }

    private fun clearPresence() {
        if (!sdkAvailable || sdkClient == null) return
        try {
            // client.ClearRichPresence()
            Log.d(TAG, "Discord presence cleared")
            _state.value = DiscordPresenceState.Connected
        } catch (e: Exception) {
            Log.e(TAG, "Discord presence clear failed", e)
        }
    }

    private fun startCallbackLoop() {
        if (callbackJob?.isActive == true) return
        callbackJob = scope.launch {
            while (isActive && isInForeground) {
                try {
                    // client.RunCallbacks()
                } catch (e: Exception) {
                    Log.w(TAG, "Discord RunCallbacks error", e)
                }
                delay(CALLBACK_INTERVAL_MS)
            }
        }
    }

    actual companion object {
        private const val TAG = "DiscordPresenceManager"
        private const val CALLBACK_INTERVAL_MS = 100L
    }
}
