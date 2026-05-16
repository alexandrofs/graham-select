package afsdigital.grahamselect.api.upload.infrastructure.storage;

import afsdigital.grahamselect.common.upload.application.repository.B3FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalB3FileStorage implements B3FileStoragePort {

    private final Path storagePath;

    public LocalB3FileStorage(@Value("${app.upload.storage-path}") String storagePath) {
        this.storagePath = Paths.get(storagePath);
        try {
            Files.createDirectories(this.storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    @Override
    public String save(InputStream inputStream, String fileName, String userId) {
        try {
            String uniqueFileName = UUID.randomUUID() + "_" + fileName;
            Path targetPath = storagePath.resolve(uniqueFileName);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Could not save file", e);
        }
    }
}
