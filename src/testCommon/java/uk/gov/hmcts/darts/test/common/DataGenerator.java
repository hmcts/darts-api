package uk.gov.hmcts.darts.test.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.util.unit.DataSize;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class produces data that presents itself as the desired fileType. The generated data will include the appropriate file signature
 * so that it will be accepted by filetype fingerprinting validation.
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public final class DataGenerator {

    private DataGenerator() {
    }

    /**
     * Generates a path to a file in the temporary filesystem of the desired size and type. Each invocation of this method will
     * produce a file with randomised content.
     *
     * @param dataSize the desired size of the data, including the necessary file signature
     * @param fileType the desired file type
     * @return a Path to the (temporary) file
     * @throws IllegalArgumentException if the provided size is less than the size of the file signature of the provided fileType.
     */
    public static Path createUniqueFile(DataSize dataSize, FileType fileType) {
        byte[] data = createUniqueData(dataSize, fileType);

        File tempFile;
        try {
            tempFile = File.createTempFile("int-test-temp-", fileType.getExtension());
            try (var outputstream = Files.newOutputStream(tempFile.toPath())) {
                outputstream.write(data);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return tempFile.toPath();
    }

    /**
     * Generates a byte array of the desired size and type. Each invocation of this method will
     * produce a byte array with randomised content.
     *
     * @param dataSize the desired size of the data, including the necessary file signature
     * @param fileType the desired file type
     * @return a byte[] representing valid file content
     * @throws IllegalArgumentException if the provided size is less than the size of the file signature of the provided fileType.
     */
    public static byte[] createUniqueData(DataSize dataSize, FileType fileType) {
        byte[] signatureBytes;
        try {
            signatureBytes = Hex.decodeHex(fileType.getHexSignature());
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
        final int signatureLength = signatureBytes.length;

        int contentSize = (int) dataSize.toBytes() - signatureLength;
        if (contentSize < 0) {
            throw new IllegalArgumentException(String.format("The provided dataSize must be at least equal to the length of the file signature (%d bytes)",
                                                             signatureLength));
        }

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeBytes(signatureBytes);

        byte[] randomisedBytes = RandomUtils.nextBytes(contentSize);
        byteArrayOutputStream.writeBytes(randomisedBytes);

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * An enum that maps file type to hex signature and extension.
     *
     * <p>For a common list of signatures see <a href="https://en.wikipedia.org/wiki/List_of_file_signatures">...</a>.
     */
    @Getter
    @RequiredArgsConstructor
    public enum FileType {
        MP2("FFFC", ".mp2"), // Ref https://www.nationalarchives.gov.uk/PRONOM/Format/proFormatSearch.aspx?status=detailReport&id=923&strPageToDisplay=signatures
        MP3("FFFB", ".mp3");

        private final String hexSignature;
        private final String extension;
    }

}
