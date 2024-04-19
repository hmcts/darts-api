package uk.gov.hmcts.darts.common.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.DigestInputStream;

import static org.apache.commons.codec.digest.DigestUtils.md5;

/**
 * Calculates the file checksum equivalent to "md5 filename" on the command line.
 * <p>
 * This is not the same as the Azure Blob Storage CONTENT-MD5 tag value as a base64 encoded
 * representation of the binary MD5 hash value. This could be done in Bash e.g. openssl md5 -binary "Test
 * Document.doc" | base64
 * </p>
 */
@Component
@SuppressWarnings({"java:S4790", "checkstyle:SummaryJavadoc"})
@Slf4j
public class FileContentChecksum {

    /**
     * @deprecated This implementation is not memory-efficient with large files, use calculate(DigestInputStream digestInputStream) instead.
     */
    @Deprecated
    public String calculate(byte[] bytes) {
        return encodeToString(md5(bytes));
    }

    /**
     * Please note: for the checksum to be computed, consumption of the source data must already have happened.
     * Otherwise use consumeAndCalculate(DigestInputStream digestInputStream)
     */
    public String calculateFromConsumedSource(DigestInputStream digestInputStream) throws IOException {
        return encodeToString(digestInputStream.getMessageDigest().digest());
    }

    @SneakyThrows
    public String consumeSourceAndCalculate(InputStream inputStream) {
        try (var digestInputStream = new DigestInputStream(new BufferedInputStream(inputStream), DigestUtils.getMd5Digest());
             var out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int readLength = -1;
            while ((readLength = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, readLength);
            }

//            digestInputStream.transferTo(out);
            return encodeToString(digestInputStream.getMessageDigest().digest());
        }
    }

    private String encodeToString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }

}
