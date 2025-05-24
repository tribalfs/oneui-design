package dev.oneuiproject.oneuiexample.ui.preference

import android.R.attr.onClick
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils.indexOf
import android.util.Log.i
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SeslSwitchPreferenceScreen
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.ktx.getRelativeLinksCard
import dev.oneuiproject.oneui.ktx.onClick
import dev.oneuiproject.oneui.ktx.onNewValue
import dev.oneuiproject.oneui.ktx.provideSummary
import dev.oneuiproject.oneui.ktx.setSummaryUpdatable
import dev.oneuiproject.oneui.preference.ColorPickerPreference
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference
import dev.oneuiproject.oneui.preference.InsetPreferenceCategory
import dev.oneuiproject.oneui.preference.SuggestionCardPreference
import dev.oneuiproject.oneui.preference.TipsCardPreference
import dev.oneuiproject.oneui.preference.UpdatableWidgetPreference
import dev.oneuiproject.oneui.widget.RelativeLink
import dev.oneuiproject.oneui.widget.RelativeLinksCard
import dev.oneuiproject.oneui.widget.replaceLinks
import dev.oneuiproject.oneuiexample.data.util.determineDarkMode
import dev.oneuiproject.oneuiexample.ui.about.SampleAboutActivity
import dev.oneuiproject.oneuiexample.ui.main.core.util.applyDarkMode
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random

class PreferencesFragment : PreferenceFragmentCompat() {

    private var relativeLinksCard: RelativeLinksCard? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preferences, rootKey)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        initPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        relativeLinksCard = getRelativeLinksCard()
    }

    override fun onResume() {
        super.onResume()
        relativeLinksCard!!.replaceLinks(
            randomRelativeLink(),
            randomRelativeLink(),
            randomRelativeLink()
        )
    }

    private fun initPreferences() {
        findPreference<TipsCardPreference>("tip")!!
            .addButton("Button") { semToast("TipsCardPreference button clicked!") }

        val suggestionInsetPref = findPreference<InsetPreferenceCategory>("suggestion_inset")!!
        findPreference<SuggestionCardPreference>("suggestion")!!.apply {
            setOnClosedClickedListener { _ -> preferenceScreen.removePreference(suggestionInsetPref) }
            setActionButtonOnClickListener { v: View? ->
                startTurnOnAnimation("Turned on")
                lifecycleScope.launch {
                    delay(1_500)
                    preferenceScreen.removePreference(this@apply)
                    preferenceScreen.removePreference(suggestionInsetPref)
                }
            }
        }

        findPreference<HorizontalRadioPreference>(PREF_DARK_MODE)!!.apply {
            setDividerEnabled(false)
            setTouchEffectEnabled(false)
            onNewValue {
                // Ensure it is saved first before updating the app's night mode,
                // which will recreate the activity that prevents the preference from being saved.
                value = it
                applyDarkModePrefs()
            }
        }


        findPreference<SwitchPreferenceCompat>(PREF_AUTO_DARK_MODE)!!.apply {
            onNewValue{
                // Ensure it is saved first before updating the app's night mode,
                // which will recreate the activity that prevents the preference from being saved.
                isChecked = it
                applyDarkModePrefs()
            }
        }

        findPreference<UpdatableWidgetPreference>("updatablePref")!!.apply {
            onClick {
                widgetLayoutResource = R.layout.view_pref_widget_progress
                lifecycleScope.launch {
                    delay(2_000)
                    widgetLayoutResource = R.layout.view_pref_widget_check
                }
            }
        }

        findPreference<SwitchPreferenceCompat>("switchPref")!!
            .provideSummary(true) { if (it.isChecked) "On" else "Off" }

        findPreference<SeslSwitchPreferenceScreen>("switchPrefScreen")!!.apply {
            provideSummary(true) { if (it.isChecked) "On" else "Off" }
            onClick { semToast( "${it.key} clicked!") }
        }

        findPreference<EditTextPreference>("editTextPref")!!.setSummaryUpdatable(true)

        findPreference<ColorPickerPreference>("colorPickerPref")!!.apply {
            @OptIn(ExperimentalStdlibApi::class)
            provideSummary(true) {
                "#${it.value.toHexString(HexFormat.UpperCase)}"
            }
        }

        findPreference<MultiSelectListPreference>("multiSelectPref")!!.apply {
            provideSummary(true) {
                it.values
                    .map { i -> it.entryValues.indexOf(i)}
                    .map { i -> it.entries[i]}
                    .joinToString(", ")
            }
        }

        findPreference<DropDownPreference>("dropDownPref")!!.setSummaryUpdatable(true)

        findPreference<DropDownPreference>("listPref")!!.setSummaryUpdatable(true)

        findPreference<Preference>("aboutPref")!!.onClick {
            startActivity(Intent(requireActivity(), SampleAboutActivity::class.java))
        }
    }


    private fun randomRelativeLink(): RelativeLink {
        return RelativeLink(
            "Related link " + (Random().nextInt(20) + 1)
        ) { v: View -> semToast((v as TextView).text.toString()) }
    }

    private fun Preference.applyDarkModePrefs() {
        val darkMode = with(sharedPreferences!!) {
            determineDarkMode(getString(PREF_DARK_MODE, "0")!!,
                getBoolean(PREF_AUTO_DARK_MODE, true))
        }
        applyDarkMode(darkMode)
    }

    companion object{
        private const val PREF_AUTO_DARK_MODE = "darkModeAuto"
        private const val PREF_DARK_MODE = "darkMode"
    }
}
