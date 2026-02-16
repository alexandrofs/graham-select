package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class IntrinsicValueRepositoryImplIT extends BaseRepositoryIT {

        @Autowired
        private IntrinsicValueRepositoryImpl intrinsicValueRepository;

        @Autowired
        private IntrinsicValueJpaRepository intrinsicValueJpaRepository;

        @Autowired
        private CompanyJpaRepository companyJpaRepository;

        private String companyId;

        @BeforeEach
        void setUp() {
                // Clean up data before each test
                intrinsicValueJpaRepository.deleteAll();
                companyJpaRepository.deleteAll();

                // Create a company for the test
                CompanyEntity company = CompanyEntity.builder()
                                .id(UUID.randomUUID().toString())
                                .ticker("AAPL")
                                .name("Apple Inc.")
                                .build();
                CompanyEntity savedCompany = companyJpaRepository.save(company);
                companyId = savedCompany.getId();
        }

        @Test
        void shouldSaveIntrinsicValueSuccessfully() {
                // Arrange
                LocalDate calculationDate = LocalDate.of(2025, 12, 21);
                BigDecimal value = new BigDecimal("150.50");

                IntrinsicValue intrinsicValue = IntrinsicValue.builder()
                                .companyId(companyId)
                                .calculationDate(calculationDate)
                                .value(value)
                                .build();

                // Act
                intrinsicValueRepository.save(intrinsicValue);

                // Assert
                assertThat(intrinsicValueJpaRepository.findAll())
                                .hasSize(1)
                                .extracting(IntrinsicValueEntity::getCompanyId)
                                .contains(companyId);
        }

        @Test
        void shouldSaveIntrinsicValueWithAllFieldsPopulated() {
                // Arrange
                LocalDate calculationDate = LocalDate.of(2025, 12, 21);
                BigDecimal value = new BigDecimal("275.99");

                IntrinsicValue intrinsicValue = IntrinsicValue.builder()
                                .companyId(companyId)
                                .calculationDate(calculationDate)
                                .value(value)
                                .build();

                // Act
                intrinsicValueRepository.save(intrinsicValue);

                // Assert
                Optional<IntrinsicValueEntity> saved = intrinsicValueJpaRepository.findAll()
                                .stream()
                                .findFirst();

                assertThat(saved)
                                .isPresent()
                                .get()
                                .satisfies(entity -> {
                                        assertThat(entity.getId()).isNotNull();
                                        assertThat(entity.getCompanyId()).isEqualTo(companyId);
                                        assertThat(entity.getCalculationDate()).isEqualTo(calculationDate);
                                        assertThat(entity.getIntrinsicValue()).isEqualByComparingTo(value);
                                });
        }

        @Test
        void shouldGenerateUniqueIdForEachSave() {
                // Arrange
                LocalDate calculationDate = LocalDate.of(2025, 12, 21);
                BigDecimal value = new BigDecimal("150.50");

                IntrinsicValue intrinsicValue = IntrinsicValue.builder()
                                .companyId(companyId)
                                .calculationDate(calculationDate)
                                .value(value)
                                .build();

                // Act
                intrinsicValueRepository.save(intrinsicValue);
                intrinsicValueRepository.save(intrinsicValue);

                // Assert
                assertThat(intrinsicValueJpaRepository.findAll())
                                .hasSize(2)
                                .extracting(IntrinsicValueEntity::getId)
                                .doesNotHaveDuplicates();
        }

        @Test
        void shouldSaveMultipleIntrinsicValuesForSameCompany() {
                // Arrange
                LocalDate date1 = LocalDate.of(2025, 12, 21);
                LocalDate date2 = LocalDate.of(2025, 12, 22);
                BigDecimal value1 = new BigDecimal("150.50");
                BigDecimal value2 = new BigDecimal("160.75");

                IntrinsicValue intrinsicValue1 = IntrinsicValue.builder()
                                .companyId(companyId)
                                .calculationDate(date1)
                                .value(value1)
                                .build();

                IntrinsicValue intrinsicValue2 = IntrinsicValue.builder()
                                .companyId(companyId)
                                .calculationDate(date2)
                                .value(value2)
                                .build();

                // Act
                intrinsicValueRepository.save(intrinsicValue1);
                intrinsicValueRepository.save(intrinsicValue2);

                // Assert
                assertThat(intrinsicValueJpaRepository.findAll())
                                .hasSize(2)
                                .allMatch(entity -> entity.getCompanyId().equals(companyId))
                                .extracting(IntrinsicValueEntity::getCalculationDate)
                                .containsExactlyInAnyOrder(date1, date2);
        }

        @Test
        void shouldPersistBigDecimalPrecision() {
                // Arrange
                BigDecimal preciseValue = new BigDecimal("123.45");

                IntrinsicValue intrinsicValue = IntrinsicValue.builder()
                                .companyId(companyId)
                                .calculationDate(LocalDate.now())
                                .value(preciseValue)
                                .build();

                // Act
                intrinsicValueRepository.save(intrinsicValue);

                // Assert
                assertThat(intrinsicValueJpaRepository.findAll())
                                .hasSize(1)
                                .extracting(IntrinsicValueEntity::getIntrinsicValue)
                                .contains(preciseValue);
        }

        @Test
        void shouldReturnTop20CompaniesByIntrinsicValueAsc() {
                // Arrange: Create 25 companies with different intrinsic values
                IntStream.range(0, 25).forEach(i -> {
                        CompanyEntity company = CompanyEntity.builder()
                                        .id(UUID.randomUUID().toString())
                                        .ticker("TICKER" + i)
                                        .name("Company " + i)
                                        .build();
                        companyJpaRepository.save(company);

                        IntrinsicValueEntity iv = IntrinsicValueEntity.builder()
                                        .id(UUID.randomUUID().toString())
                                        .companyId(company.getId())
                                        .calculationDate(LocalDate.now())
                                        .intrinsicValue(BigDecimal.valueOf(100 - i)) // Lower index = higher value, so
                                                                                     // TICKER24 has lowest value
                                                                                     // (100-24=76)
                                        .build();
                        intrinsicValueJpaRepository.save(iv);
                });

                // Act
                List<IntrinsicValue> result = intrinsicValueRepository.findTop20ByOrderByIntrinsicValueAsc();

                // Assert
                assertThat(result).hasSize(20);
                // The values should be from 76 to 95
                assertThat(result.get(0).getValue()).isEqualByComparingTo(BigDecimal.valueOf(76));
                assertThat(result.get(19).getValue()).isEqualByComparingTo(BigDecimal.valueOf(95));

                // Check ordering
                for (int i = 0; i < result.size() - 1; i++) {
                        assertThat(result.get(i).getValue()).isLessThanOrEqualTo(result.get(i + 1).getValue());
                }
        }

}
