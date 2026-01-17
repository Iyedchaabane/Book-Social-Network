package com.ichaabane.book_network.application.specification;

import com.ichaabane.book_network.domain.model.Book;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour BookSpecification.
 * Objectif : Couverture maximale (> 80%) en testant tous les aspects de la Specification.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookSpecification - Tests unitaires")
class BookSpecificationTest {

    @Mock
    private Root<Book> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Path<Object> ownerPath;

    @Mock
    private Path<Object> ownerIdPath;

    @Mock
    private Predicate predicate;

    @Test
    @DisplayName("Le constructeur privé ne devrait pas être accessible")
    void constructeurPriveNonAccessible() throws Exception {
        Constructor<BookSpecification> constructor = BookSpecification.class.getDeclaredConstructor();
        assertThat(constructor.canAccess(null)).isFalse();
        
        // Test accessing the private constructor via reflection
        constructor.setAccessible(true);
        BookSpecification instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }

    @Nested
    @DisplayName("withOwnerId() - Filtrage par propriétaire")
    class WithOwnerIdTests {

        @Test
        @DisplayName("Devrait créer une specification pour filtrer par ownerId")
        void devraitCreerSpecificationAvecOwnerId() {
            // Given
            Integer ownerId = 42;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Then
            assertThat(result).isNotNull().isEqualTo(predicate);
            verify(root).get("owner");
            verify(ownerPath).get("id");
            verify(criteriaBuilder).equal(ownerIdPath, ownerId);
        }

        @Test
        @DisplayName("Devrait gérer un ownerId de valeur zéro")
        void devraitGererOwnerIdZero() {
            // Given
            Integer ownerId = 0;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Then
            assertThat(result).isNotNull().isEqualTo(predicate);
            verify(criteriaBuilder).equal(ownerIdPath, 0);
        }

        @Test
        @DisplayName("Devrait gérer un ownerId négatif")
        void devraitGererOwnerIdNegatif() {
            // Given
            Integer ownerId = -1;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Then
            assertThat(result).isNotNull().isEqualTo(predicate);
            verify(criteriaBuilder).equal(ownerIdPath, -1);
        }

        @Test
        @DisplayName("Devrait gérer un très grand ownerId")
        void devraitGererTresGrandOwnerId() {
            // Given
            Integer ownerId = Integer.MAX_VALUE;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Then
            assertThat(result).isNotNull().isEqualTo(predicate);
            verify(criteriaBuilder).equal(ownerIdPath, Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("Devrait gérer un très petit ownerId")
        void devraitGererTresPetitOwnerId() {
            // Given
            Integer ownerId = Integer.MIN_VALUE;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Then
            assertThat(result).isNotNull().isEqualTo(predicate);
            verify(criteriaBuilder).equal(ownerIdPath, Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("Devrait retourner une specification non null")
        void devraitRetournerSpecificationNonNull() {
            // When
            Specification<Book> specification = BookSpecification.withOwnerId(1);

            // Then
            assertThat(specification).isNotNull();
        }

        @Test
        @DisplayName("Devrait être réutilisable pour différents ownerIds")
        void devraitEtreReutilisable() {
            // Given
            Integer ownerId1 = 10;
            Integer ownerId2 = 20;
            
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId1)).thenReturn(predicate);
            when(criteriaBuilder.equal(ownerIdPath, ownerId2)).thenReturn(predicate);

            // When
            Specification<Book> spec1 = BookSpecification.withOwnerId(ownerId1);
            Specification<Book> spec2 = BookSpecification.withOwnerId(ownerId2);
            spec1.toPredicate(root, query, criteriaBuilder);
            spec2.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).equal(ownerIdPath, ownerId1);
            verify(criteriaBuilder).equal(ownerIdPath, ownerId2);
        }

        @Test
        @DisplayName("Devrait fonctionner avec une requête typée")
        void devraitFonctionnerAvecRequeteTypee() {
            // Given
            Integer ownerId = 5;
            CriteriaQuery<Book> typedQuery = mock(CriteriaQuery.class);
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            Predicate result = specification.toPredicate(root, typedQuery, criteriaBuilder);

            // Then
            assertThat(result).isNotNull().isEqualTo(predicate);
        }

        @Test
        @DisplayName("Devrait créer plusieurs specifications indépendantes")
        void devraitCreerPlusieursSpecificationsIndependantes() {
            // When
            Specification<Book> spec1 = BookSpecification.withOwnerId(100);
            Specification<Book> spec2 = BookSpecification.withOwnerId(200);
            Specification<Book> spec3 = BookSpecification.withOwnerId(300);

            // Then
            assertThat(spec1).isNotNull();
            assertThat(spec2).isNotNull();
            assertThat(spec3).isNotNull();
            assertThat(spec1).isNotSameAs(spec2);
            assertThat(spec2).isNotSameAs(spec3);
        }

        @Test
        @DisplayName("Devrait naviguer correctement dans la hiérarchie owner.id")
        void devraitNaviguerCorrectementDansHierarchie() {
            // Given
            Integer ownerId = 777;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            specification.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(root, times(1)).get("owner");
            verify(ownerPath, times(1)).get("id");
        }

        @Test
        @DisplayName("Devrait appeler criteriaBuilder.equal avec les bons arguments")
        void devraitAppelerEqualAvecBonsArguments() {
            // Given
            Integer ownerId = 999;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            specification.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).equal(ownerIdPath, ownerId);
        }

        @Test
        @DisplayName("Devrait invoquer toPredicate sans exception")
        void devraitInvoquerToPredicateSansException() {
            // Given
            Integer ownerId = 123;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);

            // Then
            assertThat(specification.toPredicate(root, query, criteriaBuilder))
                    .isNotNull()
                    .isEqualTo(predicate);
        }

        @Test
        @DisplayName("Devrait gérer plusieurs appels consécutifs avec le même ownerId")
        void devraitGererPlusieursAppelsConsecutifs() {
            // Given
            Integer ownerId = 50;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            Predicate result1 = specification.toPredicate(root, query, criteriaBuilder);
            Predicate result2 = specification.toPredicate(root, query, criteriaBuilder);
            Predicate result3 = specification.toPredicate(root, query, criteriaBuilder);

            // Then
            assertThat(result1).isEqualTo(predicate);
            assertThat(result2).isEqualTo(predicate);
            assertThat(result3).isEqualTo(predicate);
            verify(criteriaBuilder, times(3)).equal(ownerIdPath, ownerId);
        }

        @Test
        @DisplayName("Devrait créer une nouvelle specification à chaque appel de withOwnerId")
        void devraitCreerNouvelleSpecification() {
            // When
            Specification<Book> spec1 = BookSpecification.withOwnerId(1);
            Specification<Book> spec2 = BookSpecification.withOwnerId(1);

            // Then
            assertThat(spec1).isNotNull();
            assertThat(spec2).isNotNull();
            // Les lambdas sont différentes instances même avec la même valeur
            assertThat(spec1).isNotSameAs(spec2);
        }

        @Test
        @DisplayName("Devrait gérer ownerId avec valeur 1")
        void devraitGererOwnerIdUn() {
            // Given
            Integer ownerId = 1;
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(predicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Then
            assertThat(result).isEqualTo(predicate);
            verify(root).get("owner");
            verify(ownerPath).get("id");
            verify(criteriaBuilder).equal(ownerIdPath, 1);
        }

        @Test
        @DisplayName("Devrait retourner le predicate créé par criteriaBuilder")
        void devraitRetournerPredicateCree() {
            // Given
            Integer ownerId = 888;
            Predicate customPredicate = mock(Predicate.class);
            when(root.get("owner")).thenReturn(ownerPath);
            when(ownerPath.get("id")).thenReturn(ownerIdPath);
            when(criteriaBuilder.equal(ownerIdPath, ownerId)).thenReturn(customPredicate);

            // When
            Specification<Book> specification = BookSpecification.withOwnerId(ownerId);
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Then
            assertThat(result).isSameAs(customPredicate);
        }
    }
}
