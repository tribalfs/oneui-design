package dev.oneuiproject.oneui

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.Issue
import dev.oneuiproject.oneui.preference.PreferenceKeyDetector
import dev.oneuiproject.oneui.app.SemBottomSheetExtensionDetector
import dev.oneuiproject.oneui.preference.PreferenceChangeListenerDetector
import dev.oneuiproject.oneui.preference.PreferenceSummaryProviderDetector
import dev.oneuiproject.oneui.widget.NavDrawerActivityDetector
import dev.oneuiproject.oneui.common.ManifestActivityConfigDetector
import dev.oneuiproject.oneui.preference.PreferenceClickListenerDetector
import dev.oneuiproject.oneui.widget.SLLTargetImageDetector
import dev.oneuiproject.oneui.widget.TabItemIdDetector

class OneUIDesignIssueRegistry : IssueRegistry() {
    override val minApi = 10
    override val api = 14
    override val issues: List<Issue>
        get() = listOf(
            ManifestActivityConfigDetector.DUMMY_ISSUE_12345,
            TabItemIdDetector.ISSUE,
            SLLTargetImageDetector.ISSUE,
            PreferenceKeyDetector.ISSUE_KEY_NOT_FOUND,
            PreferenceKeyDetector.ISSUE_TYPE_MISMATCH,
            PreferenceChangeListenerDetector.ISSUE_USE_ON_NEW_VALUE_KTX,
            PreferenceChangeListenerDetector.ISSUE_USE_ON_NEW_VALUE_UNSAFE_KTX,
            PreferenceSummaryProviderDetector.ISSUE_PROVIDE_SUMMARY_KTX,
            PreferenceClickListenerDetector.ISSUE_ON_CLICK_KTX,
            SemBottomSheetExtensionDetector.ISSUE_EXTEND_SEM_BOTTOM_SHEET,
            NavDrawerActivityDetector.ISSUE_ADAPT_NAV_DRAWER_LAYOUT
        )
    override val vendor = Vendor(
        identifier = "dev.oneuiproject.oneui",
        vendorName = "Tribalfs",
        feedbackUrl = "https://github.com/tribalfs/oneui-design/discussions"
    )
}
