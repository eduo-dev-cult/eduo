package se.ltu.eduo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.ltu.eduo.dto.LoginRequest;
import se.ltu.eduo.dto.RegisterRequest;
import se.ltu.eduo.dto.UserDto;
import se.ltu.eduo.exception.UsernameAlreadyExistsException;
import se.ltu.eduo.mapper.UserMapper;
import se.ltu.eduo.model.User;
import se.ltu.eduo.service.AuthService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        try
        {
            User user = authService.createUser(
                    request.firstName(), request.lastName(), request.username(), request.password());
            return ResponseEntity.status(201).body(userMapper.toDto(user)); //fixme xss warning
        } catch (UsernameAlreadyExistsException e)
        {
            return ResponseEntity.status(409).build();
        }


    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequest request) {
        return authService.logInUser(request.username(), request.password())
                .map(userMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}