package uk.gov.hmcts.darts.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DurationUtil Tests")
class DurationUtilTest {


    @DisplayName("String formatDurationHumanReadable(Duration duration)")
    @ParameterizedTest(name = "formatDurationHumanReadable({0}) should equal {1}")
    @MethodSource("humanReadableDurationProvider")
    void formatDurationHumanReadable_common(Duration duration, String expected) {
        String result = DurationUtil.formatDurationHumanReadable(duration);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> humanReadableDurationProvider() {
        Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(Duration.ofSeconds(1), "1 second"));
        builder.add(Arguments.of(Duration.ofSeconds(2), "2 seconds"));
        builder.add(Arguments.of(Duration.ofSeconds(60), "1 minute"));
        builder.add(Arguments.of(Duration.ofSeconds(61), "1 minute 1 second"));
        builder.add(Arguments.of(Duration.ofHours(24), "1 day"));
        builder.add(Arguments.of(Duration.ofHours(25), "1 day 1 hour"));
        return builder.build();
    }
}
