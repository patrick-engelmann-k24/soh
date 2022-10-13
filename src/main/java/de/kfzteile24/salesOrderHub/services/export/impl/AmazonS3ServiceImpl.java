package de.kfzteile24.salesOrderHub.services.export.impl;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.S3Object;
import de.kfzteile24.salesOrderHub.services.export.AmazonS3Service;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmazonS3ServiceImpl implements AmazonS3Service {

    private final AmazonS3 amazonS3;

    @Override
    @SneakyThrows
    public S3Object downloadFile(String url) {
        URI fileToBeDownloaded = new URI(url);
        AmazonS3URI s3URI = new AmazonS3URI(fileToBeDownloaded);
        try {
            S3Object response = amazonS3.getObject(s3URI.getBucket(), s3URI.getKey());
            log.info("Downloaded file " + s3URI.getKey() + "* from *" + s3URI.getBucket());
            return response;
        } catch (SdkClientException e) {
            log.error("Business Processing Engine could not download file *" + s3URI.getKey() + "* from *" + s3URI.getBucket() + "*", e);
            return null;
        }
    }

    @Override
    @SneakyThrows
    public void deleteFile(String url) {

        URI fileToBeDownloaded = new URI(url);
        AmazonS3URI s3URI = new AmazonS3URI(fileToBeDownloaded);
        try {
            amazonS3.deleteObject(s3URI.getBucket(), s3URI.getKey());
        } catch (SdkClientException e) {
            log.info("File " + s3URI.getKey() + " from " + s3URI.getBucket() + " could not be found for deletion");
            return;
        }
        log.info("Deleted file " + s3URI.getKey() + " from " + s3URI.getBucket());
    }
}
