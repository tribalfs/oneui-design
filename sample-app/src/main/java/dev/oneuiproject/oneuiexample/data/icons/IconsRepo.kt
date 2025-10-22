package dev.oneuiproject.oneuiexample.data.icons

import dev.oneuiproject.oneui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class IconsRepo @Inject constructor() {

    val iconsFlow: Flow<List<Int>> = flow {
        val iconsId = mutableListOf<Int>()
        val rClass: Class<R.drawable> = R.drawable::class.java
        for (field in rClass.getDeclaredFields()) {
            try {
                iconsId.add(field.getInt(null))
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }
        emit(iconsId)
    }.flowOn(Dispatchers.IO)
}