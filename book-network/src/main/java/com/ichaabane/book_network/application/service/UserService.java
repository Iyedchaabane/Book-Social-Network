package com.ichaabane.book_network.application.service;

import com.ichaabane.book_network.application.dto.request.ChangePasswordRequest;
import com.ichaabane.book_network.domain.model.User;
import com.ichaabane.book_network.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {
        var user = (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

        // check if the current password is correct !!
        if (!passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }

        // check if the two new passwords are the same
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalStateException("Password does not match");
        }

        // update the password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // save the new password
        userRepository.save(user);
    }
}
