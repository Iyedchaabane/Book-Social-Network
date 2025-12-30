package com.ichaabane.book_network.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationRequest {

    @Email(message = "Email is not well formatted")
    @NotNull(message = "Email is Mandatory")
    @NotEmpty(message = "Email is Mandatory")
    private String email;

    @NotNull(message = "Password is Mandatory")
    @NotEmpty(message = "Password is Mandatory")
    @Size(min = 8, message = "Password should be 8 characters long minimum")
    private String password;
}
