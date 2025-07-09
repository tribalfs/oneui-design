package dev.oneuiproject.oneui.preference

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import dev.oneuiproject.oneui.util.getClassTypeForClassName
import dev.oneuiproject.oneui.util.resolveXmlResNameFromArg
import dev.oneuiproject.oneui.util.findResourceXmlFile
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getContainingUClass
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PreferenceKeyDetector : Detector(), SourceCodeScanner {

    // Cache to store XML resource name per class
    // Key: Fully qualified class name
    // Value: The resolved XML resource name or null if not found/applicable
    private val classToXmlResNameCache = mutableMapOf<String, String?>()

    // Cache for the File object itself to avoid repeated file lookups
    // Key: XML resource name
    private val xmlResNameToProjectFileCache = mutableMapOf<String, File?>()

    // Key: XML resource name
    // Value: Map<String (key), String (xmlTagName)>
    private val xmlFileToKeyAndTagCache = mutableMapOf<String, Map<String, String>>()

    companion object {
        private const val UNSPECIFIED_PREFERENCE = "T"

        private val PREFERENCE_TAG_TO_CLASS_MAP = mapOf(
            "CheckBoxPreference" to "androidx.preference.CheckBoxPreference",
            "ColorPickerPreference" to "dev.oneuiproject.oneui.preference.ColorPickerPreference",
            "DropDownPreference" to "androidx.preference.DropDownPreference",
            "EditTextPreference" to "androidx.preference.EditTextPreference",
            "HorizontalRadioPreference" to "dev.oneuiproject.oneui.preference.HorizontalRadioPreference",
            "InsetPreferenceCategory" to "dev.oneuiproject.oneui.preference.InsetPreferenceCategory",
            "LayoutPreference" to "dev.oneuiproject.oneui.preference.LayoutPreference",
            "ListPreference" to "androidx.preference.ListPreference",
            "MultiSelectListPreference" to "androidx.preference.MultiSelectListPreference",
            "Preference" to "androidx.preference.Preference",
            "PreferenceCategory" to "androidx.preference.PreferenceCategory",
            "PreferenceScreen" to "androidx.preference.PreferenceScreen",
            "SeekBarPreference" to "androidx.preference.SeekBarPreference",
            "SeslSwitchPreferenceScreen" to "androidx.preference.SeslSwitchPreferenceScreen",
            "SuggestionCardPreference" to "dev.oneuiproject.oneui.preference.SuggestionCardPreference",
            "SwitchBarPreference" to "dev.oneuiproject.oneui.preference.SwitchBarPreference",
            "SwitchPreference" to "androidx.preference.SwitchPreference", // Older one
            "SwitchPreferenceCompat" to "androidx.preference.SwitchPreferenceCompat",
            "SeekBarPreferencePro" to "dev.oneuiproject.oneui.preference.SeekBarPreferencePro",
            "TipsCardPreference" to "dev.oneuiproject.oneui.preference.TipsCardPreference",
            "UpdatableWidgetPreference" to "dev.oneuiproject.oneui.preference.UpdatableWidgetPreference",
        )

        val ISSUE_KEY_NOT_FOUND: Issue = Issue.create(
            id = "FindPreferenceKeyNotFound", // New ID
            briefDescription = "findPreference key does not exist in preference XML",
            explanation = """
            Ensures that every key passed to findPreference() exists in the associated preference XML file.
        """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                PreferenceKeyDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val ISSUE_TYPE_MISMATCH: Issue = Issue.create(
            id = "FindPreferenceTypeMismatch",
            briefDescription = "findPreference type does not match the type in preference XML",
            explanation = """
            Ensures that the type of Preference retrieved using findPreference() is compatible
            with the type defined for that key in the preference XML file.
            For example, if the XML defines a key as an <EditTextPreference>, the code
            should expect an EditTextPreference or a superclass like Preference.
        """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                PreferenceKeyDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun applicableSuperClasses(): List<String> = listOf(
        "androidx.preference.PreferenceFragmentCompat"
    )

    override fun getApplicableMethodNames(): List<String> = listOf("findPreference")

    // In visitMethodCall, after successfully finding the key in XML:
    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val containingClass = node.getContainingUClass() ?: return
        val qualifiedClassName = containingClass.qualifiedName ?: return

        var xmlResName = classToXmlResNameCache.computeIfAbsent(qualifiedClassName) {
            containingClass.resolveXmlResNameFromArg("setPreferencesFromResource",0, "R.xml")
        }
        if (xmlResName == null) {
            xmlResName = containingClass.resolveXmlResNameFromArg("addPreferencesFromResource",0, "R.xml")
        }

        if (xmlResName == null) return

        val xmlFile = xmlResNameToProjectFileCache.computeIfAbsent(xmlResName) {
            context.findResourceXmlFile("xml", "$xmlResName.xml")
        }
        if (xmlFile == null) return

        // --- Parse Keys and Tags from XML ---
        val keyAndTagInXml: Map<String, String> =
            xmlFileToKeyAndTagCache.computeIfAbsent(xmlFile.absolutePath) { extractPreferenceKeyAndTagFromXml(xmlFile) }

        // --- Perform the Key Existence Check ---
        val keyArgExpression = node.valueArguments.getOrNull(0) ?: return
        val preferenceKeyString = resolveKeyArgument(context, keyArgExpression) ?: return

        val xmlTagName = keyAndTagInXml[preferenceKeyString]

        if (xmlTagName == null) { // Key not found in XML
            context.report(
                ISSUE_KEY_NOT_FOUND,
                node,
                context.getLocation(keyArgExpression),
                "Key \"$preferenceKeyString\" passed to findPreference does not exist in ${xmlFile.name}."
            )
            return
        }

        // --- Perform Type Check ---

        var actualPreferenceType: PsiType? = null
        val uastParent = node.uastParent

        // 1. Explicit type arguments to findPreference (e.g., findPreference<EditTextPreference>("..."))
        if (node.typeArguments.isNotEmpty()) {
            actualPreferenceType = node.typeArguments.firstOrNull() // typeArguments is List<PsiType>
        }

        // 2. Assignment to a typed variable OR part of a Kotlin 'as' cast assigned to a variable
        // Covers:
        //   val pref: EditTextPreference = findPreference("key")
        //   val pref = findPreference("key") as EditTextPreference
        //   val pref = (EditTextPreference) findPreference("key") // If Java cast results in UVariable
        if (actualPreferenceType == null && uastParent is UVariable) {
            actualPreferenceType = uastParent.type
        }

        // 3. Kotlin 'as' cast where findPreference is the direct operand
        // Covers: someFun(findPreference("key") as EditTextPreference)
        // Or if the UVariable case above didn't capture it (e.g. complex initializer)
        if (actualPreferenceType == null && uastParent is UBinaryExpressionWithType && uastParent.operand == node) {
            actualPreferenceType = uastParent.typeReference?.type
        }

        // 4. If part of a qualified expression (e.g. someObj.findPreference("key").someMethod() )
        //    or assigned after a chain: val pref: SomeType = someObj.findPreference("key")
        if (actualPreferenceType == null && uastParent is UQualifiedReferenceExpression && uastParent.selector == node) {
            var currentNestingLevel = 0 // To prevent overly deep searches
            var current: UElement? = uastParent.uastParent
            while (current != null && currentNestingLevel < 5) { // Limit search depth
                if (current is UVariable) { // val pref = someObj.findPreference("key")
                    actualPreferenceType = current.type
                    break
                }
                // (someObj.findPreference("key") as EditTextPreference).title = "..."
                if (current is UBinaryExpressionWithType && current.operand == uastParent) { // Kotlin 'as' on the chained expression
                    actualPreferenceType = current.typeReference?.type
                    break
                }
                current = current.uastParent
                currentNestingLevel++
            }
        }

        // 5. Fallback to the method's return type if still unknown.
        if (actualPreferenceType == null) {
            node.resolve().let { resolvedMethod ->
                if (resolvedMethod is PsiMethod) {
                    actualPreferenceType = resolvedMethod.returnType
                }
            }
        }

        if (actualPreferenceType != null) {
            val expectedPreferenceClass = PREFERENCE_TAG_TO_CLASS_MAP[xmlTagName] ?: return
            val expectedPreferenceType = node.getClassTypeForClassName(expectedPreferenceClass) ?: return
            val actualTypeName = actualPreferenceType.canonicalText.substringBefore('<')

            if (actualTypeName != UNSPECIFIED_PREFERENCE && !actualPreferenceType.isAssignableFrom(expectedPreferenceType)) {
                context.report(
                    ISSUE_TYPE_MISMATCH,
                    node,
                    context.getLocation(node),
                    "Type mismatch for key \"$preferenceKeyString\". " +
                            "XML defines it as $xmlTagName (expecting compatible with $expectedPreferenceClass), " +
                            "but code uses an incompatible type ${actualTypeName}."
                )
            }
        }
    }


    /** Parses the XML file and extracts all preference keys and their XML tag names. */
    private fun extractPreferenceKeyAndTagFromXml(xmlFile: File): Map<String, String> {
        val keyToTagMap = mutableMapOf<String, String>()
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
            val nodeList = doc.getElementsByTagName("*") // Get all elements
            for (i in 0 until nodeList.length) {
                val node = nodeList.item(i)
                if (node is Element) {
                    val key = node.getAttribute("android:key")
                    if (!key.isNullOrEmpty()) {
                        val xmlTagName = node.tagName
                        val tagName = xmlTagName.split(".").last() // e.g., "EditTextPreference"
                        if (PREFERENCE_TAG_TO_CLASS_MAP.containsKey(tagName)) {
                            keyToTagMap[key] = tagName
                        } else if (xmlTagName.contains(".")) {
                            // Tag is not in the map, but it contains a '.',
                            // suggesting it might be a FQCM.
                            // We'll use the tagName directly as the expected class.
                            keyToTagMap[key] = xmlTagName
                        } else {
                            // Unknown tag, default to "Preference"
                            keyToTagMap[key] = "Preference"
                        }
                    }
                }
            }
        } catch (e: Exception) { }
        return keyToTagMap
    }

    private fun resolveKeyArgument(context: JavaContext, arg: UExpression): String? {
        // Try to evaluate as a literal
        arg.evaluateString()?.let { return it }
        // Try to resolve as a constant
        if (arg is USimpleNameReferenceExpression) {
            val resolved = arg.resolve()
            if (resolved is PsiVariable) {
                val initializer = resolved.initializer
                if (initializer is PsiLiteralExpression) {
                    return initializer.value as? String
                }
            }
        }
        return null
    }
}