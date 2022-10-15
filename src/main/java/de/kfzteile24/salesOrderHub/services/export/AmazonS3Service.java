package de.kfzteile24.salesOrderHub.services.export;

import com.amazonaws.services.s3.model.S3Object;

public interface AmazonS3Service {

    S3Object downloadFile(String url);

    void deleteFile(String url);
}
