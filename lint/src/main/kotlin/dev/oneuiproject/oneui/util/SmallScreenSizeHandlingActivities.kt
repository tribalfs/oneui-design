package dev.oneuiproject.oneui.util // Or your preferred package

import com.android.tools.lint.detector.api.Project

object SmallScreenSizeHandlingActivities {
    private val KEY = Any()

    @Suppress("UNCHECKED_CAST")
    private val PER_PROJECT_KEY = Any()

    @Suppress("UNCHECKED_CAST")
    fun getForProject(project: Project): MutableSet<String>? {
        return project.getClientProperty(PER_PROJECT_KEY) as? MutableSet<String>
    }

    fun putForProject(project: Project, set: MutableSet<String>) {
        project.putClientProperty(PER_PROJECT_KEY, set)
    }
}