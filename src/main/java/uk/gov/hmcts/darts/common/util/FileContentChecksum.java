package uk.gov.hmcts.darts.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    public String calculate(DigestInputStream digestInputStream) throws IOException {
        return encodeToString(digestInputStream.getMessageDigest().digest());
    }

    private String encodeToString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }

}
