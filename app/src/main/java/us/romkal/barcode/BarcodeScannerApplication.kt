package us.romkal.barcode

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.Cache
import okhttp3.OkHttpClient

class BarcodeScannerApplication : Application(), SingletonImageLoader.Factory {

  val httpClient by lazy {
    OkHttpClient.Builder()
      .cache(
        Cache(
          directory = cacheDir,
          maxSize = 10L * 1024L * 1024L // 10 MiB
        )
      )
      .build()
  }

  override fun newImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
      .components {
        OkHttpNetworkFetcherFactory(
          callFactory = {
            httpClient
          }
        )
      }
      .build()
  }
}
