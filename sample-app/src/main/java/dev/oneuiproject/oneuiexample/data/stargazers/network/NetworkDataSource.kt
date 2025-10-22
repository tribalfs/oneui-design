package dev.oneuiproject.oneuiexample.data.stargazers.network

import dev.oneuiproject.oneuiexample.data.stargazers.model.Stargazer
import dev.oneuiproject.oneuiexample.data.stargazers.network.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object NetworkDataSource {

    suspend fun fetchStargazers(): Result<List<Stargazer>> = runCatching { getStargazers() }

    private suspend fun getStargazers() = withContext(Dispatchers.IO){
        val repoList = listOf(
            "sesl-androidx",
            "sesl-material-components-android",
            "oneui-design"
        )

        with(RetrofitClient.instance) {
            val stargazerList = repoList.map { repoName ->
                async {
                    getStargazers("tribalfs", repoName).map { stargazer ->
                        stargazer to repoName
                    }
                }
            }.awaitAll().flatten()

            val stargazerMap = stargazerList.groupBy({ it.first.login }) { it }

            val mergedStargazers = stargazerMap.map { (_, stargazerTuples) ->
                val stargazer = stargazerTuples.first().first
                val starredRepos = stargazerTuples.map { it.second }.toSet()
                stargazer.copy(starredRepos = starredRepos)
            }

            return@with mergedStargazers.map {
                async {
                    val userDetails = getUserDetails(it.login)
                    it.copy(
                        name = userDetails.name,
                        location = userDetails.location,
                        company = userDetails.company,
                        email = userDetails.email,
                        twitter_username = userDetails.twitter_username,
                        blog = userDetails.blog,
                        bio = userDetails.bio,
                    )
                }
            }.awaitAll()
                .sortedWith(compareBy(
                    { it.getDisplayName().first().isDigit() },
                    { it.getDisplayName().uppercase() }
                ))
        }
    }
}