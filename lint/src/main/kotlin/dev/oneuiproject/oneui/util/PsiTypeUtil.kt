package dev.oneuiproject.oneui.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType

fun PsiType.isSubclassOf(classFQCN: String, context: JavaContext, strict: Boolean = false) =
    context.evaluator.extendsClass(
        context.evaluator.getTypeClass(this), // This can be null if type cannot be resolved to a class
        classFQCN, strict)


fun PsiClass.iSSubclassOf(classFQCN: String, context: JavaContext, strict: Boolean = false) =
    context.evaluator.extendsClass(
        this,
        classFQCN, strict)