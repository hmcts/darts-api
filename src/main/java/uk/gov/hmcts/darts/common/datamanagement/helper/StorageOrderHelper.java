package uk.gov.hmcts.darts.common.datamanagement.helper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StorageOrderHelper {

    @Value("#{'${darts.storage.search.order}'.split(',')}")
    private List<String> storageOrderStringList;

    private List<DatastoreContainerType> storageOrderContainerList;

    public List<DatastoreContainerType> getStorageOrder() {
        return storageOrderContainerList;
    }

    @PostConstruct
    private void populateStorageOrder() {
        List<DatastoreContainerType> newList = new ArrayList<>();
        for (String storageString : storageOrderStringList) {
            try {
                DatastoreContainerType foundContainerType = DatastoreContainerType.valueOf(storageString);
                newList.add(foundContainerType);
            } catch (IllegalArgumentException iae) {
                log.error("Storage search order item '{}' is not a valid storage type, and so has been ignored.", storageString);
            }
        }
        storageOrderContainerList = newList;
    }
}
