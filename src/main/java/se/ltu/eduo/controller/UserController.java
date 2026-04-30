package se.ltu.eduo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.ltu.eduo.dto.LoginRequest;
import se.ltu.eduo.dto.RegisterRequest;
import se.ltu.eduo.dto.UserDto;
import se.ltu.eduo.mapper.UserMapper;
import se.ltu.eduo.repository.UserRepository;
import se.ltu.eduo.service.AuthService;

import java.net.URI;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        var user = authService.createUser(
                request.firstName(), request.lastName(), request.username(), request.password());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(location).body(userMapper.toDto(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequest request) {
        return authService.LogInUser(request.username(), request.password())
                .map(userMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Integer id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        authService.DeleteUser(id);
        return ResponseEntity.noContent().build();
    }
}