package org.amcbean.library.util;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.amcbean.library.exception.InvalidInputException;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@Slf4j
public class Normaliser {

    private final List<String> jobTitles;
    private static final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
    private static final double THRESHOLD = 0.85;
    private record Match(String title, double score) {}

    /**
     * The normalizer class used to normalize job titles using the JaroWinkler similarity algo.
     * Allows one to provide a custom list of job titles however, if none are provided then the ones from the assessment are used.
     * @param adjustedJobTitles The list of job titles to be used for normalization
     */
    public Normaliser(@Nullable List<String> adjustedJobTitles) {
        if (adjustedJobTitles == null || adjustedJobTitles.isEmpty() ) {
            log.warn("Empty job titles, using default");
             jobTitles = Stream.of(
                    "Architect",
                    "Software Engineer",
                    "Quantity Surveyor",
                    "Accountant"
            ).map(this::clean).toList();
        } else {
            adjustedJobTitles.forEach(this::validateInput);
            jobTitles = adjustedJobTitles.stream().map(this::clean)
                    .toList();
        }
    }

    /**
     * Validates the input ensuring no null, no digits and no empty inputs values
     * @param input The string to be validated
     */
    private void validateInput(String input) {
        if (input == null) {
            throw new InvalidInputException("input is null");
        }

        if (Pattern.compile("\\d").matcher(input).find()) {
            throw new InvalidInputException("input contains digits, was: " + input);
        }

        if (input.trim().isEmpty()) {
            throw new InvalidInputException("input is empty");
        }
    }

    /**
     * Once the string has been validated, this method cleans the string of any non-alphabetic characters,
     * lowercases everything and trims any white spaces
     * @param input The string to be cleaned
     * @return A cleaned string
     */
    private String clean(String input) {
        val cleanedInput = input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleanedInput.isBlank()) {
            throw new InvalidInputException("input is empty after cleaning, was: " + input);
        }
        return cleanedInput;
    }

    /**
     * Normalizes the input string by finding the best matching job title.
     * @param input The string to be normalized
     * @return The normalized job title
     */
    public String normalize(String input) {
        validateInput(input);
        val best = bestMatch(clean(input));
        if (best.score() < THRESHOLD) {
            throw new InvalidInputException(
                    "input does not match any job title with sufficient confidence, was: '%s', best match: '%s' (%.0f%%)"
                    .formatted(input, best.title(), best.score()*100)
            );
        }
        return best.title();
    }

    /**
     * This compares the input string to all job titles then returns the match with the highest score.
     * @param input The string to be compared
     * @return The best matching job title
     */
    private Match bestMatch(String input) {
        return jobTitles.stream()
                .map(title -> new Match(title, weighInput(input, title)))
                .max(Comparator.comparingDouble(Match::score))
                .orElseThrow();

    }

    /**
     * Weighs the entire string as-well as each word in the string and title then uses a 50/50 split to compute the final score.
     * @param input In job title to be normalized
     * @param title The title to be weighed against
     * @return A score between 0 and 1
     */
    private double weighInput(String input, String title) {
        if (input.equals(title)) return 1;
        return 0.5 * similarity.apply(input, title) + 0.5 * weighInputWords(input, title);
    }

    /**
     * The same as @weighInput but weighs each word as opposed to the entire string.
     * @param input In job title to be normalized
     * @param title The title to be weighed against
     * @return A score between 0 and 1
     */
    private double weighInputWords(String input, String title) {
        val inputWords = input.split(" ");
        val titleWords = title.split(" ");

        var total = 0.0;

        for (val titleWord: titleWords) {
            var best = 0.0;
            for (val inputWord: inputWords) best = Math.max(best, similarity.apply(inputWord, titleWord));
            total += best;
        }
        return total / titleWords.length;
    }

}
