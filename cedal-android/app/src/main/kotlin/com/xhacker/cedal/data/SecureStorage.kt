package com.xhacker.cedal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// One saved login - enough to silently mint a fresh access token via
// /auth/refresh (see AuthViewModel.switchAccount) without ever asking for a
// password again, plus enough display info to render a switcher row without
// an extra profile fetch per account. refreshToken here is kept in sync with
// whichever one is currently valid server-side - it rotates on every actual
// use (see AuthService.refresh()), so this gets updated both when this
// account is the one being switched TO and when it's the one being switched
// AWAY FROM (its in-storage token may have rotated since it was last saved).
@Serializable
data class SavedAccount(
    val userId: String,
    val refreshToken: String,
    val email: String? = null,
    val nickname: String? = null,
    val avatarUrl: String? = null,
)

// Android equivalent of the RN app's expo-secure-store `cedal_*` keys.
@Singleton
class SecureStorage @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "cedal_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    // "Offline Mode" (Settings > Privacy) - deliberately makes friends/chats
    // appear empty, exactly as if this device had no internet, regardless
    // of the real connection. A decoy for handing someone your unlocked
    // phone, not a real connectivity setting - see AuthViewModel.
    var offlineModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_OFFLINE_MODE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OFFLINE_MODE_ENABLED, value).apply()

    // "Bot View" (Settings > Privacy) - off by default, same privacy-first
    // default as Friend Hider/Offline Mode. Gates whether MemberChatThreadBody
    // is ever allowed to populate CornealBubbleState.currentChatContext -
    // when off, there is no code path that sends any chat content to
    // Corneal at all, not just a prompt-level instruction to ignore it.
    var botViewEnabled: Boolean
        get() = prefs.getBoolean(KEY_BOT_VIEW_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BOT_VIEW_ENABLED, value).apply()

    // "Bot Access" (Settings > Navigation) - off by default. Whether tapping
    // the Corneal bubble opens it full-screen (off, existing behavior) or as
    // a resizable floating window on top of whatever's on screen (on).
    var botAccessEnabled: Boolean
        get() = prefs.getBoolean(KEY_BOT_ACCESS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BOT_ACCESS_ENABLED, value).apply()

    // "Corneal Hider" (Settings > Privacy) - off by default, same decoy
    // pattern as Friend Hider/Offline Mode. When on, the bubble (and
    // therefore the only way to reach Corneal, since it's no longer a
    // Chats-list row) is hidden everywhere - see CornealBubbleOverlay.
    var cornealHiderEnabled: Boolean
        get() = prefs.getBoolean(KEY_CORNEAL_HIDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CORNEAL_HIDER_ENABLED, value).apply()

    // "Call Out" (Settings > Corneal AI), text-based - off by default, same
    // pattern as Bot View. Gates whether MemberCodeBody is ever allowed to
    // populate CornealBubbleState.currentCodeContext - when off, no code
    // path sends any Code file content to Corneal at all.
    var callOutTextEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALL_OUT_TEXT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CALL_OUT_TEXT_ENABLED, value).apply()

    // "Call Out" (Settings > Corneal AI), real screen-capture - off by
    // default. Unlike the text-based toggle above, turning this on triggers
    // a real Android MediaProjection permission prompt each time it's used
    // (see CallOutScreenCapture) - noticeably more battery-hungry, which
    // Settings warns about explicitly for small/older phones.
    var callOutScreenCaptureEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALL_OUT_SCREEN_CAPTURE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CALL_OUT_SCREEN_CAPTURE_ENABLED, value).apply()

    // Persists across restarts; the live, Compose-observable flag actually
    // driving the UI is ThemeState.isDark (see ui/theme/CedalColors.kt) -
    // MainActivity seeds it from here once on process start, and the
    // Settings toggle writes both at once.
    var darkThemeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME_ENABLED, value).apply()

    // Which purchased theme pack (see ThemePackService) is currently
    // equipped - null means the free default cyan look. Stores both the id
    // (for the Shop's "EQUIPPED" badge) and the resolved accent color (so
    // MainActivity can seed ThemeState.equippedAccentColor without an extra
    // network round-trip on every cold start).
    var equippedThemePackId: String?
        get() = prefs.getString(KEY_EQUIPPED_THEME_PACK_ID, null)
        set(value) = prefs.edit().putString(KEY_EQUIPPED_THEME_PACK_ID, value).apply()

    var equippedThemePackHex: String?
        get() = prefs.getString(KEY_EQUIPPED_THEME_PACK_HEX, null)
        set(value) = prefs.edit().putString(KEY_EQUIPPED_THEME_PACK_HEX, value).apply()

    // Every account ever successfully logged into on this device, most
    // recently used first - not cleared by clearSession()/Sign Out, since
    // the whole point is being able to switch straight back in afterward
    // without re-entering a password. Only ever removed via explicit
    // "remove" in the switcher, or Delete Account server-side (which this
    // doesn't automatically detect - a removed/deleted account just fails
    // to refresh the next time it's tapped).
    var savedAccounts: List<SavedAccount>
        get() {
            val json = prefs.getString(KEY_SAVED_ACCOUNTS, null) ?: return emptyList()
            return try { Json.decodeFromString(json) } catch (e: Exception) { emptyList() }
        }
        set(value) = prefs.edit().putString(KEY_SAVED_ACCOUNTS, Json.encodeToString(value)).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    // "Lock on exit" (like WhatsApp's Screen Lock) — when on, backgrounding
    // the app re-shows the passcode/biometric gate the next time it's
    // foregrounded, even if the process never died. Off by default, same
    // as WhatsApp's own setting.
    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    // "App View Once" - blocks screenshots/screen recording (FLAG_SECURE) and,
    // as an unavoidable side effect of that same OS flag, blanks the Recents
    // thumbnail too - FLAG_SECURE is a single all-or-nothing bit, Android has
    // no API to block one without the other. On by default (was previously
    // mandatory, unconditional), but a real user-controlled toggle per their
    // request.
    var appViewOnceEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_VIEW_ONCE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_VIEW_ONCE_ENABLED, value).apply()

    // "Seeable" - independent control over just the Recents thumbnail, via
    // Activity.setRecentsScreenshotEnabled (API 31+ only - see MainActivity),
    // a genuinely separate mechanism from FLAG_SECURE. Deliberately does NOT
    // factor into shouldBlockScreenCapture below - turning App View Once off
    // must be enough on its own to allow screenshots/recording immediately,
    // it shouldn't also require touching this. Only meaningful when App View
    // Once is off (which already forces blocking regardless via FLAG_SECURE);
    // the Settings row disables this toggle whenever App View Once is on,
    // since it'd have no effect there. Off by default (blocked), same
    // security-first default as App View Once.
    var seeableEnabled: Boolean
        get() = prefs.getBoolean(KEY_SEEABLE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEABLE_ENABLED, value).apply()

    // FLAG_SECURE (screenshot/recording block + blank Recents thumbnail,
    // bundled together, unavoidably) - on if App View Once or Lock on exit
    // want it. Seeable is deliberately not part of this - see its comment.
    val shouldBlockScreenCapture: Boolean
        get() = appViewOnceEnabled || appLockEnabled

    var rememberEmail: String?
        get() = prefs.getString(KEY_REMEMBER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_REMEMBER_EMAIL, value).apply()

    // Where Code > Pad shows a run's result: "webview" (in-app, the default)
    // or "browser" (opens the system browser instead). Applies to every
    // language except Kotlin, which always installs to the phone regardless.
    var codeOutputDestination: String
        get() = prefs.getString(KEY_CODE_OUTPUT_DESTINATION, "webview") ?: "webview"
        set(value) = prefs.edit().putString(KEY_CODE_OUTPUT_DESTINATION, value).apply()

    var passcodeDone: Boolean
        get() = prefs.getBoolean(KEY_PASSCODE_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_PASSCODE_DONE, value).apply()

    // Cached locally so we don't re-show the terms gate on every launch; the
    // server also stores this per-account as the compliance source of truth.
    var acceptedTermsVersion: String?
        get() = prefs.getString(KEY_TERMS_VERSION, null)
        set(value) = prefs.edit().putString(KEY_TERMS_VERSION, value).apply()

    // Bank settings — ported from cedal-mobile's BankSettingsContext, which
    // was in-memory only (React Context, resets every app open). Persisting
    // these is a small, deliberate deviation: they need to be shared
    // between the Settings screen and the Bank Home screen, and this is the
    // same mechanism already used for that (biometricEnabled, appLockEnabled).
    var muteBankNotifications: Boolean
        get() = prefs.getBoolean(KEY_MUTE_BANK_NOTIFICATIONS, false)
        set(value) = prefs.edit().putBoolean(KEY_MUTE_BANK_NOTIFICATIONS, value).apply()

    var bankSecurityAlerts: Boolean
        get() = prefs.getBoolean(KEY_BANK_SECURITY_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_BANK_SECURITY_ALERTS, value).apply()

    var bankLargeSpendAlerts: Boolean
        get() = prefs.getBoolean(KEY_BANK_LARGE_SPEND_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_BANK_LARGE_SPEND_ALERTS, value).apply()

    // Notification sound - real behavior now (see NotificationSound.kt),
    // replacing what used to be a UI-only placeholder in Settings. Both
    // channels (CedalApplication.kt) are created silent on purpose; Cedal
    // plays this itself via MediaPlayer so notificationVolume can actually
    // control loudness, which a plain NotificationChannel sound can't.
    var soundsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUNDS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUNDS_ENABLED, value).apply()

    // A content:// URI string (picked file) or a file:// path (recorded
    // clip) - null means "use the system default notification sound".
    var notificationSoundUri: String?
        get() = prefs.getString(KEY_NOTIFICATION_SOUND_URI, null)
        set(value) = prefs.edit().putString(KEY_NOTIFICATION_SOUND_URI, value).apply()

    // Settings > Chat > Alert sound - up to 3 saved clips (recorded or
    // picked) the user can switch between, distinct from
    // notificationSoundUri above (which is just "whichever one is ACTIVE
    // right now" - unchanged, still what NotificationSound.kt actually
    // plays). Stored as a single delimited string, not a SharedPreferences
    // string SET, since Sets don't guarantee iteration order and the order
    // these were added in matters for a stable on-screen list.
    var notificationSoundClips: List<String>
        get() = prefs.getString(KEY_NOTIFICATION_SOUND_CLIPS, null)
            ?.split(CLIP_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit().putString(KEY_NOTIFICATION_SOUND_CLIPS, value.joinToString(CLIP_SEPARATOR)).apply()

    // 0-100, applied as MediaPlayer.setVolume(value/100f, value/100f).
    var notificationVolume: Int
        get() = prefs.getInt(KEY_NOTIFICATION_VOLUME, 80)
        set(value) = prefs.edit().putInt(KEY_NOTIFICATION_VOLUME, value).apply()

    // A display name from TranslationService.LANGUAGES server-side (e.g.
    // "French") - null means "no translation" (English/pass-through). Kept
    // in sync with the server's own Users.preferredLanguage on every
    // profile fetch/update, same dual-write pattern as other settings here.
    var chatLanguage: String?
        get() = prefs.getString(KEY_CHAT_LANGUAGE, null)
        set(value) = prefs.edit().putString(KEY_CHAT_LANGUAGE, value).apply()

    var typingIndicatorsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TYPING_INDICATORS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TYPING_INDICATORS_ENABLED, value).apply()

    // Settings > Groups > "Join & leave messages" - purely a personal
    // client-side display filter (the row is always generated/stored
    // server-side regardless of this) - see GroupChatThreadScreen.kt.
    var groupJoinLeaveMessagesEnabled: Boolean
        get() = prefs.getBoolean(KEY_GROUP_JOIN_LEAVE_MESSAGES_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GROUP_JOIN_LEAVE_MESSAGES_ENABLED, value).apply()

    // Invest > Learn progress — which lessons the user has ticked off as
    // done, and their last quiz score per lesson. Not security-sensitive,
    // just piggybacking on the same encrypted-prefs instance for simplicity.
    fun isLessonCompleted(lessonTitle: String): Boolean =
        prefs.getStringSet(KEY_LEARN_COMPLETED, emptySet())!!.contains(lessonTitle)

    fun setLessonCompleted(lessonTitle: String, done: Boolean) {
        val set = prefs.getStringSet(KEY_LEARN_COMPLETED, emptySet())!!.toMutableSet()
        if (done) set.add(lessonTitle) else set.remove(lessonTitle)
        prefs.edit().putStringSet(KEY_LEARN_COMPLETED, set).apply()
    }

    fun getQuizScorePercent(lessonTitle: String): Int? {
        val value = prefs.getInt(KEY_LEARN_QUIZ_SCORE_PREFIX + lessonTitle, -1)
        return if (value < 0) null else value
    }

    fun setQuizScorePercent(lessonTitle: String, percent: Int) {
        prefs.edit().putInt(KEY_LEARN_QUIZ_SCORE_PREFIX + lessonTitle, percent).apply()
    }

    // Chat Lock, per-group/per-viewer (Group Profile's "Lock This Chat") -
    // purely local, same pattern as the app-wide lock's biometric check
    // (BiometricAuth), just scoped to one group's thread instead of the
    // whole app. No server column - see GroupChatThreadBody's lock gate.
    fun isGroupLocked(groupId: String): Boolean =
        prefs.getStringSet(KEY_LOCKED_GROUP_IDS, emptySet())!!.contains(groupId)

    fun setGroupLocked(groupId: String, locked: Boolean) {
        val set = prefs.getStringSet(KEY_LOCKED_GROUP_IDS, emptySet())!!.toMutableSet()
        if (locked) set.add(groupId) else set.remove(groupId)
        prefs.edit().putStringSet(KEY_LOCKED_GROUP_IDS, set).apply()
    }

    // One-time Group Rules sheet (Round-2 "Group Rules") - shown once per
    // group on first thread-open, always re-readable from Group Profile
    // regardless of this flag. Local-only, same reasoning as Chat Lock above.
    fun hasSeenGroupRules(groupId: String): Boolean =
        prefs.getStringSet(KEY_GROUP_RULES_SEEN, emptySet())!!.contains(groupId)

    fun markGroupRulesSeen(groupId: String) {
        val set = prefs.getStringSet(KEY_GROUP_RULES_SEEN, emptySet())!!.toMutableSet()
        set.add(groupId)
        prefs.edit().putStringSet(KEY_GROUP_RULES_SEEN, set).apply()
    }

    // Per-conversation "where you left off" - the message ID nearest the top
    // of the viewport when a chat thread was last closed (see
    // MemberChatThreadBody's onDispose/initial-scroll effect). Null = never
    // left this thread scrolled anywhere in particular yet, so it falls back
    // to the existing first-unread-or-bottom behavior. Overwritten (not
    // cleared) every time the thread is left, so it always reflects the
    // most recent close position.
    fun getChatScrollAnchor(friendId: String): String? = prefs.getString(KEY_CHAT_SCROLL_ANCHOR_PREFIX + friendId, null)

    fun setChatScrollAnchor(friendId: String, messageId: String?) {
        if (messageId == null) {
            prefs.edit().remove(KEY_CHAT_SCROLL_ANCHOR_PREFIX + friendId).apply()
        } else {
            prefs.edit().putString(KEY_CHAT_SCROLL_ANCHOR_PREFIX + friendId, messageId).apply()
        }
    }

    // The real, user-picked folder (via Storage Access Framework) that the
    // Code work mode's Documents tab reads/writes into — a "CedalCode"
    // subfolder gets created inside whatever the user picks, so the app
    // never scans or touches their existing files. Persists across app
    // restarts; the URI permission itself is also made persistable via
    // ContentResolver.takePersistableUriPermission at grant time.
    var codeFolderUri: String?
        get() = prefs.getString(KEY_CODE_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_CODE_FOLDER_URI, value).apply()

    // Which file was last open in Pad - restored on reopening Code so it
    // doesn't reset to "nothing entered" just from navigating away and back
    // (the file's actual content is separately safe either way, via
    // codeFolderUri's real on-disk storage; this is purely about not having
    // to re-pick it every time).
    var lastEnteredCodeId: String?
        get() = prefs.getString(KEY_LAST_ENTERED_CODE_ID, null)
        set(value) = prefs.edit().putString(KEY_LAST_ENTERED_CODE_ID, value).apply()

    // Whether the Code work mode's one-time walkthrough overlay has already
    // been shown/dismissed — persists for life, same as other onboarding
    // flags here, so it never reappears uninvited after the first visit
    // (replayable on demand via the "?" help page instead).
    var codeTutorialSeen: Boolean
        get() = prefs.getBoolean(KEY_CODE_TUTORIAL_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_CODE_TUTORIAL_SEEN, value).apply()

    // Force-update gate (see UpdateGateState/AppVersionDto) - the FIRST
    // moment this app noticed it was outdated. Null means "not currently
    // outdated" (or never checked yet) - cleared automatically the moment a
    // real update brings the installed versionCode back up to date.
    // Deliberately local-only: deleting and reinstalling the app wipes this
    // along with everything else, which is the explicit "fresh start"
    // escape hatch the app owner described.
    var updateNoticeFirstShownAt: Long?
        get() = prefs.getLong(KEY_UPDATE_NOTICE_FIRST_SHOWN_AT, -1L).let { if (it < 0) null else it }
        set(value) = prefs.edit().putLong(KEY_UPDATE_NOTICE_FIRST_SHOWN_AT, value ?: -1L).apply()

    // Set once the 2-week grace period from updateNoticeFirstShownAt runs
    // out - blocks sign-in/sign-up entirely (see UpdateGateState/
    // CedalNavGraph) until an actual update lands.
    var forceUpdateGate: Boolean
        get() = prefs.getBoolean(KEY_FORCE_UPDATE_GATE, false)
        set(value) = prefs.edit().putBoolean(KEY_FORCE_UPDATE_GATE, value).apply()

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_ROLE)
            .apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "cedal_access_token"
        private const val KEY_REFRESH_TOKEN = "cedal_refresh_token"
        private const val KEY_USER_ID = "cedal_session_token" // matches RN key name (stores uid)
        private const val KEY_ROLE = "cedal_role"
        private const val KEY_SAVED_ACCOUNTS = "cedal_saved_accounts"
        private const val KEY_OFFLINE_MODE_ENABLED = "cedal_offline_mode_enabled"
        private const val KEY_DARK_THEME_ENABLED = "cedal_dark_theme_enabled"
        private const val KEY_EQUIPPED_THEME_PACK_ID = "cedal_equipped_theme_pack_id"
        private const val KEY_EQUIPPED_THEME_PACK_HEX = "cedal_equipped_theme_pack_hex"
        private const val KEY_BIOMETRIC_ENABLED = "cedal_biometric_enabled"
        private const val KEY_APP_LOCK_ENABLED = "cedal_app_lock_enabled"
        private const val KEY_APP_VIEW_ONCE_ENABLED = "cedal_app_view_once_enabled"
        private const val KEY_SEEABLE_ENABLED = "cedal_seeable_enabled"
        private const val KEY_REMEMBER_EMAIL = "cedal_remember_email"
        private const val KEY_BOT_VIEW_ENABLED = "cedal_bot_view_enabled"
        private const val KEY_BOT_ACCESS_ENABLED = "cedal_bot_access_enabled"
        private const val KEY_CORNEAL_HIDER_ENABLED = "cedal_corneal_hider_enabled"
        private const val KEY_CODE_OUTPUT_DESTINATION = "cedal_code_output_destination"
        private const val KEY_PASSCODE_DONE = "cedal_passcode_done"
        private const val KEY_TERMS_VERSION = "cedal_terms_version"
        private const val KEY_MUTE_BANK_NOTIFICATIONS = "cedal_mute_bank_notifications"
        private const val KEY_BANK_SECURITY_ALERTS = "cedal_bank_security_alerts"
        private const val KEY_BANK_LARGE_SPEND_ALERTS = "cedal_bank_large_spend_alerts"
        private const val KEY_SOUNDS_ENABLED = "cedal_sounds_enabled"
        private const val KEY_NOTIFICATION_SOUND_URI = "cedal_notification_sound_uri"
        private const val KEY_NOTIFICATION_SOUND_CLIPS = "cedal_notification_sound_clips"
        private const val CLIP_SEPARATOR = "|||"
        private const val KEY_NOTIFICATION_VOLUME = "cedal_notification_volume"
        private const val KEY_CHAT_LANGUAGE = "cedal_chat_language"
        private const val KEY_TYPING_INDICATORS_ENABLED = "cedal_typing_indicators_enabled"
        private const val KEY_GROUP_JOIN_LEAVE_MESSAGES_ENABLED = "cedal_group_join_leave_messages_enabled"
        private const val KEY_LEARN_COMPLETED = "cedal_learn_completed"
        private const val KEY_LEARN_QUIZ_SCORE_PREFIX = "cedal_learn_quiz_score_"
        private const val KEY_CHAT_SCROLL_ANCHOR_PREFIX = "cedal_chat_scroll_anchor_"
        private const val KEY_CODE_FOLDER_URI = "cedal_code_folder_uri"
        private const val KEY_LAST_ENTERED_CODE_ID = "cedal_last_entered_code_id"
        private const val KEY_CODE_TUTORIAL_SEEN = "cedal_code_tutorial_seen"
        private const val KEY_UPDATE_NOTICE_FIRST_SHOWN_AT = "cedal_update_notice_first_shown_at"
        private const val KEY_FORCE_UPDATE_GATE = "cedal_force_update_gate"
        private const val KEY_CALL_OUT_TEXT_ENABLED = "cedal_call_out_text_enabled"
        private const val KEY_CALL_OUT_SCREEN_CAPTURE_ENABLED = "cedal_call_out_screen_capture_enabled"
        private const val KEY_LOCKED_GROUP_IDS = "cedal_locked_group_ids"
        private const val KEY_GROUP_RULES_SEEN = "cedal_group_rules_seen"
    }
}
