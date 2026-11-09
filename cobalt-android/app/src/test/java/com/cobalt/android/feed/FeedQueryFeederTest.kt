package com.cobalt.android.feed

import com.cobalt.android.feed.source.FeedQueryFeeder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedQueryFeederTest {

    @Test
    fun `returns the requested number of queries`() {
        FeedQueryFeeder.applyCustomQueries(emptyList()) // reset to default pool
        assertEquals(5, FeedQueryFeeder.nextQueries(5).size)
    }

    @Test
    fun `rotation advances between calls`() {
        FeedQueryFeeder.applyCustomQueries(listOf("a", "b", "c", "d"))
        val first = FeedQueryFeeder.nextQueries(2)
        val second = FeedQueryFeeder.nextQueries(2)
        assertEquals(listOf("a", "b"), first)
        assertEquals(listOf("c", "d"), second)
    }

    @Test
    fun `rotation wraps around the pool`() {
        FeedQueryFeeder.applyCustomQueries(listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), FeedQueryFeeder.nextQueries(3))
        // next two must wrap back to the start, not run off the end
        assertEquals(listOf("a", "b"), FeedQueryFeeder.nextQueries(2))
    }

    @Test
    fun `empty pool resets to the shipped default`() {
        FeedQueryFeeder.applyCustomQueries(emptyList())
        val queries = FeedQueryFeeder.nextQueries(3)
        assertEquals(3, queries.size)
        assertTrue(queries.all { it.isNotBlank() })
    }

    @Test
    fun `custom pool replaces the default`() {
        FeedQueryFeeder.applyCustomQueries(listOf("only-one"))
        assertEquals(listOf("only-one", "only-one"), FeedQueryFeeder.nextQueries(2))
    }
}
