package dev.oneuiproject.oneuiexample.ui.preference

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sec.sesl.tester.databinding.ActivityPreferencesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreferenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferencesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPreferencesBinding.inflate(layoutInflater).apply {
            setContentView(root)
            toolbarLayout.setTitle("Preferences")
            toolbarLayout.showNavigationButtonAsBack = true
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(binding.fragContainer.id,
                    PreferencesFragment()
                )
                .commitNow()
        }
    }

}