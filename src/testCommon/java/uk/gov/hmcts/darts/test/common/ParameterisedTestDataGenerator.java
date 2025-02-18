package uk.gov.hmcts.darts.test.common;

import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class ParameterisedTestDataGenerator {

    private ParameterisedTestDataGenerator() {
    }

    private static void generateCombinations(List<List<String>> params, int index, String[] current, List<Arguments> argumentList) {
        if (index == params.size()) {
            argumentList.add(Arguments.of((Object[]) current.clone()));
            return;
        }
        for (String value : params.get(index)) {
            current[index] = value;
            generateCombinations(params, index + 1, current, argumentList);
        }
    }

    public static Stream<Arguments> generateCombinationsExcludingAllNull(List<List<String>> params) {
        List<Arguments> argumentList = new ArrayList<>();
        generateCombinationsExcludingAllNull(params, 0, new String[params.size()], argumentList);
        return argumentList.stream();
    }

    private static void generateCombinationsExcludingAllNull(List<List<String>> params, int index, String[] current, List<Arguments> argumentList) {
        if (index == params.size()) {
            boolean allNull = true;
            for (String s : current) {
                if (s != null) {
                    allNull = false;
                    break;
                }
            }
            if (!allNull) {
                argumentList.add(Arguments.of((Object[]) current.clone()));
            }
            return;
        }
        for (String value : params.get(index)) {
            current[index] = value;
            generateCombinationsExcludingAllNull(params, index + 1, current, argumentList);
        }
    }
}
