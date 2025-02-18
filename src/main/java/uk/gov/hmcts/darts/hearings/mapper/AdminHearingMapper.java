package uk.gov.hmcts.darts.hearings.mapper;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.hearings.model.HearingsResponse;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseCase;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseCaseCourthouse;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseCourtroom;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class AdminHearingMapper {

    public static HearingsResponse mapToHearingsResponse(HearingEntity hearingEntity) {
        if (hearingEntity == null) {
            return null;
        }
        HearingsResponse hearingsResponse = new HearingsResponse();
        hearingsResponse.setId(hearingEntity.getId());
        hearingsResponse.setHearingDate(hearingEntity.getHearingDate());
        hearingsResponse.setHearingIsActual(hearingEntity.getHearingIsActual());
        hearingsResponse.setCase(mapToHearingsResponseCase(hearingEntity.getCourtCase()));
        hearingsResponse.setCourtroom(mapToCourtroom(hearingEntity.getCourtroom()));
        hearingsResponse.setJudges(asList(hearingEntity.getJudges(), entity -> entity.getName()));
        hearingsResponse.setCreatedAt(hearingEntity.getCreatedDateTime());
        hearingsResponse.setCreatedBy(hearingEntity.getCreatedById());
        hearingsResponse.setLastModifiedAt(hearingEntity.getLastModifiedDateTime());
        hearingsResponse.setLastModifiedBy(hearingEntity.getLastModifiedById());
        return hearingsResponse;
    }

    public static HearingsResponseCase mapToHearingsResponseCase(CourtCaseEntity courtCaseEntity) {
        if (courtCaseEntity == null) {
            return null;
        }
        HearingsResponseCase hearingsResponseCase = new HearingsResponseCase();
        hearingsResponseCase.setId(courtCaseEntity.getId());
        hearingsResponseCase.setCaseNumber(courtCaseEntity.getCaseNumber());
        hearingsResponseCase.setCourthouse(mapToCourtHouse(courtCaseEntity.getCourthouse()));
        hearingsResponseCase.setDefendants(asList(courtCaseEntity.getDefendantList(), entity -> entity.getName()));
        hearingsResponseCase.setProsecutors(asList(courtCaseEntity.getProsecutorList(), entity -> entity.getName()));
        hearingsResponseCase.setDefenders(asList(courtCaseEntity.getDefenceList(), entity -> entity.getName()));
        hearingsResponseCase.setJudges(asList(courtCaseEntity.getJudges(), entity -> entity.getName()));
        return hearingsResponseCase;
    }

    public static HearingsResponseCaseCourthouse mapToCourtHouse(CourthouseEntity courthouseEntity) {
        if (courthouseEntity == null) {
            return null;
        }
        HearingsResponseCaseCourthouse courthouse = new HearingsResponseCaseCourthouse();
        courthouse.setId(courthouseEntity.getId());
        courthouse.setDisplayName(courthouseEntity.getDisplayName());
        return courthouse;
    }

    public static HearingsResponseCourtroom mapToCourtroom(CourtroomEntity courtroomEntity) {
        if (courtroomEntity == null) {
            return null;
        }
        HearingsResponseCourtroom courtroom = new HearingsResponseCourtroom();
        courtroom.setId(courtroomEntity.getId());
        courtroom.setName(courtroomEntity.getName());
        return courtroom;
    }

    
    public static <R, P> List<R> asList(List<P> data, Function<P, R> mapperFunction) {
        if (data == null) {
            return new ArrayList<>();
        }
        return data.stream()
            .filter(p -> p != null)
            .map(p -> mapperFunction.apply(p))
            .toList();
    }

    public static String toTimeString(OffsetDateTime offsetDateTime) {
        return toTimeString(
            Optional.ofNullable(offsetDateTime)
                .map(time -> time.toLocalTime())
                .orElse(null));
    }

    public static String toTimeString(LocalTime localTime) {
        return Optional.ofNullable(localTime)
            .map(time -> time.format(DateTimeFormatter.ISO_LOCAL_TIME))
            .orElse(null);
    }
}
