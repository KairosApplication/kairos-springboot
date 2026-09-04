package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.UserRequest;
import com.kairos.kairosapipostgres.dto.request.UserUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.exception.CpfInvalidException;
import com.kairos.kairosapipostgres.exception.UserAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.UserNotFoundException;
import com.kairos.kairosapipostgres.mapper.UserMapper;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.repository.UserRepository;
import com.kairos.kairosapipostgres.utils.CpfFormatter;
import com.kairos.kairosapipostgres.utils.CpfValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse save (UserRequest user) throws UserAlreadyExistsException {
        User userEntity = UserMapper.toEntity(user);
        String cpfUnmasked = CpfFormatter.removeFormatMask (userEntity.getCpf());
        if (repository.existsByCpf(cpfUnmasked) || repository.existsByEmail(userEntity.getEmail())) {
            throw new UserAlreadyExistsException ("User already exists");
        }

        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
        return UserMapper.toResponse(repository.save(userEntity));
    }

    public Optional<UserResponse> update (Long id, UserUpdateRequest user) throws UserNotFoundException {
        User userEntity = repository.findById (id).orElseThrow (() -> new UserNotFoundException ("User not found"));
        String cpfUnmasked = (user.cpf ()) != null ? CpfFormatter.removeFormatMask (user.cpf ()) : userEntity.getCpf();
        String email = (user.email ()) != null ? user.email().trim() : userEntity.getEmail();
        Plan plan = (user.plan ()) != null ? user.plan () : userEntity.getPlan();

        repository.findByCpf(cpfUnmasked)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException("CPF already in use");
                });
        repository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException("Email already in use");
                });

        userEntity.setCpf (cpfUnmasked);
        userEntity.setEmail (email);
        userEntity.setPlan (plan);
        return Optional.of(UserMapper.toResponse(repository.save(userEntity)));
    }

    public boolean deleteById (Long id) {
        repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        repository.deleteById(id);
        return true;
    }

    public List<UserResponse> findAll () {
        return repository.findAll ().stream ().map (UserMapper::toResponse).toList ();
    }

    public Optional<UserResponse> findById (Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return Optional.of(UserMapper.toResponse(user));
    }

    public Optional<UserResponse> findByCpf (String cpf) {
        if (CpfValidator.isValid (cpf)) {
            String cpfSemMascara = CpfFormatter.removeFormatMask (cpf);
            if (cpfSemMascara == null) {
                throw new CpfInvalidException ("Invalid CPF");
            }

            return Optional.of(UserMapper.toResponse(repository.findByCpf (cpfSemMascara)
                    .orElseThrow (() ->
                            new UserNotFoundException ("User not found")
                    )));
        }
        throw new CpfInvalidException ("Invalid CPF");

    }
}
