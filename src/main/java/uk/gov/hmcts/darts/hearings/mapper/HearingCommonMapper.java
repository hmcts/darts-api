package uk.gov.hmcts.darts.hearings.mapper;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
import uk.gov.hmcts.darts.common.entity.base.LastModifiedBy;
import uk.gov.hmcts.darts.hearings.model.Audit;
import uk.gov.hmcts.darts.hearings.model.Courthouse;
import uk.gov.hmcts.darts.hearings.model.Courtroom;
import uk.gov.hmcts.darts.hearings.model.Location;
import uk.gov.hmcts.darts.hearings.model.NameAndIdResponse;
import uk.gov.hmcts.darts.hearings.model.User;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.task.runner.IsNamedEntity;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class HearingCommonMapper {

    public static <R, P> List<R> asList(List<P> data, Function<P, R> mapperFunction) {
        if (data == null) {
            return new ArrayList<>();
        }
        return data.stream()
            .filter(p -> p != null)
            .map(p -> mapperFunction.apply(p))
            .toList();
    }


    public static <T extends IsNamedEntity & HasIntegerId> NameAndIdResponse mapToNameAndIdResponse(T isNamedEntity) {
        if (isNamedEntity == null) {
            return null;
        }
        NameAndIdResponse nameAndIdResponse = new NameAndIdResponse();
        nameAndIdResponse.setId(isNamedEntity.getId());
        nameAndIdResponse.setName(isNamedEntity.getName());
        return nameAndIdResponse;
    }

    public static <T extends LastModifiedBy & CreatedBy> Audit mapToAudit(T entity) {
        if (entity == null) {
            return null;
        }
        Audit audit = new Audit();
        audit.setCreatedBy(mapToUser(entity.getCreatedBy()));
        audit.setCreatedAt(entity.getCreatedDateTime());
        audit.setUpdatedBy(mapToUser(entity.getLastModifiedBy()));
        audit.setUpdatedAt(entity.getLastModifiedDateTime());
        return audit;
    }

    public static User mapToUser(UserAccountEntity userAccount) {
        if (userAccount == null) {
            return null;
        }
        User user = new User();
        user.setId(userAccount.getId());
        user.setName(userAccount.getUserFullName());
        return user;
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

    public static Location mapToLocation(CourtroomEntity courtroom) {
        if (courtroom == null) {
            return null;
        }
        Location location = new Location();
        location.setCourthouse(mapToCourthouse(courtroom.getCourthouse()));
        location.setCourtroom(mapToCourtroom(courtroom));
        return location;
    }

    public static Courtroom mapToCourtroom(CourtroomEntity courtroomEntity) {
        if(courtroomEntity == null) {
            return null;
        }
        Courtroom courtroom = new Courtroom();
        courtroom.setId(courtroomEntity.getId());
        courtroom.setName(courtroomEntity.getName());
        return courtroom;
    }

    public static Courthouse mapToCourthouse(CourthouseEntity courthouseEntity) {
        if(courthouseEntity == null){
            return null;
        }
        Courthouse courthouse = new Courthouse();
        courthouse.setId(courthouseEntity.getId());
        courthouse.setCode(courthouseEntity.getCode());
        courthouse.setName(courthouseEntity.getCourthouseName());
        courthouse.setDisplayName(courthouseEntity.getDisplayName());
        return courthouse;
    }
}
