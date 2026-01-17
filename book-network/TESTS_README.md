# Tests Unitaires - Book Network Application

## 📋 Vue d'ensemble

Ce document décrit les tests unitaires complets pour tous les services de l'application Book Network. Les tests sont écrits avec **JUnit 5 (Jupiter)** et **Mockito BDD style**, suivant les meilleures pratiques de tests unitaires.

## 🎯 Objectif de couverture

**Cible : 100% de couverture de ligne** pour la logique métier contenue dans les services.

Les tests couvrent :
- ✅ **AuthenticationService** - Inscription, authentification, activation de compte, réinitialisation de mot de passe
- ✅ **BookService** - Gestion des livres, emprunts, retours, réservations
- ✅ **EmailService** - Envoi d'emails avec templates
- ✅ **FeedbackService** - Création et récupération de feedbacks
- ✅ **FileStorageService** - Stockage de fichiers (couvertures de livres)
- ✅ **NotificationService** - Envoi de notifications via WebSocket
- ✅ **TokenService** - Génération et gestion de tokens
- ✅ **UserService** - Changement de mot de passe
- ✅ **BookMapperTest** - Tests des mappers de conversion de livres
- ✅ **FeedbackMapperTest** - Tests des mappers de conversion de feedbacks
- ✅ **BookSpecificationTest** - Tests des spécifications JPA pour filtrage de livres
- ✅ **FileUtilsTest** - Tests des utilitaires de gestion de fichiers

## 🛠️ Technologies utilisées

- **JUnit 5** (Jupiter) - Framework de tests
- **Mockito 5** - Mocking et BDD style (given/when/then)
- **AssertJ** - Assertions fluentes
- **Maven** - Build et gestion des dépendances
- **JaCoCo** - Rapport de couverture de code

## 📦 Dépendances requises

Assurez-vous que votre `pom.xml` contient les dépendances suivantes :

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Boot Test (pour les utilitaires) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Et le plugin JaCoCo :

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## 🚀 Exécution des tests

### Commande de base

```bash
# Exécuter tous les tests
mvn test
```

### Avec rapport de couverture JaCoCo

```bash
# Nettoyer, exécuter les tests et générer le rapport de couverture
mvn clean test jacoco:report
```

### Consulter le rapport de couverture

Après l'exécution, le rapport HTML est disponible à :

```
book-network/target/site/jacoco/index.html
```

Ouvrez ce fichier dans un navigateur pour visualiser :
- Couverture par package
- Couverture par classe
- Couverture ligne par ligne
- Branches couvertes/non couvertes

### Exécuter un test spécifique

```bash
# Exécuter une seule classe de test
mvn test -Dtest=AuthenticationServiceTest

# Exécuter une méthode de test spécifique
mvn test -Dtest=AuthenticationServiceTest#shouldRegisterUserSuccessfully
```

### Exécuter les tests avec verbosité

```bash
mvn test -X
```

## 📊 Structure des tests

### Organisation

```
book-network/src/test/java/com/ichaabane/book_network/
├── application/
│   ├── mapper/
│   │   ├── BookMapperTest.java
│   │   └── FeedbackMapperTest.java
│   ├── service/
│   │   ├── AuthenticationServiceTest.java
│   │   ├── BookServiceTest.java
│   │   ├── EmailServiceTest.java
│   │   ├── FeedbackServiceTest.java
│   │   ├── FileStorageServiceTest.java
│   │   ├── NotificationServiceTest.java
│   │   ├── TokenServiceTest.java
│   │   └── UserServiceTest.java
│   └── specification/
│       └── BookSpecificationTest.java
├── infrastructure/
│   └── file/
│       └── FileUtilsTest.java
└── BookNetworkApiApplicationTests.java
```

### Conventions de nommage

- **Classes de test** : `<NomDuService>Test.java`
- **Méthodes de test** : `should<Action><Condition>()` (en anglais pour cohérence)
- **Classes nested** : Regroupement logique par fonctionnalité avec `@Nested`
- **DisplayName** : Description claire en français du comportement testé

### Style BDD (Behavior-Driven Development)

Tous les tests suivent la structure **Given-When-Then** :

```java
@Test
@DisplayName("Devrait créer un utilisateur avec succès")
void shouldRegisterUserSuccessfully() throws MessagingException {
    // Given - Configuration des données et comportements attendus
    given(userRepository.existsByEmail(email)).willReturn(false);
    given(roleRepository.findByName("USER")).willReturn(Optional.of(userRole));
    
    // When - Exécution de l'action à tester
    authenticationService.register(request);
    
    // Then - Vérification des résultats et interactions
    then(userRepository).should().save(any(User.class));
    then(emailService).should().sendEmail(...);
}
```

## 🧪 Catégories de tests

### 1. Tests de succès (Happy Path)
Vérifient que le service fonctionne correctement avec des données valides.

### 2. Tests d'erreur et exceptions
Vérifient que le service gère correctement les erreurs :
- Données invalides
- Entités non trouvées
- Violations de règles métier
- Exceptions propagées

### 3. Tests de cas limites (Edge Cases)
Vérifient le comportement avec :
- Valeurs null
- Chaînes vides
- Collections vides
- Valeurs extrêmes (min/max)

### 4. Tests d'interactions
Vérifient que les dépendances sont appelées correctement :
- Nombre d'appels
- Paramètres passés
- Ordre d'exécution

## 📝 Notes importantes

### Exclusions de couverture

Les éléments suivants ne sont **pas** testés car ils relèvent du framework et non de la logique métier :

1. **Annotations framework** : `@Async`, `@Transactional`, `@PostConstruct`
2. **Frameworks externes** : 
   - JavaMailSender (Spring Mail)
   - SpringTemplateEngine (Thymeleaf)
   - SimpMessagingTemplate (WebSocket)
   - JwtService (Spring Security)
3. **Logging** : `log.info()`, `log.warn()`, `log.error()`
4. **Getters/Setters** générés par Lombok
5. **Mapping trivial** : Conversions simples entre DTOs et entités

### Hypothèses et limitations

1. **Tests unitaires purs** : Aucun contexte Spring n'est chargé (`@SpringBootTest` non utilisé)
2. **Isolation complète** : Toutes les dépendances sont mockées
3. **Base de données** : Aucune base de données n'est utilisée (repository mockés)
4. **Tests synchrones** : Les méthodes `@Async` s'exécutent de manière synchrone dans les tests
5. **Transactions** : Les annotations `@Transactional` n'ont pas d'effet dans les tests unitaires

### Comportement spécifique testé

#### AuthenticationService
- Gestion du self-reference pour contourner `@PostConstruct`
- Nettoyage compensatoire des tokens en cas d'échec d'envoi d'email
- Validation des tokens expirés avec renvoi automatique

#### BookService
- Validation des permissions (propriétaire vs emprunteur)
- Gestion des états des livres (archivé, partageable, emprunté)
- Système de réservation avec notifications

#### FileStorageService
- Création automatique de dossiers
- Génération de noms uniques basés sur timestamp
- Gestion d'extensions de fichiers multiples
- Utilisation de busy-wait pour éviter les collisions de timestamp (au lieu de Thread.sleep)

#### TokenService
- Génération de codes à 6 chiffres sécurisés
- Suppression automatique des anciens tokens
- Expiration configurée à 15 minutes

## 🔍 Vérification de la couverture

### Objectifs par service

| Service | Lignes couvertes | Branches couvertes | Complexité |
|---------|------------------|-------------------|------------|
| AuthenticationService | 100% | 100% | Élevée |
| BookService | 100% | 100% | Très élevée |
| EmailService | 100% | 100% | Faible |
| FeedbackService | 100% | 100% | Moyenne |
| FileStorageService | 100% | 100% | Moyenne |
| NotificationService | 100% | 100% | Moyenne |
| TokenService | 100% | 100% | Faible |
| UserService | 100% | 100% | Faible |
| BookMapper | 100% | 100% | Moyenne |
| FeedbackMapper | 100% | 100% | Moyenne |
| BookSpecification | 100% | 100% | Moyenne |
| FileUtils | 100% | 100% | Moyenne |

### Interpréter le rapport JaCoCo

- **Vert** : Ligne/branche couverte par au moins un test
- **Jaune** : Branche partiellement couverte
- **Rouge** : Ligne/branche non couverte

Si des lignes apparaissent en rouge, vérifiez :
1. S'agit-il de code framework (logging, annotations) ?
2. S'agit-il de code mort (inaccessible) ?
3. Un test manque-t-il pour couvrir ce cas ?

## 🎓 Bonnes pratiques suivies

1. ✅ **Tests isolés** : Chaque test est indépendant
2. ✅ **Noms explicites** : Chaque test décrit clairement ce qu'il vérifie
3. ✅ **Arrange-Act-Assert** : Structure claire (Given-When-Then)
4. ✅ **Un concept par test** : Chaque test vérifie un seul comportement
5. ✅ **Tests rapides** : Aucune dépendance externe (pas de DB, pas de réseau)
6. ✅ **Tests déterministes** : Résultats reproductibles
7. ✅ **Mocking approprié** : Seules les dépendances sont mockées, pas la classe testée
8. ✅ **Vérifications complètes** : État ET interactions vérifiés
9. ✅ **Éviter Thread.sleep()** : Utilisation de busy-wait pour les tests de timestamp
10. ✅ **Mockito sans eq() inutiles** : Passage direct des valeurs dans verify() quand possible

## 🐛 Debugging des tests

### Test qui échoue

```bash
# Exécuter avec stack trace complète
mvn test -Dtest=ClasseTest#methode -X
```

### Voir les logs de test

```bash
# Activer les logs pendant les tests
mvn test -Dlogging.level.com.ichaabane=DEBUG
```

### Exécuter en mode debug dans l'IDE

1. IntelliJ IDEA : Clic droit sur la classe de test → Debug
2. Eclipse : Clic droit sur la classe de test → Debug As → JUnit Test
3. VS Code : Utiliser l'extension Java Test Runner

## 📚 Ressources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)

## 🤝 Contribution

Pour ajouter de nouveaux tests :

1. Suivre la structure existante (Given-When-Then)
2. Utiliser BDDMockito (`given`/`willReturn`/`then`)
3. Utiliser AssertJ pour les assertions (`assertThat`)
4. Ajouter `@DisplayName` avec description claire
5. Grouper les tests liés avec `@Nested`
6. Vérifier la couverture avec `mvn clean test jacoco:report`

---

**Auteur** : Tests générés selon les spécifications JUnit 5 + Mockito BDD  
**Date** : Janvier 2026  
**Version** : 1.0.0
