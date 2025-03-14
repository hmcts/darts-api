package uk.gov.hmcts.darts.test.common;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A thread local class that holds file references for the thread and allows them to be cleaned up.
 */
@Slf4j
public class FileStore extends ThreadLocal<List<String>> {

    private static FileStore fileCreatedForThread = new FileStore();

    private FileStore() {
    }

    public static FileStore getFileStore() {
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

    private void store(String fileStr) {
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