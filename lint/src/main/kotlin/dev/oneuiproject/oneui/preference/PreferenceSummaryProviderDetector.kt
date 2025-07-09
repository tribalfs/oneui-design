package dev.oneuiproject.oneui.preference


import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import dev.oneuiproject.oneui.util.resolveLintComponents
import dev.oneuiproject.oneui.util.indent
import dev.oneuiproject.oneui.util.isSubclassOf
import dev.oneuiproject.oneui.util.joinExpressionsUnIndented
import dev.oneuiproject.oneui.util.resolveReceiverTypeByIdentifier
import dev.oneuiproject.oneui.util.resolveReceiverTypeAndElementById
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UastBinaryOperator

class PreferenceSummaryProviderDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ANDROIDX_PREFERENCE_CLASS = "androidx.preference.Preference"
        private const val SUMMARY_PROVIDER = "summaryProvider"
        private const val SET_SUMMARY_PROVIDER = "setSummaryProvider"

        val ISSUE_PROVIDE_SUMMARY_KTX: Issue = Issue.create(
            id = "UseProvideSummaryKtx",
            briefDescription = "Replace `summaryProvider` with `provideSummary` KTX for conciseness.",
            explanation = """
                Instead of directly setting `summaryProvider`, prefer using the `provideSummary`\
                Kotlin extension functions provided by OneUI Design library. This will simplify the listener setup.
            """,
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.WARNING,
            implementation = Implementation(
                PreferenceSummaryProviderDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? = listOf(
        UCallExpression::class.java, // For setOnPreferenceChangeListener() method calls
        UBinaryExpression::class.java // For onPreferenceChangeListener = ... assignments
    )

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        val psiFile: PsiFile? = context.psiFile
        if (psiFile !is KtFile) return null // Don't create a handler for non-Kotlin files

        return object : UElementHandler() {
            private val reportedLocations = mutableSetOf<Pair<String, Int>>()

            private fun isAlreadyReported(
                uElementForLocation: UElement,
                context: JavaContext
            ): Boolean {
                val location = context.getLocation(uElementForLocation)
                val file = location.file
                val startLine = location.start?.line ?: -1
                if (startLine == -1) return false // Cannot determine location
                val locationKey = Pair(file.absolutePath, startLine)
                if (reportedLocations.contains(locationKey)) {
                    return true
                }
                reportedLocations.add(locationKey)
                return false
            }

            override fun visitCallExpression(node: UCallExpression) {
                val (preferenceType, elementToReport) = node.resolveReceiverTypeAndElementById(
                    SET_SUMMARY_PROVIDER,
                    context,
                    listOf("androidx.preference")
                ) ?: return
                filterAndReportIssue(preferenceType, elementToReport, node.methodIdentifier ?: node)
            }


            override fun visitBinaryExpression(node: UBinaryExpression) {
                // Only interested in assignments
                if (node.operator !is UastBinaryOperator.AssignOperator) return

                // This will be handled in visitCallExpression
                if (node.resolveOperator()?.name == SET_SUMMARY_PROVIDER
                    && node.leftOperand is UQualifiedReferenceExpression
                ) return

                val preferenceType =
                    node.resolveReceiverTypeByIdentifier(SUMMARY_PROVIDER, context)
                        ?: return
                filterAndReportIssue(preferenceType, node, node.leftOperand)
            }

            private fun filterAndReportIssue(
                preferenceType: PsiType,
                elementToReport: UElement,
                trackingElement: UElement
            ) {
                if (!preferenceType.isSubclassOf(ANDROIDX_PREFERENCE_CLASS, context)) return
                if (isAlreadyReported(trackingElement, context)) return
                reportIssue(
                    context,
                    elementToReport
                )
            }


            private fun reportIssue(
                context: JavaContext,
                node: UElement
            ) {
                val (reportNode, locationToReplace, replaceWithText, range) =
                    resolveLintComponents(
                        node,
                        context,
                        "androidx.preference.Preference.SummaryProvider",
                        "provideSummary",
                        onGetReplaceWithText = { expsAndArgs, receiverIndent ->
                            getReplaceWithText(
                                expsAndArgs,
                                receiverIndent,
                                "provideSummary",
                                context
                            )
                        }
                    )

                val lintFix: LintFix = LintFix.create()
                    .name("Replace with provideSummary KTX")
                    .replace()
                    .range(range)
                    .with(replaceWithText)
                    .autoFix()
                    .imports("dev.oneuiproject.oneui.ktx.provideSummary")
                    .build()

                context.report(
                    ISSUE_PROVIDE_SUMMARY_KTX,
                    reportNode,
                    locationToReplace,
                    "This can be replaced with the `provideSummary` preference extension function.",
                    lintFix
                )
            }
        }
    }


    private fun getReplaceWithText(
        expsAndArgs: Pair<UExpression?, List<PsiParameter?>>,
        baseIndent: Int,
        ktxType: String,
        context: JavaContext
    ): String {

        val (bodyExpression, originalParameters) = expsAndArgs
        val param = originalParameters.firstOrNull()?.name.let { if (it.isNullOrBlank()) "" else "${it.trim()} ->" }

        val bodyContent = when (bodyExpression) {
            null -> "/* Place your summary provider logic here */"

            is UBlockExpression -> {
                bodyExpression.joinExpressionsUnIndented(
                    omitLastStatement = false,
                    isLambdaTarget = true
                )
            }

            else -> {//single expression
                bodyExpression.asSourceString().trim()
            }
        }

        return "$ktxType { $param" +
                "\n${bodyContent.indent(baseIndent + 4)}" +
                "\n${"}".indent(baseIndent)}"

    }
}
