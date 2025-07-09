package dev.oneuiproject.oneui.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass

fun PsiClass.resolveLayoutNameIfBindingClass(context: JavaContext): String? {
    if (!isViewBindingClass(context)) return null

    val simpleName = qualifiedName?.substringAfterLast('.') ?: return null
    if (!simpleName.endsWith("Binding")) return null

    val nameWithoutBinding = simpleName.removeSuffix("Binding")
    // Convert CamelCase to snake_case
    return nameWithoutBinding
        .replace(Regex("(?<=[a-z0-9])(?=[A-Z])")) { "_" + it.value }
            .lowercase()
}

fun PsiClass.isViewBindingClass(
    context: JavaContext
): Boolean = context.evaluator.extendsClass(this, "androidx.viewbinding.ViewBinding", false)
