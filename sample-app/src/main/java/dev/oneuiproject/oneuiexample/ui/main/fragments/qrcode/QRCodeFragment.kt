package dev.oneuiproject.oneuiexample.ui.main.fragments.qrcode

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.qr.app.QrScanConfig
import dev.oneuiproject.oneui.qr.app.QrScanContract
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QRCodeFragment : AbsBaseFragment(R.layout.fragment_qr_code) {

    private var qrScanLauncher  = registerForActivityResult(QrScanContract()) { result ->
        if (result != null) {
            val wifi = parseWifiQrContent(result) ?: return@registerForActivityResult
            lifecycleScope.launch {
                delay(500)
                AlertDialog.Builder(requireContext())
                    .setTitle("Scanned Wi-Fi")
                    .setMessage(
                        "SSID: ${wifi.ssid}" +
                                "\nPassword: ${wifi.password}" +
                                "\nAuthType: ${wifi.authType}"
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_qr_code, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when(menuItem.itemId){
                R.id.menu_qr_scan -> {
                    val config = QrScanConfig(
                        title = "Scan a Wi-Fi QR",
                        requiredPrefix = "WIFI:",
                        regex = "^WIFI:.*$"
                    )
                    qrScanLauncher.launch(config)
                    return true
                }
            }
            return false
        }
    }
}