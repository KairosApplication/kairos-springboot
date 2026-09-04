package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.UserRequest;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void shouldMapEveryRequestFieldToEntity() {
        UserRequest request = new UserRequest(
                " Davi ",
                " Dias ",
                LocalDate.of(2000, 2, 12),
                "secret",
                " 01310-100 ",
                Plan.STANDART,
                " dias@example.com ",
                "529.982.247-25"
        );

        User user = UserMapper.toEntity(request);

        assertThat(user.getName()).isEqualTo("Davi");
        assertThat(user.getLastName()).isEqualTo("Dias");
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(2000, 2, 12));
        assertThat(user.getPassword()).isEqualTo("secret");
        assertThat(user.getZipCode()).isEqualTo("01310-100");
        assertThat(user.getPlan()).isEqualTo(Plan.STANDART);
        assertThat(user.getEmail()).isEqualTo("dias@example.com");
        assertThat(user.getCpf()).isEqualTo("52998224725");
    }

    @Test
    void shouldMapEntityToResponseWithoutExposingSensitiveFields() {
        User user = user(7L);

        UserResponse response = UserMapper.toResponse(user);

        assertThat(response).isEqualTo(new UserResponse(
                7L,
                "Davi",
                "dias@example.com",
                "529.982.247-25"
        ));
    }

    private User user(Long id) {
        return new User(
                id,
                "Davi",
                "Dias",
                LocalDate.of(2000, 2, 12),
                "52998224725",
                "dias@example.com",
                "encoded-password",
                "01310-100",
                Plan.STANDART
        );
    }
}
