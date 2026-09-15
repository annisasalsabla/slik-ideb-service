package co.id.skyworx.slikideb.repository;

import co.id.skyworx.slikideb.entity.IdebFailureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IdebFailureLogRepository extends JpaRepository<IdebFailureLog, Long> {
    List<IdebFailureLog> findByRequestIdOrderByCreatedAtDesc(String requestId);
    List<IdebFailureLog> findAllByOrderByCreatedAtDesc();
}
