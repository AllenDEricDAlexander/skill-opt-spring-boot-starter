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
 * Stores one complete skill optimization round.
 */
@Data
@Entity
@Table(name = "skill_opt_round",
    indexes = { @Index(name = "idx_skill_opt_round_skill_name", columnList = "skill_name"),
        @Index(name = "idx_skill_opt_round_status", columnList = "gate_status") })
public class SkillOptRoundEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "round_id", nullable = false, unique = true)
  private String roundId;

  @Column(name = "skill_name", nullable = false)
  private String skillName;

  @Column(name = "base_skill_file", nullable = false)
  private String baseSkillFile;

  @Column(name = "base_skill_hash", nullable = false)
  private String baseSkillHash;

  @Column(name = "base_skill_backup_file")
  private String baseSkillBackupFile;

  @Column(name = "candidate_skill_file")
  private String candidateSkillFile;

  @Column(name = "candidate_skill_hash")
  private String candidateSkillHash;

  @Column(name = "best_skill_file")
  private String bestSkillFile;

  @Column(name = "baseline_score")
  private Double baselineScore;

  @Column(name = "candidate_score")
  private Double candidateScore;

  @Column(name = "min_validation_improvement", nullable = false)
  private Double minValidationImprovement;

  @Column(name = "accepted", nullable = false)
  private Boolean accepted;

  @Column(name = "gate_status", nullable = false)
  private String gateStatus;

  @Column(name = "gate_reason", columnDefinition = "text")
  private String gateReason;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

}
