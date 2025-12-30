package com.ichaabane.book_network.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UserRequest(
        @NotEmpty(message = "Firstname is mandatory")
        @NotNull(message = "Firstname is mandatory")
        String firstName,
        @NotEmpty(message = "Lastname is mandatory")
        @NotNull(message = "Lastname is mandatory")
        String lastName,
        @NotEmpty(message = "dateOfBirth is mandatory")
        @NotNull(message = "dateOfBirth is mandatory")
        LocalDate dateOfBirth,
        @Email(message = "Email is not well formatted")
        @NotNull(message = "Email is Mandatory")
        @NotEmpty(message = "Email is Mandatory")
        String email
) {
}
