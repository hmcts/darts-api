package uk.gov.hmcts.darts.common.repository.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
@SuppressWarnings({"InterfaceTypeParameterName"})
public interface ReadOnlyRepository<T, I> extends Repository<T, I> {

    List<T> findAll();

    List<T> findAll(Sort sort);

    Page<T> findAll(Pageable pageable);

    Optional<T> findById(I id);

    long count();
}
