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
import dev.oneuiproject.oneui.util.isLastExpressionTrueLiteral
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

class PreferenceChangeListenerDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ANDROIDX_PREFERENCE_CLASS = "androidx.preference.Preference"
        private const val ON_PREFERENCE_CHANGE_LISTENER = "onPreferenceChangeListener"
        private const val SET_ON_PREFERENCE_CHANGE_LISTENER = "setOnPreferenceChangeListener"

        private val TYPESAFE_PREFS_FQCN = listOf(
            "androidx.preference.TwoStatePreference",
            "dev.oneuiproject.oneui.preference.ColorPickerPreference",
            "androidx.preference.DropDownPreference",
            "androidx.preference.EditTextPreference",
            "dev.oneuiproject.oneui.preference.HorizontalRadioPreference",
            "androidx.preference.ListPreference",
            "androidx.preference.MultiSelectListPreference",
            "androidx.preference.SeekBarPreference",
        )

        val ISSUE_USE_ON_NEW_VALUE_KTX: Issue = Issue.create(
            id = "UseTypeSafeOnNewValueKtx",
            briefDescription = "Replace Preference change listener with type-safe KTX.",
            explanation = """
                Instead of directly setting `onPreferenceChangeListener`, prefer using the \
                type-safe `onNewValue` or `onNewValueConditional` Kotlin extension functions \
                provided by OneUI Design library. These extensions will also simplify the listener setup.\
            """,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                PreferenceChangeListenerDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val ISSUE_USE_ON_NEW_VALUE_UNSAFE_KTX: Issue = Issue.create(
            id = "UseOnNewValueUnsafeKtx",
            briefDescription = "Replace Preference change listener with a KTX.",
            explanation = """
                The`onNewValueUnsafe` Kotlin extension function provided by OneUI Design library \
                can be used in place of `onPreferenceChangeListener` for conciseness and easier setup.
            """,
            category = Category.CORRECTNESS,
            priority = 3,
            severity = Severity.WARNING,
            implementation = Implementation(
                PreferenceChangeListenerDetector::class.java,
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
                if (reportedLocations.contains(locationKey)) return true
                reportedLocations.add(locationKey)
                return false
            }

            override fun visitCallExpression(node: UCallExpression) {
                val (preferenceType, elementToReport) = node.resolveReceiverTypeAndElementById(
                        SET_ON_PREFERENCE_CHANGE_LISTENER,
                        context,
                        listOf("androidx.preference")
                    ) ?: return
                filterAndReportIssue(preferenceType, elementToReport, node.methodIdentifier ?: node)
            }


            override fun visitBinaryExpression(node: UBinaryExpression) {
                // Only interested in assignments
                if (node.operator !is UastBinaryOperator.AssignOperator) return

                // This will be handled in visitCallExpression
                if (node.resolveOperator()?.name == SET_ON_PREFERENCE_CHANGE_LISTENER
                    && node.leftOperand is UQualifiedReferenceExpression) return

                val preferenceType =
                    node.resolveReceiverTypeByIdentifier(ON_PREFERENCE_CHANGE_LISTENER, context)
                        ?: return
                filterAndReportIssue(preferenceType, node, node.leftOperand)
            }

            private fun filterAndReportIssue(preferenceType: PsiType, elementToReport: UElement, trackingElement: UElement) {
                if (!preferenceType.isSubclassOf(ANDROIDX_PREFERENCE_CLASS, context)) return
                if (isAlreadyReported(trackingElement, context)) return
                val withTypeSafe = TYPESAFE_PREFS_FQCN.any { preferenceType.isSubclassOf(it, context) }
                reportIssue(
                    context,
                    elementToReport,
                    withTypeSafe
                )
            }


            private fun reportIssue(
                context: JavaContext,
                node: UElement,
                useTypeSafe: Boolean
            ) {

                if (useTypeSafe) {
                    var onNewValueAsFirstOption = true

                    val (reportNode, locationToReplace, replaceWithText, range) =
                        resolveLintComponents(
                            node,
                            context,
                            "androidx.preference.Preference.OnPreferenceChangeListener",
                            "onPreferenceChange",
                            onGetReplaceWithText = { expsAndArgs, receiverIndent ->
                                onNewValueAsFirstOption = expsAndArgs.first == null || expsAndArgs.first!!.isLastExpressionTrueLiteral()
                                getReplaceWithText(
                                    expsAndArgs,
                                    receiverIndent,
                                    "onNewValue",
                                    context
                                )
                            }
                        )

                    val (_, _, replaceWithTextC, rangeC) =
                        resolveLintComponents(
                            node,
                            context,
                            "androidx.preference.Preference.OnPreferenceChangeListener",
                            "onPreferenceChange",
                            onGetReplaceWithText = { argExpression, receiverIndent ->
                                getReplaceWithText(
                                    argExpression,
                                    receiverIndent,
                                    "onNewValueConditional",
                                    context
                                )
                            }
                        )


                    val lintFix: LintFix = LintFix.create()
                        .name("Replace with onNewValue KTX")
                        .replace()
                        .range(range)
                        .with(replaceWithText)
                        .autoFix()
                        .imports("dev.oneuiproject.oneui.ktx.onNewValue")
                        .build()

                    val lintFixC: LintFix = LintFix.create()
                        .name("Replace with onNewValueConditional KTX")
                        .replace()
                        .range(rangeC)
                        .with(replaceWithTextC)
                        .autoFix()
                        .imports("dev.oneuiproject.oneui.ktx.onNewValueConditional")
                        .build()

                    val lintFixGroup = LintFix.create().group().run {
                        if (onNewValueAsFirstOption) {
                            join(lintFix, lintFixC)
                        } else {
                            join(lintFixC, lintFix)
                        }
                        build()
                    }

                    context.report(
                        ISSUE_USE_ON_NEW_VALUE_KTX,
                        reportNode,
                        locationToReplace,
                        "Replace with type-safe `onNewValue` or `onNewValueConditional` preference extension functions.",
                        lintFixGroup
                    )
                } else {
                    val (reportNode, locationToReplace, replaceWithText, range) =
                        resolveLintComponents(
                            node,
                            context,
                            "androidx.preference.Preference.OnPreferenceChangeListener",
                            "onPreferenceChange",
                            onGetReplaceWithText = { expsAndArgs, receiverIndent ->
                                getReplaceWithText(
                                    expsAndArgs,
                                    receiverIndent,
                                    "onNewValueUnsafe<Any>",
                                    context
                                )
                            }
                        )

                    val lintFix: LintFix = LintFix.create()
                        .name("Replace with onNewValueUnsafe KTX")
                        .replace()
                        .range(range)
                        .with(replaceWithText)
                        .autoFix()
                        .imports("dev.oneuiproject.oneui.ktx.onNewValueUnsafe")
                        .build()

                    context.report(
                        ISSUE_USE_ON_NEW_VALUE_UNSAFE_KTX,
                        reportNode,
                        locationToReplace,
                        "This can be replaced with the `onNewValueUnsafe` preference extension function.",
                        lintFix
                    )
                }
            }
        }
    }


    private fun getReplaceWithText(
        expsAndArgs:  Pair<UExpression?, List<PsiParameter?>>,
        baseIndent: Int,
        ktxType: String,
        context: JavaContext
    ): String {

        val (bodyExpression, originalParameters) = expsAndArgs

        val paramName = originalParameters.getOrNull(1)?.name?.ifBlank { "newValue" } ?: "newValue"

        var bodyContent: String
        when (bodyExpression) {
            null -> bodyContent = "/* Place your onPreferenceChange logic here */"

            is UBlockExpression -> {
                val shouldOmitLast = ktxType == "onNewValue" && bodyExpression.isLastExpressionTrueLiteral()
                bodyContent = bodyExpression.joinExpressionsUnIndented(
                    omitLastStatement = shouldOmitLast,
                    isLambdaTarget = true
                )
            }

            else -> {//single expression
                bodyContent = bodyExpression.asSourceString().trim()
            }
        }

        return "$ktxType { $paramName -> " +
                "\n${bodyContent.indent(baseIndent + 4)}" +
                "\n${"}".indent(baseIndent)}"

    }
}
