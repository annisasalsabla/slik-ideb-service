package co.id.skyworx.slikideb.repository.custom;

import co.id.skyworx.slikideb.entity.IdebReport;
import co.id.skyworx.slikideb.entity.QIdebReport;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Custom QueryDSL repository for dynamic, type-safe IDEB report queries.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class IdebReportQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * Executes dynamic search using optional query predicates.
     */
    public Page<IdebReport> searchReports(
            String nasabahName,
            String nik,
            String statusKredit,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        QIdebReport report = QIdebReport.idebReport;
        BooleanBuilder builder = new BooleanBuilder();

        // BooleanBuilder safely ignores null predicates, allowing modular predicate composition
        builder.and(buildNasabahNamePredicate(report, nasabahName));
        builder.and(buildNikPredicate(report, nik));
        builder.and(buildStatusKreditPredicate(report, statusKredit));
        builder.and(buildDateRangePredicate(report, startDate, endDate));

        log.debug("QueryDSL predicate: {}", builder.getValue());

        long total = queryFactory
                .selectFrom(report)
                .where(builder)
                .fetchCount();

        List<IdebReport> results = queryFactory
                .selectFrom(report)
                .where(builder)
                .orderBy(report.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(results, pageable, total);
    }

    private com.querydsl.core.types.Predicate buildNasabahNamePredicate(
            QIdebReport report, String nasabahName) {
        if (nasabahName == null || nasabahName.isBlank()) return null;
        return report.nasabahName.containsIgnoreCase(nasabahName);
    }

    private com.querydsl.core.types.Predicate buildNikPredicate(
            QIdebReport report, String nik) {
        if (nik == null || nik.isBlank()) return null;
        return report.nik.eq(nik);
    }

    private com.querydsl.core.types.Predicate buildStatusKreditPredicate(
            QIdebReport report, String statusKredit) {
        if (statusKredit == null || statusKredit.isBlank()) return null;
        return report.statusKredit.eq(statusKredit);
    }

    private com.querydsl.core.types.Predicate buildDateRangePredicate(
            QIdebReport report, LocalDate startDate, LocalDate endDate) {
        BooleanBuilder dateBuilder = new BooleanBuilder();
        if (startDate != null) {
            dateBuilder.and(report.createdAt.goe(startDate.atStartOfDay()));
        }
        if (endDate != null) {
            dateBuilder.and(report.createdAt.loe(endDate.atTime(23, 59, 59)));
        }
        return dateBuilder.hasValue() ? dateBuilder : null;
    }
}
