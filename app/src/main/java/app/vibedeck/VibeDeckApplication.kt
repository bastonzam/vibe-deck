package app.vibedeck

import android.app.Application
import app.vibedeck.data.metadata.PublicHostDns
import coil.ImageLoader
import coil.ImageLoaderFactory

class VibeDeckApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { PublicHostDns.safeClientBuilder().callTimeout(10, java.util.concurrent.TimeUnit.SECONDS).build() }
        .crossfade(true).build()
}
