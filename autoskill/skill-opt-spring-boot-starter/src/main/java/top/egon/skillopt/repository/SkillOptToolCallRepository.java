package top.egon.skillopt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.skillopt.entity.SkillOptToolCallEntity;

/**
 * Repository for tool call records.
 */
public interface SkillOptToolCallRepository extends JpaRepository<SkillOptToolCallEntity, Long> {
}
