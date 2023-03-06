package gentle.hilt.playground.presentation.app

import android.content.res.Configuration
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import gentle.hilt.playground.R
import gentle.hilt.playground.databinding.PlaygroundActivityBinding

@AndroidEntryPoint
class PlayGroundActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE = -1
        var RINGTONE_ALARM: Ringtone? = null
        var RINGTONE_TIMER: Ringtone? = null
        var ALARM_TONE_URI: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        var RINGTONE_TONE_URI: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        const val MAP_API_KEY: String = "7b4ec66c-573a-4b85-aedc-eb1623b32070"
    }

    private lateinit var binding: PlaygroundActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        binding = PlaygroundActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.setupWithNavController(navController)

        val animations = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setEnterAnim(androidx.navigation.ui.R.anim.nav_default_enter_anim)
            .setExitAnim(androidx.navigation.ui.R.anim.nav_default_pop_exit_anim)
            .setPopEnterAnim(androidx.navigation.ui.R.anim.nav_default_pop_enter_anim)
            .setPopExitAnim(androidx.navigation.ui.R.anim.nav_default_pop_exit_anim)
            .setPopUpTo(navController.graph.startDestinationId, false)
            .build()

        binding.bottomNavigationView.setOnItemSelectedListener { item->
            when(item.itemId){
                R.id.timerFragment ->{
                    navController.navigate(R.id.timerFragment,null,animations)
                }
                R.id.alarmsFragment ->{
                    navController.navigate(R.id.alarmsFragment,null,animations)
                }
                R.id.mapScreenFragment->{
                    navController.navigate(R.id.mapScreenFragment,null,animations)
                }
            }
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mapScreenFragment,
                R.id.alarmsFragment,
                R.id.timerFragment
                -> binding.bottomNavigationView.visibility = View.VISIBLE

                else -> binding.bottomNavigationView.visibility = View.GONE
            }

            when (destination.id) {
                R.id.timerFragment -> {
                    hideBottomNavigationInLandscape()
                }
                R.id.mapScreenFragment -> {
                    hideBottomNavigationInLandscape()
                }
            }
        }
    }

    private fun hideBottomNavigationInLandscape() {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.bottomNavigationView.visibility = View.GONE
        }
    }

    private val navController by lazy {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navHostFragment.navController
    }
}
