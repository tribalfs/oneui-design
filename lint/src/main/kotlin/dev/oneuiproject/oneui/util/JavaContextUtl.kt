package dev.oneuiproject.oneui.util

import com.android.tools.lint.detector.api.JavaContext
import java.io.File

/**
 * This function is responsible for looking up the File
 * corresponding to the provided `xmlResName` in the "xml" resource folder.
 *
 * @param resourceFolder The name of the folder containing the XML resources.
 * @param xmlResNameWithExtension The name of the XML resource file without the ".xml" extension.
 * @return The File object if found, null otherwise.
 */
fun JavaContext.findResourceXmlFile(resourceFolder: String, xmlResNameWithExtension: String): File? {
    return project.resourceFolders
        .flatMap { folder ->
            val xmlDir = File(folder, resourceFolder)
            if (xmlDir.isDirectory) {
                xmlDir.listFiles()?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }
        .firstOrNull { it.isFile && it.name == xmlResNameWithExtension }
}
