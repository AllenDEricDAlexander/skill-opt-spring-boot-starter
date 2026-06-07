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
 * Stores one tool call captured during a rollout.
 */
@Data
@Entity
@Table(name = "skill_opt_tool_call",
    indexes = { @Index(name = "idx_skill_opt_tool_call_rollout_id", columnList = "rollout_id"),
        @Index(name = "idx_skill_opt_tool_call_tool_name", columnList = "tool_name") })
public class SkillOptToolCallEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "rollout_id", nullable = false)
  private Long rolloutId;

  @Column(name = "round_id", nullable = false)
  private String roundId;

  @Column(name = "sequence_no", nullable = false)
  private Integer sequenceNo;

  @Column(name = "tool_call_id")
  private String toolCallId;

  @Column(name = "tool_name", nullable = false)
  private String toolName;

  @Column(name = "arguments", columnDefinition = "text")
  private String arguments;

  @Column(name = "result", columnDefinition = "text")
  private String result;

  @Column(name = "error", columnDefinition = "text")
  private String error;

  @Column(name = "duration_millis")
  private Long durationMillis;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

}
