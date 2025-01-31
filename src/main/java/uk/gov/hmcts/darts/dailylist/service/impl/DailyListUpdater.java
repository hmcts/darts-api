package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.CreateCoreObjectService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.mapper.CitizenNameMapper;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;
import uk.gov.hmcts.darts.dailylist.model.CourtList;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.Defendant;
import uk.gov.hmcts.darts.dailylist.model.Hearing;
import uk.gov.hmcts.darts.dailylist.model.PersonalDetails;
import uk.gov.hmcts.darts.dailylist.model.Sitting;
import uk.gov.hmcts.darts.dets.service.DetsApiService;
import uk.gov.hmcts.darts.task.runner.dailylist.mapper.DailyListRequestMapper;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.DailyListStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.XmlParser;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.deserializer.LocalDateTimeTypeDeserializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.deserializer.LocalDateTypeDeserializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.deserializer.OffsetDateTimeTypeDeserializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.serializer.LocalDateTimeTypeSerializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.serializer.LocalDateTypeSerializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.serializer.OffsetDateTimeTypeSerializer;

import java.nio.charset.Charset;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
class DailyListUpdater {
    public static final String DL_TIME_NOT_BEFORE = "NOT BEFORE ";
    public static final String DL_TIME_SITTING_AT = "SITTING AT ";
    public static final String TIME_MARKING_NOTE_FORMAT = "h:mm a";
    public static final String SITTING_AT_FORMAT = "HH:mm:ss";

    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final CreateCoreObjectService createCoreObjectService;
    private final CourthouseRepository courthouseRepository;
    private final HearingRepository hearingRepository;
    private final ObjectMapper objectMapper;
    private final SystemUserHelper systemUserHelper;
    private final CurrentTimeHelper currentTimeHelper;
    private final CitizenNameMapper citizenNameMapper;
    private final DetsApiService detsApiService;

    private final XmlParser xmlParser;
    private final DailyListRequestMapper dailyListRequestMapper;


    @SuppressWarnings({"checkstyle:VariableDeclarationUsageDistance", "PMD.CognitiveComplexity"})
    @Transactional
    public void processDailyList(DailyListEntity dailyListEntity) throws JsonProcessingException {
        UserAccountEntity dailyListSystemUser = systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR);
        JobStatusType statusType = validateJsonExistsElseUpdate(dailyListEntity) ? JobStatusType.PROCESSED : JobStatusType.FAILED;
        if (statusType != JobStatusType.FAILED) {
            DailyListJsonObject dailyList = objectMapper.readValue(dailyListEntity.getContent(), DailyListJsonObject.class);
            for (CourtList courtList : dailyList.getCourtLists()) {

                String courtHouseName = courtList.getCourtHouse().getCourtHouseName().toUpperCase(Locale.ROOT);
                Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseName(
                    courtHouseName);

                if (foundCourthouse.isPresent()) {
                    List<Sitting> sittings = courtList.getSittings();
                    for (Sitting sitting : sittings) {
                        List<Hearing> hearings = sitting.getHearings();
                        for (Hearing dailyListHearing : hearings) {

                            String caseNumber = getCaseNumber(dailyListEntity, dailyListHearing);
                            if (caseNumber == null) {
                                statusType = JobStatusType.PARTIALLY_PROCESSED;
                                continue;
                            }

                            LocalTime scheduledStartTime = getScheduledStartTime(sitting, dailyListHearing);
                            LocalDateTime hearingDateTime = dailyListHearing.getHearingDetails().getHearingDate().atTime(scheduledStartTime);

                            HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
                                courtHouseName, sitting.getCourtRoomNumber(),
                                caseNumber, hearingDateTime,
                                dailyListSystemUser
                            );

                            hearing.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());

                            CourtCaseEntity courtCase = hearing.getCourtCase();
                            courtCase.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());
                            addJudges(sitting, hearing);
                            addDefendants(courtCase, dailyListHearing.getDefendants());
                            addProsecution(courtCase, dailyListHearing);
                            addDefenders(courtCase, dailyListHearing.getDefendants());
                            hearingRepository.saveAndFlush(hearing);
                        }
                    }
                } else {
                    statusType = JobStatusType.PARTIALLY_PROCESSED;
                    log.error("Unregistered courthouse {} daily list entry with id {} has not been processed",
                              courtHouseName, dailyListEntity.getId());
                }
            }
        }
        dailyListEntity.setLastModifiedBy(dailyListSystemUser);
        dailyListEntity.setStatus(statusType);
    }

    /**
     * Temporary change will be reverted downstream: DMP-4191.
     */
    boolean validateJsonExistsElseUpdate(DailyListEntity dailyListEntity) {
        if (dailyListEntity.getContent() != null) {
            log.debug("Daily list with id {} has JSON no need to fetch XML", dailyListEntity.getId());
            return true;
        }
        if (!validateXmlElseUpdate(dailyListEntity)) {
            return false;
        }
        return mapXmlToJson(dailyListEntity);
    }

    /**
     * Temporary change will be reverted downstream: DMP-4191.
     */
    boolean validateXmlElseUpdate(DailyListEntity dailyListEntity) {
        if (dailyListEntity.getExternalLocation() == null) {
            log.error("Daily list with id {} has no external location", dailyListEntity.getId());
            return false;
        }
        if (dailyListEntity.getExternalLocationTypeEntity() == null
            || !dailyListEntity.getExternalLocationTypeEntity().getId().equals(ExternalLocationTypeEnum.DETS.getId())) {
            log.error("Daily list with id {} has an invalid elt_id", dailyListEntity.getId());
            return false;
        }

        try (DownloadResponseMetaData downloadResponseMetaData = detsApiService.downloadData(dailyListEntity.getExternalLocation())) {
            dailyListEntity.setXmlContent(downloadResponseMetaData.getResource().getContentAsString(Charset.defaultCharset()));
            return true;
        } catch (Exception e) {
            log.error("Failed to download file for daily list with id {}", dailyListEntity.getId(), e);
            return false;
        }
    }

    /**
     * Temporary change will be reverted downstream: DMP-4191.
     */
    boolean mapXmlToJson(DailyListEntity dailyListEntity) {
        DailyListStructure legacyDailyListObject;
        try {
            legacyDailyListObject = xmlParser.unmarshal(dailyListEntity.getXmlContent().trim(), DailyListStructure.class);
        } catch (Exception e) {
            log.error("Failed to unmarshal XML for daily list with id {}", dailyListEntity.getId(), e);
            return false;
        }

        try {
            DailyListJsonObject modernisedDailyList = dailyListRequestMapper.mapToEntity(legacyDailyListObject);
            dailyListEntity.setContent(getServiceObjectMapper().writeValueAsString(modernisedDailyList));
        } catch (Exception ex) {
            log.error("Failed to map XML to JSON for daily list with id {}", dailyListEntity.getId(), ex);
            return false;
        }
        return true;
    }

    private LocalTime getScheduledStartTime(Sitting sitting, Hearing dailyListHearing) {
        String timeMarkingNoteText = dailyListHearing.getTimeMarkingNote();
        if (StringUtils.isNotBlank(timeMarkingNoteText)) {
            try {
                return getTimeFromTimeMarkingNote(timeMarkingNoteText);
            } catch (DateTimeException dateTimeException) {
                log.warn("Ignore error and continue, Parsing failed for field TimeMarkingNote with value: {}",
                         timeMarkingNoteText, dateTimeException);
            }
        }

        if (StringUtils.isNotBlank(sitting.getSittingAt())) {
            try {
                return getTimeFromSittingAt(sitting);
            } catch (DateTimeException dateTimeException) {
                log.warn("Ignore error and continue, Parsing failed for field SittingAt with value: "
                             + sitting.getSittingAt(), dateTimeException);
            }
        }
        return LocalTime.of(9, 0);
    }

    private LocalTime getTimeFromSittingAt(Sitting sitting) {
        if (StringUtils.isNotBlank(sitting.getSittingAt())) {
            return LocalTime.parse(sitting.getSittingAt(), new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(SITTING_AT_FORMAT)
                .toFormatter(Locale.ENGLISH));
        }
        return null;
    }


    protected LocalTime getTimeFromTimeMarkingNote(final String timeMarkingNote) {
        String rawTime;
        if (StringUtils.isNotBlank(timeMarkingNote)) {

            if (timeMarkingNote.startsWith(DL_TIME_NOT_BEFORE)) {
                rawTime = timeMarkingNote.substring(DL_TIME_NOT_BEFORE.length());
            } else if (timeMarkingNote.startsWith(DL_TIME_SITTING_AT)) {
                rawTime = timeMarkingNote.substring(DL_TIME_SITTING_AT.length());
            } else {
                rawTime = timeMarkingNote;
            }

            return LocalTime.parse(rawTime.strip(), new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(TIME_MARKING_NOTE_FORMAT)
                .toFormatter(Locale.ENGLISH));
        }
        return null;
    }

    private String getCaseNumber(DailyListEntity dailyListEntity, Hearing hearing) {
        // CPP don't provide case id, use URN
        if (String.valueOf(SourceType.CPP).equalsIgnoreCase(dailyListEntity.getSource())) {
            if (hearing.getDefendants().isEmpty()) {
                return hearing.getCaseNumber();
            } else {
                String urn = hearing.getDefendants().get(0).getUrn();
                if (StringUtils.isBlank(urn)) {
                    log.warn("Case number not found for hearing: daily_list_id={}, hearing_date={}",
                             dailyListEntity.getId(), hearing.getHearingDetails().getHearingDate());
                    return null;
                } else {
                    return urn;
                }
            }
        }
        return hearing.getCaseNumber();

    }


    private void addProsecution(CourtCaseEntity courtCase, Hearing hearing) {
        if (hearing.getProsecution() == null) {
            return;
        }
        List<PersonalDetails> advocates = hearing.getProsecution().getAdvocates();
        UserAccountEntity dailyListSystemUser = systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR);
        advocates.forEach(advocate -> {
            if (!isExistingProsecutor(courtCase, advocate)) {
                courtCase.addProsecutor(createCoreObjectService.createProsecutor(
                    citizenNameMapper.getCitizenName(advocate.getName()), courtCase, dailyListSystemUser));
            }
        });
    }

    private void addDefenders(CourtCaseEntity courtCase, List<Defendant> defendants) {
        UserAccountEntity dailyListSystemUser = systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR);
        for (Defendant defendant : defendants) {
            for (PersonalDetails counselDetails : defendant.getCounsel()) {
                if (counselDetails == null) {
                    continue;
                }
                if (!isExistingDefenders(courtCase, counselDetails)) {
                    courtCase.addDefence(createCoreObjectService.createDefence(
                        citizenNameMapper.getCitizenName(counselDetails.getName()), courtCase, dailyListSystemUser));
                }
            }
        }
    }

    private void addDefendants(CourtCaseEntity courtCase, List<Defendant> defendants) {
        UserAccountEntity dailyListSystemUser = systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR);
        for (Defendant defendant : defendants) {
            if (!isExistingDefendant(courtCase, defendant)) {
                courtCase.addDefendant(createCoreObjectService.createDefendant(
                    citizenNameMapper.getCitizenName(defendant.getPersonalDetails().getName()),
                    courtCase,
                    dailyListSystemUser
                ));
            }
        }
    }

    private void addJudges(Sitting sitting, HearingEntity hearing) {
        UserAccountEntity dailyListSystemUser = systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR);
        for (CitizenName judge : sitting.getJudiciary()) {
            JudgeEntity judgeEntity = retrieveCoreObjectService.retrieveOrCreateJudge(judge.getCitizenNameRequestedName(), dailyListSystemUser);
            hearing.addJudge(judgeEntity, true);
        }
    }

    private boolean isExistingDefenders(CourtCaseEntity courtCase, PersonalDetails defenders) {
        boolean existingDefendant = false;
        for (DefenceEntity defenceEntity : courtCase.getDefenceList()) {
            if (citizenNameMapper.getCitizenName(defenders.getName()).equalsIgnoreCase(defenceEntity.getName())) {
                existingDefendant = true;
            }
        }

        return existingDefendant;
    }

    private boolean isExistingDefendant(CourtCaseEntity courtCase, Defendant defendant) {
        boolean existingDefendant = false;
        for (DefendantEntity defendantEntity : courtCase.getDefendantList()) {
            if (citizenNameMapper.getCitizenName(defendant.getPersonalDetails().getName()).equalsIgnoreCase(defendantEntity.getName())) {
                existingDefendant = true;
            }
        }

        return existingDefendant;
    }

    private boolean isExistingProsecutor(CourtCaseEntity courtCase, PersonalDetails prosecutor) {
        boolean existingDefendant = false;
        for (ProsecutorEntity prosecutorEntity : courtCase.getProsecutorList()) {

            if (citizenNameMapper.getCitizenName(prosecutor.getName()).equalsIgnoreCase(prosecutorEntity.getName())) {
                existingDefendant = true;
            }
        }

        return existingDefendant;
    }

    /**
     * Temporary change will be reverted downstream: DMP-4191.
     */
    ObjectMapper getServiceObjectMapper() {
        JavaTimeModule module = new JavaTimeModule();

        module.addSerializer(LocalDateTime.class, new LocalDateTimeTypeSerializer())
            .addSerializer(LocalDate.class, new LocalDateTypeSerializer())
            .addSerializer(OffsetDateTime.class, new OffsetDateTimeTypeSerializer())
            .addDeserializer(LocalDateTime.class, new LocalDateTimeTypeDeserializer())
            .addDeserializer(LocalDate.class, new LocalDateTypeDeserializer())
            .addDeserializer(OffsetDateTime.class, new OffsetDateTimeTypeDeserializer());

        return new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(module);
    }
}