package dev.oneuiproject.oneuiexample.data.stargazers.model

sealed class RefreshResult{
    object Updated : RefreshResult()
    object UpdateRunning : RefreshResult()
    /** Bad credentials*/
    object UnauthorizedError: RefreshResult()
    /** Rate limit exceeded or other permission issue */
    object ForbiddenError : RefreshResult()
    /** Repository not found */
    object NotFoundError : RefreshResult()
    data class OtherHttpException(val code: Int, val message: String) : RefreshResult()
    data class OtherException(val exception: Throwable) : RefreshResult()
}
