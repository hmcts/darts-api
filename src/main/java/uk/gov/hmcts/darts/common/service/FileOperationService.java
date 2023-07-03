package uk.gov.hmcts.darts.common.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public interface FileOperationService {

    Path saveFileToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException;

}
