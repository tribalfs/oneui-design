package dev.oneuiproject.oneui.widget

import com.android.SdkConstants
import com.android.tools.lint.detector.api.*
import dev.oneuiproject.oneui.util.APP_NS_URI
import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

class SLLTargetImageDetector : ResourceXmlDetector() {

    companion object {
        private const val ATTR_CHECK_MODE = "checkMode"
        private const val CHECK_MODE_OVERLAY_CIRCLE = "overlayCircle"
        private const val ATTR_CHECK_MODE_OVERLAY_CIRCLE = "app:checkMode=\"overlayCircle\""
        private const val ATTR_TARGET_IMAGE = "targetImage"
        private const val ATTR_TARGET_IMAGE_PLACEHOLDER = "app:$ATTR_TARGET_IMAGE=\"@id/target_image_view_id\""
        private const val IMAGE_VIEW_TAG = "ImageView"
        private const val APPCOMPAT_IMAGE_VIEW_TAG = "AppCompatImageView"

        val ISSUE: Issue = Issue.create(
            id = "SLLTargetImageMissing",
            briefDescription = "Missing targetImage for overlayCircle checkMode",
            explanation = """
                When `app:checkMode` is set to `overlayCircle` on a `SelectableLinearLayout`,
                the `app:targetImage` attribute must also be provided. This attribute specifies
                the `ImageView` over which the selection circle will be drawn.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(
                SLLTargetImageDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE
            )
        )
    }

    override fun getApplicableElements(): Collection<String> = listOf("dev.oneuiproject.oneui.widget.SelectableLinearLayout")

    override fun visitElement(context: XmlContext, element: Element) {
        val checkModeAttr = element.getAttributeNodeNS(APP_NS_URI, ATTR_CHECK_MODE)
        val checkModeValue = checkModeAttr?.value

        if (checkModeValue == CHECK_MODE_OVERLAY_CIRCLE) {
            val targetImageAttr = element.getAttributeNodeNS(APP_NS_URI, ATTR_TARGET_IMAGE)
            if (targetImageAttr == null || targetImageAttr.value.isNullOrBlank()) {
                context.report(
                    ISSUE,
                    element,
                    context.getLocation(checkModeAttr),
                    "When `app:checkMode` is `overlayCircle`, `app:targetImage` must be specified.",
                    createAddTargetImageQuickfix(context, element, checkModeAttr)
                )
            } else {
                val targetImageValue = targetImageAttr.value
                if (!targetImageValue.isIdReference()) {
                    context.report(
                        ISSUE,
                        targetImageAttr,
                        context.getLocation(targetImageAttr),
                        "`app:targetImage` should reference an ID (e.g., `@id/your_image_view`).",
                        null
                    )
                }
            }
        }
    }

    private fun createAddTargetImageQuickfix(
        context: XmlContext,
        parentElement: Element,
        checkModeAttr: Attr
    ): LintFix? {
        // Attempt to find the first ImageView child with an ID
        val childImageViews = getChildImageViewsWithId(parentElement)
        val indent = getIndent(context, checkModeAttr)
        var foundImageViewId: String? = null

        if (childImageViews.isNotEmpty()) {
            val firstImageView = childImageViews.first()
            val idValue = firstImageView.getAttributeNodeNS(SdkConstants.ANDROID_URI, "id")?.value
            if (idValue?.isIdReference() == true) {
                foundImageViewId = idValue.replace("@+id/", "@id/")
            }

            if (foundImageViewId != null) {
                return LintFix.create()
                    .name("Set targetImage to '$foundImageViewId'")
                    .replace()
                    .range(context.getLocation(checkModeAttr))
                    .with(
                        ATTR_CHECK_MODE_OVERLAY_CIRCLE +
                                "\n${indent}app:$ATTR_TARGET_IMAGE=\"$foundImageViewId\""
                    )
                    .autoFix()
                    .build()
            }
        }

        // Fallback: If no suitable ImageView is found, offer to add a placeholder
        return LintFix.create()
            .name("Add placeholder app:targetImage attribute")
            .replace()
            .range(context.getLocation(checkModeAttr))
            .with(
                ATTR_CHECK_MODE_OVERLAY_CIRCLE +
                        "\n${indent}$ATTR_TARGET_IMAGE_PLACEHOLDER"
            )
            .build()
    }

    /**
     * Finds direct child ImageView elements that have an android:id attribute.
     */
    private fun getChildImageViewsWithId(element: Element): List<Element> {
        val imageViews = mutableListOf<Element>()
        val childNodes: NodeList = element.childNodes
        for (i in 0 until childNodes.length) {
            val node: Node = childNodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val childElement = node as Element
                // Check for standard ImageView and AppCompatImageView
                if ((childElement.tagName == IMAGE_VIEW_TAG ||
                            childElement.tagName.endsWith(APPCOMPAT_IMAGE_VIEW_TAG)) &&
                    childElement.hasAttributeNS(SdkConstants.ANDROID_URI, "id")
                ) {
                    imageViews.add(childElement)
                }
            }
        }
        return imageViews
    }

    private fun getIndent(
        context: XmlContext,
        checkModeAttr: Attr
    ): String {
        val sourceText = context.getContents() ?: return ""
        val attrOffset = context.getLocation(checkModeAttr).start?.offset ?: return ""
        val lineStart =
            sourceText.lastIndexOf('\n', attrOffset - 1).let { if (it == -1) 0 else it + 1 }
        return buildString {
            var i = lineStart
            while (i < sourceText.length && (sourceText[i] == ' ' || sourceText[i] == '\t')) {
                append(sourceText[i])
                i++
            }
        }
    }

    private fun String.isIdReference(): Boolean = startsWith("@id/") || startsWith("@+id/")

}
