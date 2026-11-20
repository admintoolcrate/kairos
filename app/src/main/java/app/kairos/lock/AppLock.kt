package app.kairos.lock

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.kairos.data.LockSecrets
import app.kairos.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional lock for history, settings and backups.
 *
 * It is never consulted by panic help, urge mode, the tile, the widget or the
 * shortcuts: those must always open instantly. Unlocking lasts until Kairos
 * goes to the background (apart from trips out to a file picker or the dialer).
 */
@Singleton
class AppLock @Inject constructor(private val prefs: Prefs) {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()
    private var expectingReturn = false
    private var backoff = PinBackoff()
    private var observing = false

    fun observeProcess() {
        if (observing) return
        observing = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (expectingReturn) expectingReturn = false else _unlocked.value = false
            }
        })
    }

    /** Call before opening a picker or another app, so returning doesn't ask again. */
    fun expectReturn() { expectingReturn = true }

    fun unlock() { _unlocked.value = true; backoff = PinBackoff() }

    sealed interface PinResult {
        data object Ok : PinResult
        data object Wrong : PinResult
        data class Wait(val seconds: Long) : PinResult
    }

    fun tryPin(pin: String, secrets: LockSecrets, now: Long = System.currentTimeMillis()): PinResult {
        if (!backoff.canTry(now)) return PinResult.Wait((backoff.waitMillis(now) + 999) / 1000)
        return if (PinHasher.verify(pin, PinHasher.Hashed(secrets.pinHash, secrets.pinSalt))) {
            unlock(); PinResult.Ok
        } else {
            backoff = backoff.failed(now)
            if (backoff.canTry(now)) PinResult.Wrong else PinResult.Wait((backoff.waitMillis(now) + 999) / 1000)
        }
    }

    suspend fun setPin(pin: String, biometric: Boolean) {
        val h = PinHasher.hash(pin)
        prefs.setLock(LockSecrets(enabled = true, biometric = biometric, pinHash = h.hash, pinSalt = h.salt))
        unlock()
    }

    suspend fun setBiometric(on: Boolean, current: LockSecrets) = prefs.setLock(current.copy(biometric = on))

    suspend fun turnOff() = prefs.setLock(LockSecrets(false, true, "", ""))

    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { unlock(); onSuccess() }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Kairos")
            .setSubtitle("For your history and settings")
            .apply {
                if (Build.VERSION.SDK_INT >= 30) setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                else setAllowedAuthenticators(BIOMETRIC_WEAK).setNegativeButtonText("Use PIN")
            }
            .build()
        prompt.authenticate(info)
    }

    companion object {
        fun biometricAvailable(context: Context): Boolean {
            val auth = if (Build.VERSION.SDK_INT >= 30) BIOMETRIC_WEAK or DEVICE_CREDENTIAL else BIOMETRIC_WEAK
            return BiometricManager.from(context).canAuthenticate(auth) == BiometricManager.BIOMETRIC_SUCCESS
        }
    }
}
