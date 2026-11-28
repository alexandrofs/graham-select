package afsdigital.grahamselect.api.cerrado;

import afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.entities.CerradoAssetGradeEntity;
import afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.repository.CerradoAssetGradeJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.cerrado.application.repository.CerradoAssetGradePort;
import afsdigital.grahamselect.common.cerrado.application.repository.CerradoPreferencesPort;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAssetGrade;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoPreferences;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CerradoPersistenceIT extends BaseRepositoryIT {

    @Autowired
    private CerradoPreferencesPort preferencesPort;

    @Autowired
    private CerradoAssetGradePort assetGradePort;

    @Autowired
    private CerradoAssetGradeJpaRepository assetGradeJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_cerrado_asset_grade");
        jdbcTemplate.update("DELETE FROM tb_cerrado_preferences");
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should persist and load CerradoPreferences with exact BigDecimal precision")
    void shouldPersistPreferencesWithExactDecimalPrecision() {
        User user = createUser("cerrado-pref-1", "pref1@example.com");

        CerradoPreferences saved = preferencesPort.save(CerradoPreferences.builder()
                .userId(user.getId())
                .targetAcoesBr(new BigDecimal("35.25"))
                .targetFiis(new BigDecimal("24.75"))
                .targetRendaFixa(new BigDecimal("25.00"))
                .targetInternacional(new BigDecimal("14.00"))
                .targetCripto(new BigDecimal("1.00"))
                .grahamMultiplier(new BigDecimal("1.30"))
                .grahamFilterEnabled(true)
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<CerradoPreferences> loaded = preferencesPort.findByUserId(user.getId());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getTargetAcoesBr()).isEqualByComparingTo("35.25");
        assertThat(loaded.get().getTargetFiis()).isEqualByComparingTo("24.75");
        assertThat(loaded.get().getTargetRendaFixa()).isEqualByComparingTo("25.00");
        assertThat(loaded.get().getTargetInternacional()).isEqualByComparingTo("14.00");
        assertThat(loaded.get().getTargetCripto()).isEqualByComparingTo("1.00");
        assertThat(loaded.get().getGrahamMultiplier()).isEqualByComparingTo("1.30");
        assertThat(loaded.get().isGrahamFilterEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should enforce unique user_id constraint on tb_cerrado_preferences")
    void shouldRejectDuplicatePreferencesPerUser() {
        User user = createUser("cerrado-pref-dup", "prefdup@example.com");

        CerradoPreferences first = CerradoPreferences.builder()
                .userId(user.getId())
                .targetAcoesBr(new BigDecimal("40.00"))
                .targetFiis(new BigDecimal("20.00"))
                .targetRendaFixa(new BigDecimal("25.00"))
                .targetInternacional(new BigDecimal("14.00"))
                .targetCripto(new BigDecimal("1.00"))
                .build();
        preferencesPort.save(first);

        CerradoPreferences duplicate = CerradoPreferences.builder()
                .userId(user.getId())
                .targetAcoesBr(new BigDecimal("50.00"))
                .targetFiis(new BigDecimal("20.00"))
                .targetRendaFixa(new BigDecimal("20.00"))
                .targetInternacional(new BigDecimal("10.00"))
                .targetCripto(new BigDecimal("0.00"))
                .build();

        assertThatThrownBy(() -> preferencesPort.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should persist CerradoAssetGrade, isolate by user_id, and reject duplicate (user_id, ticker)")
    void shouldPersistAndIsolateAssetGrades() {
        User user1 = createUser("cerrado-grade-1", "grade1@example.com");
        User user2 = createUser("cerrado-grade-2", "grade2@example.com");

        CerradoAssetGrade saved1 = assetGradePort.save(CerradoAssetGrade.builder()
                .userId(user1.getId())
                .ticker("ITUB4")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(9)
                .customRiskCeiling(new BigDecimal("8.50"))
                .quarantineCorporateEvent(false)
                .build());

        assetGradePort.save(CerradoAssetGrade.builder()
                .userId(user2.getId())
                .ticker("ITUB4")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(5)
                .build());

        assertThat(saved1.getId()).isNotNull();
        assertThat(saved1.getUpdatedAt()).isNotNull();

        List<CerradoAssetGrade> user1Grades = assetGradePort.findAllByUserId(user1.getId());
        assertThat(user1Grades).hasSize(1);
        assertThat(user1Grades.get(0).getQualityGrade()).isEqualTo(9);
        assertThat(user1Grades.get(0).getCustomRiskCeiling()).isEqualByComparingTo("8.50");

        assertThat(assetGradePort.findByUserIdAndTicker(user2.getId(), "ITUB4"))
                .isPresent()
                .get()
                .extracting(CerradoAssetGrade::getQualityGrade)
                .isEqualTo(5);

        CerradoAssetGrade duplicateForUser1 = CerradoAssetGrade.builder()
                .userId(user1.getId())
                .ticker("ITUB4")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_GROWTH)
                .qualityGrade(7)
                .build();

        assertThatThrownBy(() -> assetGradePort.save(duplicateForUser1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should reject JPA entity with qualityGrade outside 1..10 via Bean Validation")
    void shouldRejectInvalidQualityGradeOnEntityPersistence() {
        User user = createUser("cerrado-val", "val@example.com");

        CerradoAssetGradeEntity invalidLow = CerradoAssetGradeEntity.builder()
                .userId(user.getId())
                .ticker("BBAS3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(0)
                .build();

        assertThatThrownBy(() -> assetGradeJpaRepository.saveAndFlush(invalidLow))
                .isInstanceOf(ConstraintViolationException.class);

        CerradoAssetGradeEntity invalidHigh = CerradoAssetGradeEntity.builder()
                .userId(user.getId())
                .ticker("BBAS3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(11)
                .build();

        assertThatThrownBy(() -> assetGradeJpaRepository.saveAndFlush(invalidHigh))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Should cascade delete Cerrado rows when user is deleted")
    void shouldCascadeDeleteOnUserDeletion() {
        User user = createUser("cerrado-cascade", "cascade@example.com");

        preferencesPort.save(CerradoPreferences.builder()
                .userId(user.getId())
                .targetAcoesBr(new BigDecimal("40.00"))
                .targetFiis(new BigDecimal("20.00"))
                .targetRendaFixa(new BigDecimal("25.00"))
                .targetInternacional(new BigDecimal("14.00"))
                .targetCripto(new BigDecimal("1.00"))
                .build());

        assetGradePort.save(CerradoAssetGrade.builder()
                .userId(user.getId())
                .ticker("HGLG11")
                .macroClass(MacroClass.FIIS)
                .subClass(CerradoSubClass.FII_BRICK)
                .qualityGrade(8)
                .build());

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", user.getId());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tb_cerrado_preferences WHERE user_id = ?", Integer.class, user.getId()))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tb_cerrado_asset_grade WHERE user_id = ?", Integer.class, user.getId()))
                .isZero();
    }

    private User createUser(String sub, String email) {
        return userRepository.save(User.builder()
                .googleSub(sub)
                .email(email)
                .fullName("Cerrado Test User")
                .tier(SubscriptionTier.FREE)
                .build());
    }
}
