package uk.gov.hmcts.darts.dailylist.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.dailylist.mapper.CitizenNameMapper;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class CitizenNameComparator implements Comparator<CitizenName> {

    private final CitizenNameMapper mapper;

    @Override
    public int compare(CitizenName o1, CitizenName o2) {

        // for now just use a string comparator to determine equality
        return mapper.getCitizenName(o1).compareTo(mapper.getCitizenName(o2));
    }
}