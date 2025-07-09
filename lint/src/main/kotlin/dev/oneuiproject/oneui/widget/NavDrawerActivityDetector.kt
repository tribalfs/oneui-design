package dev.oneuiproject.oneui.widget

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import dev.oneuiproject.oneui.util.SmallScreenSizeHandlingActivities
import dev.oneuiproject.oneui.util.findResourceXmlFile
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import javax.xml.parsers.DocumentBuilderFactory

class NavDrawerActivityDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ONE_UI_NAV_DRAWER_LAYOUT_TAG = "dev.oneuiproject.oneui.layout.NavDrawerLayout"
        private const val CLASS_ANDROIDX_FRAGMENT_ACTIVITY = "androidx.fragment.app.FragmentActivity"

        val ISSUE_ADAPT_NAV_DRAWER_LAYOUT = Issue.create(
            id = "AdaptNavDrawerLayoutIssue",
            briefDescription = "NavDrawerLayout activity should not handle smallestScreenSize",
            explanation = """
                Activities using `dev.oneuiproject.oneui.layout.NavDrawerLayout` as their root view \
                should not include `smallestScreenSize` in the `android:configChanges` attribute \
                of their AndroidManifest.xml entry. Handling this configuration change manually \
                will cause `NavDrawerLayout`'s adaptive layout behavior to not function correctly.
            """,
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                NavDrawerActivityDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun applicableSuperClasses(): List<String> = listOf(CLASS_ANDROIDX_FRAGMENT_ACTIVITY)

    override fun getApplicableMethodNames(): List<String> = listOf("setContentView")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val containingClass = node.getContainingUClass() ?: return
        val componentFqcn = containingClass.qualifiedName ?: return
        val smallScreenSizeHandlingActivitiesFQCN = SmallScreenSizeHandlingActivities.getForProject(context.project) ?: return
        if (!smallScreenSizeHandlingActivitiesFQCN.contains(componentFqcn)) return

        var layoutName: String? = null

        // This is R.layout.X or binding.root
        val argument = node.valueArguments.getOrNull(0) ?: return
        if (argument is UQualifiedReferenceExpression && argument.selector.sourcePsi?.text == "root") {
            // Argument is potentially "binding.root"
            val bindingVariableExpr = argument.receiver
            if (bindingVariableExpr is USimpleNameReferenceExpression) {
                val resolvedBindingVar = bindingVariableExpr.resolve()
                if (resolvedBindingVar is PsiVariable) {
                    val bindingPsiType = resolvedBindingVar.type
                    val bindingPsiClass = context.evaluator.getTypeClass(bindingPsiType)
                    if (bindingPsiClass != null && isViewBindingClass(
                            context,
                            bindingPsiClass
                        )
                    ) {
                        layoutName = convertBindingClassNameToLayoutName(
                            bindingPsiClass.qualifiedName ?: ""
                        )
                    }
                }
            }
        } else {
            // Argument is potentially R.layout.xxx
            layoutName = extractLayoutNameFromArgument(argument, context)
        }


        if (layoutName != null) {
            val isUsingNavDrawerLayout = isUsingNavDrawerLayout(layoutName, context)
            if (!isUsingNavDrawerLayout) return
            context.report(
                ISSUE_ADAPT_NAV_DRAWER_LAYOUT,
                node,
                context.getLocation(node),
                "Manually handling `smallestScreenSize` config changes will cause `NavDrawerLayout`'s adaptive" +
                        " layout behavior to not function correctly."
            )

        }
    }

    private fun isViewBindingClass(
        context: JavaContext,
        psiClass: PsiClass
    ): Boolean {
        return context.evaluator.extendsClass(psiClass, "androidx.viewbinding.ViewBinding", false)
    }

    private fun convertBindingClassNameToLayoutName(bindingClassName: String): String? {
        val simpleName = bindingClassName.substringAfterLast('.')
        if (!simpleName.endsWith("Binding")) return null

        val nameWithoutBinding = simpleName.removeSuffix("Binding")
        // Convert CamelCase to snake_case
        val snakeCaseName = nameWithoutBinding.replace(Regex("(?<=[a-z0-9])(?=[A-Z])")) {
            "_" + it.value
        }.lowercase()
        return snakeCaseName
    }

    private fun extractLayoutNameFromArgument(
        layoutArgExpression: UExpression,
        context: JavaContext,
    ): String? {
        var name: String? = null
        if (layoutArgExpression is UQualifiedReferenceExpression) {
            val receiver = layoutArgExpression.receiver.sourcePsi?.text
            if (receiver != null && (receiver.endsWith(".layout")
                        || receiver.endsWith("$" + "layout"))) {
                name = layoutArgExpression.selector.sourcePsi?.text
            }
        } else if (layoutArgExpression is USimpleNameReferenceExpression) {
            val resolved = layoutArgExpression.resolve()
            if (resolved is PsiField) {
                val field = resolved
                val parentClass = field.containingClass
                if (parentClass != null && parentClass.name == "layout") {
                    val grandparentClass = parentClass.containingClass
                    if (grandparentClass != null && (grandparentClass.name == "R"
                                || grandparentClass.qualifiedName == context.project.`package` + ".R")) {
                        name = field.name
                    }
                }
            }
        }
        return name
    }

    private fun isUsingNavDrawerLayout(layoutName: String, context: JavaContext): Boolean {
        // Find the layout XML file in the project
        val layoutFile = context.findResourceXmlFile("layout", "$layoutName.xml")
        return if (layoutFile != null && layoutFile.exists()) {
            try {
                val documentBuilder = DocumentBuilderFactory.newInstance().run {
                    isNamespaceAware = true
                    newDocumentBuilder()
                }
                val document = documentBuilder.parse(layoutFile)
                val rootTagName = document.documentElement?.tagName
                // Check if the root tag is NavDrawerLayout (fully qualified or short name)
                rootTagName == ONE_UI_NAV_DRAWER_LAYOUT_TAG ||
                        rootTagName == ONE_UI_NAV_DRAWER_LAYOUT_TAG.substringAfterLast('.')
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

}