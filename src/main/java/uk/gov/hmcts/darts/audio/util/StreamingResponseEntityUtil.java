package uk.gov.hmcts.darts.audio.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.darts.audio.model.PreviewRange;

import java.io.IOException;
import java.io.InputStream;

@UtilityClass
public class StreamingResponseEntityUtil {


    public ResponseEntity<byte[]> createResponseEntity(InputStream inputStream, PreviewRange previewRange) throws IOException {
        byte[] bytes = IOUtils.toByteArray(inputStream);
        long fileSize = AudioFileSizeUtil.mp2ToMp3FileSize(previewRange.getContentLength());
        long rangeStart = previewRange.getStartRange();
        long rangeEnd = getRangeEnd(fileSize, previewRange.getEndRange());
        long requestedContentLength = (rangeEnd - rangeStart) + 1;
        //String contentLengthStr = String.valueOf(previewRange.getContentLength());
        String contentLengthStr = String.valueOf(bytes.length);
        String contentRange = "bytes " + rangeStart + "-" + rangeEnd + "/" + fileSize;
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .header("Content-Type", "audio/mpeg")
            //.header("Content-Length", contentLengthStr)
            .header("Content-Range", contentRange)
            .body(bytes);

    }

    private static long getRangeEnd(long fileSize, long requestedEnd) {
        long rangeEnd = requestedEnd;
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
