package org.eclipse.tractusx.puris.backend.user.logic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class UserDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private boolean admin;

}
