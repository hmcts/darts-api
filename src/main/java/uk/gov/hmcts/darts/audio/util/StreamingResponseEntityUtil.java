package uk.gov.hmcts.darts.audio.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;

@UtilityClass
public class StreamingResponseEntityUtil {

    public ResponseEntity<byte[]> createResponseEntity(InputStream inputStream, String httpRangeList) throws IOException {
        byte[] bytes = IOUtils.toByteArray(inputStream);
        long fileSize = bytes.length;
        if (StringUtils.isNotBlank(httpRangeList)) {
            httpRangeList = StringUtils.trim(httpRangeList);
            String rangeListValue = StringUtils.substringAfter(httpRangeList, "=");
            String[] ranges = rangeListValue.split("-");
            long rangeStart = Long.parseLong(ranges[0]);
            long rangeEnd = getRangeEnd(fileSize, ranges);
            long requestedContentLength = (rangeEnd - rangeStart) + 1;
            String contentLengthStr = String.valueOf(requestedContentLength);
            String contentRange = "bytes " + rangeStart + "-" + rangeEnd + "/" + fileSize;
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header("Content-Type", "audio/mpeg")
                .header("Content-Length", contentLengthStr)
                .header("Content-Range", contentRange)
                .body(readByteRange(bytes, rangeStart, rangeEnd));
        }
        long rangeEnd = fileSize - 1;
        String contentRange = "bytes 0-" + rangeEnd + "/" + fileSize;
        return ResponseEntity.status(HttpStatus.OK)
            .header("Content-Type", "audio/mpeg")
            .header("Content-Length", String.valueOf(bytes.length))
            .header("Content-Range", contentRange)
            .body(bytes);
    }

    private static long getRangeEnd(long fileSize, String[] ranges) {
        long rangeEnd;
        if (ranges.length > 1) {
            rangeEnd = Long.parseLong(ranges[1]);
        } else {
            rangeEnd = fileSize - 1;
        }
        if (fileSize < rangeEnd) {
            rangeEnd = fileSize - 1;
        }
        return rangeEnd;
    }


    public byte[] readByteRange(byte[] wholeFile, long start, long end) {
        int srcPos;
        if (start > Integer.MIN_VALUE && start < Integer.MAX_VALUE) {
            srcPos = (int) start;
        } else {
            throw new IllegalArgumentException("Invalid input: start bytes truncated");
        }

        byte[] result = new byte[(int) (end - start) + 1];
        System.arraycopy(wholeFile, srcPos, result, 0, result.length);
        return result;
    }
}
