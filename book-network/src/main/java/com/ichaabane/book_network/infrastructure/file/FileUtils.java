package com.ichaabane.book_network.infrastructure.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class FileUtils {

    // Private constructor to hide the implicit public constructor
    private FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] readFileFromLocation(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return new byte[0];
        }
        try {
            Path path = new File(fileUrl).toPath();
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.warn("No file found at {}", fileUrl);
        }
        return new byte[0];
    }
}
