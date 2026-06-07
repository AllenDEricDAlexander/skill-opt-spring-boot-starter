package top.egon.skillopt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

/**
 * Stores optimizer reflection output for one optimization round.
 */
@Data
@Entity
@Table(name = "skill_opt_reflection")
public class SkillOptReflectionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "round_id", nullable = false, unique = true)
  private String roundId;

  @Column(name = "summary", columnDefinition = "text")
  private String summary;

  @Column(name = "recommendations_json", columnDefinition = "text")
  private String recommendationsJson;

  @Column(name = "request_snapshot", columnDefinition = "text")
  private String requestSnapshot;

  @Column(name = "response_snapshot", columnDefinition = "text")
  private String responseSnapshot;

  @Column(name = "model_name")
  private String modelName;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

}
