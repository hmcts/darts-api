package uk.gov.hmcts.darts.cases.mapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.SQLQueryModel;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AdvancedSearchRequestMapper {


    /*
    select court_case.cas_id
    from court_case, hearing, courthouse, courtroom
    where courtroom.ctr_id = hearing.ctr_id
    and court_case.cth_id = courthouse.cth_id
    and hearing.cas_id = court_case.cas_id
    and hearing.ctr_id = courtroom.ctr_id
    and upper(court_case.case_number) like upper('%0001%')
    and upper(courthouse.courthouse_name) like upper('%%')
    and upper(courtroom.courtroom_name) like upper('%%')
    and hearing.hearing_date >= date('2023-06-15')


     */
    private boolean joinHearing = false;
    private boolean joinCourtroom = false;
    private boolean joinCourthouse = false;
    private boolean joinJudges = false;
    private boolean joinDefendants = false;
    private boolean joinEvents = false;

    SQLQueryModel sqlModel = new SQLQueryModel();

    public String mapToSQL(GetCasesSearchRequest request) {
        sqlModel.addSelectColumn("distinct(court_case.cas_id)");
        sqlModel.addTable(CaseEntity.TABLE_NAME);
        addCaseCriteria(request);
        addCourtroomCriteria(request);
        addCourthouseCriteria(request);
        addHearingCriteria(request);
        addDefendantCriteria(request);
        addEventCriteria(request);


        if (joinJudges) {
            sqlModel.addTable("( select cas_id,unnest(judge_list) as judge from hearing) as judges");
            sqlModel.addWhereCriteria("judges.cas_id = " + CaseEntity.TABLE_NAME + ".cas_id");
        }
        if (joinDefendants) {
            sqlModel.addTable("( select cas_id,unnest(defendant_list) as defendant from hearing) as defendants");
            sqlModel.addWhereCriteria("defendants.cas_id = " + CaseEntity.TABLE_NAME + ".cas_id");
        }
        if (joinHearing) {
            sqlModel.addTable("hearing");
            sqlModel.addWhereCriteria("hearing.cas_id = " + CaseEntity.TABLE_NAME + ".cas_id");
        }
        if (joinCourtroom) {
            sqlModel.addTable("courtroom");
            sqlModel.addWhereCriteria("hearing.hea_id = " + CourtroomEntity.TABLE_NAME + ".hea_id");
        }
        if (joinCourthouse) {
            sqlModel.addTable("courthouse");
            sqlModel.addWhereCriteria("courthouse.cth_id = " + CaseEntity.TABLE_NAME + ".cth_id");
        }
        if (joinEvents) {
            sqlModel.addTable("hearing_event_ae");
            sqlModel.addTable("event");
            sqlModel.addTable("event_handler");
            sqlModel.addWhereCriteria("hearing_event_ae.hea_id = " + HearingEntity.TABLE_NAME + ".hea_id");
            sqlModel.addWhereCriteria("hearing_event_ae.eve_id = " + "event.eve_id");
            sqlModel.addWhereCriteria("event_handler.evh_id = " + "event.evh_id");
        }
        return sqlModel.toString();

    }

    private void addCaseCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            String where = MessageFormat.format(
                "upper(court_case.case_number) like upper(''%{0}%'')",
                request.getCaseNumber()
            );
            sqlModel.addWhereCriteria(where);
        }
    }

    private void addCourtroomCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getCourtroom())) {
            joinCourtroom = true;
            joinHearing = true;
            String where = MessageFormat.format(
                "upper(courtroom.courtroom_name) like upper(''%{0}%'')",
                request.getCourtroom()
            );
            sqlModel.addWhereCriteria(where);
        }
    }

    private void addCourthouseCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getCourthouse())) {
            joinCourthouse = true;
            String where = MessageFormat.format(
                "upper(courthouse.courthouse_name) like upper(''%{0}%'')",
                request.getCourthouse()
            );
            sqlModel.addWhereCriteria(where);
        }
    }

    private void addHearingCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getJudgeName())) {
            joinHearing = true;
            joinJudges = true;

            String where = MessageFormat.format(
                "upper(judges.judge) like upper(''%{0}%'')",
                request.getJudgeName()
            );
            sqlModel.addWhereCriteria(where);
        }
        addHearingDateCriteria(request);

    }

    private void addDefendantCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getDefendantName())) {
            joinHearing = true;
            joinDefendants = true;

            String where = MessageFormat.format(
                "upper(defendants.defendant) like upper(''%{0}%'')",
                request.getDefendantName()
            );
            sqlModel.addWhereCriteria(where);
        }
    }

    private void addEventCriteria(GetCasesSearchRequest request) {
        if (StringUtils.isNotBlank(request.getKeywords())) {
            joinHearing = true;
            joinEvents = true;

            String[] keywords = request.getKeywords().split(" ");
            List<String> whereClauses = new ArrayList<>();
            for (String keyword : keywords) {
                String where = MessageFormat.format(
                    "upper(event.event_text) like upper(''%{0}%'') " +
                        "or upper(event_handler.event_name) like upper(''%{0}%'')",
                    keyword
                );
                whereClauses.add(where);
            }
            sqlModel.addWhereCriteria("(" + String.join("\n OR ", whereClauses) + ")");
        }
    }

    private void addHearingDateCriteria(GetCasesSearchRequest request) {
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            joinHearing = true;
            if (request.getDateFrom() != null) {
                String where = MessageFormat.format(
                    "hearing.hearing_date >= date(''{0}'')",
                    request.getDateFrom().toString()
                );
                sqlModel.addWhereCriteria(where);
            }
            if (request.getDateTo() != null) {
                String where = MessageFormat.format(
                    "hearing.hearing_date <= date(''{0}'')",
                    request.getDateTo().toString()
                );
                sqlModel.addWhereCriteria(where);
            }
        }
    }


}
