package afsdigital.grahamselect.common.upload.application.repository;

import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;

public interface B3UploadEventPort {
    void publish(FileUploadedEvent event);
}
