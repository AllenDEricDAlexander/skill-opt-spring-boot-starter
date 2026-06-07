package top.egon.skillopt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.skillopt.entity.SkillOptRoundEntity;

import java.util.Optional;

/**
 * Repository for optimization round records.
 */
public interface SkillOptRoundRepository extends JpaRepository<SkillOptRoundEntity, Long> {

  Optional<SkillOptRoundEntity> findByRoundId(String roundId);
}
