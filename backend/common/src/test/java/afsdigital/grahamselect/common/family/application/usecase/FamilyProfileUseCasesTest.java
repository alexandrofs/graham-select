package afsdigital.grahamselect.common.family.application.usecase;

import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.application.service.exceptions.DuplicateFamilyProfileException;
import afsdigital.grahamselect.common.family.application.service.exceptions.FamilyProfileNotFoundException;
import afsdigital.grahamselect.common.family.application.service.exceptions.InvalidCpfException;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyProfileUseCasesTest {

    @Mock
    private FamilyProfilePort familyProfilePort;

    private LinkFamilyProfileUseCase linkUseCase;
    private ListFamilyProfilesUseCase listUseCase;
    private UnlinkFamilyProfileUseCase unlinkUseCase;

    @BeforeEach
    void setUp() {
        linkUseCase = new LinkFamilyProfileUseCase(familyProfilePort);
        listUseCase = new ListFamilyProfilesUseCase(familyProfilePort);
        unlinkUseCase = new UnlinkFamilyProfileUseCase(familyProfilePort);
    }

    @Test
    void shouldLinkNewFamilyProfileWhenValid() {
        when(familyProfilePort.findByMasterUserIdAndCpfDigits("user-sub-1", "52998224725"))
                .thenReturn(Optional.empty());
        when(familyProfilePort.save(any(FamilyProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FamilyProfile created = linkUseCase.execute("user-sub-1", "529.982.247-25", "Maria Silva", "Cônjuge");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getMasterUserId()).isEqualTo("user-sub-1");
        assertThat(created.getCpfDigits()).isEqualTo("52998224725");
        assertThat(created.getName()).isEqualTo("Maria Silva");
        assertThat(created.getRelationship()).isEqualTo("Cônjuge");
        assertThat(created.isActive()).isTrue();
        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrowDuplicateWhenActiveProfileWithSameCpfExists() {
        FamilyProfile existing = FamilyProfile.builder()
                .id(UUID.randomUUID())
                .masterUserId("user-sub-1")
                .cpfDigits("52998224725")
                .name("Maria")
                .active(true)
                .build();

        when(familyProfilePort.findByMasterUserIdAndCpfDigits("user-sub-1", "52998224725"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> linkUseCase.execute("user-sub-1", "52998224725", "Maria Silva", "Cônjuge"))
                .isInstanceOf(DuplicateFamilyProfileException.class);
    }

    @Test
    void shouldReactivateInactiveProfileWithSameCpf() {
        UUID existingId = UUID.randomUUID();
        FamilyProfile inactive = FamilyProfile.builder()
                .id(existingId)
                .masterUserId("user-sub-1")
                .cpfDigits("52998224725")
                .name("Nome Antigo")
                .relationship("Outro")
                .active(false)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10))
                .build();

        when(familyProfilePort.findByMasterUserIdAndCpfDigits("user-sub-1", "52998224725"))
                .thenReturn(Optional.of(inactive));
        when(familyProfilePort.save(any(FamilyProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FamilyProfile reactivated = linkUseCase.execute("user-sub-1", "52998224725", "Maria Nova", "Cônjuge");

        assertThat(reactivated.getId()).isEqualTo(existingId);
        assertThat(reactivated.isActive()).isTrue();
        assertThat(reactivated.getName()).isEqualTo("Maria Nova");
        assertThat(reactivated.getRelationship()).isEqualTo("Cônjuge");
        assertThat(reactivated.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectInvalidCpfOnLink() {
        assertThatThrownBy(() -> linkUseCase.execute("user-sub-1", "11111111111", "Maria", null))
                .isInstanceOf(InvalidCpfException.class);
    }

    @Test
    void shouldListActiveProfilesForMasterUser() {
        FamilyProfile profile = FamilyProfile.builder()
                .id(UUID.randomUUID())
                .masterUserId("user-sub-1")
                .name("João")
                .active(true)
                .build();
        when(familyProfilePort.findActiveByMasterUserId("user-sub-1")).thenReturn(List.of(profile));

        List<FamilyProfile> result = listUseCase.execute("user-sub-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("João");
    }

    @Test
    void shouldUnlinkActiveProfile() {
        UUID id = UUID.randomUUID();
        FamilyProfile active = FamilyProfile.builder()
                .id(id)
                .masterUserId("user-sub-1")
                .active(true)
                .build();

        when(familyProfilePort.findActiveByIdAndMasterUserId(id, "user-sub-1")).thenReturn(Optional.of(active));
        when(familyProfilePort.save(any(FamilyProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        unlinkUseCase.execute(id, "user-sub-1");

        assertThat(active.isActive()).isFalse();
        assertThat(active.getUpdatedAt()).isNotNull();
        verify(familyProfilePort).save(active);
    }

    @Test
    void shouldThrowNotFoundWhenUnlinkingUnknownOrOtherUserProfile() {
        UUID id = UUID.randomUUID();
        when(familyProfilePort.findActiveByIdAndMasterUserId(id, "user-sub-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> unlinkUseCase.execute(id, "user-sub-1"))
                .isInstanceOf(FamilyProfileNotFoundException.class);
    }
}
