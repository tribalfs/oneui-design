package dev.oneuiproject.oneui.util

import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.intellij.psi.PsiParameter
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import kotlin.contracts.ExperimentalContracts

data class LintComponents(
    val reportNode: UElement,
    val reportLocation: Location,
    val replaceWithText: String,
    val range: Location
)

@OptIn(ExperimentalContracts::class)
fun resolveLintComponents(
    node: UElement,
    context: JavaContext,
    interfaceOrSuperclassFCQN: String,
    methodName: String,
    onGetReplaceWithText: (expsAndArgs: Pair<UExpression?, List<PsiParameter?>>, receiverIndent: Int) -> String
): LintComponents {
    return when {
        node is UCallExpression -> node.resolveLintComponents(
            context,
            interfaceOrSuperclassFCQN,
            methodName,
            onGetReplaceWithText
        )
        node is UBinaryExpression -> node.resolveLintComponents(
            context,
            interfaceOrSuperclassFCQN,
            methodName,
            onGetReplaceWithText
        )
        else -> {//Fallback
            LintComponents(
                node,
                context.getLocation(node),
                onGetReplaceWithText(null to emptyList(), node.getReceiverIndent(context)),
                context.getLocation(node)
            )
        }
    }
}
