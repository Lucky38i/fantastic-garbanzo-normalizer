package org.amcbean.library.util;

import lombok.val;
import org.amcbean.library.exception.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NormaliserTest {

    private final Normaliser normaliser = new Normaliser(null);

    private static List<Arguments> happyTitles() {
        return List.of(
                Arguments.of("Software Engineer", "software engineer"),
                Arguments.of("Software-Engineer", "software engineer"),
                Arguments.of("Software Engineer!", "software engineer"),
                Arguments.of("Software Engineer  ", "software engineer"),
                Arguments.of("  Software Engineer", "software engineer"),
                Arguments.of("Software   Engineer", "software engineer"),
                Arguments.of("Software Developer", "software engineer"),
                Arguments.of("SOFTWARE ENGINEER", "software engineer"),
                Arguments.of("senior software engineer", "software engineer"),
                Arguments.of("Java engineer", "software engineer"),
                Arguments.of("C# engineer", "software engineer"),
                Arguments.of("lead architect", "architect"),
                Arguments.of("junior architect", "architect"),
                Arguments.of("architect", "architect"),
                Arguments.of("Chief Accountant", "accountant"),
                Arguments.of("Lead Surveyor", "quantity surveyor"),
                Arguments.of("ground Surveyor", "quantity surveyor"),
                Arguments.of("quantity assessor", "quantity surveyor")
        );
    }

    private static List<Arguments> unhappyTitles() {
        return List.of(
                Arguments.of(null, "input is null"),
                Arguments.of("!!!", "input is empty after cleaning, was: !!!"),
                Arguments.of("", "input is empty"),
                Arguments.of("   ", "input is empty"),
                Arguments.of("Software Engineer 123", "input contains digits, was: Software Engineer 123"),
                Arguments.of("Tech Lead", "input does not match any job title with sufficient confidence, was: 'Tech Lead', best match: 'architect' (56%)"),
                Arguments.of("Developer", "input does not match any job title with sufficient confidence, was: 'Developer', best match: 'software engineer' (48%)"),
                Arguments.of("这是中文", "input is empty after cleaning, was: 这是中文"),
                Arguments.of("ground assessor", "input does not match any job title with sufficient confidence, was: 'ground assessor', best match: 'accountant' (58%)")
        );
    }

    private static List<Arguments> unhappyDefaultTitles() {
        return List.of(
                Arguments.of("1234", "input contains digits, was: 1234"),
                Arguments.of(" ", "input is empty"),
                Arguments.of("No. 1 Architect", "input contains digits, was: No. 1 Architect")
        );
    }

    @ParameterizedTest(name = "GIVEN title: {0} WHEN normalize THEN {1} returned")
    @MethodSource("happyTitles")
    void happyInputFlows(String title, String normalisedTitle) {
        assertThat(normaliser.normalize(title)).isEqualTo(normalisedTitle);
    }

    @ParameterizedTest(name = "Given title: {0} WHEN normalize called THEN exception thrown")
    @MethodSource("unhappyTitles")
    void unhappyInputFlows(String title, String expectedExceptionMessage) {
        val exception = assertThrows(InvalidInputException.class, () -> normaliser.normalize(title));
        assertThat(exception.getMessage()).isEqualTo(expectedExceptionMessage);
    }

    @ParameterizedTest(name = "Given default titles: {0} WHEN Normaliser constructor THEN exception thrown")
    @MethodSource("unhappyDefaultTitles")
    void unhappyTitleFlows(String title, String expectedExceptionMessage) {
        val inputList = List.of(title);
        val exception = assertThrows(InvalidInputException.class, () -> new Normaliser(inputList));
        assertThat(exception.getMessage()).isEqualTo(expectedExceptionMessage);
    }

    @Test()
    void happyDefaultTitleFlows() {
        assertDoesNotThrow(() -> new Normaliser(List.of("Software Engineer!!!")));
    }

    @Test()
    void unhappyEmptyDefaultTitleFlow() {
        val inputList = List.of("");
        val exception = assertThrows(InvalidInputException.class, () -> new Normaliser(inputList));
        assertThat(exception.getMessage()).isEqualTo("input is empty");
    }

}
