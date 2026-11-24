package com.ecommerce.user.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.dto.Dtos.LoginRequest;
import com.ecommerce.user.dto.Dtos.RegisterRequest;
import com.ecommerce.user.dto.Dtos.TokenResponse;
import com.ecommerce.user.dto.Dtos.UpdateUserRequest;
import com.ecommerce.user.dto.Dtos.UserResponse;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository repository;
    private final JwtService jwtService;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repository, JwtService jwtService, UserMapper mapper) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        if (repository.existsByUsername(req.username())) {
            throw new ConflictException("Username already taken: " + req.username());
        }
        if (repository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered: " + req.email());
        }
        return mapper.toResponse(repository.save(User.builder()
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(Role.USER)
                .createdAt(Instant.now())
                .build()));
    }

    public TokenResponse login(LoginRequest req) {
        User user = repository.findByUsername(req.username())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));
        return new TokenResponse(jwtService.generate(user), "Bearer", jwtService.getExpirationSeconds(),
                mapper.toResponse(user));
    }

    public UserResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    public List<UserResponse> list() {
        return mapper.toResponses(repository.findAll());
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        User user = find(id);
        if (req.email() != null && !req.email().equals(user.getEmail())) {
            if (repository.existsByEmail(req.email())) {
                throw new ConflictException("Email already registered: " + req.email());
            }
            user.setEmail(req.email());
        }
        if (req.password() != null) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }
        return mapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private User find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
