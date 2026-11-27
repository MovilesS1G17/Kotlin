package com.centralia.app.data.local

import android.content.SharedPreferences
import com.centralia.app.domain.rediscover.Rediscover
import com.centralia.app.domain.rediscover.RediscoverDismissals
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Rediscover's "Not now" (Sprint 4, Enrique): key/value pairs in
 * SharedPreferences (short id → when it was dismissed, epoch ms). A handful of
 * small values that are read whole, so a database would be overkill. Expired
 * entries are removed when read. Disk work runs on Dispatchers.IO.
 */
class SharedPreferencesRediscoverDismissals(
    private val preferences: SharedPreferences
) : RediscoverDismissals {

    override suspend fun active(now: Instant): Set<UUID> = withContext(Dispatchers.IO) {
        val active = HashSet<UUID>()
        val expired = ArrayList<String>()
        for ((key, value) in preferences.all) {
            val at = (value as? Long)?.let(Instant::ofEpochMilli) ?: continue
            val id = runCatching { UUID.fromString(key) }.getOrNull() ?: continue
            if (Duration.between(at, now).toDays() < Rediscover.DISMISS_DAYS) active += id else expired += key
        }
        if (expired.isNotEmpty()) preferences.edit().apply { expired.forEach { key -> remove(key) } }.apply()
        active
    }

    override suspend fun dismiss(videoID: UUID, at: Instant) = withContext(Dispatchers.IO) {
        preferences.edit().putLong(videoID.toString(), at.toEpochMilli()).apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit().clear().apply()
    }
}
