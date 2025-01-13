package dev.oneuiproject.oneuiexample.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sec.sesl.tester.databinding.ActivityPreferencesBinding
import dev.oneuiproject.oneuiexample.ui.fragment.PreferencesFragment


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
                .replace(binding.fragContainer.id, PreferencesFragment())
                .commitNow()
        }
    }

}