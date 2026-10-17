package com.nendo.argosy.data.remote.romm

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.CONNECTIVITY_SERVICE
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformConnectivityManager as ConnectivityManager
import com.nendo.argosy.platform.activeNetwork
import com.nendo.argosy.platform.getNetworkCapabilities
import com.nendo.argosy.platform.registerDefaultNetworkCallback
import com.nendo.argosy.platform.unregisterNetworkCallback
import com.nendo.argosy.platform.hasCapability
import com.nendo.argosy.platform.PlatformNetworkCapabilities as NetworkCapabilities
import com.nendo.argosy.platform.NET_CAPABILITY_INTERNET
import com.nendo.argosy.platform.PlatformSystemClock as SystemClock
import com.nendo.argosy.data.remote.ssl.UserCertStore
import com.nendo.argosy.data.remote.ssl.UserCertTrustManager.withUserCertTrust
import com.nendo.argosy.util.Logger
import com.nendo.argosy.di.ApplicationContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.X509TrustManager

private const val TAG = "RomMReachability"
private const val REACHABLE_FRESH_MS = 30_000L
private const val UNREACHABLE_FRESH_MS = 10_000L
private const val PROBE_TIMEOUT_SECONDS = 5L
private const val HEARTBEAT_PATH = "api/heartbeat"

class RomMUnreachableException(message: String) : IOException(message)

private val GATEWAY_FAILURE_CODES = setOf(502, 503, 504)

internal fun isGatewayFailure(code: Int): Boolean = code in GATEWAY_FAILURE_CODES

internal fun heartbeatGotAnyResponse(callFactory: Call.Factory, root: String): Boolean = try {
    callFactory.newCall(Request.Builder().url(root + HEARTBEAT_PATH).build()).execute()
        .use { !isGatewayFailure(it.code) }
} catch (_: Exception) {
    false
}

@Singleton
class RomMReachability @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userCertStore: UserCertStore
) {
    private val reprobeExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "RomMReprobe").apply { isDaemon = true }
    }
    private val ledger = ReachabilityLedger(
        now = SystemClock::elapsedRealtime,
        probe = { root -> heartbeatGotAnyResponse(probeClient(), root) },
        runInBackground = reprobeExecutor::execute
    )

    private var cachedProbeClient: OkHttpClient? = null
    private var probeClientTrust: X509TrustManager? = null

    private fun probeClient(): OkHttpClient {
        val trust = userCertStore.trustManager()
        cachedProbeClient?.takeIf { probeClientTrust === trust }?.let { return it }
        return OkHttpClient.Builder()
            .connectTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .withUserCertTrust(userCertStore)
            .build()
            .also {
                cachedProbeClient = it
                probeClientTrust = trust
            }
    }

    val interceptor: Interceptor = Interceptor { chain ->
        val request = chain.request()
        val root = serverRoot(request.url)
        if (root == null || request.url.encodedPath.endsWith(HEARTBEAT_PATH)) {
            return@Interceptor chain.proceed(request)
        }
        if (!isReachable(root)) {
            throw RomMUnreachableException("RomM at $root is not reachable")
        }
        proceedRecording(chain, request, root)
    }

    fun hasActiveNetwork(): Boolean {
        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NET_CAPABILITY_INTERNET)
    }

    fun recordReachable(baseUrl: String) = ledger.recordReachable(baseUrl.trimEnd('/') + "/")

    fun mark(): Long = SystemClock.elapsedRealtime()

    fun answeredSince(baseUrl: String, mark: Long): Boolean = ledger.answeredSince(baseUrl.trimEnd('/') + "/", mark)

    fun isReachable(baseUrl: String): Boolean {
        if (!hasActiveNetwork()) return false
        return ledger.isReachable(baseUrl.trimEnd('/') + "/")
    }

    private fun proceedRecording(chain: Interceptor.Chain, request: Request, root: String): Response =
        try {
            chain.proceed(request).also { response ->
                if (isGatewayFailure(response.code)) ledger.recordUnreachable(root) else recordReachable(root)
            }
        } catch (e: IOException) {
            if (chain.call().isCanceled()) throw e
            ledger.recordUnreachable(root)
            throw e
        }

    private fun serverRoot(url: HttpUrl): String? {
        val full = url.toString()
        val apiIndex = full.indexOf("/api/")
        if (apiIndex < 0) return null
        return full.substring(0, apiIndex + 1)
    }
}

internal class ReachabilityLedger(
    private val now: () -> Long,
    private val probe: (String) -> Boolean,
    private val runInBackground: (Runnable) -> Unit
) {
    private val lock = Any()
    private val lastReachableAt = mutableMapOf<String, Long>()
    private val lastUnreachableAt = mutableMapOf<String, Long>()
    private val reprobing = mutableSetOf<String>()

    fun isReachable(root: String): Boolean {
        synchronized(lock) {
            val at = now()
            lastReachableAt[root]?.let { if (at - it < REACHABLE_FRESH_MS) return true }
            lastUnreachableAt[root]?.let { failedAt ->
                if (at - failedAt >= UNREACHABLE_FRESH_MS && reprobing.add(root)) {
                    runInBackground(Runnable { reprobe(root) })
                }
                return false
            }
            return record(root, probe(root))
        }
    }

    fun recordReachable(root: String) {
        synchronized(lock) { record(root, true) }
    }

    fun recordUnreachable(root: String) {
        synchronized(lock) { record(root, false) }
    }

    fun answeredSince(root: String, mark: Long): Boolean =
        synchronized(lock) { lastReachableAt[root]?.let { it >= mark } == true }

    private fun reprobe(root: String) {
        val reachable = probe(root)
        synchronized(lock) {
            reprobing.remove(root)
            record(root, reachable)
        }
        if (reachable) Logger.info(TAG, "heartbeat answered again at $root")
    }

    private fun record(root: String, reachable: Boolean): Boolean {
        if (reachable) {
            lastReachableAt[root] = now()
            lastUnreachableAt.remove(root)
        } else {
            lastReachableAt.remove(root)
            lastUnreachableAt[root] = now()
            Logger.info(TAG, "heartbeat probe failed at $root, failing calls fast until a probe answers")
        }
        return reachable
    }
}
