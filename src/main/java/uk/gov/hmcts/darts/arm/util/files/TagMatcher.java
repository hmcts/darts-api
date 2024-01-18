package uk.gov.hmcts.darts.arm.util.files;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagMatcher {

    private TagMatcher() {
    }

    private static final String XML_PATTERN = "<%s>(.+?)</%s>";
    private static final String JSON_PATTERN = "\"%s\":([^\\n]*)";

    public static List<String> getXmlTagValuesFromLine(final String line, final String tagToSearchFor) {
        final List<String> tagValues = new ArrayList<>();
        String tagPatternStr = String.format(XML_PATTERN, tagToSearchFor, tagToSearchFor);
        final Pattern xmlTagPattern = Pattern.compile(tagPatternStr, Pattern.DOTALL);
        final Matcher matcher = xmlTagPattern.matcher(line);

        while (matcher.find()) {
            tagValues.add(matcher.group(1));
        }
        return tagValues;
    }

    public static List<String> getJsonTagValuesFromLine(final String line, final String tagToSearchFor) {
        final List<String> tagValues = new ArrayList<>();
        String tagPatternStr = String.format(JSON_PATTERN, tagToSearchFor);
        final Pattern jsonTagPattern = Pattern.compile(tagPatternStr, Pattern.DOTALL);
        final Matcher matcher = jsonTagPattern.matcher(line);

        while (matcher.find()) {
            tagValues.add(matcher.group(1));
        }
        return tagValues;
    }

}
