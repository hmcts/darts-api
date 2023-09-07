package uk.gov.hmcts.darts.common.service;

import com.azure.core.util.BinaryData;

import java.io.IOException;
import java.nio.file.Path;

public interface FileOperationService {

    Path saveFileToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException;

    BinaryData saveFileToBinaryData(String fileName);
}
