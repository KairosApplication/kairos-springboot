package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.UserRequest;
import com.kairos.kairosapipostgres.dto.request.UserUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.exception.CpfInvalidException;
import com.kairos.kairosapipostgres.exception.UserAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.UserNotFoundException;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, passwordEncoder);
    }

    @Test
    void shouldSaveCompleteUserWithEncodedPassword() {
        when(repository.existsByCpf("52998224725")).thenReturn(false);
        when(repository.existsByEmail("dias@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse response = service.save(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Davi");
        assertThat(saved.getLastName()).isEqualTo("Dias");
        assertThat(saved.getBirthDate()).isEqualTo(LocalDate.of(2000, 2, 12));
        assertThat(saved.getZipCode()).isEqualTo("01310-100");
        assertThat(saved.getPlan()).isEqualTo(Plan.STANDART);
        assertThat(saved.getPassword()).isEqualTo("encoded-secret");
        assertThat(response).isEqualTo(new UserResponse(
                1L, "Davi", "dias@example.com", "529.982.247-25"
        ));
    }

    @Test
    void shouldRejectExistingCpf() {
        when(repository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.save(request()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User already exists");
        verify(repository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldRejectExistingEmail() {
        when(repository.existsByCpf("52998224725")).thenReturn(false);
        when(repository.existsByEmail("dias@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.save(request()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User already exists");
        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        User existing = user(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(existing));
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(repository.save(existing)).thenReturn(existing);

        UserResponse response = service.update(
                1L,
                new UserUpdateRequest(null, " new@example.com ", null)
        ).orElseThrow();

        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getCpf()).isEqualTo("52998224725");
        assertThat(existing.getPlan()).isEqualTo(Plan.STANDART);
        assertThat(response.email()).isEqualTo("new@example.com");
        verify(repository).save(existing);
    }

    @Test
    void shouldRejectCpfOwnedByAnotherUserDuringUpdate() {
        User existing = user(1L);
        User anotherUser = user(2L);
        anotherUser.setCpf("11144477735");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByCpf("11144477735")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> service.update(
                1L,
                new UserUpdateRequest(null, null, "111.444.777-35")
        )).isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("CPF already in use");
        verify(repository, never()).save(any());
    }

    @Test
    void shouldFindAllUsers() {
        when(repository.findAll()).thenReturn(List.of(user(1L), user(2L)));

        List<UserResponse> users = service.findAll();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void shouldFindUserByIdWithSingleRepositoryLookup() {
        when(repository.findById(1L)).thenReturn(Optional.of(user(1L)));

        UserResponse response = service.findById(1L).orElseThrow();

        assertThat(response.id()).isEqualTo(1L);
        verify(repository).findById(1L);
    }

    @Test
    void shouldThrowWhenUserIdDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldFindUserByFormattedCpf() {
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(user(1L)));

        UserResponse response = service.findByCpf("529.982.247-25").orElseThrow();

        assertThat(response.cpf()).isEqualTo("529.982.247-25");
    }

    @Test
    void shouldRejectInvalidCpfBeforeRepositoryLookup() {
        assertThatThrownBy(() -> service.findByCpf("123.456.789-00"))
                .isInstanceOf(CpfInvalidException.class)
                .hasMessage("Invalid CPF");
        verify(repository, never()).findByCpf(any());
    }

    @Test
    void shouldDeleteExistingUser() {
        when(repository.findById(1L)).thenReturn(Optional.of(user(1L)));

        assertThat(service.deleteById(1L)).isTrue();
        verify(repository).deleteById(1L);
    }

    @Test
    void shouldRejectDeletingMissingUser() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(repository, never()).deleteById(any());
    }

    private UserRequest request() {
        return new UserRequest(
                "Davi",
                "Dias",
                LocalDate.of(2000, 2, 12),
                "secret",
                "01310-100",
                Plan.STANDART,
                "dias@example.com",
                "529.982.247-25"
        );
    }

    private User user(Long id) {
        return new User(
                id,
                "Davi",
                "Dias",
                LocalDate.of(2000, 2, 12),
                "52998224725",
                "dias@example.com",
                "encoded-secret",
                "01310-100",
                Plan.STANDART
        );
    }
}
