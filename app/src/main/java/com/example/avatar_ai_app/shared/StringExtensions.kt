package com.example.avatar_ai_app.shared

import android.util.Log
import com.example.avatar_ai_app.shared.StringExtensionsConstants.levenshteinDistance
import com.example.avatar_ai_app.shared.StringExtensionsConstants.wordSeparatorRegex
import org.apache.commons.text.similarity.LevenshteinDistance

private const val TAG = "StringExtensions"

private object StringExtensionsConstants {
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
    val regex = Regex("(^|\\s)$phrase(\\s|\$)", RegexOption.IGNORE_CASE)
    return this.contains(regex)
}

/**
 * Checks if a given phrase is approximately matched within the string.
 *
 * Compares its similarity with combinations of words from the string using the Levenshtein ratio.
 * The combinations contain the same number of words as the phrase.
 *
 * @param phrase The phrase to search for.
 * @param minLevenshteinRatio The minimum Levenshtein similarity ratio for a match to occur.
 * @return `true` if a similar phrase is found, `false` otherwise.
 */
fun String.containsPhraseFuzzy(phrase: String, minLevenshteinRatio: Double): Boolean {
    val phraseLowerCase = phrase.lowercase()
    // Count the number of words in the target phrase.
    val wordsInPhrase =
        phraseLowerCase.splitToSequence(wordSeparatorRegex).count()

    this.lowercase().generateCombinations(wordsInPhrase).forEach {
        val levenshteinRatio = calculateLevenshteinRatio(it, phraseLowerCase)

        if (levenshteinRatio >= minLevenshteinRatio) {
            Log.i(TAG, "containsPhraseFuzzy: $it - $phraseLowerCase ($levenshteinRatio)")
            return true
        }
    }
    return false
}

/*
* Generates combinations of words from the string based on the provided
* combination size. For example, if combination size is three, it
* generates sets of three consecutive words.
 */
private fun String.generateCombinations(combinationSize: Int): List<String> {
    val wordList = this.split(wordSeparatorRegex)
    val combinationsList = mutableListOf<String>()

    for (i in 0..wordList.size - combinationSize) {
        combinationsList.add(
            wordList.subList(i, i + combinationSize).joinToString(" ")
        )
    }
    return combinationsList
}

/*
* Calculates the Levenshtein similarity ratio between two strings.
* The Levenshtein distance is normalized by the maximum length of the strings.
 */
private fun calculateLevenshteinRatio(string1: String, string2: String): Double {
    val distance = levenshteinDistance.apply(string1, string2)
    val maxLength = maxOf(string1.length, string2.length)

    return 1.0 - distance.toDouble() / maxLength
}