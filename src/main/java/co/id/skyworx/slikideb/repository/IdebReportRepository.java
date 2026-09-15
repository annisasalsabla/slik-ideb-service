package co.id.skyworx.slikideb.repository;

import co.id.skyworx.slikideb.entity.IdebReport;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for IdebReport entities with QueryDSL predicate support.
 */
@Repository
public interface IdebReportRepository extends JpaRepository<IdebReport, Long>,
        QuerydslPredicateExecutor<IdebReport> {

    Optional<IdebReport> findByRequestId(String requestId);

    Page<IdebReport> findByNasabahNameContainingIgnoreCase(String nasabahName, Pageable pageable);
}
