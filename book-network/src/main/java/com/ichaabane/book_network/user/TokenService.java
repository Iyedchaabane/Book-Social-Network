package com.ichaabane.book_network.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;

    // Utilise REQUIRES_NEW pour une transaction indépendante
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateAndSaveActivationToken(User user, TokenType type) {
        // Supprimer les anciens tokens
        tokenRepository.deleteByUserIdAndType(user.getId(), type);

        // Créer et sauvegarder le nouveau token
        String generatedToken = generateActivationCode(6);
        Token token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // Durée augmentée
                .type(type)
                .user(user)
                .build();

        tokenRepository.save(token);
        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String code = "0123456789";
        StringBuilder activationCode = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for(int i = 0; i < length; i++) {
            int numberIndex = random.nextInt(code.length());
            activationCode.append(code.charAt(numberIndex));
        }
        return activationCode.toString();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteToken(String tokenValue) {
        tokenRepository.deleteByToken(tokenValue);
    }
}
