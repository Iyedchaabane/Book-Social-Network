package com.ichaabane.book_network.domain.repository;

import com.ichaabane.book_network.domain.enums.TokenType;
import com.ichaabane.book_network.domain.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Integer> {
    Optional<Token> findByToken(String token);
    Optional<Token> findByTokenAndType(String token, TokenType type);

    // Supprime tous les tokens d'un utilisateur pour un type donné
    @Modifying
    @Query("DELETE FROM Token t WHERE t.user.id = :userId AND t.type = :type")
    void deleteByUserIdAndType(@Param("userId") Integer userId, @Param("type") TokenType type);

    void deleteByToken(String tokenValue);

    // On récupère le token le plus récent pour un type donné
    Optional<Token> findFirstByTokenAndTypeOrderByCreatedAtDesc(String token, TokenType type);
}
