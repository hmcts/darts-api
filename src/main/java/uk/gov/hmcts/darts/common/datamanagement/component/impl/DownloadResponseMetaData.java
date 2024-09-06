package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A general download response that can be extended. Always use in combination with a try resources to ensure the file resources are cleaned up
 */
public abstract class DownloadResponseMetaData implements Closeable {
    protected InputStream inputStream;

    protected OutputStream outputStream;


    public abstract Resource getResource()  throws IOException;

    @Getter
    @Setter
    private ExternalObjectDirectoryEntity eodEntity;

    @Getter
    @Setter
    private DatastoreContainerType containerTypeUsedToDownload;

    public abstract OutputStream getOutputStream(StorageConfiguration configuration) throws IOException;

    public void markInputStream(InputStream is) {
        this.inputStream = is;
    }

    @Override
    public void close() throws IOException {

        if (inputStream != null) {
            inputStream.close();
        }

        if (outputStream != null) {
            outputStream.close();
        }
    }

}