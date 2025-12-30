package com.ichaabane.book_network.infrastructure.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {

    ACTIVATE_ACCOUNT("activate_account"),
    SET_PASSWORD("set_password"),
    FORGOT_PASSWORD("forgot_password");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}
