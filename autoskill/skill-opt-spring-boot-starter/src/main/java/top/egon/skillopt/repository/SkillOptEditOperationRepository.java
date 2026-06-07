package top.egon.skillopt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.skillopt.entity.SkillOptEditOperationEntity;

/**
 * Repository for bounded edit operation records.
 */
public interface SkillOptEditOperationRepository
    extends JpaRepository<SkillOptEditOperationEntity, Long> {
}
