package uk.gov.hmcts.darts.audio.util;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.contract.spec.internal.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StreamingResponseEntityUtilTest {

    @Test
    void blankRangeList() throws IOException {
        String httpRangeList = "";

        try (InputStream inputStream = Files.newInputStream(Paths.get("src/test/resources/Tests/audio/testAudio.mp2"))) {
            ResponseEntity<byte[]> response = StreamingResponseEntityUtil.createResponseEntity(inputStream, httpRangeList, "testFileName");

            assertEquals(HttpStatus.OK, response.getStatusCode().value());
            String contentLength = response.getHeaders().get("Content-Length").get(0);
            assertEquals("3248752", contentLength);
        }
    }

    @Test
    void startRangeList() throws IOException {
        String httpRangeList = "bytes=10000";

        try (InputStream inputStream = Files.newInputStream(Paths.get("src/test/resources/Tests/audio/testAudio.mp2"))) {
            ResponseEntity<byte[]> response = StreamingResponseEntityUtil.createResponseEntity(inputStream, httpRangeList, "testFileName");

            assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode().value());
            String contentLength = response.getHeaders().get("Content-Length").get(0);
            assertEquals("3238752", contentLength);
            assertEquals("bytes 10000-3248751/3248752", response.getHeaders().get("Content-Range").get(0));
        }
    }

    @Test
    void startAndEndRangeList() throws IOException {
        String httpRangeList = "bytes=1000-2500";

        try (InputStream inputStream = Files.newInputStream(Paths.get("src/test/resources/Tests/audio/testAudio.mp2"))) {
            ResponseEntity<byte[]> response = StreamingResponseEntityUtil.createResponseEntity(inputStream, httpRangeList, "testFileName");

            assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode().value());

            String contentLength = response.getHeaders().get("Content-Length").get(0);

            assertEquals("1501", contentLength);
            assertEquals("bytes 1000-2500/3248752", response.getHeaders().get("Content-Range").get(0));
        }

    }

    @Test
    void startAndEndRangeListSameAsFile() throws IOException {
        String httpRangeList = "bytes=0-3248751";

        try (InputStream inputStream = Files.newInputStream(Paths.get("src/test/resources/Tests/audio/testAudio.mp2"))) {
            ResponseEntity<byte[]> response = StreamingResponseEntityUtil.createResponseEntity(inputStream, httpRangeList, "testFileName");

            assertEquals(HttpStatus.OK, response.getStatusCode().value());

            assertEquals("3248752", response.getHeaders().get("Content-Length").get(0));
            assertNull(response.getHeaders().get("Content-Range"));
        }

    }
}
