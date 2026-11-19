package app.kairos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Kept deliberately empty: Kairos must open in under two seconds from a widget,
 * tile or lock screen, so nothing is initialised eagerly here. Room, DataStore
 * and everything else are created lazily by Hilt on first use.
 */
@HiltAndroidApp
class KairosApp : Application()
