package dev.oneuiproject.oneuiexample.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import com.sec.sesl.tester.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.oneuiproject.oneui.ktx.startPopOverActivity
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.navigation.setupNavigation
import dev.oneuiproject.oneui.widget.BottomTabLayout
import dev.oneuiproject.oneuiexample.ui.preference.PreferenceActivity
import dev.oneuiproject.oneui.R as iconsLibR

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initDrawer()
    }

    private fun initDrawer() {
        binding.drawerLayout.apply {
            setupHeaderButton(
                icon = ContextCompat.getDrawable(context, iconsLibR.drawable.ic_oui_settings_outline)!!,
                tooltipText = "Preferences",
                listener = { startPopOverActivity(Intent(this@MainActivity, PreferenceActivity::class.java)) }
            )
            lockNavRailOnActionMode = true
            lockNavRailOnSearchMode = true
            setDrawerLockListener { binding.navigationView.updateLock(it) }
            setNavRailContentMinSideMargin(14)

            val navHostFragment = binding.navHostMain.getFragment<NavHostFragment>()
            setupNavigation(binding.navigationView, navHostFragment)

        }
    }

    val drawerLayout: DrawerLayout get() = binding.drawerLayout
    val bottomTab: BottomTabLayout get() = binding.bottomTab

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_SEARCH) {
            binding.drawerLayout.setSearchQueryFromIntent(intent)
        }
    }
}
