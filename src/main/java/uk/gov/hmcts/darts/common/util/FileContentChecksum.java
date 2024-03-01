package uk.gov.hmcts.darts.common.util;

import org.springframework.stereotype.Component;

import java.security.DigestInputStream;

import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.codec.digest.DigestUtils.md5;

/**
 * Calculates the file checksum equivalent to the Azure Blob Storage CONTENT-MD5 tag value as a base64 encoded
 * representation of the binary MD5 hash value. This could be done in Bash e.g. openssl md5 -binary "Test
 * Document.doc" | base64
 */
@Component
@SuppressWarnings({"java:S4790", "checkstyle:SummaryJavadoc"})
public class FileContentChecksum {

    /**
     * @deprecated This implementation is not memory-efficient with large files, use calculate(DigestInputStream digestInputStream) instead.
     */
    @Deprecated
    public String calculate(byte[] bytes) {
        return encodeToString(md5(bytes));
    }

    public String calculate(DigestInputStream digestInputStream) {
        return encodeToString(digestInputStream.getMessageDigest().digest());
    }

    private String encodeToString(byte[] bytes) {
        return new String(encodeBase64(bytes));
    }

}
