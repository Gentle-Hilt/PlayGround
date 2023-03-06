package gentle.hilt.playground.presentation.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.MAP_API_KEY
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PlayGroundApplication : Application() {
    @Inject
    lateinit var dataStore: DataStoreManager

    companion object {
        const val TIMBER_WILL_JUMP_TO_MESSAGE_FROM_LOGCAT = 4
    }

    override fun onCreate() {
        super.onCreate()
        val formatStrategy: FormatStrategy = PrettyFormatStrategy
            .newBuilder()
            .tag("")
            .methodCount(1)
            .methodOffset(TIMBER_WILL_JUMP_TO_MESSAGE_FROM_LOGCAT)
            .build()
        Logger.addLogAdapter(AndroidLogAdapter(formatStrategy))
        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                Logger.log(priority, "-$tag", message, t)
            }
        })

        MapKitFactory.setApiKey(MAP_API_KEY)

        runBlocking {
            withContext(Dispatchers.IO) {
                val nightModeEnabled = dataStore.darkModeEnabled.first()
                if (nightModeEnabled) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }
}
