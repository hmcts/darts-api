package uk.gov.hmcts.darts.arm.helper;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.exception.UnableToReadArmFileException;
import uk.gov.hmcts.darts.arm.mapper.ArmResponseUploadFileMapper;
import uk.gov.hmcts.darts.arm.model.InputUploadAndAssociatedFilenames;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecordObject;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class ArmResponseFileHelper {

    public static final String DOT_A360 = ".a360";
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ArmDataManagementApi armDataManagementApi;
    private final ArmResponseUploadFileMapper uploadFileMapper;

    public List<InputUploadAndAssociatedFilenames> getCorrespondingArmFilesForManifestFilename(String manifestFileName) throws UnableToReadArmFileException {

        List<InputUploadAndAssociatedFilenames> responseList = new ArrayList<>();
        String manifestFilePrefix = armDataManagementConfiguration.getManifestFilePrefix();
        if (manifestFileName.startsWith(manifestFilePrefix) &&
            manifestFileName.endsWith(DOT_A360)) {
            //is a Batch response UI file.
            String armUuidValue = StringUtils.removeEnd(manifestFileName, DOT_A360);
            List<String> matchingInputUploadFiles = armDataManagementApi.listResponseBlobs(armUuidValue);
            for (String inputUploadFile : matchingInputUploadFiles) {
                InputUploadAndAssociatedFilenames inputUploadAndAssociatedFilenames = new InputUploadAndAssociatedFilenames();
                inputUploadAndAssociatedFilenames.setInputUploadFilename(inputUploadFile);

                List<String> relatedArmResponseFilenamesForIuFile = getRelatedArmResponseFilenamesForIuFile(inputUploadFile);
                for (String associatedFilename : relatedArmResponseFilenamesForIuFile) {
                    Integer eodIdFromArmFile = getEodIdFromArmFile(associatedFilename);
                    inputUploadAndAssociatedFilenames.addAssociatedFile(eodIdFromArmFile, associatedFilename);
                }
                responseList.add(inputUploadAndAssociatedFilenames);
            }
        } else {
            log.warn("Manifest filename format of ''{}'' not recognised", manifestFileName);
        }
        return responseList;
    }

    private List<String> getRelatedArmResponseFilenamesForIuFile(String inputUploadFilename) {
        BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor = new BatchInputUploadFileFilenameProcessor(inputUploadFilename);
        String iuHashcode = batchUploadFileFilenameProcessor.getHashcode();
        return armDataManagementApi.listResponseBlobs(iuHashcode);
    }

    public Integer getEodIdFromArmFile(String armFilename) throws UnableToReadArmFileException {
        BinaryData blobData = armDataManagementApi.getBlobData(armFilename);
        if (blobData == null) {
            log.error("Blob data is null for {}.", armFilename);
            throw new UnableToReadArmFileException(armFilename);
        }
        try {
            ArmResponseUploadFileRecordObject armResponseFile = uploadFileMapper.map(blobData.toString());
            String eodIdStr = armResponseFile.getInput().getRelationId();
            return Integer.parseInt(eodIdStr);
        } catch (JsonProcessingException e) {
            log.error("Unable to retrieve EodId from arm file {}", armFilename);
            throw new UnableToReadArmFileException(armFilename, e);
        }
    }
}
