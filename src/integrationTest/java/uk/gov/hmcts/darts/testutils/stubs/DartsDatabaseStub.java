package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditTable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.AnnotationUtils;
import org.springframework.data.history.Revisions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.entity.base.LastModifiedBy;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.common.repository.DataAnonymisationRepository;
import uk.gov.hmcts.darts.common.repository.DefenceRepository;
import uk.gov.hmcts.darts.common.repository.DefendantRepository;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.NodeRegisterRepository;
import uk.gov.hmcts.darts.common.repository.NotificationRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.repository.ProsecutorRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionConfidenceCategoryMapperRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionCommentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.task.runner.SoftDelete;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.CourthouseTestData;
import uk.gov.hmcts.darts.test.common.data.DailyListTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.TransactionalUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.EventHandlerTestData.createEventHandlerWith;

@Service
@AllArgsConstructor
@SuppressWarnings({
    "PMD.ExcessiveImports", "PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity"})
@Getter
@Slf4j
public class DartsDatabaseStub {

    private static final int SEQUENCE_START_VALUE = 15_000;

    private static final List<String> SEQUENCES_NO_RESET = List.of(
        "revinfo_seq"
    );

    private static final List<String> SEQUENCES_RESET_FROM = List.of(
        "usr_seq",
        "grp_seq",
        "aut_seq",
        "rpt_seq",
        "evh_seq"
    );

    private final DataAnonymisationRepository dataAnonymisationRepository;
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;
    private final EntityManagerFactory entityManagerFactory;
    private final AnnotationDocumentRepository annotationDocumentRepository;
    private final AnnotationRepository annotationRepository;
    private final ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;
    private final AuditRepository auditRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseManagementRetentionRepository caseManagementRetentionRepository;
    private final CaseRepository caseRepository;
    private final CaseRetentionRepository caseRetentionRepository;
    private final CourthouseRepository courthouseRepository;
    private final CourtroomRepository courtroomRepository;
    private final DailyListRepository dailyListRepository;
    private final DefenceRepository defenceRepository;
    private final DefendantRepository defendantRepository;
    private final EventHandlerRepository eventHandlerRepository;
    private final EventRepository eventRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;
    private final HearingRepository hearingRepository;
    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;
    private final JudgeRepository judgeRepository;
    private final MediaRepository mediaRepository;
    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final NodeRegisterRepository nodeRegisterRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ObjectStateRecordRepository objectStateRecordRepository;
    private final ProsecutorRepository prosecutorRepository;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final SecurityGroupRepository securityGroupRepository;
    private final SecurityRoleRepository securityRoleRepository;
    private final TranscriptionCommentRepository transcriptionCommentRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    private final TransformedMediaRepository transformedMediaRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final RegionRepository regionRepository;
    private final AutomatedTaskRepository automatedTaskRepository;
    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final EventLinkedCaseRepository eventLinkedCaseRepository;
    private final RetentionConfidenceCategoryMapperRepository retentionConfidenceCategoryMapperRepository;
    private final TranscriptionLinkedCaseRepository transcriptionLinkedCaseRepository;

    private final AnnotationStub annotationStub;
    private final AuditStub auditStub;
    private final CaseDocumentStub caseDocumentStub;
    private final CaseRetentionStub caseRetentionStub;
    private final CourtCaseStub courtCaseStub;
    private final CourthouseStub courthouseStub;
    private final CourtroomStub courtroomStub;
    private final EventStub eventStub;
    private final ExternalObjectDirectoryStub externalObjectDirectoryStub;
    private final HearingStub hearingStub;
    private final MediaStub mediaStub;
    private final MediaRequestStub mediaRequestStub;
    private final TranscriptionStub transcriptionStub;
    private final TranscriptionDocumentStub transcriptionDocumentStub;
    private final TransformedMediaStub transformedMediaStub;
    private final UserAccountStub userAccountStub;
    private final TransientObjectDirectoryStub transientObjectDirectoryStub;

    private final EntityManager entityManager;
    private final CurrentTimeHelper currentTimeHelper;
    private final TransactionalUtil transactionalUtil;
    private final EntityGraphPersistence entityGraphPersistence;
    private final DartsPersistence dartsPersistence;

    public void resetSequences() {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            final Query query = em.createNativeQuery("SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'darts'");
            final List sequences = query.getResultList();
            for (Object seqName : sequences) {
                if (SEQUENCES_RESET_FROM.contains(seqName.toString())) {
                    em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART WITH " + SEQUENCE_START_VALUE).executeUpdate();
                } else if (!SEQUENCES_NO_RESET.contains(seqName.toString())) {
                    em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART").executeUpdate();
                }
            }
            em.getTransaction().commit();
        }
    }

    @Transactional
    public void resetTablesWithPredefinedTestData() {
        entityManager.getMetamodel().getEntities().stream()
            .filter(entityType -> LastModifiedBy.class.isAssignableFrom(entityType.getJavaType()))
            .forEach(entityType -> {
                String table = entityType.getJavaType().getAnnotation(Table.class).name();
                entityManager.createNativeQuery("update darts." + table + " set last_modified_by = 0").executeUpdate();
            });
        retentionPolicyTypeRepository.deleteAll(
            retentionPolicyTypeRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );

        eventHandlerRepository.deleteAll(
            eventHandlerRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );

        automatedTaskRepository.deleteAll(
            automatedTaskRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );

        securityGroupRepository.deleteAll(
            securityGroupRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );

        userAccountRepository.deleteAll(
            userAccountRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );
    }

    //@Transactional
    public void clearDatabaseInThisOrder() {
        TestUtils.retryLoop(10, 500, () -> {
            removeDeleteFlag(AnnotationDocumentEntity.class,
                             CaseDocumentEntity.class,
                             MediaEntity.class,
                             TranscriptionDocumentEntity.class);
            transcriptionLinkedCaseRepository.deleteAll();
            dataAnonymisationRepository.deleteAll();
            armRpoExecutionDetailRepository.deleteAll();
            objectAdminActionRepository.deleteAll();
            auditRepository.deleteAll();
            externalObjectDirectoryRepository.deleteAll();
            annotationDocumentRepository.deleteAll();
            caseDocumentRepository.deleteAll();
            caseRetentionRepository.deleteAll();
            caseManagementRetentionRepository.deleteAll();
            transcriptionDocumentRepository.deleteAll();
            transcriptionCommentRepository.deleteAll();
            transcriptionWorkflowRepository.deleteAll();
            transcriptionDocumentRepository.deleteAll();
            transcriptionRepository.deleteAll();
            transientObjectDirectoryRepository.deleteAll();
            transformedMediaRepository.deleteAll();
            mediaRequestRepository.deleteAll();
            eventLinkedCaseRepository.deleteAll();
            eventRepository.deleteAll();
            hearingRepository.deleteAll();
            annotationRepository.deleteAll();
            mediaLinkedCaseRepository.deleteAll();
            mediaRepository.deleteAll();
            notificationRepository.deleteAll();
            nodeRegisterRepository.deleteAll();
            courtroomRepository.deleteAll();
            defenceRepository.deleteAll();
            defendantRepository.deleteAll();
            prosecutorRepository.deleteAll();
            caseRepository.deleteAll();
            judgeRepository.deleteAll();
            dailyListRepository.deleteAll();
            courthouseRepository.deleteAll();
            regionRepository.deleteAll();
            annotationRepository.deleteAll();
            transcriptionRepository.deleteAll();
            transcriptionWorkflowRepository.deleteAll();
            retentionConfidenceCategoryMapperRepository.deleteAll();
        });
    }

    public void removeAllAudits() {
        removeAudits(UserAccountEntity.class,
                     MediaRequestEntity.class,
                     ArmAutomatedTaskEntity.class,
                     AutomatedTaskEntity.class,
                     CourthouseEntity.class,
                     EventHandlerEntity.class,
                     NodeRegisterEntity.class,
                     RetentionPolicyTypeEntity.class,
                     SecurityGroupEntity.class,
                     TranscriptionCommentEntity.class,
                     TranscriptionEntity.class,
                     TranscriptionWorkflowEntity.class,
                     UserAccountEntity.class);
    }

    private void removeAudits(Class<?>... classes) {
        stream(classes).forEach(tClass -> {
            AuditTable table = tClass.getAnnotation(AuditTable.class);
            if (table != null) {
                entityManager.createNativeQuery("delete from darts." + table.value())
                    .executeUpdate();
            }
            AnnotationUtils.findAnnotatedFields(tClass, AuditJoinTable.class, field -> true)
                .forEach(field -> {
                    AuditJoinTable auditJoinTable = field.getAnnotation(AuditJoinTable.class);
                    entityManager.createNativeQuery("delete from darts." + auditJoinTable.name())
                        .executeUpdate();
                });
        });
        entityManager.createNativeQuery("delete from darts.revinfo")
            .executeUpdate();
    }

    @SafeVarargs
    private void removeDeleteFlag(Class<? extends SoftDelete>... classes) {
        stream(classes).forEach(tClass -> {
            Table table = tClass.getAnnotation(Table.class);
            entityManager.createNativeQuery("UPDATE darts." + table.name() + " set is_deleted = false where is_deleted = true")
                .executeUpdate();
        });
    }

    public List<EventHandlerEntity> findByHandlerAndActiveTrue(String handlerName) {
        return eventHandlerRepository.findByHandlerAndActiveTrue(handlerName);
    }

    public Optional<CourtCaseEntity> findByCaseByCaseNumberAndCourtHouseName(String someCaseNumber,
                                                                             String someCourthouse) {
        return caseRepository.findByCaseNumberAndCourthouse_CourthouseName(
            someCaseNumber,
            someCourthouse
        );
    }

    public List<HearingEntity> findByCourthouseCourtroomAndDate(String someCourthouse, String someRoom,
                                                                LocalDate toLocalDate) {
        return hearingRepository.findByCourthouseCourtroomAndDate(someCourthouse, someRoom, toLocalDate);
    }

    public List<EventEntity> getAllEvents() {
        return eventRepository.findAll();
    }

    public JudgeEntity createSimpleJudge(String name) {
        return retrieveCoreObjectService.retrieveOrCreateJudge(name, userAccountRepository.getReferenceById(0));
    }

    public EventEntity createEvent(HearingEntity hearing, int eventHandlerId) {
        return eventStub.createEvent(hearing, eventHandlerId);
    }

    public EventEntity createEvent(HearingEntity hearing) {
        return eventStub.createEvent(hearing);
    }

    @Transactional
    public CourtroomEntity givenTheCourtHouseHasRoom(CourthouseEntity courthouse, String roomName) {
        return retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouse, roomName, userAccountRepository.getReferenceById(0));
    }

    @Transactional
    public HearingEntity givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
        String caseNumber, String courthouseName, String courtroomName, LocalDateTime hearingDate) {

        createCourthouseUnlessExists(courthouseName);
        HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccountRepository.getReferenceById(0)
        );
        hearing.setHearingIsActual(true);
        hearing.addJudge(createSimpleJudge(caseNumber + "judge1"), false);
        return dartsDatabaseSaveStub.save(hearing);
    }

    @Transactional
    public CourtCaseEntity givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(String caseNumber,
                                                                                  String courthouseName,
                                                                                  String courtroomName) {
        givenTheDatabaseContainsCourthouseWithRoom(courthouseName, courtroomName);
        return retrieveCoreObjectService.retrieveOrCreateCase(courthouseName, caseNumber);
    }

    public CourtCaseEntity createCase(String courthouseName, String caseNumber) {
        courthouseStub.createCourthouseUnlessExists(courthouseName);
        return retrieveCoreObjectService.retrieveOrCreateCase(courthouseName, caseNumber, userAccountRepository.getReferenceById(0));
    }

    public CourtroomEntity createCourtroomUnlessExists(String courthouseName, String courtroomName) {
        return courtroomStub.createCourtroomUnlessExists(courthouseName, courtroomName, userAccountRepository.getReferenceById(0));
    }

    @Transactional
    public CourtroomEntity givenTheDatabaseContainsCourthouseWithRoom(String courthouseName, String courtroomName) {
        return createCourtroomUnlessExists(courthouseName, courtroomName);
    }

    public List<NotificationEntity> getNotificationsForCase(Integer caseId) {
        return notificationRepository.findByCourtCase_Id(caseId);
    }

    @Transactional
    public HearingEntity hasSomeHearing() {
        return createHearing("NEWCASTLE", "r1", "c1", now());
    }

    public HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber,
                                       LocalDateTime hearingDate) {
        createCourthouseUnlessExists(courthouseName);
        var hearingEntity = retrieveCoreObjectService.retrieveOrCreateHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccountRepository.getReferenceById(0)
        );
        hearingEntity.setHearingIsActual(true);
        return hearingEntity;
    }

    public CourthouseEntity createCourthouseUnlessExists(String courthouseName) {
        return courthouseStub.createCourthouseUnlessExists(courthouseName);
    }

    public CourthouseEntity createCourthouseWithNameAndCode(String name, Integer code, String displayName) {
        var courthouse = CourthouseTestData.createCourthouseWithName(name);
        courthouse.setCode(code);
        courthouse.setDisplayName(displayName);
        UserAccountEntity defaultUser = userAccountRepository.getReferenceById(0);
        courthouse.setCreatedBy(defaultUser);
        courthouse.setLastModifiedBy(defaultUser);
        return save(courthouse);
    }

    @Transactional
    public CourthouseEntity createCourthouseWithTwoCourtrooms() {
        CourthouseEntity swanseaCourtEntity = createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");
        save(createCourtRoomWithNameAtCourthouse(swanseaCourtEntity, "1"));
        save(createCourtRoomWithNameAtCourthouse(swanseaCourtEntity, "2"));
        return swanseaCourtEntity;

    }

    @Transactional
    public void createDailyLists(String listingCourthouse) throws IOException {
        DailyListEntity xhbDailyList = DailyListTestData.createDailyList(
            LocalTime.of(13, 0),
            String.valueOf(SourceType.XHB),
            listingCourthouse,
            "tests/dailyListProcessorTest/dailyListXHB.json"
        );

        DailyListEntity cppDailyList = DailyListTestData.createDailyList(
            LocalTime.of(13, 0),
            String.valueOf(SourceType.CPP),
            listingCourthouse,
            "tests/dailyListProcessorTest/dailyListCPP.json"
        );

        save(xhbDailyList);
        save(cppDailyList);
    }

    @Transactional
    public MediaEntity createMediaEntity(String courthouseName, String courtroomName, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return mediaStub.createMediaEntity(courthouseName, courtroomName, startTime, endTime, channel);
    }

    public CourtroomEntity findCourtroomBy(String courthouseName, String courtroomName) {
        return courtroomRepository.findByCourthouseNameAndCourtroomName(courthouseName, courtroomName).orElse(null);
    }

    public CourthouseEntity findCourthouseWithName(String name) {
        return courthouseRepository.findByCourthouseName(name).get();
    }

    public ExternalLocationTypeEntity getExternalLocationTypeEntity(ExternalLocationTypeEnum externalLocationTypeEnum) {
        return externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId());
    }

    public ObjectRecordStatusEntity getObjectRecordStatusEntity(ObjectRecordStatusEnum objectRecordStatusEnum) {
        return objectRecordStatusRepository.getReferenceById(objectRecordStatusEnum.getId());
    }

    @Transactional
    public MediaRequestEntity createAndLoadOpenMediaRequestEntity(UserAccountEntity requestor, AudioRequestType audioRequestType) {

        HearingEntity hearing = createHearing("NEWCASTLE", "Int Test Courtroom 2", "2", LocalDateTime.of(2023, 6, 10, 10, 0, 0));

        return dartsPersistence.save(
            PersistableFactory.getMediaRequestTestData().createCurrentMediaRequest(
                hearing,
                requestor,
                OffsetDateTime.parse("2023-06-26T13:00:00Z"),
                OffsetDateTime.parse("2023-06-26T13:45:00Z"),
                audioRequestType,
                OPEN
            ));
    }

    @Transactional
    public MediaRequestEntity createAndLoadNonAccessedCurrentMediaRequestEntity(UserAccountEntity requestor,
                                                                                AudioRequestType audioRequestType) {

        MediaRequestEntity mediaRequestEntity = PersistableFactory.getMediaRequestTestData().someMinimalBuilder()
            .requestor(requestor)
            .currentOwner(requestor)
            .startTime(OffsetDateTime.parse("2023-06-26T13:00:00Z"))
            .endTime(OffsetDateTime.parse("2023-06-26T14:00:00Z"))
            .requestType(audioRequestType)
            .status(MediaRequestStatus.COMPLETED)
            .build().getEntity();
        dartsPersistence.save(mediaRequestEntity);

        OffsetDateTime expiryTime = OffsetDateTime.of(2023, 7, 2, 13, 0, 0, 0, UTC);
        OffsetDateTime lastAccessed = OffsetDateTime.of(2023, 6, 30, 13, 0, 0, 0, UTC);
        transformedMediaStub.createTransformedMediaEntity(mediaRequestEntity, "T20231010_0", expiryTime, lastAccessed);

        return mediaRequestEntity;
    }

    @Transactional
    public MediaEntity addMediaToHearing(HearingEntity hearing, MediaEntity mediaEntity) {
        hearing.addMedia(mediaEntity);
        mediaEntity.setCourtroom(hearing.getCourtroom());
        save(hearing);
        dartsDatabaseSaveStub.save(mediaEntity);
        dartsDatabaseSaveStub.save(hearing);
        return mediaEntity;
    }

    public MediaEntity addMediaToHearingNonTransactional(HearingEntity hearing, MediaEntity mediaEntity) {
        hearing.addMedia(mediaEntity);
        mediaEntity.setCourtroom(hearing.getCourtroom());
        mediaEntity.setIsCurrent(true);
        dartsDatabaseSaveStub.save(mediaEntity);
        dartsDatabaseSaveStub.save(hearing.getCourtroom().getCourthouse());
        dartsDatabaseSaveStub.save(hearing.getCourtroom());
        dartsDatabaseSaveStub.save(hearing);
        return mediaEntity;
    }

    @Transactional
    public HearingEntity saveEventsForHearing(HearingEntity hearing, EventEntity... eventEntities) {
        return saveEventsForHearing(hearing, List.of(eventEntities));
    }

    @Transactional
    public HearingEntity saveEventsForHearing(HearingEntity hearing, List<EventEntity> eventEntities) {
        var hearingEntity = save(hearing);
        eventEntities.forEach(event -> saveSingleEventForHearing(hearing, event));
        return hearingEntity;
    }

    @Transactional
    public AnnotationEntity save(AnnotationEntity annotationEntity) {
        save(annotationEntity.getCurrentOwner());
        return dartsDatabaseSaveStub.save(annotationEntity);
    }

    @Transactional
    public HearingEntity save(HearingEntity hearingEntity) {
        save(hearingEntity.getCourtroom().getCourthouse());
        save(hearingEntity.getCourtroom());
        save(hearingEntity.getCourtCase());
        saveAllCollection(hearingEntity.getJudges());
        return dartsDatabaseSaveStub.save(hearingEntity);
    }

    @Transactional
    public CourthouseEntity save(CourthouseEntity courthouse) {
        if (courthouse == null) {
            return null;
        }
        UserAccountEntity createdBy = dartsDatabaseSaveStub.save(courthouse.getCreatedBy());
        courthouse.setCreatedBy(createdBy);
        courthouse.setLastModifiedBy(createdBy);
        return dartsDatabaseSaveStub.save(courthouse);
    }

    @Transactional
    public CourtroomEntity save(CourtroomEntity courtroom) {
        save(courtroom.getCourthouse());
        UserAccountEntity createdBy = dartsDatabaseSaveStub.save(courtroom.getCreatedBy());
        courtroom.setCreatedBy(createdBy);
        return dartsDatabaseSaveStub.save(courtroom);
    }

    @Transactional
    public CourtCaseEntity save(CourtCaseEntity courtCase) {
        if (courtCase == null) {
            return null;
        }
        save(courtCase.getCourthouse());
        saveAllCollection(courtCase.getJudges());
        courtCase.getDefenceList().forEach(dartsDatabaseSaveStub::updateCreatedByLastModifiedBy);
        courtCase.getDefendantList().forEach(dartsDatabaseSaveStub::updateCreatedByLastModifiedBy);
        courtCase.getProsecutorList().forEach(dartsDatabaseSaveStub::updateCreatedByLastModifiedBy);
        return dartsDatabaseSaveStub.save(courtCase);
    }

    @Transactional
    public CaseManagementRetentionEntity save(CaseManagementRetentionEntity caseManagementRetentionEntity) {
        if (caseManagementRetentionEntity == null) {
            return null;
        }
        save(caseManagementRetentionEntity.getRetentionPolicyTypeEntity());
        save(caseManagementRetentionEntity.getCourtCase());
        save(caseManagementRetentionEntity.getEventEntity());
        return dartsDatabaseSaveStub.save(caseManagementRetentionEntity);
    }


    @Transactional
    public RetentionPolicyTypeEntity save(RetentionPolicyTypeEntity retentionPolicyTypeEntity) {
        if (retentionPolicyTypeEntity == null) {
            return null;
        }
        save(retentionPolicyTypeEntity.getCreatedBy());
        return dartsDatabaseSaveStub.save(retentionPolicyTypeEntity);
    }


    @Transactional
    public EventEntity save(EventEntity eventEntity) {
        if (eventEntity == null) {
            return null;
        }
        save(eventEntity.getCourtroom());
        return dartsDatabaseSaveStub.save(eventEntity);
    }

    @Transactional
    public TranscriptionEntity save(TranscriptionEntity transcriptionEntity) {
        save(transcriptionEntity.getCourtCase());
        dartsDatabaseSaveStub.save(transcriptionEntity.getCreatedBy());
        dartsDatabaseSaveStub.save(transcriptionEntity.getLastModifiedBy());
        var transcription = dartsDatabaseSaveStub.save(transcriptionEntity);
        dartsDatabaseSaveStub.save(transcription.getCreatedBy());
        transcription.getTranscriptionDocumentEntities().forEach(td -> {
            dartsDatabaseSaveStub.save(td.getUploadedBy());
            dartsDatabaseSaveStub.save(td.getLastModifiedBy());
            dartsDatabaseSaveStub.save(td);
        });
        return transcription;
    }

    @Transactional
    public CaseRetentionEntity save(CaseRetentionEntity caseRetentionEntity) {
        save(caseRetentionEntity.getSubmittedBy());
        save(caseRetentionEntity.getRetentionPolicyType());
        save(caseRetentionEntity.getCaseManagementRetention());
        return dartsDatabaseSaveStub.save(caseRetentionEntity);
    }


    public <T> T save(T entity) {
        return dartsDatabaseSaveStub.save(entity);
    }

    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.AvoidAccessibilityAlteration"})
    @Transactional
    public <T> T saveWithTransientEntities(T entity) {
        try {
            // Check and persist transient entities
            Class<?> clazz = entity.getClass();
            while (clazz != null) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(entity);
                    if (fieldValue != null && isEntity(fieldValue)) {
                        // Check if the entity is transient
                        Method getIdMethod = fieldValue.getClass().getMethod("getId");
                        Object id = getIdMethod.invoke(fieldValue);
                        if (id == null || (id instanceof Integer && (Integer) id == 0)) {
                            // Save the transient entity
                            entityManager.persist(fieldValue);
                            entityManager.flush();
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }

            // Proceed with original save logic
            Method getIdInstanceMethod = entity.getClass().getMethod("getId");
            Integer id = (Integer) getIdInstanceMethod.invoke(entity);
            if (id == null) {
                entityManager.persist(entity);
                return entity;
            } else {
                return entityManager.merge(entity);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new JUnitException("Failed to save entity", e);
        }
    }

    private boolean isEntity(Object obj) {
        return obj.getClass().isAnnotationPresent(Entity.class);
    }

    @Transactional
    public <T> void saveAllCollection(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        entities.forEach(this::save);
    }

    @Transactional
    public void saveAll(Object... entities) {
        if (entities == null || entities.length == 0) {
            return;
        }
        stream(entities).forEach(this::save);
    }

    @Transactional
    public void saveAll(UserAccountEntity... testUsers) {
        stream(testUsers).forEach(user -> {
            UserAccountEntity systemUser = userAccountRepository.getReferenceById(0);
            user.setCreatedBy(systemUser);
            user.setLastModifiedBy(systemUser);
        });
        userAccountRepository.saveAll(asList(testUsers));
    }

    @Transactional
    public void saveAll(HearingEntity... hearingEntities) {
        stream(hearingEntities).forEach(this::save);
    }

    public List<DailyListEntity> saveAll(DailyListEntity... dailyListEntity) {
        return dailyListRepository.saveAll(asList(dailyListEntity));
    }

    @Transactional
    public void saveAllWithTransient(Object... entities) {
        if (entities == null) {
            return;
        }
        stream(entities).forEach(this::saveWithTransientEntities);
    }

    @Transactional
    public MediaRequestEntity saveWithMediaRequestWithTransientEntities(MediaRequestEntity mediaRequestEntity) {
        save(mediaRequestEntity.getHearing());
        save(mediaRequestEntity.getRequestor());
        save(mediaRequestEntity.getCurrentOwner());
        return save(mediaRequestEntity);
    }

    public UserAccountEntity createTestUserAccount() {
        Optional<UserAccountEntity> userAccountEntity = userAccountRepository.findFirstByEmailAddressIgnoreCase("test.user@example.com");
        if (userAccountEntity.isEmpty()) {
            UserAccountEntity testUser = new UserAccountEntity();
            testUser.setEmailAddress("test.user@example.com");
            testUser.setUserFullName("testuser");
            testUser.setAccountGuid(UUID.randomUUID().toString());
            testUser.setIsSystemUser(false);
            testUser.setActive(true);
            testUser.setCreatedById(0);
            testUser.setCreatedDateTime(OffsetDateTime.now());
            testUser.setLastModifiedById(0);
            testUser.setLastModifiedDateTime(OffsetDateTime.now());
            return userAccountRepository.saveAndFlush(testUser);
        }
        return userAccountEntity.get();
    }

    private void saveSingleEventForHearing(HearingEntity hearing, EventEntity event) {
        if (event.getHearingEntities().isEmpty()) {
            event.setHearingEntities(List.of(hearingRepository.getReferenceById(hearing.getId())));
            dartsDatabaseSaveStub.save(event);
        } else {
            List<HearingEntity> hearingEntities = new ArrayList<>();
            hearingEntities.addAll(event.getHearingEntities());
            boolean alreadyExists = hearingEntities.stream().anyMatch(hearingEntity -> hearingEntity.getId().equals(hearing.getId()));
            if (!alreadyExists) {
                hearingEntities.add(hearingRepository.getReferenceById(hearing.getId()));
            }

            event.setHearingEntities(hearingEntities);
            dartsDatabaseSaveStub.save(event);
        }
    }

    public EventEntity addHandlerToEvent(EventEntity event, int handlerId) {
        var handler = eventHandlerRepository.getReferenceById(handlerId);
        event.setEventType(handler);
        event.setLogEntry(false);
        return dartsDatabaseSaveStub.save(event);
    }

    @Transactional
    public CourtCaseEntity addHandlerToCase(CourtCaseEntity caseEntity, int handlerId) {
        var handler = eventHandlerRepository.findById(handlerId).orElseThrow();
        caseEntity.setReportingRestrictions(handler);
        caseEntity.getHearings().forEach(this::save);
        save(caseEntity.getCourthouse());
        return save(caseEntity);
    }


    public RetentionPolicyTypeEntity getRetentionPolicyTypeEntity(RetentionPolicyEnum retentionPolicyEnum) {
        return retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(
            retentionPolicyEnum.getPolicyKey(),
            currentTimeHelper.currentOffsetDateTime()
        ).get(0);
    }

    @Transactional
    @SneakyThrows
    @SuppressWarnings("PMD.DoNotUseThreads")//Required for test stability
    public List<CaseRetentionEntity> createCaseRetention(CourtCaseEntity courtCase) {
        RetentionPolicyTypeEntity retentionPolicyTypeEntity =
            getRetentionPolicyTypeEntity(RetentionPolicyEnum.MANUAL);

        CaseRetentionEntity caseRetentionEntity1 = createCaseRetentionObject(courtCase, retentionPolicyTypeEntity, "a_state");
        caseRetentionEntity1 = dartsDatabaseSaveStub.save(caseRetentionEntity1);
        Thread.sleep(10);//Wait 10ms to ensure createdAt and lastModifiedAt times are different to other entities
        CaseRetentionEntity caseRetentionEntity2 = createCaseRetentionObject(courtCase, retentionPolicyTypeEntity, "b_state");
        caseRetentionEntity2 = dartsDatabaseSaveStub.save(caseRetentionEntity2);
        Thread.sleep(10);//Wait 10ms to ensure createdAt and lastModifiedAt times are different to other entities
        CaseRetentionEntity caseRetentionEntity3 = createCaseRetentionObject(courtCase, retentionPolicyTypeEntity, "c_state");
        caseRetentionEntity3 = dartsDatabaseSaveStub.save(caseRetentionEntity3);
        Thread.sleep(10);//Wait 10ms to ensure createdAt and lastModifiedAt times are different to other entities
        return List.of(caseRetentionEntity1, caseRetentionEntity2, caseRetentionEntity3);
    }

    public CaseRetentionEntity createCaseRetentionObject(CourtCaseEntity courtCase,
                                                         RetentionPolicyTypeEntity retentionPolicyTypeEntity, String state) {

        return createCaseRetentionObject(courtCase,
                                         OffsetDateTime.now().plusYears(7),
                                         retentionPolicyTypeEntity, state, false);
    }

    public CaseRetentionEntity createCaseRetentionObject(CourtCaseEntity courtCase,
                                                         OffsetDateTime retainUntil,
                                                         RetentionPolicyTypeEntity retentionPolicyTypeEntity, String state,
                                                         boolean save) {
        CaseRetentionEntity caseRetentionEntity = new CaseRetentionEntity();
        caseRetentionEntity.setCourtCase(courtCase);
        caseRetentionEntity.setRetentionPolicyType(retentionPolicyTypeEntity);
        caseRetentionEntity.setTotalSentence("10y0m0d");
        caseRetentionEntity.setRetainUntil(retainUntil);
        caseRetentionEntity.setRetainUntilAppliedOn(OffsetDateTime.now().plusYears(1));
        caseRetentionEntity.setCurrentState(state);
        caseRetentionEntity.setComments("a comment");
        caseRetentionEntity.setCreatedDateTime(OffsetDateTime.now());
        caseRetentionEntity.setCreatedBy(userAccountRepository.getReferenceById(0));
        caseRetentionEntity.setLastModifiedDateTime(OffsetDateTime.now());
        caseRetentionEntity.setLastModifiedBy(userAccountRepository.getReferenceById(0));
        caseRetentionEntity.setSubmittedBy(userAccountRepository.getReferenceById(0));
        if (save) {
            return dartsDatabaseSaveStub.save(caseRetentionEntity);
        }
        return caseRetentionEntity;
    }

    public CaseRetentionEntity createCaseRetentionObject(CourtCaseEntity courtCase,
                                                         CaseRetentionStatus retentionStatus, OffsetDateTime retainUntilDate, boolean isManual) {
        return caseRetentionStub.createCaseRetentionObject(courtCase, retentionStatus, retainUntilDate, isManual);
    }

    public UserAccountEntity saveUserWithGroup(UserAccountEntity user) {
        securityGroupRepository.saveAll(user.getSecurityGroupEntities());
        return dartsDatabaseSaveStub.save(user);
    }

    public List<NotificationEntity> getNotificationFor(String someCaseNumber) {
        return notificationRepository.findAll().stream()
            .filter(notification -> notification.getCourtCase().getCaseNumber().equals(someCaseNumber))
            .toList();
    }

    @Transactional
    public AnnotationEntity findAnnotationById(Integer annotationId) {
        return annotationRepository.findById(annotationId).orElseThrow();
    }

    public AnnotationDocumentEntity findAnnotationDocumentFor(Integer annotationId) {
        return annotationDocumentRepository.findAll().stream()
            .filter(annotationDocument -> annotationDocument.getAnnotation().getId().equals(annotationId))
            .findFirst().orElseThrow(() -> new RuntimeException("No annotation document found for annotation id: " + annotationId));
    }

    public List<ExternalObjectDirectoryEntity> findExternalObjectDirectoryFor(Integer annotationId) {
        var annotationDocumentEntity = annotationDocumentRepository.findAll().stream()
            .filter(annotationDocument -> annotationDocument.getAnnotation().getId().equals(annotationId))
            .findFirst().orElseThrow(() -> new RuntimeException("No annotation document found for annotation id: " + annotationId));

        return externalObjectDirectoryRepository.findAll().stream()
            .filter(externalObjectDirectory -> externalObjectDirectory.getAnnotationDocumentEntity().getId().equals(annotationDocumentEntity.getId()))
            .toList();
    }

    @Transactional
    public List<AnnotationEntity> findAnnotationsFor(Integer hearingId) {
        var hearingEntity = hearingRepository.findById(hearingId).orElseThrow();
        return hearingEntity.getAnnotations().stream().toList();
    }

    @Transactional
    public void addUserToGroup(UserAccountEntity userAccount, SecurityGroupEntity securityGroup) {
        dartsDatabaseSaveStub.save(securityGroup);
        if (userAccount.getSecurityGroupEntities().stream().anyMatch(securityGroupEntity -> {
            return securityGroup.getId().equals(securityGroupEntity.getId());
        })) {
            return;
        }
        userAccount.getSecurityGroupEntities().add(securityGroup);
        dartsDatabaseSaveStub.save(userAccount);
    }

    @Transactional
    public void addUserToGroup(UserAccountEntity userAccount, SecurityGroupEnum securityGroup) {
        Optional<SecurityGroupEntity> groupEntity
            = securityGroupRepository.findByGroupNameIgnoreCase(securityGroup.getName());
        addUserToGroup(userAccount, groupEntity.get());
    }

    @Transactional
    public Integer getLastModifiedByUserId(CreatedModifiedBaseEntity createdModifiedBaseEntity) {
        return createdModifiedBaseEntity.getLastModifiedBy().getId();
    }

    @Transactional
    public EventHandlerEntity createEventHandlerData(String subtype) {
        var eventHandler = createEventHandlerWith("DarStartHandler", "99999", subtype);
        return save(eventHandler);
    }

    @Transactional
    public EventHandlerEntity findEventHandlerMappingFor(Integer eventHandlerMappingId) {
        return eventHandlerRepository.findById(eventHandlerMappingId).orElseThrow();
    }

    public SecurityGroupEntity getSecurityGroupRef(int id) {
        return securityGroupRepository.getReferenceById(id);
    }


    public CourthouseEntity findCourthouseById(int id) {
        return courthouseRepository.findById(id).orElseThrow();
    }

    public Optional<NodeRegisterEntity> findByNodeId(int id) {
        return nodeRegisterRepository.findById(id);
    }

    public AutomatedTaskEntity getAutomatedTask(int id) {
        return automatedTaskRepository.findById(id).orElseThrow();
    }

    public List<AutomatedTaskEntity> getAllAutomatedTasks() {
        return automatedTaskRepository.findAll();
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public void lockTaskUntil(String taskName, OffsetDateTime taskReleaseDateTime) {
        var updateRowForTaskSql = """
            update darts.shedlock
            set lock_until = (?)
            where name = (?)
            """;
        var numOfRowsUpdated = entityManager.createNativeQuery(updateRowForTaskSql, Integer.class)
            .setParameter(1, taskReleaseDateTime)
            .setParameter(2, taskName)
            .executeUpdate();

        if (numOfRowsUpdated == 0) {
            var insertRowForTaskSql = """
                insert into darts.shedlock (name, lock_until, locked_at, locked_by)
                values ((?), (?), (?), (?))
                """;
            entityManager.createNativeQuery(insertRowForTaskSql, Integer.class)
                .setParameter(1, taskName)
                .setParameter(2, taskReleaseDateTime)
                .setParameter(3, OffsetDateTime.now())
                .setParameter(4, "some-user")
                .executeUpdate();
        }
    }

    public List<AuditEntity> findAudits() {
        return auditRepository.findAll();
    }

    public Revisions<Long, MediaRequestEntity> findMediaRequestRevisionsFor(Integer id) {
        return mediaRequestRepository.findRevisions(id);
    }

    public Revisions<Long, CourthouseEntity> findCourthouseRevisionsFor(Integer id) {
        return courthouseRepository.findRevisions(id);
    }

    public Revisions<Long, UserAccountEntity> findUserAccountRevisionsFor(Integer id) {
        return userAccountRepository.findRevisions(id);
    }

    public Revisions<Long, SecurityGroupEntity> findSecurityGroupRevisionsFor(Integer id) {
        return securityGroupRepository.findRevisions(id);
    }

    public Revisions<Long, TranscriptionEntity> findTranscriptionRevisionsFor(Integer id) {
        return transcriptionRepository.findRevisions(id);
    }

    @Transactional
    public Revisions<Long, TranscriptionWorkflowEntity> findTranscriptionWorkflowRevisionsFor(Integer transcriptionId) {
        var transcription = transcriptionRepository.findById(transcriptionId).orElseThrow();
        var latestWorkflow = transcription.getTranscriptionWorkflowEntities().stream()
            .min(comparing(TranscriptionWorkflowEntity::getWorkflowTimestamp))
            .orElseThrow();

        return transcriptionWorkflowRepository.findRevisions(latestWorkflow.getId());
    }

    @Transactional
    public Revisions<Long, TranscriptionCommentEntity> findTranscriptionCommentRevisionsFor(Integer transcriptionId) {
        var transcription = transcriptionRepository.findById(transcriptionId).orElseThrow();
        var latestComment = transcription.getTranscriptionCommentEntities().stream()
            .min(comparing(TranscriptionCommentEntity::getCreatedDateTime))
            .orElseThrow();

        return transcriptionCommentRepository.findRevisions(latestComment.getId());
    }

    public Revisions<Long, RetentionPolicyTypeEntity> findRetentionPolicyRevisionsFor(Integer id) {
        return retentionPolicyTypeRepository.findRevisions(id);
    }

    @Transactional
    public SecurityRoleEntity findSecurityRole(SecurityRoleEnum role) {
        return securityRoleRepository.findById(role.getId()).orElseThrow();
    }

    @Transactional
    public MediaLinkedCaseEntity createMediaLinkedCase(
        MediaEntity mediaEntity,
        CourtCaseEntity courtCase,
        String caseNumber,
        String courthouseName,
        MediaLinkedCaseSourceType source
    ) {
        var mediaLinkedCase = new MediaLinkedCaseEntity();
        mediaLinkedCase.setMedia(mediaEntity);
        mediaLinkedCase.setCourtCase(courtCase);
        mediaLinkedCase.setCaseNumber(caseNumber);
        mediaLinkedCase.setCourthouseName(courthouseName);
        mediaLinkedCase.setSource(source);
        mediaLinkedCase.setCreatedBy(userAccountRepository.getReferenceById(0));
        return mediaLinkedCaseRepository.save(mediaLinkedCase);
    }

    // (court case exists - modernised data)
    @Transactional
    public MediaLinkedCaseEntity createMediaLinkedCase(
        MediaEntity mediaEntity,
        CourtCaseEntity courtCase
    ) {
        return createMediaLinkedCase(
            mediaEntity,
            courtCase,
            null,  // case number comes from court case
            null,  // courthouse name comes from court case
            MediaLinkedCaseSourceType.LEGACY
        );
    }

    // migrated data - case number and courthouse name are provided
    @Transactional
    public MediaLinkedCaseEntity createMediaLinkedCase(
        MediaEntity mediaEntity,
        String caseNumber,
        String courthouseName
    ) {
        return createMediaLinkedCase(
            mediaEntity,
            null,  // no court case
            caseNumber,
            courthouseName,
            MediaLinkedCaseSourceType.LEGACY
        );
    }

    @Transactional
    public void clearDb() {
        removeAllAudits();
        resetSequences();
        clearDatabaseInThisOrder();
        resetTablesWithPredefinedTestData();
        removeAllAudits();//Ensures any newly added delete audits are removed
    }
}