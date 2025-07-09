package dev.oneuiproject.oneui.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.getContainingUClass
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

data class ResolvedTypeAndElement(
    val type: PsiType,
    val element: UExpression
)

fun UCallExpression.resolveReceiverTypeAndElementById(
    methodName: String,
    context: JavaContext,
    filterPackageOrClass: List<String>
): ResolvedTypeAndElement? {
    val expression = this

    when (expression.methodName) {
        methodName if expression.valueArgumentCount == 1 -> {
            val explicitReceiverType = expression.receiver?.getExpressionType()
            if (explicitReceiverType != null) {
                return ResolvedTypeAndElement(explicitReceiverType, expression)
            } else {
                val containingUClass = expression.getContainingUClass()
                if (containingUClass != null) {
                    val foundMethods = containingUClass.findMethodsByName(
                        methodName,
                        true
                    )

                    if (foundMethods.any {
                            val isMethodVerified = filterPackageOrClass.any { classPkgName ->
                                it.containingClass?.qualifiedName?.startsWith(classPkgName) == true
                            }
                            val hasOneParameter = it.parameterList.parametersCount == 1
                            isMethodVerified && hasOneParameter
                        }
                    ) {
                        // This part for implicit 'this' should be fine
                        val psiClass =
                            context.evaluator.getTypeClass(containingUClass.javaPsi as PsiType?)
                        if (psiClass != null && psiClass.qualifiedName != null) {
                            val type = PsiType.getTypeByName(
                                psiClass.qualifiedName!!,
                                context.project as Project,
                                psiClass.resolveScope
                            )
                            return ResolvedTypeAndElement(type, expression)
                        }
                    }
                }
            }
        }


        // Detect setOnPreferenceChangeListener inside apply/also/run lambda blocks ---
        // If this is an apply/also/run call, visit its lambda body for setOnPreferenceChangeListener calls
        in listOf("apply", "also", "run", "let") -> {
            val lambdaArgument =
                expression.valueArguments.filterIsInstance<ULambdaExpression>().firstOrNull()

            if (lambdaArgument != null) {
                val lambdaBody = lambdaArgument.body
                val expressionsToSearch = when (lambdaBody) {
                    is UBlockExpression -> lambdaBody.expressions
                    else -> listOf(lambdaBody)
                }

                expressionsToSearch.forEach { exprInScope ->
                    var actualExprToCheck: UExpression? = exprInScope
                    // Unwrap if it's an implicit return
                    if (actualExprToCheck is UReturnExpression) { // Covers KotlinUImplicitReturnExpression
                        actualExprToCheck = actualExprToCheck.returnExpression
                    }

                    if (actualExprToCheck is UCallExpression &&
                        actualExprToCheck.methodName == methodName &&
                        actualExprToCheck.valueArgumentCount == 1
                    ) {
                        val receiverType = expression.receiver?.getExpressionType()
                        if (receiverType != null) {
                            return ResolvedTypeAndElement(
                                receiverType,
                                actualExprToCheck
                            )
                        }
                    }
                }
            }
        }
    }
    return null
}

@OptIn(ExperimentalContracts::class)
fun UCallExpression.resolveLintComponents(
    context: JavaContext,
    interfaceOrSuperclassFCQN: String,
    methodName: String,
    onGetReplaceWithText: (expsAndArgs: Pair<UExpression?, List<PsiParameter?>>, receiverIndent: Int) -> String
): LintComponents {
    contract {
        callsInPlace(onGetReplaceWithText, InvocationKind.EXACTLY_ONCE)
    }
    val expression = this
    val receiverIndent = expression.getReceiverIndent(context)
    val expsAndArgs = expression.valueArguments.firstOrNull().extractBodyExpressionAndParams(
        interfaceOrSuperclassFCQN,
        methodName,
        context
    )
    val replaceWithText = onGetReplaceWithText(expsAndArgs, receiverIndent)
    val receiverSource = expression.receiver?.asSourceString()
    val replacement = if (!receiverSource.isNullOrEmpty()) {
        "$receiverSource.$replaceWithText"
    } else {
        replaceWithText
    }

    val methodId = expression.methodIdentifier ?: expression

    return LintComponents(
        methodId,
        context.getLocation(methodId),
        replacement,
        context.getLocation(expression)
    )
}

fun UCallExpression.resolveActivityLayoutNameIfSetContentView(context: JavaContext): String? {
    val node = this
    if (node.methodName == "setContentView" && node.valueArgumentCount == 1) {
        val argument = node.valueArguments[0] // This is R.layout.X or binding.root

        var layoutName = argument.resolveLayoutNameIfLayoutResReference(context)
        if (layoutName == null)
            layoutName = argument.resolveLayoutNameIfViewBindingReference(context)

        return layoutName
    }
    return null
}