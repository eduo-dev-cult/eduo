package se.ltu.eduo.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.ltu.eduo.user.dto.UserPreferencesDto;
import se.ltu.eduo.user.request.LoginRequest;
import se.ltu.eduo.user.request.RegisterRequest;
import se.ltu.eduo.user.dto.UserDto;
import se.ltu.eduo.exception.UsernameAlreadyExistsException;
import se.ltu.eduo.user.mapper.UserMapper;
import se.ltu.eduo.user.model.User;
import se.ltu.eduo.user.service.AuthService;
import se.ltu.eduo.user.service.UserPreferencesService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final UserPreferencesService userPreferencesService;

    @PostMapping
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        try
        {
            User user = authService.createUser(
                    request.firstName(), request.lastName(), request.username(), request.password());
            return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(user)); //fixme xss warning
        } catch (UsernameAlreadyExistsException e)
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }


    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequest request) {
        return authService.logInUser(request.username(), request.password())
                .map(userMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // gets user preferences (fixa url så den hittar rätt)
    @GetMapping("/{id}/preferences")
    public ResponseEntity<UserPreferencesDto> getUserPreferences(@PathVariable Integer id) {
        UserPreferencesDto preferences = userPreferencesService.getPreferencesByUserId(id);
        return ResponseEntity.ok(preferences);
    }

    // updates user preferences (fixa url så den hittar rätt)
    @PutMapping("/{id}/preferences")
    public ResponseEntity<Void> updateUserPreferences(@PathVariable Integer id,
            @RequestBody UserPreferencesDto userPreferencesDto) {
        userPreferencesService.updatePreferencesByUserId(id, userPreferencesDto);
        return ResponseEntity.noContent().build();
    }

}