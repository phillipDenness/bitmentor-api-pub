package bitmentor.api.model.auth.github

data class GithubAccessResponse(
        val access_token: String,
        val scope: String,
        val token_type: String
)