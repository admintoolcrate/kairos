package app.kairos.stagira

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Hands "an urge resisted" to Stagira on this phone with an explicit intent.
 * No server, no account, nothing leaves the device. Stagira shows its own
 * confirmation and goal picker; Kairos only learns whether it was recorded.
 */
object StagiraBridge {
    const val PACKAGE = "cc.toolcrate.stagira"
    const val ACTION = "cc.toolcrate.stagira.action.RECORD_URGE"
    const val EXTRA_SOURCE = "source"
    const val EXTRA_AT = "at"

    fun intent(atMillis: Long = System.currentTimeMillis()): Intent =
        Intent(ACTION).setPackage(PACKAGE).putExtra(EXTRA_SOURCE, "kairos").putExtra(EXTRA_AT, atMillis)

    /** True only when an installed Stagira understands the request. */
    fun isAvailable(context: Context): Boolean = runCatching {
        context.packageManager.resolveActivity(intent(), PackageManager.MATCH_DEFAULT_ONLY) != null
    }.getOrDefault(false)
}
