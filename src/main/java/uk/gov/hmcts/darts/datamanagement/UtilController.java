package uk.gov.hmcts.darts.datamanagement;

import com.azure.core.util.BinaryData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.service.impl.AudioTransformationServiceImpl;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("test")
class UtilController {

    @Autowired
    DataManagementService dataManagementService;

    @Autowired
    AudioTransformationServiceImpl audioTransformationService;

    @Autowired
    RetrieveCoreObjectService retrieveCoreObjectService;


    @Value("${darts.storage.blob.container-name.unstructured}")
    String unstructuredStorageContainerName;

    @PostMapping(value = "/create-blob", produces = "application/json", consumes = "text/plain")
    public ResponseEntity<String> createBlob(@RequestBody String testString) {
        BinaryData data = BinaryData.fromBytes(testString.getBytes(StandardCharsets.UTF_8));

        UUID uuid = dataManagementService.saveBlobData(unstructuredStorageContainerName, data);
        return new ResponseEntity<>("{\"uuid\": \"" + uuid + "\"}", HttpStatus.OK);
    }

    /*
    @GetMapping(path = "/process-audio")
    public void processAudioRequest(@RequestParam Integer requestId) {
        log.info("Received request to processAAudioRequest for " + requestId);
        audioTransformationService.processAudioRequest(requestId);
    }

     */

    /*
    @GetMapping(path = "/move-audio")
    public ResponseEntity<String> moveBlobToOutbound(@RequestParam String unstructuredUuid) {
        log.info("Received request to move blob to outbound data storage for " + unstructuredUuid);
        BinaryData binaryData = audioTransformationService.getAudioBlobData(UUID.fromString(unstructuredUuid));
        UUID outboundUuid = audioTransformationService.saveAudioBlobData(binaryData);
        return new ResponseEntity<>("{\"uuid\": \"" + outboundUuid + "\"}", HttpStatus.OK);
    }

     */

    @PostMapping(value = "/create-container", produces = "application/json")
    public void createContainer(@RequestParam String containerName) {
        dataManagementService.createContainer(containerName);
    }

    @DeleteMapping(value = "delete-blob")
    public void deleteBlob(@RequestParam String containerName, @RequestParam String uuid) {
        dataManagementService.deleteBlobData(containerName, UUID.fromString(uuid));
    }

    @PostMapping(value = "/create-hearing")
    public ResponseEntity<String> createHearing() {
        HearingEntity hearingEntity =  retrieveCoreObjectService.retrieveOrCreateHearing("Swansea", "1", "Swansea_case_1", LocalDate.now());
        return new ResponseEntity<>("{\"hearing_id\": \"" + hearingEntity.getId() + "\"}", HttpStatus.OK);
    }


}
