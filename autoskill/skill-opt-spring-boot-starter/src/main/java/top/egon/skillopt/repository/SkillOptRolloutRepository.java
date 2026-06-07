package top.egon.skillopt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.skillopt.entity.SkillOptRolloutEntity;

/**
 * Repository for rollout evidence records.
 */
public interface SkillOptRolloutRepository extends JpaRepository<SkillOptRolloutEntity, Long> {
}
