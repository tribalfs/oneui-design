package dev.oneuiproject.oneui.util

import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Finds the XML resource name (e.g., "filename" from `R.xml.filename`)
 * passed as the first argument to a specific method call within this UClass.
 *
 * This function traverses the UAST (Universal Abstract Syntax Tree) of the class
 * looking for method calls that match the provided `methodName`.
 * If such a call is found, it attempts to resolve the first argument
 * as an XML resource name (e.g., `R.xml.some_file`).
 *
 * @param methodName The name of the method to search for (e.g., "addPreferencesFromResource").
 * @param receiverString The expected string representation of the receiver for the XML resource
 *                       (e.g., "R.xml", R.layout or a other equivalent).
 * @return The resolved XML resource name (e.g., "some_file") if found, otherwise `null`.
 *         It stops searching and returns the first match found.
 */
fun UClass.resolveXmlResNameFromArg(methodName: String, argumentIndex: Int, receiverString: String): String? {
    var resolvedXmlResName: String? = null
    accept(object : AbstractUastVisitor() {
        override fun visitCallExpression(node: UCallExpression): Boolean {
            if (node.methodName == methodName) {
                val firstArgument = node.valueArguments.getOrNull(argumentIndex) ?: return true// Continue visiting
                val xmlResourceName = firstArgument.resolveXmlResourceName(receiverString)
                if (xmlResourceName != null) {
                    resolvedXmlResName = xmlResourceName
                    return false // Found it, stop visiting this class for this purpose
                }
            }
            return true // Continue visiting
        }
    })
    return resolvedXmlResName
}

/**
 * Resolves R.xml.some_file to "some_file"
 */
private fun UExpression.resolveXmlResourceName(receiverString: String): String? {
    val expr = this
    // Handles R.xml.some_file
    if (expr is UQualifiedReferenceExpression) {
        val selector = expr.selector as? USimpleNameReferenceExpression
        if (expr.receiver.asRenderString() == receiverString && selector != null) {
            return selector.identifier
        }
    }
    // Handles just a simple name (rare)
    if (expr is USimpleNameReferenceExpression) {
        return expr.identifier
    }
    return null
}
