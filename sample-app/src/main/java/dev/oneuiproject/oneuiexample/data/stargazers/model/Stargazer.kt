package dev.oneuiproject.oneuiexample.data.stargazers.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import dev.oneuiproject.oneuiexample.data.stargazers.util.Converters
import dev.oneuiproject.oneuiexample.data.stargazers.util.getAvatarBase64Data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File

@Keep
@Parcelize
@Entity(tableName = "stargazers")
@TypeConverters(Converters::class)
data class Stargazer(
    @PrimaryKey
    val login: String,
    val id: Int,
    val node_id: String,
    val avatar_url: String,
    val html_url: String,
    val name: String? = null,
    val organizations_url: String?,
    val location: String?,
    val company: String?,
    val email: String?,
    val twitter_username: String?,
    val blog: String?,
    val bio: String?,
    @ColumnInfo(defaultValue = "")
    val starredRepos: Set<String> = emptySet()
): Parcelable {

    fun getDisplayName(): String = name ?: login
    fun getSearchableString(): String = "${name?:""} $html_url"

    suspend fun asVCardFile(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, "stargazer_${id}_${getDisplayName()}.vcf")
            .apply { writeText(getVCardContent(context)) }
    }

    private suspend fun getVCardContent(context: Context) = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:2.1")
        appendLine("FN:${getDisplayName()}")
        appendLine("NICKNAME:${login}")
        // appendLine("PHOTO;VALUE=URI:${avatar_url}")
        appendLine("URL:${html_url}")
        email?.let { appendLine("EMAIL:$it") }
        company?.let { appendLine("ORG:$it") }
        location?.let { appendLine("ADR:$it") }
        bio?.let { appendLine("TITLE:$it") }
        blog?.let { appendLine("URL:$it") }
        organizations_url?.let { appendLine("URL:$it") }
        twitter_username?.let { appendLine("X-TWITTER:https://x.com/$it") }
        getAvatarBase64Data(avatar_url, context)?.let { base64Image ->
            appendLine("PHOTO;ENCODING=BASE64;TYPE=PNG:$base64Image")
        }
        appendLine("NOTE:Starred repos: ${starredRepos.joinToString(", ")}")
        appendLine("END:VCARD")
    }
}