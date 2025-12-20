package com.ichaabane.book_network.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeVerificationRequest {
    private String token;
}
