package com.nadelon.app.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

// Wired into the manifest via OPTIONS_PROVIDER_CLASS_NAME meta-data. Uses the default
// Styled Media Receiver (CC1AD845) — no custom receiver needed for casting MP4 / HLS
// directly from Nadelon to a Chromecast.
class NadelonCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId("CC1AD845")
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
