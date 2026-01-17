package com.ichaabane.book_network.infrastructure.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileUtils - Tests unitaires")
class FileUtilsTest {

    @Nested
    @DisplayName("readFileFromLocation() - Lecture de fichiers")
    class ReadFileFromLocationTests {

        @ParameterizedTest(name = "Devrait lire un fichier: {0}")
        @CsvSource({
            "test.txt, Contenu de test",
            "fichier avec espaces.txt, Test avec espaces",
            "fichier-test_123.txt, Contenu spécial"
        })
        @DisplayName("Devrait lire différents types de fichiers")
        void devraitLireDifferentsTypesDeFichiers(String filename, String content, @TempDir Path tempDir) throws IOException {
            // Given
            Path testFile = tempDir.resolve(filename);
            Files.writeString(testFile, content);
            String fileUrl = testFile.toAbsolutePath().toString();

            // When
            byte[] result = FileUtils.readFileFromLocation(fileUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(new String(result)).isEqualTo(content);
        }

        @ParameterizedTest(name = "Devrait retourner un tableau vide pour: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "/chemin/inexistant/fichier.txt", "/tmp/fichier_qui_nexiste_vraiment_pas_12345.txt"})
        @DisplayName("Devrait retourner un tableau vide pour des entrées invalides")
        void devraitRetournerTableauVidePourEntreesInvalides(String fileUrl) {
            // When
            byte[] result = FileUtils.readFileFromLocation(fileUrl);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Devrait lire un fichier binaire correctement")
        void devraitLireFichierBinaire(@TempDir Path tempDir) throws IOException {
            // Given
            Path testFile = tempDir.resolve("binary.dat");
            byte[] binaryContent = new byte[]{0x00, 0x01, 0x02, 0x03, (byte) 0xFF};
            Files.write(testFile, binaryContent);
            String fileUrl = testFile.toAbsolutePath().toString();

            // When
            byte[] result = FileUtils.readFileFromLocation(fileUrl);

            // Then
            assertThat(result).isNotNull().isEqualTo(binaryContent);
        }

        @Test
        @DisplayName("Devrait lire un fichier vide")
        void devraitLireFichierVide(@TempDir Path tempDir) throws IOException {
            // Given
            Path testFile = tempDir.resolve("empty.txt");
            Files.writeString(testFile, "");
            String fileUrl = testFile.toAbsolutePath().toString();

            // When
            byte[] result = FileUtils.readFileFromLocation(fileUrl);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Devrait retourner un tableau vide pour un répertoire au lieu d'un fichier")
        void devraitRetournerTableauVidePourRepertoire(@TempDir Path tempDir) {
            // Given
            String fileUrl = tempDir.toAbsolutePath().toString();

            // When
            byte[] result = FileUtils.readFileFromLocation(fileUrl);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
