package uk.gov.hmcts.darts.common.service;

import com.azure.core.util.BinaryData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileOperationService {

    Path saveFileToTempWorkspace(InputStream mediaFile, String fileName) throws IOException;

    Path saveBinaryDataToSpecifiedWorkspace(BinaryData binaryData, String fileName, String workspace, boolean appendUuidToWorkspace) throws IOException;

    BinaryData convertFileToBinaryData(String fileName);
}
