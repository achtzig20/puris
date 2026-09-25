package org.eclipse.tractusx.puris.backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.user.domain.model.User;
import org.eclipse.tractusx.puris.backend.user.logic.dto.UserDto;
import org.eclipse.tractusx.puris.backend.user.logic.dto.UserSearchCriteria;
import org.eclipse.tractusx.puris.backend.user.logic.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private Validator validator;

    private final ModelMapper modelMapper = new ModelMapper();

    @PostMapping
    @Operation(description = "Creates a new User with the data given in the request body.")
    public ResponseEntity<?> createUser(@RequestBody UserDto userDto) {
        if (!validator.validate(userDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            return ResponseEntity.status(400).build();
        }
        User user = modelMapper.map(userDto, User.class);
        User created = userService.create(user);
        if (created == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(409));
        }
        UserDto responseDto = modelMapper.map(created, UserDto.class);
        return new ResponseEntity<>(responseDto, HttpStatusCode.valueOf(200));
    }

    @PutMapping
    @Operation(description = "Updates an existing User with the data given in the request body.")
    public ResponseEntity<?> updateUser(@RequestBody UserDto userDto) {
        if (!validator.validate(userDto).isEmpty()) {
            log.warn("Rejected invalid message body.");
            return ResponseEntity.status(400).build();
        }
        User user = modelMapper.map(userDto, User.class);
        User updated = userService.update(user);
        if (updated == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }
        UserDto responseDto = modelMapper.map(updated, UserDto.class);
        return new ResponseEntity<>(responseDto, HttpStatusCode.valueOf(200));
    }

    @GetMapping("/getUser")
    @Operation(description = "Returns the requested User, specified by id.")
    public ResponseEntity<UserDto> getUser(@RequestParam UUID id) {
        User user = userService.findById(id);
        UserDto dto = modelMapper.map(user, UserDto.class);
        return new ResponseEntity<>(dto, HttpStatusCode.valueOf(200));
    }

    @GetMapping("/all")
    @Operation(description = "Returns a list of all Users.")
    public ResponseEntity<List<User>> listUsers() {
        return new ResponseEntity<>(userService.findAll(), HttpStatusCode.valueOf(200));
    }

    @GetMapping("/{id}")
    @Operation(description = "Deletes the User specified by id.")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return new ResponseEntity<>("User deleted", HttpStatusCode.valueOf(200));
    }

    @GetMapping("/search")
    @Operation(description = "Searches Users matching the given criteria.")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestBody UserSearchCriteria criteria) {
        List<UserDto> results = userService.search(criteria).stream()
            .map(u -> modelMapper.map(u, UserDto.class))
            .collect(Collectors.toList());
        return new ResponseEntity<>(results, HttpStatusCode.valueOf(200));
    }

}
