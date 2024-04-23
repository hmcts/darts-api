package uk.gov.hmcts.darts.testutils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/**
 * An in memory multipart file. To be used as part of integration tests.
 */
@Slf4j
@Getter
public class InMemoryMultipart implements MultipartFile {
    private final String name;

    private final String contentType;

    private final byte[] contents;

    public InMemoryMultipart(
        String name, @Nullable String contentType, byte[] content) {
        this.name = name;
        this.contentType = contentType;
        this.contents = Arrays.copyOf(content, content.length);
    }

    public static InMemoryMultipart getMultiPartOfRandomisedLengthKb(String name, @Nullable String contentType, int kbLength) {
        byte[] fileBytesOverThreshold = new byte[kbLength * 1000];
        new Random().nextBytes(fileBytesOverThreshold);
        return new InMemoryMultipart(name, contentType, fileBytesOverThreshold);
    }

    @Override
    @NonNull
    public String getOriginalFilename() {
        return getName();
    }

    @Override
    public boolean isEmpty() {
        return getSize() == 0;
    }

    @Override
    public long getSize() {
        return contents.length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return IOUtils.toInputStream(Arrays.toString(contents), Charset.defaultCharset());
    }

    @Override
    public void transferTo(File dest) throws IOException {
        IOUtils.readFully(Files.newInputStream(Path.of(dest.getAbsolutePath())), contents);
    }

    @Override
    public byte @NotNull [] getBytes() throws IOException {
        return Arrays.copyOf(contents, contents.length);
    }
}