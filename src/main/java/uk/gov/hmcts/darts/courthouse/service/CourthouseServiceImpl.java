package uk.gov.hmcts.darts.courthouse.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;

import java.util.List;

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
    public Courthouse amendCourthouseById(uk.gov.hmcts.darts.courthouse.model.Courthouse courthouse, Integer id) {
        Courthouse originalEntity = repository.getReferenceById(id);

        originalEntity.setCourthouseName(courthouse.getCourthouseName());
        originalEntity.setCode(courthouse.getCode());

        return repository.saveAndFlush(originalEntity);
    }

    @Override
    public Courthouse getCourtHouseById(Integer id) {
        return repository.getReferenceById(id);
    }

    @Override
    public List<Courthouse> getAllCourthouses() {
        return repository.findAll();
    }

    @Override
    public Courthouse addCourtHouse(uk.gov.hmcts.darts.courthouse.model.Courthouse courthouse) {
        Courthouse mappedEntity = this.mapper.mapToEntity(courthouse);
        return repository.saveAndFlush(mappedEntity);
    }
}
