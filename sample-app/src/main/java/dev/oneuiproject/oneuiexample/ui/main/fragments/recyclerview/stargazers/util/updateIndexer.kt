package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util

import android.database.MatrixCursor
import androidx.indexscroll.widget.SeslCursorIndexer
import androidx.indexscroll.widget.SeslIndexScrollView
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model.StargazersListItemUiModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.toIndexCharsArray
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.toStringsList

internal fun SeslIndexScrollView.updateIndexer(items: List<StargazersListItemUiModel>) {
    val cursor = MatrixCursor(arrayOf("item"))
    for (item in items.toStringsList()){
        cursor.addRow(arrayOf(item))
    }
    val indexChars = items.toIndexCharsArray()

    cursor.moveToFirst()
    setIndexer(SeslCursorIndexer(cursor, 0,indexChars, 0).apply {
        //setGroupItemsCount(1)
        //setMiscItemsCount(3)
    }
    )
    postInvalidate()
}
