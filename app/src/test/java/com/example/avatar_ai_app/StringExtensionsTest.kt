package com.example.avatar_ai_app

import com.example.avatar_ai_app.shared.containsPhrase
import com.example.avatar_ai_app.shared.containsPhraseFuzzy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN_LEVENSHTEIN_RATIO = 0.75

class StringExtensionsTest {

    private fun testShouldNotContain(message: String, phrase: String) {
        val errorMessage = "'$message' should not contain '$phrase'"
        assertFalse(errorMessage, message.containsPhrase(phrase))
    }

    private fun testShouldContain(message: String, phrase: String) {
        val errorMessage = "'$message' should contain '$phrase'"
        assertTrue(errorMessage, message.containsPhrase(phrase))
    }

    @Test
    fun testContainsPhrase() {
        testShouldContain("Hello there!", "")
        testShouldNotContain("", "bat")
        testShouldContain("", "")

        testShouldNotContain("Where is the painting?", "broom")
        testShouldNotContain("Where is the bat", "cat")
        testShouldNotContain("Where is thecat in the room?", "cat")

        testShouldContain("Where is the painting!", "painting")
        testShouldContain("Where is the painting", "painting")
        testShouldContain("Where is the painting", "PainTing")
        testShouldContain("Where is the painting in here?", "PainTing")
        testShouldContain("Painting is where?", "painting")
        testShouldContain("Is there a cat in the room?", "Cat")
        testShouldContain("Is there a cAt, in the room?", "Cat")
        testShouldContain("Is there a -cAt' in the room?", "Cat")

        testShouldContain("Where is the painting", "where is")
        testShouldContain("Where is?", "where is")
        testShouldNotContain("Where cat is the painting", "where is")
    }

    private fun testShouldNotContainFuzzy(message: String, phrase: String) {
        val errorMessage = "'$message' should not contain '$phrase' (fuzzy)"
        assertFalse(errorMessage, message.containsPhraseFuzzy(phrase, MIN_LEVENSHTEIN_RATIO))
    }

    private fun testShouldContainFuzzy(message: String, phrase: String) {
        val errorMessage = "'$message' should contain '$phrase' (fuzzy)"
        assertTrue(errorMessage, message.containsPhraseFuzzy(phrase, MIN_LEVENSHTEIN_RATIO))
    }

    @Test
    fun testContainsPhraseFuzzy() {
        testShouldContainFuzzy("Hello there!", "")
        testShouldNotContainFuzzy("", "bat")
        testShouldContainFuzzy("", "")

        testShouldNotContainFuzzy("Where is the painting?", "pot")
        testShouldNotContainFuzzy("Where is thecat in the room?", "cat")

        testShouldContainFuzzy("Where is the painting?", "painting")
        testShouldContainFuzzy("Where is the painting", "painting")
        testShouldContainFuzzy("Where is the painting", "PainTing")
        testShouldContainFuzzy("Where is the painting in here?", "PainTing")
        testShouldContainFuzzy("Painting is where?", "painting")
        testShouldContainFuzzy("Is there a cat in the room?", "Cat")
        testShouldContainFuzzy("Is there a cAt, in the room?", "Cat")
        testShouldContainFuzzy("Is there a -cAt' in the room?", "Cat")

        // Fuzzy Matches
        testShouldNotContainFuzzy("Where is the bat?", "cat")
        testShouldContainFuzzy("Where is the sign?", "sigh")
        testShouldContainFuzzy("Where are the painting?", "paintings")

        testShouldContainFuzzy("Where is the painting", "where is")
        testShouldContainFuzzy("Where is?", "where is")
        testShouldNotContainFuzzy("Where cat is the painting", "where is")
    }
}