package app.kairos.entry

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.kairos.MainActivity
import app.kairos.data.Mode
import app.kairos.data.Prefs
import app.kairos.panic.PanicActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile: opens whichever mode was used last. Works from the lock
 * screen: panic help opens over it straight away; urge mode asks to unlock first.
 * Until anything has been used, it opens panic help, the one that can't wait.
 */
@AndroidEntryPoint
class KairosTileService : TileService() {
    @Inject lateinit var prefs: Prefs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var mode: Mode = Mode.Panic

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            mode = prefs.lastUsedMode() ?: Mode.Panic
            refresh()
        }
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        tile.label = "Kairos"
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = if (mode == Mode.Panic) "Panic help" else "Ride out an urge"
        tile.contentDescription = if (mode == Mode.Panic) "Kairos, panic help" else "Kairos, ride out an urge"
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (mode == Mode.Panic) {
            open(PanicActivity.intent(this), 1)
        } else if (isLocked) {
            unlockAndRun { open(MainActivity.urgeIntent(this), 2) }
        } else {
            open(MainActivity.urgeIntent(this), 2)
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun open(intent: Intent, code: Int) {
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
