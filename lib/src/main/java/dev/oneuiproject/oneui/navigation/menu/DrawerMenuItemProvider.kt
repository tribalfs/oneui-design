package dev.oneuiproject.oneui.navigation.menu

import android.animation.AnimatorInflater
import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.core.content.ContextCompat
import androidx.core.view.updateMargins
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.navigation.widget.DrawerMenuItemView

internal object DrawerMenuItemProvider {

    fun createCategoryItemView(context: Context) : DrawerMenuItemView {
        val itemMinHeight = context.resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_min_height)
        val drawerItemView = DrawerMenuItemView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginTop =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_margin_top)
                val marginBottom =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_margin_bottom)
                updateMargins(top = marginTop, bottom = marginBottom)
            }
            this.minHeight = itemMinHeight
        }

        val rippleBackground = View(context).apply {
            id = R.id.ripple_background
            layoutParams = ConstraintLayout.LayoutParams(0, itemMinHeight).apply {
                marginStart =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_margin_start)
                marginEnd =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_margin_end)
                topMargin =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_elevation)
                bottomMargin =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_elevation)
                topToTop = PARENT_ID
                bottomToBottom = PARENT_ID
                startToStart = PARENT_ID
                endToEnd = PARENT_ID
            }
            elevation =
                resources.getDimension(R.dimen.oui_des_drawer_menu_category_view_elevation)
            foreground =
                ContextCompat.getDrawable(context,R.drawable.oui_des_drawer_menu_category_fg)
            background =
                ContextCompat.getDrawable(context,R.drawable.oui_des_drawer_menu_category_bg)
        }

        val iconBackground = View(context).apply {
            id = R.id.icon_background
            val width =
                resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_item_selected_layout_width_collapsed)
            layoutParams = ConstraintLayout.LayoutParams(width, 0).apply {
                marginStart =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_margin_start)
                topMargin =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_item_selected_layout_margin_vertical)
                bottomMargin =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_item_selected_layout_margin_vertical)
                topToTop = PARENT_ID
                bottomToBottom = PARENT_ID
                startToStart = PARENT_ID
            }
            elevation =
                resources.getDimension(R.dimen.oui_des_drawer_menu_category_view_elevation)
            background =
                ContextCompat.getDrawable(context,R.drawable.oui_des_drawer_menu_item_bg)
            isDuplicateParentStateEnabled = false
            outlineProvider = null
        }

        val icon = ImageView(context).apply {
            id = R.id.drawer_menu_item_icon
            val size = resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_item_icon_size)
            layoutParams = ConstraintLayout.LayoutParams(size, size).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_icon_margin_start)
                topToTop = PARENT_ID
                bottomToBottom = PARENT_ID
                startToStart = PARENT_ID
            }
            elevation = resources.getDimension(R.dimen.oui_des_drawer_menu_category_view_elevation)
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = null
            outlineProvider = null
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val title = TextView(context, null, 0, R.style.OneUI_Drawer_Category_Title).apply {
            id = R.id.drawer_menu_item_title
            layoutParams = ConstraintLayout.LayoutParams(0, WRAP_CONTENT).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_group_margin_vertical)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_group_margin_vertical)
                marginStart = resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_title_margin_start)
                marginEnd = resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_title_margin_end)
                bottomToBottom = PARENT_ID
                topToTop = PARENT_ID
                endToStart = R.id.drawer_menu_item_count_stub
                startToEnd = R.id.drawer_menu_item_icon
            }
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            elevation = resources.getDimension(R.dimen.oui_des_drawer_menu_category_view_elevation)
            outlineProvider = null
        }

        val countStub = ViewStub(context).apply {
            id = R.id.drawer_menu_item_count_stub
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_category_view_count_margin_end
                    )
                constrainedWidth = true
            }

            layoutResource = R.layout.oui_des_drawer_menu_category_count
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            elevation =
                resources.getDimension(R.dimen.oui_des_drawer_menu_category_view_elevation)
            outlineProvider = null
        }

        (title.layoutParams as ConstraintLayout.LayoutParams).endToStart = countStub.id
        drawerItemView.addView(rippleBackground)
        drawerItemView.addView(iconBackground)
        drawerItemView.addView(icon)
        drawerItemView.addView(title)
        drawerItemView.addView(countStub)
        return drawerItemView
    }

    fun createCategoryItemViewTablet(context: Context) : DrawerMenuItemView {
        val itemMinHeight = context.resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_min_height)
        val drawerItemView = DrawerMenuItemView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginBottom =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_margin_bottom)
                updateMargins(bottom = marginBottom)
            }
            this.minHeight = itemMinHeight
            setAddStatesFromChildren(true)
            stateListAnimator =
                AnimatorInflater.loadStateListAnimator(
                    context,
                    context.obtainStyledAttributes(
                        intArrayOf(androidx.appcompat.R.attr.seslMediumTouchAnimator)
                    ).getResourceId(0, 0)
                )
        }

        val rippleBackground = View(context).apply {
            id = R.id.ripple_background
            layoutParams = ConstraintLayout.LayoutParams(0, 0).apply {
                marginStart =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_margin_start)
                marginEnd =
                    resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_category_view_margin_end)
                topToTop = PARENT_ID
                bottomToBottom = PARENT_ID
                startToStart = PARENT_ID
                endToEnd = PARENT_ID
            }
            background = ContextCompat.getDrawable(context, R.drawable.oui_des_drawer_menu_category_bg_navrail)
        }

        val iconBackground = View(context).apply {
            id = R.id.icon_background
            layoutParams = ConstraintLayout.LayoutParams(
                resources.getDimensionPixelSize(
                    R.dimen.oui_des_drawer_menu_item_selected_layout_width_collapsed
                ),
                0
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                marginStart =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_category_view_margin_start
                    )
                marginEnd =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_category_view_margin_end
                    )
            }

            background =
                ContextCompat.getDrawable(context, R.drawable.oui_des_drawer_menu_item_bg)
            isDuplicateParentStateEnabled = false
        }

        val icon = ImageView(context).apply {
            id = R.id.drawer_menu_item_icon
            layoutParams = ConstraintLayout.LayoutParams(
                resources.getDimensionPixelSize(
                    R.dimen.oui_des_drawer_menu_item_icon_size
                ),
                resources.getDimensionPixelSize(
                    R.dimen.oui_des_drawer_menu_item_icon_size
                )
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                marginStart =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_category_view_icon_margin_start
                    )
                horizontalBias = 0f
            }

            scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val title = TextView(context, null, 0, R.style.OneUI_Drawer_Category_Title).apply {
            id = R.id.drawer_menu_item_title
            layoutParams = ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToEnd = icon.id
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                marginStart =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_category_view_title_margin_start
                    )
                marginEnd =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_category_title_margin
                    )
                topMargin =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_group_margin_vertical
                    )
                bottomMargin =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_group_margin_vertical
                    )
            }

            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val countStub = ViewStub(context).apply {
            id = R.id.drawer_menu_item_count_stub
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_category_view_count_margin_end
                    )
                constrainedWidth = true
            }

            layoutResource = R.layout.oui_des_drawer_menu_category_count
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            elevation =
                resources.getDimension(
                    R.dimen.oui_des_drawer_menu_category_view_elevation
                )
            outlineProvider = null
        }

        (title.layoutParams as ConstraintLayout.LayoutParams).endToStart = countStub.id
        drawerItemView.addView(rippleBackground)
        drawerItemView.addView(iconBackground)
        drawerItemView.addView(icon)
        drawerItemView.addView(title)
        drawerItemView.addView(countStub)
        return drawerItemView
    }


    fun createDrawerMenuItem(context: Context): DrawerMenuItemView {

        val root = DrawerMenuItemView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val rippleBackground = View(context).apply {
            id = R.id.ripple_background
            layoutParams = ConstraintLayout.LayoutParams(
                0,
                0
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                marginStart =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_selected_layout_margin_start
                    )
                marginEnd =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_selected_layout_margin_end
                    )
                topMargin =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_selected_layout_margin_vertical
                    )
                bottomMargin =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_selected_layout_margin_vertical
                    )
            }

            background =
                ContextCompat.getDrawable(
                    context,
                    R.drawable.oui_des_drawer_menu_item_bg
                )
        }

        val mainContent = ConstraintLayout(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            minimumHeight =
                resources.getDimensionPixelSize(
                    R.dimen.oui_des_drawer_menu_category_min_height
                )
            background = null
        }

        val iconBackground = View(context).apply {
            id = R.id.icon_background
            layoutParams = ConstraintLayout.LayoutParams(
                resources.getDimensionPixelSize(
                    R.dimen.oui_des_drawer_menu_item_selected_layout_width_collapsed
                ),
                0
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                marginStart =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_selected_layout_margin_start
                    )
                topMargin =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_selected_layout_margin_vertical
                    )
                bottomMargin =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_selected_layout_margin_vertical
                    )
            }

            background =
                ContextCompat.getDrawable(
                    context,
                    R.drawable.oui_des_drawer_menu_item_bg
                )
        }

        val icon = ImageView(context).apply {
            id = R.id.drawer_menu_item_icon
            layoutParams = ConstraintLayout.LayoutParams(
                resources.getDimensionPixelSize(
                    R.dimen.oui_des_drawer_menu_item_icon_size
                ),
                resources.getDimensionPixelSize(
                    R.dimen.oui_des_drawer_menu_item_icon_size
                )
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                marginStart =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_icon_margin_start
                    )
            }

            scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            imageTintList =
                ContextCompat.getColorStateList(
                    context,
                    R.color.oui_des_drawer_menu_item_text_color_selector
                )
        }

        val title = TextView(context, null, 0, R.style.OneUI_DrawerCategoryTitle).apply {
            id = R.id.drawer_menu_item_title
            layoutParams = ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToEnd = icon.id
                endToStart = View.generateViewId() // replaced after stub creation
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                horizontalBias = 0f

                marginStart =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_category_title_margin
                    )
                marginEnd =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_category_title_margin
                    )
                topMargin =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_margin_vertical
                    )
                bottomMargin =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_margin_vertical
                    )
            }

            ellipsize = TextUtils.TruncateAt.END
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val expandButtonStub = ViewStub(context).apply {
            id = R.id.drawer_menu_expand_button_stub
            inflatedId = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                20.dpToPx(resources),
                20.dpToPx(resources)
            ).apply {
                startToEnd = title.id
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                horizontalBias = 1f

                marginStart = 11.dpToPx(resources)
                marginEnd =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_count_margin_end
                    )
            }

            layoutResource = R.layout.oui_des_drawer_menu_expand_button
        }


        val countStub = ViewStub(context).apply {
            id = R.id.drawer_menu_item_count_stub
            inflatedId = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToEnd = title.id
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                horizontalBias = 1f

                marginEnd =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_count_margin_end
                    )
            }

            layoutResource = R.layout.oui_des_drawer_menu_item_count
        }

        (title.layoutParams as ConstraintLayout.LayoutParams).endToStart = countStub.id


        mainContent.addView(iconBackground)
        mainContent.addView(icon)
        mainContent.addView(title)
        mainContent.addView(expandButtonStub)
        mainContent.addView(countStub)

        val actionAreaStub = ViewStub(context).apply {
            id = R.id.nav_drawer_menu_item_action_area_stub
            inflatedId = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = mainContent.id
                bottomToBottom = rippleBackground.id
                startToStart = rippleBackground.id
                endToEnd = rippleBackground.id
                constrainedWidth = true

                marginStart =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_action_container_margin
                    )
                marginEnd =
                    resources.getDimensionPixelSize(
                        R.dimen.oui_des_drawer_menu_item_action_container_margin
                    )
            }

            layoutResource = R.layout.oui_des_drawer_menu_item_action_area
        }

        root.addView(rippleBackground)
        root.addView(mainContent)
        root.addView(actionAreaStub)

        return root
    }

}