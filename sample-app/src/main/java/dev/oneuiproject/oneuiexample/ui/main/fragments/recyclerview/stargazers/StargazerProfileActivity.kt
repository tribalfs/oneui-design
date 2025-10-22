package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers

import SharingUtils.isSamsungQuickShareAvailable
import SharingUtils.share
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.IntentCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.ActivityStargazerBinding
import dev.oneuiproject.oneui.ktx.onSingleClick
import dev.oneuiproject.oneui.ktx.semSetToolTipText
import dev.oneuiproject.oneui.widget.CardItemView
import dev.oneuiproject.oneuiexample.data.stargazers.model.Stargazer
import dev.oneuiproject.oneuiexample.ui.main.core.util.openUrl
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.loadImageFromUrl
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.openEmail
import kotlinx.coroutines.launch
import kotlin.jvm.java
import dev.oneuiproject.oneui.R as designR


class StargazerProfileActivity : AppCompatActivity(){

    companion object{
        private const val TAG = "ProfileActivity"
        const val KEY_STARGAZER = "key_stargazer"
        const val KEY_TRANSITION_CONTAINER = "key_transition_name"
        const val KEY_TRANSITION_AVATAR = "key_transition_name_avatar"
        const val KEY_TRANSITION_NAME = "key_transition_name_sgname"
    }

    private lateinit var binding: ActivityStargazerBinding
    private lateinit var stargazer: Stargazer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStargazerBinding.inflate(layoutInflater)

        binding.toolbarLayout.showNavigationButtonAsBack = true
        setContentView(binding.root)
        initContent()
        setupBottomNav()
    }



    private fun initContent() {
        stargazer = IntentCompat.getParcelableExtra(intent, KEY_STARGAZER, Stargazer::class.java)!!

        with (stargazer) {
            binding.stargazerAvatar.loadImageFromUrl(avatar_url)
            binding.stargazerName.text = getDisplayName()
            binding.stargazerGithubUrl.text = html_url

            binding.stargazerButtons.stargazerGithubBtn.apply {
                onSingleClick { openUrl(html_url) }
                semSetToolTipText(html_url)
            }

            email?.let {e ->
                binding.stargazerButtons.stargazerEmailBtn.apply {
                    isVisible = true
                    onSingleClick { openEmail(e) }
                    semSetToolTipText(e)
                }
            }

            twitter_username?.let {x ->
                binding.stargazerButtons.stargazerTwitterBtn.apply {
                    isVisible = true
                    onSingleClick { openUrl("https://x.com/$x") }
                    semSetToolTipText(x)
                }
            }

            stargazer.blog?.let {b ->
                if (b.isEmpty()) return@let
                binding.stargazerButtons.stargazerBlog.apply {
                    isVisible = true
                    onSingleClick { openUrl(b) }
                    semSetToolTipText(b)
                }
            }

            setupStargazerDetails()
        }
    }

    private fun setupStargazerDetails(){
        with (stargazer) {
            val cardDetailsMap = mapOf(
                location to designR.drawable.ic_oui_location,
                company to designR.drawable.ic_oui_work,
                email to designR.drawable.ic_oui_email,
                bio to designR.drawable.ic_oui_tag
            )

            var added = 0
            for (i in cardDetailsMap) {
                if (i.key.isNullOrEmpty()) continue
                addCardItemView(
                    icon = AppCompatResources.getDrawable(this@StargazerProfileActivity, i.value)!!,
                    title = i.key!!,
                    showTopDivider = added > 0
                )
                added += 1
            }
        }
    }

    private fun addCardItemView(
        icon: Drawable,
        title: String,
        showTopDivider: Boolean
    ) {
        binding.stargazerDetailsContainer.addView(
            CardItemView(this@StargazerProfileActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                this.icon = icon
                this.title = title
                this.showTopDivider = showTopDivider
            }
        )
    }

    private fun setupBottomNav(){
        binding.bottomNav.menu.findItem(R.id.menu_sg_share).apply {
            title = if (isSamsungQuickShareAvailable()) "Quick share" else "Share"
        }

        binding.bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.menu_sg_share -> {
                    lifecycleScope.launch {
                        stargazer.asVCardFile(this@StargazerProfileActivity).share(this@StargazerProfileActivity)
                    }
                    true
                }
                R.id.menu_sg_qrcode -> {
                    QRBottomSheet.newInstance(stargazer).show(supportFragmentManager, null)
                    true
                }
                else -> false
            }
        }
    }
}