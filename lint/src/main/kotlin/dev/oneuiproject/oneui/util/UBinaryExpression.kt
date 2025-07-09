package dev.oneuiproject.oneui.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Resolves the receiver type of a binary expression (e.g., assignment)
 * for a specific property name.
 *
 * This function handles cases like:
 * - `myPref.onPreferenceChangeListener = ...` (Qualified reference)
 * - `onPreferenceChangeListener = ...` (Simple name reference, potentially in a scope function like `apply`)
 *
 * @param identifierPropertyName The name of the property being assigned to (e.g., "onPreferenceChangeListener").
 * @param context The Lint JavaContext.
 * @return The PsiType of the receiver, or null if it cannot be resolved or doesn't match the property name.
 */
fun UBinaryExpression.resolveReceiverTypeByIdentifier(
    identifierPropertyName: String,
    context: JavaContext
): PsiType? {
    val expression = this
    val leftOperant = expression.leftOperand
    var psiType: PsiType? = null
    when (leftOperant) {
        is UReferenceExpression -> {// e.g., myPref.onPreferenceChangeListener
            when (leftOperant) {
                is UQualifiedReferenceExpression -> {
                    (leftOperant.selector as? USimpleNameReferenceExpression)?.let {
                        val propertyName = it.identifier
                        if (propertyName != identifierPropertyName) return null
                        psiType = leftOperant.receiver.getExpressionType()
                    }
                }

                is USimpleNameReferenceExpression -> {
                    val propertyName = leftOperant.identifier
                    if (propertyName != identifierPropertyName) return null
                    psiType =
                        expression.uastParent?.resolveScopedReceiverOrNull()?.getExpressionType()
                            ?: run {
                                // Fallback: If couldn't find its receiver,
                                // assume 'this' is the containing class (for cases where PrefFragment extends Preference)
                                val preferenceClass =
                                    leftOperant.getContainingUClassIfFieldExist(
                                        identifierPropertyName
                                    ) ?: return null
                                val psiClass =
                                    context.evaluator.getTypeClass(preferenceClass.javaPsi as PsiType?)
                                        ?: return null

                                if (psiClass.qualifiedName != null) {
                                    PsiType.getTypeByName(
                                        psiClass.qualifiedName!!,
                                        context.project as Project,
                                        psiClass.resolveScope
                                    )
                                } else null
                            }
                }

                else -> return null
            }
        }
    }

    return psiType
}

/**
 * Generates components needed for a Lint quickfix, specifically for replacing
 * an assignment with a KTX extension function call.
 *
 * This function handles different types of left-hand side expressions in an assignment:
 * - Qualified reference (e.g., `myObject.property = ...`)
 * - Simple name reference (e.g., `property = ...`, often within a scope function like `apply`)
 * - Other expressions (fallback, uses the KTX call directly)
 *
 * @param context The Lint JavaContext.
 * @param onGetReplaceWithText A lambda function that takes the right-hand side expression
 *                             (the argument to the KTX function) and the receiver's indent
 *                             level, and returns the string representation of the KTX function call.
 * @return A [LintComponents] object containing the necessary information for creating a Lint fix.
 */
@OptIn(ExperimentalContracts::class)
fun UBinaryExpression.resolveLintComponents(
    context: JavaContext,
    interfaceOrSuperclassFCQN: String,
    methodName: String,
    onGetReplaceWithText: (expsAndArgs: Pair<UExpression?, List<PsiParameter?>>, receiverIndent: Int) -> String
): LintComponents {
    contract {
        callsInPlace(onGetReplaceWithText, InvocationKind.EXACTLY_ONCE)
    }
    val node = this
    val leftOperand = node.leftOperand
    val fullReplacementText: String
    val reportHighlightElement: UExpression
    val expsAndArgs = node.rightOperand.extractBodyExpressionAndParams(
        interfaceOrSuperclassFCQN,
        methodName,
        context
    )
    val ktxCallText = onGetReplaceWithText(expsAndArgs, node.getReceiverIndent(context))

    when (leftOperand) {
        is UReferenceExpression -> {

            when (leftOperand) {
                is UQualifiedReferenceExpression -> {
                    val propertyReceiver = leftOperand.receiver
                    fullReplacementText = "${propertyReceiver.asSourceString()}.$ktxCallText"
                    reportHighlightElement = leftOperand.selector
                }

                is USimpleNameReferenceExpression -> {
                    fullReplacementText = ktxCallText
                    reportHighlightElement = leftOperand
                }

                else -> {
                    fullReplacementText = ktxCallText
                    reportHighlightElement = leftOperand
                }
            }
        }

        else -> {//Fallback
            fullReplacementText = ktxCallText
            reportHighlightElement = leftOperand
        }
    }


    return LintComponents(
        node.leftOperand,
        context.getLocation(reportHighlightElement),
        fullReplacementText,
        context.getLocation(node)
    )
}
