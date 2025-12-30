package com.ichaabane.book_network.application.dto.request;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
    private String confirmPassword;
}
