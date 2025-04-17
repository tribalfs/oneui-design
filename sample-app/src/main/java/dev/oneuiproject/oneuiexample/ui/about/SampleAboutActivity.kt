package dev.oneuiproject.oneuiexample.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sec.sesl.tester.BuildConfig
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.layout.AppInfoLayout
import dev.oneuiproject.oneui.layout.AppInfoLayout.Status
import dev.oneuiproject.oneui.widget.Toast
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast

class SampleAboutActivity : AppCompatActivity() {
    private var appInfoLayout: AppInfoLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample3_activity_about)

        appInfoLayout = findViewById<AppInfoLayout?>(R.id.appInfoLayout).apply {
            addOptionalText("OneUI Design version " + BuildConfig.ONEUI_DESIGN_VERSION)
            setMainButtonClickListener {
                semToast("Main button clicked! updateState: $updateStatus")
            }
        }

    }

    fun changeStatus(v: View?) {
        appInfoLayout!!.updateStatus = when (appInfoLayout!!.updateStatus){
            is Status.Failed -> Status.Loading
            Status.Loading ->  Status.NoConnection
            Status.NoConnection -> Status.NoUpdate
            Status.NoUpdate -> Status.NotUpdatable
            Status.NotUpdatable -> Status.UpdateAvailable
            Status.UpdateAvailable -> Status.UpdateDownloaded
            Status.UpdateDownloaded -> Status.Unset
            Status.Unset -> Status.Failed("Failed!")
        }
    }

    fun openGitHubPage(v: View?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(Uri.parse("https://github.com/tribalfs/oneui-design"))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this, "No suitable activity found", Toast.LENGTH_SHORT
            ).show()
        }
    }
}