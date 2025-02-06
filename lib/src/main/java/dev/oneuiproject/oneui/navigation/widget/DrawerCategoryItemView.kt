package dev.oneuiproject.oneui.navigation.widget

import android.content.Context
import android.view.View

/**
 * Set this as the actionViewClass in a menuItem to make show it as catergory item.
 * Example:
 * ```
 * <menu xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto">
 *     <item
 *         android:id="@+id/profile"
 *         android:icon="@drawable/ic_profile_icon"
 *         android:title="Profile"
 *         app:actionViewClass="dev.oneuiproject.oneui.navigation.widget.NavDrawerCategoryItemView"/>
 *    //Other menu items
 * ```
 *
 */
internal class DrawerCategoryItemView(context: Context) : View(context)