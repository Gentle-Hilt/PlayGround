package gentle.hilt.playground.playground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import gentle.hilt.playground.R

@AndroidEntryPoint
class PlayGroundActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.playground_activity)
    }
}