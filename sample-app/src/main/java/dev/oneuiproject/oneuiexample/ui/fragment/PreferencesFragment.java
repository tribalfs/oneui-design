package dev.oneuiproject.oneuiexample.ui.fragment;

import static dev.oneuiproject.oneui.ktx.ContextKt.isLightMode;
import static dev.oneuiproject.oneui.ktx.PreferenceFragmentCompatKt.getRelativeLinksCard;
import static dev.oneuiproject.oneui.ktx.PreferenceKt.setSummaryUpdatable;
import static dev.oneuiproject.oneui.widget.RelativeLinksCardKt.replaceLinks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.util.SeslMisc;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeslSwitchPreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.sec.sesl.tester.R;

import java.util.Random;

import dev.oneuiproject.oneui.preference.HorizontalRadioPreference;
import dev.oneuiproject.oneui.preference.InsetPreferenceCategory;
import dev.oneuiproject.oneui.preference.SuggestionCardPreference;
import dev.oneuiproject.oneui.preference.TipsCardPreference;
import dev.oneuiproject.oneui.preference.UpdatableWidgetPreference;
import dev.oneuiproject.oneui.widget.RelativeLink;
import dev.oneuiproject.oneui.widget.RelativeLinksCard;
import dev.oneuiproject.oneui.widget.Toast;
import dev.oneuiproject.oneuiexample.ui.activity.SampleAboutActivity;
import dev.oneuiproject.oneuiexample.ui.core.DarkModeUtils;

public class PreferencesFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {
    private Context mContext;
    private RelativeLinksCard mRelativeLinksCard;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.sample3_preferences, rootKey);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        initPreferences();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRelativeLinksCard = getRelativeLinksCard(PreferencesFragment.this);
    }

    @Override
    public void onResume() {
        super.onResume();
        replaceLinks(mRelativeLinksCard, randomRelativeLink(), randomRelativeLink(), randomRelativeLink());
    }

    private void initPreferences() {
        TipsCardPreference tipsCardPref = findPreference("tip");
        tipsCardPref.addButton("Button", v -> Toast.makeText(mContext, "onClick", Toast.LENGTH_SHORT).show());

        SuggestionCardPreference suggestionCardPref = findPreference("suggestion");
        InsetPreferenceCategory suggestionInsetPref = findPreference("suggestion_inset");

        suggestionCardPref.setOnClosedClickedListener((v) ->
                getPreferenceScreen().removePreference(suggestionInsetPref));

        suggestionCardPref.setActionButtonOnClickListener(v -> {
                    suggestionCardPref.startTurnOnAnimation("Turned on");
                    getView().postDelayed(() -> {
                        getPreferenceScreen().removePreference(suggestionCardPref);
                        getPreferenceScreen().removePreference(suggestionInsetPref);
                    }, 1_500);
                }
        );

        int darkMode = DarkModeUtils.getDarkMode(mContext);

        HorizontalRadioPreference darkModePref = findPreference("dark_mode");
        darkModePref.setOnPreferenceChangeListener(this);
        darkModePref.setDividerEnabled(false);
        darkModePref.setTouchEffectEnabled(false);
        darkModePref.setEnabled(darkMode != DarkModeUtils.DARK_MODE_AUTO);
        darkModePref.setValue(isLightMode(mContext) ? "0" : "1");

        SwitchPreferenceCompat autoDarkModePref = findPreference("dark_mode_auto");
        autoDarkModePref.setOnPreferenceChangeListener(this);
        autoDarkModePref.setChecked(darkMode == DarkModeUtils.DARK_MODE_AUTO);

        SeslSwitchPreferenceScreen switchPref = findPreference("key2");
        boolean enabled = switchPref.isChecked();
        switchPref.setSummary(enabled ? "Enabled" : "Disabled");
        setSummaryUpdatable(switchPref, true);
        switchPref.setOnPreferenceClickListener(this);
        switchPref.setOnPreferenceChangeListener(this);

        EditTextPreference editTextPref = findPreference("key4");
        setSummaryUpdatable(editTextPref, editTextPref.getText().length() > 0);
        editTextPref.setOnPreferenceChangeListener(this);

        DropDownPreference dropDownPref = findPreference("key5");
        setSummaryUpdatable(dropDownPref, true);
        ListPreference listPref = findPreference("key6");
        setSummaryUpdatable(listPref, true);

        findPreference("about").setOnPreferenceClickListener(this);

        findPreference("updatable").setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("key2")) {
            Toast.makeText(mContext, "onPreferenceClick", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference.getKey().equals("about")) {
            startActivity(new Intent(requireActivity(), SampleAboutActivity.class));
            return true;
        }
        if (preference.getKey().equals("updatable")) {
            UpdatableWidgetPreference uwp = (UpdatableWidgetPreference)preference;
            uwp.setWidgetLayoutResource(R.layout.sample_pref_widget_progress);
            getView().postDelayed(() -> uwp.setWidgetLayoutResource(R.layout.sample_pref_widget_check), 2_000);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String currentDarkMode = String.valueOf(DarkModeUtils.getDarkMode(mContext));
        HorizontalRadioPreference darkModePref = (HorizontalRadioPreference) findPreference("dark_mode");

        switch (preference.getKey()) {
            case "dark_mode":
                if (currentDarkMode != newValue) {
                    DarkModeUtils.setDarkMode((AppCompatActivity) requireActivity(), ((String) newValue).equals("0")
                            ? DarkModeUtils.DARK_MODE_DISABLED
                            : DarkModeUtils.DARK_MODE_ENABLED);
                }
                return true;
            case "dark_mode_auto":
                if ((boolean) newValue) {
                    darkModePref.setEnabled(false);
                    DarkModeUtils.setDarkMode((AppCompatActivity) requireActivity(),
                            DarkModeUtils.DARK_MODE_AUTO);
                } else {
                    darkModePref.setEnabled(true);
                }
                return true;
            case "key2":
                Boolean enabled = (Boolean) newValue;
                preference.setSummary(enabled ? "Enabled" : "Disabled");
                setSummaryUpdatable(preference, enabled);
                return true;
            case "key4":
                String text = (String) newValue;
                setSummaryUpdatable(preference, text.length() > 0);
                return true;
        }

        return false;
    }

    private RelativeLink randomRelativeLink()  {
        return new RelativeLink(
                "Related link " + (new Random().nextInt(20) + 1),
                v -> Toast.makeText(mContext, ((TextView)v).getText().toString(), Toast.LENGTH_SHORT).show());
    }
}
