package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.CaseCommonService;
import uk.gov.hmcts.darts.common.service.CourtroomCommonService;
import uk.gov.hmcts.darts.common.service.HearingCommonService;
import uk.gov.hmcts.darts.task.runner.HasLongId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
@Service
@Slf4j
public class HearingCommonServiceImpl implements HearingCommonService {

    private final HearingRepository hearingRepository;
    private final CaseCommonService caseCommonService;
    private final CourtroomCommonService courtroomCommonService;

    @Override
    @Transactional
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                 UserAccountEntity userAccount) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate());

        return foundHearing.map(hearingEntity -> setHearingLastDateModifiedBy(hearingEntity, userAccount))
            .orElseGet(() -> createHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, null));
    }

    @Override
    @Transactional
    @Deprecated(since = "This method is used only in tests and will be removed in future.")
    public HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                          UserAccountEntity userAccount, MediaEntity mediaEntity) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate());

        return foundHearing.map(hearingEntity -> setHearingLastDateModifiedBy(hearingEntity, userAccount))
            .orElseGet(() -> createHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity));
    }

    private HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                        UserAccountEntity userAccount, MediaEntity mediaEntity) {
        final CourtCaseEntity courtCase = caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
        final CourtroomEntity courtroom = courtroomCommonService.retrieveOrCreateCourtroom(courtCase.getCourthouse(), courtroomName, userAccount);

        HearingEntity hearing = new HearingEntity();
        hearing.setCourtCase(courtCase);
        hearing.setCourtroom(courtroom);
        hearing.setHearingDate(hearingDate.toLocalDate());
        hearing.setScheduledStartTime(hearingDate.toLocalTime().withNano(0));
        hearing.setNew(true);
        hearing.setHearingIsActual(false);
        hearing.setCreatedBy(userAccount);
        hearing.setLastModifiedBy(userAccount);
        if (mediaEntity != null) {
            hearing.getMedias().add(mediaEntity);
        }
        return hearingRepository.saveAndFlush(hearing);
    }

    private HearingEntity setHearingLastDateModifiedBy(final HearingEntity hearingEntity, final UserAccountEntity userAccountEntity) {
        hearingEntity.setLastModifiedBy(userAccountEntity);
        return hearingRepository.saveAndFlush(hearingEntity);
    }

    /**
     * Links an audio media entity to a hearing if it exists.
     *
     * @param courtCaseEntity the court case entity associated with the hearing
     * @param mediaEntity     the media entity to link
     * @return true if the media was successfully linked, false otherwise
     */
    @Override
    public boolean linkAudioToHearings(CourtCaseEntity courtCaseEntity, MediaEntity mediaEntity) {
        return linkEntityToHearing(
            "media",
            mediaEntity,
            courtCaseEntity,
            mediaEntity.getCourtroom(),
            mediaEntity.getStart().toLocalDate(),
            HearingEntity::addMedia
        );
    }

    @Override
    public boolean linkEventToHearings(CourtCaseEntity courtCaseEntity, EventEntity eventEntity) {
        return linkEntityToHearing(
            "event",
            eventEntity,
            courtCaseEntity,
            eventEntity.getCourtroom(),
            eventEntity.getTimestamp().toLocalDate(),
            HearingEntity::addEvent
        );
    }

    <T extends HasLongId> boolean linkEntityToHearing(
        String type,
        T entity,
        CourtCaseEntity courtCaseEntity,
        CourtroomEntity courtroom,
        LocalDate hearingDate,
        BiConsumer<HearingEntity, T> linker
    ) {
        if (courtCaseEntity == null) {
            log.info("Can not link hearing to {} {} as CourtCaseEntity is null", type, entity.getId());
            return false;
        }

        Optional<HearingEntity> hearingEntityOptional = hearingRepository
            .findHearing(
                courtCaseEntity,
                courtroom,
                hearingDate
            );

        if (hearingEntityOptional.isEmpty()) {
            log.info("Can not link hearing to {} {} as no hearings could be found for cas_id {}, ctr_id {}, and Date {}",
                     type,
                     courtCaseEntity.getId(),
                     courtroom.getId(),
                     hearingDate
            );
            return false;
        }
        HearingEntity hearing = hearingEntityOptional.get();
        log.debug("Linking {} {} to hearing {}",
                  type,
                  entity.getId(),
                  hearing.getId()
        );
        linker.accept(hearing, entity);
        hearing.setHearingIsActual(true);
        hearingRepository.saveAndFlush(hearing);
        return true;
    }
}
