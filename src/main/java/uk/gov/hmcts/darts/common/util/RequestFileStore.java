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
        for (String file : get()) {
            try {
                if (Path.of(file).toFile().exists()) {
                    Files.delete(Path.of(file));
                }
            } catch (IOException e) {
                log.error("Could not clean up file", e);
            }
        }

        super.remove();
    }

    public File create(Path filePath) throws IOException {
        Path file = Files.createFile(filePath);

        store(file.toFile());

        return file.toFile();
    }

    public File create(String directory) throws IOException {
        Files.createDirectories(Path.of(directory));
        File file = Files.createFile(Path.of(
            directory,
            UUID.randomUUID().toString())).toFile();

        store(file);

        return file;
    }

    private void store(File file) {
        List<String> files = get();
        if (files == null) {
            ArrayList<String> list = new ArrayList<>();
            list.add(file.getAbsolutePath());
            set(list);
        } else {
            files.add(file.getAbsolutePath());
        }
    }
}