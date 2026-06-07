package top.egon.skillopt.autoconfigure;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.skillopt.entity.SkillOptRoundEntity;
import top.egon.skillopt.repository.SkillOptEditOperationRepository;
import top.egon.skillopt.repository.SkillOptReflectionRepository;
import top.egon.skillopt.repository.SkillOptRolloutRepository;
import top.egon.skillopt.repository.SkillOptRoundRepository;
import top.egon.skillopt.repository.SkillOptToolCallRepository;
import top.egon.skillopt.storage.JpaSkillOptTraceStore;
import top.egon.skillopt.storage.SkillOptTraceStore;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures the dedicated SQLite-backed JPA store for SkillOpt traces.
 */
@AutoConfiguration
@ConditionalOnClass({ JpaRepository.class, org.sqlite.JDBC.class })
@EnableConfigurationProperties(SkillOptSqliteProperties.class)
@EnableJpaRepositories(basePackageClasses = SkillOptRoundRepository.class,
    entityManagerFactoryRef = "skillOptEntityManagerFactory",
    transactionManagerRef = "skillOptTransactionManager")
public class SkillOptSqliteAutoConfiguration {

  /**
   * Creates the SQLite datasource used only by the SkillOpt starter.
   */
  @Bean("skillOptDataSource")
  @ConditionalOnMissingBean(name = "skillOptDataSource")
  public DataSource skillOptDataSource(SkillOptSqliteProperties properties) throws IOException {
    Path database = properties.getDatabase();
    Path parent = database.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.sqlite.JDBC");
    dataSource.setUrl("jdbc:sqlite:" + database);
    return dataSource;
  }

  /**
   * Creates an isolated entity manager for SkillOpt entities.
   */
  @Bean("skillOptEntityManagerFactory")
  @ConditionalOnMissingBean(name = "skillOptEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean skillOptEntityManagerFactory(
      @Qualifier("skillOptDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean entityManagerFactory =
        new LocalContainerEntityManagerFactoryBean();
    entityManagerFactory.setDataSource(dataSource);
    entityManagerFactory.setPackagesToScan(SkillOptRoundEntity.class.getPackageName());
    entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    entityManagerFactory.setJpaPropertyMap(jpaProperties());
    entityManagerFactory.setPersistenceUnitName("skillOpt");
    return entityManagerFactory;
  }

  /**
   * Creates the transaction manager used by SkillOpt repositories.
   */
  @Bean("skillOptTransactionManager")
  @ConditionalOnMissingBean(name = "skillOptTransactionManager")
  public PlatformTransactionManager skillOptTransactionManager(
      @Qualifier("skillOptEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }

  /**
   * Creates the repository-backed trace store used by optimization flows.
   */
  @Bean
  @ConditionalOnMissingBean
  public SkillOptTraceStore skillOptTraceStore(SkillOptRoundRepository roundRepository,
      SkillOptRolloutRepository rolloutRepository, SkillOptToolCallRepository toolCallRepository,
      SkillOptReflectionRepository reflectionRepository,
      SkillOptEditOperationRepository editOperationRepository) {
    return new JpaSkillOptTraceStore(roundRepository, rolloutRepository, toolCallRepository,
        reflectionRepository, editOperationRepository);
  }

  private Map<String, Object> jpaProperties() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.hbm2ddl.auto", "update");
    properties.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
    properties.put("hibernate.show_sql", "false");
    return properties;
  }
}
