package top.egon.skillopt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

/**
 * Stores one target-model rollout against one case and one skill version.
 */
@Data
@Entity
@Table(name = "skill_opt_rollout",
    indexes = { @Index(name = "idx_skill_opt_rollout_round_id", columnList = "round_id"),
        @Index(name = "idx_skill_opt_rollout_case", columnList = "case_id, case_split"),
        @Index(name = "idx_skill_opt_rollout_skill_hash", columnList = "skill_hash") })
public class SkillOptRolloutEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "round_id", nullable = false)
  private String roundId;

  @Column(name = "phase", nullable = false)
  private String phase;

  @Column(name = "case_id", nullable = false)
  private String caseId;

  @Column(name = "case_split", nullable = false)
  private String caseSplit;

  @Column(name = "input", nullable = false, columnDefinition = "text")
  private String input;

  @Column(name = "expected", columnDefinition = "text")
  private String expected;

  @Column(name = "skill_file", nullable = false)
  private String skillFile;

  @Column(name = "skill_hash", nullable = false)
  private String skillHash;

  @Column(name = "output", columnDefinition = "text")
  private String output;

  @Column(name = "score", nullable = false)
  private Double score;

  @Column(name = "passed", nullable = false)
  private Boolean passed;

  @Column(name = "error", columnDefinition = "text")
  private String error;

  @Column(name = "duration_millis", nullable = false)
  private Long durationMillis;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

}
