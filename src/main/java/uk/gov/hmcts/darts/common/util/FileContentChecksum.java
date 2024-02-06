package uk.gov.hmcts.darts.common.util;

import org.springframework.stereotype.Component;

import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.codec.digest.DigestUtils.md5;

@Component
@SuppressWarnings({"java:S4790"})
public class FileContentChecksum {

    /**
     * Calculates the file checksum equivalent to the Azure Blob Storage CONTENT-MD5 tag value as a base64 encoded representation of the binary MD5 hash value.
     * This could be done in Bash e.g. openssl md5 -binary "Test Document.doc" | base64
     *
     * @param bytes the file content
     * @return the calculated checksum
     */
    public String calculate(byte[] bytes) {
        return new String(encodeBase64(md5(bytes)));
    }

}
