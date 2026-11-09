package com.cobalt.android.feed.comments

import org.json.JSONObject

/**
 * Parses YouTube (Innertube) comment responses (Phase E11).
 *
 * Kept pure and `internal`-friendly so it is unit tested against synthetic JSON
 * — the response shape is the fragile part of this feature, not the networking.
 *
 * Two jobs:
 *  - [commentContinuationToken]: dig the "load comments" continuation token out
 *    of a `next` response (comments are never inlined in the first response).
 *  - [parseComments]: turn a continuation response into [Comment]s.
 *
 * Both walk the JSON defensively (by key, anywhere in the tree) rather than by
 * fixed path, because YouTube nests these under shifting wrappers.
 */
object CommentsParser {

    data class Comment(
        val author: String,
        val text: String,
        val likeCount: String,
        val publishedTime: String
    )

    data class Page(val comments: List<Comment>, val nextToken: String?)

    /**
     * The comments continuation token, or null if there are none.
     *
     * A `next` response carries several continuation tokens (comments, related
     * videos, …), so the naive "first token" is wrong. The reliable anchor is
     * the comments header: if any subtree is a comments section
     * (`commentsHeaderRenderer`/`commentsSectionRenderer`), the token inside it
     * is the one we want. Only if there is no such subtree do we fall back to
     * the first token found — better than nothing, and rare in practice.
     */
    fun commentContinuationToken(json: JSONObject): String? {
        val sections = mutableListOf<JSONObject>()
        walk(json) { node ->
            if (node.has("commentsHeaderRenderer") || node.has("commentsSectionRenderer")) {
                sections += node
            }
        }
        sections.firstOrNull()?.let { section ->
            var token: String? = null
            walk(section) { node -> token = token ?: firstToken(node) }
            if (token != null) return token
        }
        var fallback: String? = null
        walk(json) { node -> fallback = fallback ?: firstToken(node) }
        return fallback
    }

    private fun firstToken(node: JSONObject): String? {
        val item = node.optJSONObject("continuationItemRenderer") ?: return null
        val token = item.optJSONObject("continuationEndpoint")
            ?.optJSONObject("continuationCommand")?.optString("token").orEmpty()
        return token.ifEmpty { null }
    }

    /** Parses a comments continuation response. */
    fun parseComments(json: JSONObject): Page {
        val comments = mutableListOf<Comment>()
        var nextToken: String? = null
        walk(json) { node ->
            val entity = node.optJSONObject("commentEntityPayload") ?: return@walk
            val props = entity.optJSONObject("properties")
            val text = props?.optJSONObject("content")?.optString("content").orEmpty()
            val author = entity.optJSONObject("author")
                ?.optJSONObject("name")?.optString("content").orEmpty()
            val toolbar = entity.optJSONObject("toolbar")
            val likes = toolbar?.optJSONObject("likeCountNotliked")
                ?.optString("content").orEmpty()
            val published = props?.optString("publishedTime").orEmpty()
            if (text.isNotBlank()) {
                comments += Comment(author, text, likes, published)
            }
        }
        // The continuation for the *next* page is a bare token under
        // `continuationCommand` in the trailing `continuationItemRenderer`.
        walk(json) { node ->
            val item = node.optJSONObject("continuationItemRenderer") ?: return@walk
            val token = item.optJSONObject("continuationEndpoint")
                ?.optJSONObject("continuationCommand")?.optString("token").orEmpty()
            if (token.isNotEmpty()) nextToken = token
        }
        return Page(comments, nextToken)
    }

    /** Depth-first walk invoking [onMatch] for every JSON object in the tree. */
    private fun walk(node: Any?, onMatch: (JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                onMatch(node)
                for (key in node.keys()) walk(node.opt(key), onMatch)
            }
            is org.json.JSONArray -> {
                for (i in 0 until node.length()) walk(node.opt(i), onMatch)
            }
        }
    }
}
