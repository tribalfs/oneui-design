package dev.oneuiproject.oneui.app

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

class SemBottomSheetExtensionDetector : Detector(), SourceCodeScanner {

    companion object {
        const val GOOGLE_BOTTOM_SHEET_DIALOG_FRAGMENT = "com.google.android.material.bottomsheet.BottomSheetDialogFragment"
        const val ONEUI_SEM_BOTTOM_SHEET_DIALOG_FRAGMENT = "dev.oneuiproject.oneui.app.SemBottomSheetDialogFragment"

        val ISSUE_EXTEND_SEM_BOTTOM_SHEET: Issue = Issue.create(
            id = "ExtendSemBottomSheetDialogFragment",
            briefDescription = "Extend SemBottomSheetDialogFragment for One UI style",
            explanation = """
        For a consistent One UI bottom sheet appearance and behavior, \
        classes should extend `dev.oneuiproject.oneui.app.SemBottomSheetDialogFragment` \
        instead of directly extending `com.google.android.material.bottomsheet.BottomSheetDialogFragment`.
    """,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.INFORMATIONAL,
            implementation = Implementation(
                SemBottomSheetExtensionDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UClass::class.java) // We are interested in class declarations
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                val superTypeReference = node.uastSuperTypes.find { it.getQualifiedName() == GOOGLE_BOTTOM_SHEET_DIALOG_FRAGMENT }

                if (superTypeReference != null) {
                    val fix = LintFix.create()
                        .name("Extend SemBottomSheetDialogFragment instead")
                        .replace()
                        .range(context.getLocation(superTypeReference)) // Target the supertype reference
                        .shortenNames()
                        .reformat(true)
                        .with(ONEUI_SEM_BOTTOM_SHEET_DIALOG_FRAGMENT.split(".").last())
                        .imports(ONEUI_SEM_BOTTOM_SHEET_DIALOG_FRAGMENT)
                        .build()

                    context.report(
                        ISSUE_EXTEND_SEM_BOTTOM_SHEET,
                        superTypeReference,
                        context.getLocation(superTypeReference),
                        "Consider extending `$ONEUI_SEM_BOTTOM_SHEET_DIALOG_FRAGMENT` instead of `$GOOGLE_BOTTOM_SHEET_DIALOG_FRAGMENT` for One UI styling.",
                        fix
                    )
                }
            }
        }
    }
}
