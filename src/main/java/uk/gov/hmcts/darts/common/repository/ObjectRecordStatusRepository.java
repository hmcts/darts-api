package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface ObjectRecordStatusRepository extends JpaRepository<ObjectRecordStatusEntity, Integer> {

    default List<ObjectRecordStatusEntity> getReferencesByStatus(List<ObjectRecordStatusEnum> statusEnumList) {
        List<ObjectRecordStatusEntity> responseList = new ArrayList<>();
        for (ObjectRecordStatusEnum statusEnum : statusEnumList) {
            responseList.add(getReferenceById(statusEnum.getId()));
        }
        return responseList;
    }
}
