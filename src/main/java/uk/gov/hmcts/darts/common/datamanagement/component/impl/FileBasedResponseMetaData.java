package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileBasedResponseMetaData extends ResponseMetaData{
    private File f;

    public FileBasedResponseMetaData(File f) throws IOException {
        super(Files.newOutputStream(Path.of(f.toURI()), StandardOpenOption.CREATE));
        this.f = f;

    }

    public InputStream getInputStream()  throws IOException {
        InputStream stream = Files.newInputStream(Path.of(f.toURI()), StandardOpenOption.READ);
        return stream;
    }
}