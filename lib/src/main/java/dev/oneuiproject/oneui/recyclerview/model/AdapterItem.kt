package dev.oneuiproject.oneui.recyclerview.model

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * A data class representing an item in a RecyclerView for used as a parameter
 * in the selection listener lambdas. This provides relevant information about
 * the item that was interacted with.
 *
 * @property itemView The child view of the selected item. Can be null if the view is not currently laid out.
 * @property position The adapter position of the selected item.
 * @property id The stable ID of the selected item, or [RecyclerView.NO_ID] if not available.
 */
data class AdapterItem(
    val itemView: View?,
    val position: Int,
    val id: Long
)
