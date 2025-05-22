package dev.oneuiproject.oneui.utils.internal

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import java.lang.reflect.InvocationTargetException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("PrivateApi")
internal fun getSystemProp(key: String?): String? {
    val value: String? = try {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", java.lang.String::class.java).invoke(null, key) as String
    } catch (e: IllegalAccessException) {
        "Unknown"
    } catch (e: InvocationTargetException) {
        "Unknown"
    } catch (e: NoSuchMethodException) {
        "Unknown"
    } catch (e: ClassNotFoundException) {
        "Unknown"
    }
    return value
}
