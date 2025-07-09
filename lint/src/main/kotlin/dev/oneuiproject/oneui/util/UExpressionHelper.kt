package dev.oneuiproject.oneui.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiVariable
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UAnonymousClass
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UObjectLiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElement

/**
 * Joins the expressions in the lambda body, removing the last statement if it is a boolean literal
 */
fun UExpression.joinExpressionsUnIndented(
    omitLastStatement: Boolean,
    isLambdaTarget: Boolean
): String {
    if (this is UBlockExpression) {
        val exprs = expressions
        if (exprs.isEmpty()) return ""

        val processedExprs = exprs.mapIndexed { index, uExpression ->
            val isLastStatement = index == exprs.size - 1
            if (isLambdaTarget && isLastStatement && !omitLastStatement && uExpression is UReturnExpression) {
                // If it's the last statement of a lambda target, and we're not omitting it,
                // and it's a return expression, use its returned expression's source string.
                // If returnExpression is null (e.g., "return"), it becomes an empty string.
                uExpression.returnExpression?.asSourceString()?.trim() ?: ""
            } else {
                uExpression.asSourceString().trim()
            }
        }
        val effectiveExprs = if (omitLastStatement) {
            if (processedExprs.isNotEmpty()) processedExprs.dropLast(1) else emptyList()
        } else {
            processedExprs
        }

        return effectiveExprs.joinToString(separator = "\n")
    } else {
        return if (omitLastStatement) {
            ""
        } else {
            asSourceString().trim()
        }
    }
}


/**
 * Returns true if the last expression is a boolean literal `true`.
 */
fun UExpression.isLastExpressionTrueLiteral(): Boolean {
    val last = if (this is UBlockExpression) {
        val exprs = this.expressions
        if (exprs.isEmpty()) return false
        exprs.last()
    } else {
        this
    }

    when (last) {
        is UReturnExpression -> {
            val returnExpr =
                last.returnExpression?.asSourceString()?.trim()?.toBooleanStrictOrNull()
            return returnExpr == true
        }

        else -> return last.asSourceString().trim().toBooleanStrictOrNull() == true

    }
}


/**
 * Checks if the class containing the preference expression is a Kotlin class.
 *
 * This function determines the `PsiClass` of the context in which the `UExpression`
 * is used. This context can be:
 * 1. The explicit receiver of a qualified reference or call expression (e.g., `someObject.preferenceKey`).
 * 2. The class that directly contains the expression if there's no explicit receiver.
 *
 * Once the relevant `PsiClass` is identified, it checks if this class is a Kotlin class
 * by inspecting its language or whether it's an instance of common Kotlin PSI representations
 * like `KtLightClass` or `KtClass`.
 *
 * @param context The `JavaContext` providing access to PSI and type resolution.
 * @return `true` if the context class is identified as a Kotlin class, `false` otherwise
 *         (including cases where the class cannot be determined).
 */// Helper to check if the class containing the preference expression is a Kotlin class
fun UExpression.isReceiverContextKotlinClass(context: JavaContext): Boolean {
    var receiverPsiClass: PsiClass? = null
    val preferenceExpression = this
    // Determine the PsiClass of the context (either explicit receiver or containing class)
    if (preferenceExpression is UQualifiedReferenceExpression) {
        val receiver = preferenceExpression.receiver
        val receiverType = receiver.getExpressionType()
        receiverPsiClass = context.evaluator.getTypeClass(receiverType)
    } else if (preferenceExpression is UCallExpression && preferenceExpression.receiver != null) {
        val receiver = preferenceExpression.receiver!! // Already checked not null
        val receiverType = receiver.getExpressionType()
        receiverPsiClass = context.evaluator.getTypeClass(receiverType)
    } else {
        // No explicit receiver, or it's a simple name reference.
        // Get the UClass containing the expression, then its PsiClass.
        val containingUClass = preferenceExpression.getContainingUClass()
        if (containingUClass?.sourcePsi is PsiClass) {
            receiverPsiClass = containingUClass.sourcePsi as PsiClass
        } else if (containingUClass != null) {
            // Fallback if sourcePsi isn't directly a PsiClass, try to get it via JavaPsi
            val qualifiedName = containingUClass.qualifiedName
            if (qualifiedName != null) {
                receiverPsiClass = context.evaluator.findClass(qualifiedName)
            }
        }
    }


    if (receiverPsiClass == null) {
        // If we can't determine the class, default to 'false' to be safe,
        return false
    }

    // Now check if this PsiClass represents a Kotlin class
    // 1. Check if the PsiClass's language is KotlinLanguage
    // 2. Check if the PsiClass is an instance of KtLightClass (common for Kotlin classes in PSI)
    //    or directly a KtClass (less common at this PSI level but good to check)
    val isKotlin = receiverPsiClass.language == KotlinLanguage.INSTANCE ||
            receiverPsiClass is org.jetbrains.kotlin.psi.KtClass || // Direct Kotlin PSI class
            // KtLightClass is often how Kotlin classes are represented in the PSI for Java interop
            (receiverPsiClass::class.java.name.startsWith("org.jetbrains.kotlin.asJava.classes.KtLightClass"))


    return isKotlin
}


data class LambdaExtractionResult(
    val bodyExpression: UExpression?,
    val parameterNames: List<String>, // preferencia, newValue
    val preferenceParameterName: String?,
    val newValueParameterName: String?
)

/**
 * Extracts the body expression and parameters from a [UExpression] that represents a listener.
 *
 * This function handles various ways listeners can be defined in Kotlin and Java:
 * - Lambdas (e.g., `setOnClickListener { ... }`)
 * - Anonymous classes (e.g., `object : View.OnClickListener { override fun onClick(...) { ... } }`)
 * - SAM conversions (e.g., `OnClickListener { ... }` in Kotlin, or `new OnClickListener() { ... }` in Java)
 * - References to existing listener implementations (e.g., `setOnClickListener(myListener)`)
 *
 * @param interfaceOrSuperclassFQCN The fully qualified class name of the listener interface or superclass.
 *                                  This is used to identify if a class instance is a valid listener.
 * @param methodNameIfAnonymous The name of the method to look for if the listener is an anonymous class
 *                              or an object literal (e.g., "onClick" for `View.OnClickListener`).
 * @param context The [JavaContext] for resolving types and symbols.
 * @return A [Pair] containing:
 *         - The [UExpression] representing the body of the listener method, or `null` if not found.
 *         - A list of [UParameter]s for the listener method, or `null` if not found.
 *         Returns `null to null` if the listener body and parameters cannot be extracted.
 */
fun UExpression?.extractBodyExpressionAndParams(
    interfaceOrSuperclassFQCN: String,
    methodNameIfAnonymous: String,
    context: JavaContext
): Pair<UExpression?, List<PsiParameter?>> {
    when (this) {
        // This could be ClassName { ... }
        // where the { ... } is attached to the selector, or the selector is a call.
        is UQualifiedReferenceExpression -> {
            when (val selector = this.selector) {
                is UCallExpression,
                is ULambdaExpression -> return selector.extractBodyExpressionAndParams(
                    interfaceOrSuperclassFQCN,
                    methodNameIfAnonymous,
                    context
                )
            }
        }

        //Anonymous class (Java-style or explicit object : Interface in Kotlin)
        is UObjectLiteralExpression if declaration is UAnonymousClass -> {
            val method = declaration.methods.find { it.name == methodNameIfAnonymous }
            if (method != null) {
                @Suppress("UNCHECKED_CAST")
                return method.uastBody to (method.parameters as Array<PsiParameter?>).toList()
            }
        }

        //Named classes or SAM Conversion via UCallExpression (e.g., Interface { lambda } )
        //This should be tried AFTER UQualifiedReferenceExpression
        is UCallExpression -> {
            // If this is a constructor call (e.g., MyClassInstance())
            val resolvedClass = this.resolve()?.containingClass
            if (resolvedClass != null) {
                // Check if the class implements the listener interface
                val isInstanceClass =
                    resolvedClass.iSSubclassOf(interfaceOrSuperclassFQCN, context) ||
                            resolvedClass.superTypes.any {
                                it.isSubclassOf(
                                    interfaceOrSuperclassFQCN,
                                    context
                                )
                            }
                if (isInstanceClass) {
                    val uClass = this.classReference?.resolve()?.toUElement() as? UClass
                        ?: resolvedClass.toUElement() as? UClass
                    val method = uClass?.methods?.find { it.name == methodNameIfAnonymous }
                    if (method != null) {
                        return method.uastBody to method.uastParameters
                    }
                }
            }

            when (val arg = valueArguments.firstOrNull()) {
                is ULambdaExpression,
                is UObjectLiteralExpression -> return arg.extractBodyExpressionAndParams(
                    interfaceOrSuperclassFQCN,
                    methodNameIfAnonymous,
                    context
                )
            }
        }

        //The UExpression IS the lambda (most direct)
        is ULambdaExpression -> {
            return body to valueParameters
        }

        //Direct Anonymous class (less common as the direct listenerArgument in an assignment)
        is UAnonymousClass -> {
            val method = methods.find { it.name == methodNameIfAnonymous }
            if (method != null) {
                @Suppress("UNCHECKED_CAST")
                return method.uastBody to (method.parameters as Array<PsiParameter?>).toList()
            }
        }
    }

    return null to emptyList()
}


fun UExpression.getClassTypeForClassName(fqcn: String): PsiClassType? {
    val expectedPreferenceClass = fqcn
    // Get the actual IntelliJ Project from a PSI element
    val intellijProject = sourcePsi?.project ?: return null

    // Preferred: Scope from the specific PSI element
    // Fallback: A scope covering the entire project and its libraries
    val resolveScope = sourcePsi?.resolveScope ?: GlobalSearchScope.allScope(intellijProject)

    return JavaPsiFacade.getInstance(intellijProject)
        .elementFactory.createTypeByFQClassName(
            expectedPreferenceClass,
            resolveScope
        )
}

fun UExpression.resolveLayoutNameIfViewBindingReference(
    context: JavaContext
): String? {

    var argument: UExpression? = this
    while (argument is USimpleNameReferenceExpression) {
        argument = (argument.resolve() as? PsiVariable)?.initializer as UExpression?
    }


    if (argument is UQualifiedReferenceExpression && argument.selector.sourcePsi?.text == "root") {
        // Argument is potentially "binding.root"
        val bindingVariableExpr = argument.receiver // This is "binding"
        if (bindingVariableExpr is USimpleNameReferenceExpression) {
            val resolvedBindingVar = bindingVariableExpr.resolve()
            if (resolvedBindingVar is com.intellij.psi.PsiVariable) {
                val bindingPsiType = resolvedBindingVar.type
                val bindingPsiClass = context.evaluator.getTypeClass(bindingPsiType)
                return bindingPsiClass?.resolveLayoutNameIfBindingClass(context)
            }
        }
    }
    return null
}


// For case: R.layout.xxx
fun UExpression.resolveLayoutNameIfLayoutResReference(
    context: JavaContext
): String? {
    var name: String? = null
    if (this is UQualifiedReferenceExpression) {
        val receiver = receiver.sourcePsi?.text
        if (receiver != null && (receiver.endsWith(".layout") || receiver.endsWith("$" + "layout"))) {
            name = selector.sourcePsi?.text
        }
    } else if (this is USimpleNameReferenceExpression) {
        val resolved = resolve()
        if (resolved is PsiField) {
            val field = resolved
            val parentClass = field.containingClass
            if (parentClass?.name == "layout") {
                val grandparentClass = parentClass.containingClass
                if (grandparentClass?.name == "R" || grandparentClass?.qualifiedName == context.project.`package` + ".R") {
                    name = field.name
                }
            }
        }
    }
    return name
}
