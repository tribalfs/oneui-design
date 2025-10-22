package dev.oneuiproject.oneuiexample.data.stargazers.model

import androidx.annotation.Keep

@Keep
//Intermediary class
data class StargazerDetails(
    val login: String,
    val name: String?,
    val id: Int,
    val email: String?,
    val twitter_username: String?,
    val location: String?,
    val company: String?,
    val blog: String?,
    val bio: String?
)