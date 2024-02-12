package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;

import java.util.Optional;

@Repository
public interface NodeRegisterRepository extends JpaRepository<NodeRegisterEntity, Integer> {

    @Query("""
            SELECT nr FROM NodeRegisterEntity nr 
            WHERE nr.courtroom.id = :courtroomId
            AND nr.nodeType = 'DAR'
            ORDER BY nodeId desc
            LIMIT 1
            """)
    Optional<NodeRegisterEntity> findByCourtroomId(Integer courtroomId);
}
