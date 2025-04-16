package uk.gov.hmcts.darts.task.runner.dailylist.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.dailylist.model.Charge;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;
import uk.gov.hmcts.darts.dailylist.model.CourtHouse;
import uk.gov.hmcts.darts.dailylist.model.CourtList;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.Defendant;
import uk.gov.hmcts.darts.dailylist.model.Hearing;
import uk.gov.hmcts.darts.dailylist.model.PersonalDetails;
import uk.gov.hmcts.darts.dailylist.model.Sitting;
import uk.gov.hmcts.darts.task.runner.dailylist.govtalk.people.addressandpersonaldetails.CitizenNameStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.AdvocateStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.ChargeStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.CourtHouseStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.DailyCourtListStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.DailyListStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.DefenceStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.DefendantStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.HearingStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.JudiciaryStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.PersonStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.PersonalDetailsStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.ProsecutingAuthorityType;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.SittingStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.DateUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.OverloadMethodsDeclarationOrder",
    "PMD.TooManyMethods",
    "PMD.ReturnEmptyCollectionRatherThanNull"})
public interface DailyListRequestMapper {

    @Mappings({
        @Mapping(source = "listHeader.publishedTime", target = "listHeader.publishedTime", qualifiedByName = "XMLGregorianCalendarToOffsetDateTime"),
        @Mapping(source = "documentID", target = "documentId"),
        @Mapping(source = "documentID.uniqueID", target = "documentId.uniqueId"),
        @Mapping(source = "documentID.timeStamp", target = "documentId.timeStamp", qualifiedByName = "XMLGregorianCalendarToOffsetDateTime"),
        @Mapping(source = "crownCourt.courtHouseCode.value", target = "crownCourt.courtHouseCode.code")
    })
    DailyListJsonObject mapToEntity(DailyListStructure dailyListStructure);

    @Named("XMLGregorianCalendarToOffsetDateTime")
    default OffsetDateTime toOffsetDateTime(XMLGregorianCalendar date) {
        return DateUtil.toOffsetDateTime(date);
    }

    default List<CourtList> map(DailyListStructure.CourtLists courtlists) {
        if (courtlists == null) {
            return null;
        }
        return CollectionUtils.emptyIfNull(courtlists.getCourtList()).stream().map(this::map).toList();
    }

    CourtList map(DailyCourtListStructure dailyCourtListStructure);

    default List<Sitting> map(DailyCourtListStructure.Sittings sittings) {
        if (sittings == null) {
            return null;
        }
        return CollectionUtils.emptyIfNull(sittings.getSitting()).stream().map(this::map).toList();
    }

    Sitting map(SittingStructure dailyCourtListStructure);

    default List<CitizenName> map(JudiciaryStructure value) {
        return List.of(map(value.getJudge()));
    }

    @Mappings({
        @Mapping(source = "citizenNameForename", target = "citizenNameForename", qualifiedByName = "nameMapper")
    })
    CitizenName map(JudiciaryStructure.Judge judge);

    default List<Hearing> map(SittingStructure.Hearings hearings) {
        if (hearings == null) {
            return null;
        }
        return CollectionUtils.emptyIfNull(hearings.getHearing()).stream().map(this::map).toList();
    }


    @Mappings({
        @Mapping(source = "prosecution.advocate", target = "prosecution.advocates")
    })
    Hearing map(HearingStructure hearing);

    default String map(ProsecutingAuthorityType prosecutingAuthorityType) {
        if (prosecutingAuthorityType == null) {
            return null;
        }
        return prosecutingAuthorityType.value();
    }

    default PersonalDetails map(AdvocateStructure advocate) {
        return map(advocate.getPersonalDetails());
    }

    default PersonalDetails map(PersonalDetailsStructure personalDetailsStructure) {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setIsMasked(BooleanUtils.toBoolean(personalDetailsStructure.getIsMasked().toString()));
        personalDetails.name(map(personalDetailsStructure.getName()));
        return personalDetails;
    }

    @Mappings({
        @Mapping(source = "citizenNameForename", target = "citizenNameForename", qualifiedByName = "nameMapper")
    })
    CitizenName map(CitizenNameStructure citizenNameStructure);

    PersonalDetails map(PersonStructure personStructure);

    default List<Defendant> map(HearingStructure.Defendants defendants) {
        if (defendants == null) {
            return null;
        }
        return CollectionUtils.emptyIfNull(defendants.getDefendant()).stream().map(this::map).toList();
    }

    @Mappings({
        @Mapping(source = "URN", target = "urn")
    })
    Defendant map(DefendantStructure defendant);

    default List<Charge> map(DefendantStructure.Charges charges) {
        if (charges == null) {
            return null;
        }
        return CollectionUtils.emptyIfNull(charges.getCharge()).stream().map(this::map).toList();
    }

    Charge map(ChargeStructure defendant);

    default List<PersonalDetails> map(List<DefenceStructure> defenceStructureList) {
        List<PersonalDetails> list = new ArrayList<>();
        for (DefenceStructure defenceStructure : defenceStructureList) {
            list.addAll(defenceStructure.getAdvocate().stream().map(this::map).toList());
            list.addAll(defenceStructure.getSolicitor().stream().map(solicitor -> map(solicitor.getParty().getPerson())).toList());
        }
        return list;
    }

    @Mappings({
        @Mapping(source = "courtHouseCode.value", target = "courtHouseCode.code")
    })
    CourtHouse map(CourtHouseStructure courtHouseStructure);

    @Named("nameMapper")
    default String mapName(List<String> names) {
        return String.join(" ", names);
    }

}
