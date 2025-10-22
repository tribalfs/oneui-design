package dev.oneuiproject.oneuiexample.data.stargazers.network.api

import dev.oneuiproject.oneuiexample.data.stargazers.model.Stargazer
import dev.oneuiproject.oneuiexample.data.stargazers.model.StargazerDetails

import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubService {
    @GET("repos/{owner}/{repo}/stargazers")
    suspend fun getStargazers(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<Stargazer>

    @GET("users/{username}")
    suspend fun getUserDetails(
        @Path("username") username: String
    ): StargazerDetails

}

