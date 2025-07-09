package dev.oneuiproject.oneui.common

import com.android.SdkConstants
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import dev.oneuiproject.oneui.util.SmallScreenSizeHandlingActivities
import org.w3c.dom.Document
import org.w3c.dom.Element

class ManifestActivityConfigDetector : Detector(), XmlScanner {

    companion object {
        private const val ATTR_CONFIG_CHANGES = "configChanges"

        val DUMMY_ISSUE_12345: Issue = Issue.create(
            id = "DUMMY_ISSUE_12345",
            briefDescription = "DUMMY_ISSUE_12345",
            explanation = "DUMMY_ISSUE_12345",
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.WARNING,
            implementation = Implementation(
                ManifestActivityConfigDetector::class.java,
                Scope.MANIFEST_SCOPE
            )
        )
    }

    override fun getApplicableElements(): Collection<String>? = listOf(SdkConstants.TAG_ACTIVITY)

    private lateinit var set: MutableSet<String>

    override fun visitDocument(context: XmlContext, document: Document) {
        set = SmallScreenSizeHandlingActivities.getForProject(context.project)
            ?: mutableSetOf<String>().also { SmallScreenSizeHandlingActivities.putForProject(context.project, it) }
        set.clear()
        super.visitDocument(context, document)
    }

    override fun visitElement(context: XmlContext, element: Element) {
        if (context.file.name != SdkConstants.ANDROID_MANIFEST_XML) return
        if (element.tagName != SdkConstants.TAG_ACTIVITY) return

        val configChangesAttrNode = element.getAttributeNodeNS(SdkConstants.ANDROID_URI, ATTR_CONFIG_CHANGES)
        if (configChangesAttrNode?.value?.split('|')?.any { it.trim() == "smallestScreenSize" } == true) {
            val activityNameAttr =
                element.getAttributeNS(SdkConstants.ANDROID_URI, SdkConstants.ATTR_NAME) ?: return
            val activityFqcn = if (activityNameAttr.startsWith(".")) {
                (context.project.`package` ?: "") + activityNameAttr
            } else {
                activityNameAttr
            }
            set.add(activityFqcn)
        }
    }

}