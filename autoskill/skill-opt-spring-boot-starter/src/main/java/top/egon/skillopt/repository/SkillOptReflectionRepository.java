package top.egon.skillopt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.skillopt.entity.SkillOptReflectionEntity;

/**
 * Repository for reflection records.
 */
public interface SkillOptReflectionRepository
    extends JpaRepository<SkillOptReflectionEntity, Long> {
}
