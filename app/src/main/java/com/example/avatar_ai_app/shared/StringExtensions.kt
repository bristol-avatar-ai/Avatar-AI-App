package com.example.avatar_ai_app.shared

import com.example.avatar_ai_app.shared.StringExtensionsConstants.levenshteinDistance
import com.example.avatar_ai_app.shared.StringExtensionsConstants.phraseParserPattern
import com.example.avatar_ai_app.shared.StringExtensionsConstants.wordSeparatorRegex
import org.apache.commons.text.similarity.LevenshteinDistance

private object StringExtensionsConstants {
    const val phraseParserPattern = "(^|\\s)%s(\\s|\$)"

    // LevenshteinDistance instance for fuzzy comparison.
    val levenshteinDistance = LevenshteinDistance()

    // Regex pattern for splitting a string into words.
    val wordSeparatorRegex = Regex("\\s+")
}

/**
 * Extension function that checks if the string contains the specified phrase.
 *
 * Uses a case-insensitive regular expression.
 *
 * @param phrase The phrase to search for.
 * @return `true` if the string contains the phrase, `false` otherwise.
 */
fun String.containsPhrase(phrase: String): Boolean {
    val regex = Regex(phraseParserPattern.format(phrase), RegexOption.IGNORE_CASE)
    return this.contains(regex)
}

/**
 * Checks if the current string contains the given phrase with a fuzzy matching approach.
 *
 * Applies the provided minimum Levenshtein distance ratio to individual word matches.
 *
 * @param phrase The phrase to search for.
 * @param minLevenshteinRatio The minimum acceptable Levenshtein distance ratio for a word match.
 * @return True if a fuzzy match is found,¬ false otherwise.
 */
fun String.containsPhraseFuzzy(phrase: String, minLevenshteinRatio: Double): Boolean {
    val messageWords = this.split(wordSeparatorRegex)
    val phraseWords = phrase.split(wordSeparatorRegex)
    val phraseWordsSize = phraseWords.size
    var phraseWordIndex = 0

    messageWords.forEach {
        if (phraseWordIndex >= phraseWordsSize) {
            return true
        } else if (it.calculateLevenshteinRatio(phraseWords[phraseWordIndex]) >= minLevenshteinRatio) {
            phraseWordIndex++
        } else if (phraseWordIndex != 0) {
            return false
        }
    }
    return phraseWordIndex == phraseWordsSize
}

/**
 * Calculates the Levenshtein distance ratio between the current string and a target string.
 *
 * @param string The target string for which the Levenshtein distance ratio is calculated.
 * @return The Levenshtein distance ratio between the two strings.
 */
fun String.calculateLevenshteinRatio(string: String): Double {
    val distance = levenshteinDistance.apply(this.lowercase(), string.lowercase())
    val maxLength = maxOf(this.length, string.length)
    return 1 - distance.toDouble() / maxLength
}