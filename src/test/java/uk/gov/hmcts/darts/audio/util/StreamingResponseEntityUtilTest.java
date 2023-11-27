package uk.gov.hmcts.darts.audio.util;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.contract.spec.internal.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamingResponseEntityUtilTest {

    @Test
    public void blankRangeList() throws IOException {
        String httpRangeList = "";
        File file = new File("src/test/resources/Tests/audio/testAudio.mp2");
        InputStream inputStream = new FileInputStream(file);
        ResponseEntity<byte[]> response = StreamingResponseEntityUtil.createResponseEntity(inputStream, httpRangeList, "testFileName");

        assertEquals(HttpStatus.OK, response.getStatusCode().value());
        String contentLength = response.getHeaders().get("Content-Length").get(0);
        assertEquals("3248752", contentLength);
    }

    @Test
    public void startRangeList() throws IOException {
        String httpRangeList = "bytes=10000";
        File file = new File("src/test/resources/Tests/audio/testAudio.mp2");
        InputStream inputStream = new FileInputStream(file);
        ResponseEntity<byte[]> response = StreamingResponseEntityUtil.createResponseEntity(inputStream, httpRangeList, "testFileName");

        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode().value());

        String contentLength = response.getHeaders().get("Content-Length").get(0);

        assertEquals("3238752", contentLength);
        assertEquals("bytes 10000-3248751/3248752", response.getHeaders().get("Content-Range").get(0));
    }

    @Test
    public void startAndEndRangeList() throws IOException {
        String httpRangeList = "bytes=1000-2500";
        File file = new File("src/test/resources/Tests/audio/testAudio.mp2");
        InputStream inputStream = new FileInputStream(file);
        ResponseEntity<byte[]> response = StreamingResponseEntityUtil.createResponseEntity(inputStream, httpRangeList, "testFileName");

        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode().value());

        String contentLength = response.getHeaders().get("Content-Length").get(0);

        assertEquals("1501", contentLength);
        assertEquals("bytes 1000-2500/3248752", response.getHeaders().get("Content-Range").get(0));
    }
}
