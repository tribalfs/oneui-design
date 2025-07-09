package dev.oneuiproject.oneui.widget

import com.android.SdkConstants
import com.android.tools.lint.detector.api.*
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import dev.oneuiproject.oneui.util.TOOLS_NS_URI
import org.w3c.dom.Element

class TabItemIdDetector : ResourceXmlDetector() {

    companion object {
        val ISSUE: Issue = Issue.create(
            id = "TabItemIdInViewBindingLayout",
            briefDescription = "TabItem with android:id in ViewBinding layout",
            explanation = """
                Using android:id on <TabItem> in layouts with ViewBinding and TabLayout (or its subclass) will cause a runtime crash. \
                TabItems should be accessed by index, not by id. Remove the android:id attribute from TabItem. 
                Check https://github.com/material-components/material-components-android/issues/1162 for more info.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                TabItemIdDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )

        private const val ATTR_VIEW_BINDING_IGNORE = "viewBindingIgnore"
        private const val MATERIAL_TAB_LAYOUT_FQCN = "com.google.android.material.tabs.TabLayout"
    }

    override fun getApplicableElements(): Collection<String> =
        listOf("TabItem", "com.google.android.material.tabs.TabItem")

    override fun visitElement(context: XmlContext, element: Element) {
        // Only proceed if TabItem has android:id
        val idAttr = element.getAttributeNodeNS(SdkConstants.ANDROID_URI, "id") ?: return

        // Check if the parent is a TabLayout or subclass (by tag name)
        val parentElement = element.parentNode as? Element ?: return
        val parentTag = parentElement.tagName
        var isTabLayoutOrSubclass = false
        if (parentTag == MATERIAL_TAB_LAYOUT_FQCN) {
            isTabLayoutOrSubclass = true
        } else if (parentTag.contains(".")) {
            // Parent tag looks like a Fully Qualified Class Name (but not the standard TabLayout)
            // Attempt to resolve it and check if it's a subclass of TabLayout.
            val project = context.mainProject.ideaProject
            if (project != null) {
                val psiFacade = JavaPsiFacade.getInstance(project)
                // Try to find the class corresponding to the parentTag FQCN
                val parentPsiClass = psiFacade.findClass(parentTag, GlobalSearchScope.allScope(project))
                if (parentPsiClass != null) {
                    // We found the class. Now check if it extends/is TabLayout.
                    val tabLayoutPsiClass = psiFacade.findClass(MATERIAL_TAB_LAYOUT_FQCN, GlobalSearchScope.allScope(project))
                    if (tabLayoutPsiClass != null) {
                        if (parentPsiClass.isInheritor(tabLayoutPsiClass, true)) {
                            isTabLayoutOrSubclass = true
                        }
                    }
                }
            } else {
                // Fallback to naming convention if project access fails
                if (parentTag.endsWith("TabLayout")) {
                    isTabLayoutOrSubclass = true
                }
            }
        } else {
            // Parent tag is a simple name (not FQCN), e.g., "MyTabLayout"
            // Use the existing heuristic: endsWith("TabLayout")
            if (parentTag.endsWith("TabLayout")) {
                isTabLayoutOrSubclass = true
            }
        }

        // Check if the layout is eligible for ViewBinding
        val root = element.ownerDocument?.documentElement ?: return
        if (!isViewBindingEnabled(context) || !isLayoutEligibleForViewBinding(root)) {
            return
        }

        if (isTabLayoutOrSubclass) {
            context.report(
                ISSUE,
                element,
                context.getLocation(idAttr),
                "TabItem with android:id in a ViewBinding layout using TabLayout (or subclass) will cause a runtime crash. Remove the id and access TabItems by index.",
                LintFix.create()
                    .name("Remove android:id from TabItem")
                    .replace()
                    .range(context.getLocation(idAttr))
                    .all()
                    .with("")
                    .build()
            )
        }
    }

    /**
     * Checks if viewBinding is enabled for the module.
     */
    private fun isViewBindingEnabled(context: XmlContext): Boolean {
        return context.project.buildVariant?.buildFeatures?.viewBinding == true
    }

    /**
     * Checks if the root element is not <merge> and does not have tools:viewBindingIgnore="true".
     */
    private fun isLayoutEligibleForViewBinding(root: Element): Boolean {
        if (root.tagName == "merge") return false
        val ignore = root.getAttributeNS(TOOLS_NS_URI, ATTR_VIEW_BINDING_IGNORE).toBooleanStrictOrNull()
        return ignore != true
    }
}