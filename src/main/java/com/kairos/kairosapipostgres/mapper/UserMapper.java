package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.UserRequest;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.utils.CpfFormatter;

public final class UserMapper {

    private UserMapper () {
    }

    public static User toEntity(UserRequest request) {
        User user = new User();

        user.setName(request.name().trim());
        user.setLastName(request.lastName().trim());
        user.setBirthDate(request.birthDate());
        user.setPassword(request.password());
        user.setZipCode(request.zipCode().trim());
        user.setPlan(request.plan());
        user.setEmail(request.email().trim());
        user.setCpf(CpfFormatter.removeFormatMask(request.cpf()));

        return user;
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                CpfFormatter.addFormatMask(user.getCpf())
        );
    }
}
