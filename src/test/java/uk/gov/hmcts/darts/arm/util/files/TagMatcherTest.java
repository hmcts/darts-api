package uk.gov.hmcts.darts.arm.util.files;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
class TagMatcherTest {
    @Test
    void givenGetXmlTagValuesFromLine() {
        String line = "<tag>one</tag>";
        final String tagToSearchFor = "tag";
        List<String> results = TagMatcher.getXmlTagValuesFromLine(line, tagToSearchFor);
        log.info("found: {}", results);
    }

    @Test
    void givenGetJsonTagValuesFromLine() {
        String line = "\"relation_id\": \"152820\",";
        final String tagToSearchFor = "exception_description";
        List<String> results = TagMatcher.getJsonTagValuesFromLine(line, tagToSearchFor);
        log.info("found: {}", results);
    }
}
