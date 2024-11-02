package dev.oneuiproject.oneuiexample.ui.fragment.contacts.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.util.SeslRoundedCorner
import androidx.appcompat.util.SeslSubheaderRoundedCorner
import androidx.recyclerview.widget.RecyclerView
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.adapter.ContactsAdapter

class ContactsListItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val mDivider: Drawable?
        private val mRoundedCorner: SeslSubheaderRoundedCorner

        init {
            val outValue = TypedValue()
            context.theme.resolveAttribute(androidx.appcompat.R.attr.isLightTheme, outValue, true)

            mDivider = context.getDrawable(
                if (outValue.data == 0)
                    androidx.appcompat.R.drawable.sesl_list_divider_dark
                else
                    androidx.appcompat.R.drawable.sesl_list_divider_light
            )

            mRoundedCorner = SeslSubheaderRoundedCorner(context)
            mRoundedCorner.roundedCorners = SeslRoundedCorner.ROUNDED_CORNER_ALL
        }

        override fun onDraw(
            c: Canvas, parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.onDraw(c, parent, state)

            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val holder = parent.getChildViewHolder(child) as ContactsAdapter.ViewHolder
                if (!holder.isSeparator) {
                    val top = (child.bottom
                            + (child.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin)
                    val bottom = mDivider!!.intrinsicHeight + top

                    mDivider.setBounds(parent.left, top, parent.right, bottom)
                    mDivider.draw(c)
                }
            }
        }

        override fun seslOnDispatchDraw(
            c: Canvas,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val holder = parent.getChildViewHolder(child) as ContactsAdapter.ViewHolder
                if (holder.isSeparator) {
                    mRoundedCorner.drawRoundedCorner(child, c)
                }
            }
        }
    }
