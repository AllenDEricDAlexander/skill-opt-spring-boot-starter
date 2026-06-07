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
 * Stores one bounded add/delete/replace edit for a candidate skill.
 */
@Data
@Entity
@Table(name = "skill_opt_edit_operation",
    indexes = { @Index(name = "idx_skill_opt_edit_operation_round_id", columnList = "round_id") })
public class SkillOptEditOperationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "round_id", nullable = false)
  private String roundId;

  @Column(name = "sequence_no", nullable = false)
  private Integer sequenceNo;

  @Column(name = "edit_type", nullable = false)
  private String editType;

  @Column(name = "target", columnDefinition = "text")
  private String target;

  @Column(name = "content", columnDefinition = "text")
  private String content;

  @Column(name = "applied", nullable = false)
  private Boolean applied;

  @Column(name = "error", columnDefinition = "text")
  private String error;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
