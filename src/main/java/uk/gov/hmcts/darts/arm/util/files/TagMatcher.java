package uk.gov.hmcts.darts.arm.util.files;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagMatcher {

    private static final String xmlPattern = "<%s>(.+?)</%s>";
    private static final String jsonPattern = "\"%s\":([^\\n]*)";

    public static List<String> getXmlTagValuesFromLine(final String line, final String tagToSearchFor) {
        final List<String> tagValues = new ArrayList<>();
        String tagPatternStr = String.format(xmlPattern, tagToSearchFor, tagToSearchFor);
        final Pattern xmlTagPattern = Pattern.compile(tagPatternStr, Pattern.DOTALL);
        final Matcher matcher = xmlTagPattern.matcher(line);

        while (matcher.find()) {
            tagValues.add(matcher.group(1));
        }
        return tagValues;
    }

    public static List<String> getJsonTagValuesFromLine(final String line, final String tagToSearchFor) {
        final List<String> tagValues = new ArrayList<>();
        String tagPatternStr = String.format(jsonPattern, tagToSearchFor);
        final Pattern jsonTagPattern = Pattern.compile(tagPatternStr, Pattern.DOTALL);
        final Matcher matcher = jsonTagPattern.matcher(line);

        while (matcher.find()) {
            tagValues.add(matcher.group(1));
        }
        return tagValues;
    }
}
