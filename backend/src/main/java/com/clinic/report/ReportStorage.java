package com.clinic.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stores report PDFs on the local filesystem, in a directory kept OUTSIDE any web
 * root so files are never served statically — the only way out is the authorized
 * download endpoint. Filenames are random UUIDs (no user input in the path), which
 * also rules out path-traversal.
 */
@Component
public class ReportStorage {

    private final Path root;

    public ReportStorage(@Value("${app.reports.dir}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create reports directory: " + root, e);
        }
    }

    /** Write bytes to a new random file and return its (relative) stored name. */
    public String store(byte[] bytes) {
        String name = UUID.randomUUID() + ".pdf";
        try {
            Files.write(root.resolve(name), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store report", e);
        }
        return name;
    }

    public byte[] read(String storedName) {
        try {
            return Files.readAllBytes(root.resolve(storedName));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read report " + storedName, e);
        }
    }
}
