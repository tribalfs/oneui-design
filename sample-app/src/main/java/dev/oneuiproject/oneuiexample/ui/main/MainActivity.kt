package dev.oneuiproject.oneuiexample.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.oneuiproject.oneui.ktx.startPopOverActivity
import dev.oneuiproject.oneui.layout.NavDrawerLayout
import dev.oneuiproject.oneui.navigation.setupNavigation
import dev.oneuiproject.oneui.widget.BottomTabLayout
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast
import dev.oneuiproject.oneuiexample.ui.preference.PreferenceActivity
import dev.oneuiproject.oneui.R as iconsLibR

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel>()

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

            binding.navigationView.findMenuItem(R.id.popup_menu)?.apply {
                setOnMenuItemClickListener {
                    if (binding.drawerLayout.drawerOffset == 0f) {
                        binding.drawerLayout.setDrawerOpen(true)
                    } else {
                        val anchor = binding.navigationView.findViewById<View>(R.id.popup_menu)
                        PopupMenu(context, anchor).apply {
                            seslSetOverlapAnchor(false)
                            setForceShowIcon(true)
                            seslSetOffset(140, 0)
                            inflate(R.menu.menu_popup)
                            menu.findItem(R.id.menu2).isCheckable = true
                            menu.findItem(R.id.menu2).isChecked = true
                            setOnMenuItemClickListener { menuItem ->
                                setTitle(menuItem.title)
                                semToast("${menuItem.title} clicked")
                                true
                            }
                            show()
                        }
                    }

                    true
                }
            }
        }
    }

    val drawerLayout: NavDrawerLayout get() = binding.drawerLayout
    val bottomTab: BottomTabLayout get() = binding.bottomTab

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_SEARCH) {
            binding.drawerLayout.setSearchQueryFromIntent(intent)
        }
    }

    override fun dispatchKeyEvent(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.keyCode
        val action = keyEvent.action
        if (keyCode == KeyEvent.KEYCODE_CTRL_RIGHT || keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
            viewModel.isCtrlKeyPressed.value = action == KeyEvent.ACTION_DOWN
        }
        return super.dispatchKeyEvent(keyEvent)
    }
}
