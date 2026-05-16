package afsdigital.grahamselect.common.upload.application.repository;

import java.io.InputStream;

public interface B3FileStoragePort {
    String save(InputStream inputStream, String fileName, String userId);
}
