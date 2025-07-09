package dev.oneuiproject.oneui.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getContainingUClass
import kotlin.text.isWhitespace


/**
 * Retrieves the indent of the receiver element of a given UElement.
 *
 * This function determines the receiver of a UCallExpression or UBinaryExpression.
 * - For UCallExpression, it's the `receiver` property.
 * - For UBinaryExpression (typically assignments), it's the receiver of the left operand if it's a UQualifiedReferenceExpression.
 * - For other UElement types, it defaults to the element itself.
 *
 * If the identified receiver is null (e.g., for static calls or assignments to non-qualified properties without an explicit receiver),
 * the indent of the original `callOrBinaryNode` is returned.
 *
 * @param context The JavaContext used to get the source code and calculate indents.
 * @return The indentation level (number of leading spaces) of the receiver element,
 *         or the indentation of the original element if no receiver is found.
 */
fun UElement.getReceiverIndent(context: JavaContext): Int {
    val callOrBinaryNode = this
    // If receiverElement is null (e.g., a static call or non-qualified property assignment without explicit receiver),
    // then get the indent of the callOrBinaryNode itself.
    val receiverElement = when (callOrBinaryNode) {
        is UCallExpression -> callOrBinaryNode.receiver ?: callOrBinaryNode
        is UBinaryExpression -> (callOrBinaryNode.leftOperand as? UQualifiedReferenceExpression)?.receiver ?: callOrBinaryNode
        else -> callOrBinaryNode // Fallback to the node itself
    }
    val psiElement = receiverElement.sourcePsi ?: return 0
    val containingFile = psiElement.containingFile ?: return 0

    val document = PsiDocumentManager.getInstance(containingFile.project).getDocument(containingFile) ?: return 0
    // Alternative if context.psiFile is reliable and matches containingFile:
    // val document: Document? = context.psiFile?.viewProvider?.document

    try {
        val lineNumber = document.getLineNumber(psiElement.textOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)

        if (lineStartOffset < 0 || lineEndOffset > document.textLength || lineStartOffset > lineEndOffset) {
            return 0
        }

        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        return lineText.takeWhile { it.isWhitespace() }.length
    } catch (e: IndexOutOfBoundsException) {
        // TextOffset  invalid or document changed
        return 0
    }
}

/**
 * Finds the receiver of the nearest enclosing scope function (`apply`, `also`, `run`, `let`)
 * for the given `UElement`.
 *
 * This function traverses up the UAST tree from the current element.
 * For each `UCallExpression` encountered, it checks if the method name is one of the
 * standard Kotlin scope functions. If a scope function is found, its `receiver`
 * expression is returned.
 *
 * The search stops if a `UClass` boundary is reached without finding a scope function,
 * in which case `null` is returned.
 *
 * @return The `UExpression` representing the receiver of the enclosing scope function,
 *         or `null` if no such scope function is found in the ancestry up to the
 *         containing class.
 */
fun UElement.resolveScopedReceiverOrNull(): UExpression? {

    var current: UElement? = this

    while (current != null) {
        current.getScopeReceiverOrNull()?.let {
            return it
        }
        // Stop if we reach a class boundary without finding apply
        if (current is UClass) break
        current = current.uastParent
    }
    return null
}

fun UElement.getScopeReceiverOrNull(): UExpression? {
    if ((this as? UCallExpression)?.isMethodNameOneOf(
            setOf(
                "apply",
                "also",
                "run",
                "let"
            )
        ) == true
    ) {
        return receiver
    }
    return null
}


fun UElement.getContainingUClassIfFieldExist(fieldName: String): UClass? {
    val containingClass = getContainingUClass() ?: return null
    return if (containingClass.findFieldByName(fieldName, true) != null){
        containingClass
    } else null
}