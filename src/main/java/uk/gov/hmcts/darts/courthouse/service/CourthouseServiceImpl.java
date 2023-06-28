package uk.gov.hmcts.darts.courthouse.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
@Service
@Slf4j
public class CourthouseServiceImpl implements CourthouseService {

    private CourthouseRepository repository;

    private CourthouseToCourthouseEntityMapper mapper;

    @Override
    public void deleteCourthouseById(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public CourthouseEntity amendCourthouseById(uk.gov.hmcts.darts.courthouse.model.Courthouse courthouse, Integer id) {
        CourthouseEntity originalEntity = repository.getReferenceById(id);

        originalEntity.setCourthouseName(courthouse.getCourthouseName());
        originalEntity.setCode(courthouse.getCode());

        return repository.saveAndFlush(originalEntity);
    }

    @Override
    public CourthouseEntity getCourtHouseById(Integer id) {
        return repository.getReferenceById(id);
    }

    @Override
    public List<CourthouseEntity> getAllCourthouses() {
        return repository.findAll();
    }

    @Override
    public CourthouseEntity addCourtHouse(uk.gov.hmcts.darts.courthouse.model.Courthouse courthouse) {
        CourthouseEntity mappedEntity = this.mapper.mapToEntity(courthouse);
        return repository.saveAndFlush(mappedEntity);
    }


    /**
     * retrieves the courtroom from the database. If the database doesn't have the code, then it will insert it.
     *
     * @param courthouseCode Optional parameter. If it is not provided, then name will be used by itself.
     * @param courthouseName Name of the courthouse to search for.
     * @return the found courtroom
     * @throws CourthouseNameNotFoundException when the courthouse isn't found
     * @throws CourthouseCodeNotMatchException when the courtroom is found, but it has a different code that expected.
     */
    @Override
    @SuppressWarnings("PMD.UselessParentheses")
    public CourthouseEntity retrieveAndUpdateCourtHouse(Integer courthouseCode, String courthouseName)
        throws CourthouseNameNotFoundException, CourthouseCodeNotMatchException {
        CourthouseEntity foundCourthouse = retrieveCourthouse(courthouseCode, courthouseName);
        if (foundCourthouse.getCode() == null && courthouseCode != null) {
            //update courthouse in database with new code
            foundCourthouse.setCode(courthouseCode);
            repository.saveAndFlush(foundCourthouse);
        } else {
            if (!StringUtils.equalsIgnoreCase(foundCourthouse.getCourthouseName(), courthouseName)
                || (courthouseCode != null && !Objects.equals(courthouseCode, foundCourthouse.getCode()))) {
                throw new CourthouseCodeNotMatchException(foundCourthouse, courthouseCode, courthouseName);
            }
        }
        return foundCourthouse;
    }

    private CourthouseEntity retrieveCourthouse(Integer courthouseCode, String courthouseName) throws CourthouseNameNotFoundException {
        String courthouseNameUC = StringUtils.upperCase(courthouseName);
        Optional<CourthouseEntity> courthouseOptional = Optional.empty();
        if (courthouseCode != null) {
            courthouseOptional = repository.findByCode(courthouseCode.shortValue());
        }
        if (courthouseOptional.isEmpty()) {
            //code not found, lookup name instead
            courthouseOptional = repository.findByCourthouseName(courthouseNameUC);
            if (courthouseOptional.isEmpty()) {
                throw new CourthouseNameNotFoundException(courthouseNameUC);
            }
        }
        return courthouseOptional.get();
    }
}
