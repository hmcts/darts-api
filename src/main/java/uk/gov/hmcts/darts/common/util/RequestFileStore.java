package uk.gov.hmcts.darts.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * A thread local class that holds file references for the thread. Works in combination with the CleanupResourceHandlerInterceptor
 * to protect against files hanging about
 */
@Slf4j
@SuppressWarnings("java:S6548")
public class RequestFileStore extends ThreadLocal<List<String>> {

    private static RequestFileStore fileCreatedForThread;

    public static RequestFileStore getFileStore() {
        if (fileCreatedForThread == null) {
            fileCreatedForThread = new RequestFileStore();
        }
        return fileCreatedForThread;
    }

    @Override
    public void remove() {
        if (get() != null) {
            for (String file : get()) {
                try {
                    if (Path.of(file).toFile().exists()) {
                        Files.delete(Path.of(file));
                    }
                } catch (IOException e) {
                    log.error("Could not clean up file %s. Please manually delete it".formatted(file), e);
                }
            }
        }

        super.remove();
    }

    public File create(Path filePath) throws IOException {
        Path file = Files.createFile(filePath);

        store(file.toFile().getAbsolutePath());

        return file.toFile();
    }

    public File create(Path directory, Path filePath) throws IOException {
        Files.createDirectories(directory);

        Path file = Files.createFile(directory.resolve(filePath));

        store(file.toFile().getAbsolutePath());

        return file.toFile();
    }


    public File createTempFile(Path directory) throws IOException {
        Files.createDirectories(directory);
        File file = Files.createFile(
            directory.resolve(UUID.randomUUID().toString())).toFile();
        store(file.getAbsolutePath());

        return file;
    }

    public void store(String fileStr) {
        List<String> files = get();
        if (files == null) {
            ArrayList<String> list = new ArrayList<>();
            list.add(fileStr);
            set(list);
        } else {
            if (!files.contains(fileStr)) {
                files.add(fileStr);
            }
        }
    }
}