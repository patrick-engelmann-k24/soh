package de.kfzteile24.salesOrderHub.services.export.impl;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AmazonS3ServiceTest {

    private final static String BUCKET_NAME = "production-k24-invoices";
    private final static String KEY = "www-k24-de/2021/06/04/123456789-987654321.pdf";
    private final static String URL = "s3://" + BUCKET_NAME + "/" + KEY;

    @InjectMocks
    private AmazonS3ServiceImpl amazonS3ServiceImpl;

    @Mock
    private AmazonS3Client amazonS3Client;

    @Test
    void testDownloadFile() {

        amazonS3ServiceImpl.downloadFile(URL);

        verify(amazonS3Client).getObject(BUCKET_NAME, KEY);
    }

    @Test
    void testDeleteFile() {

        amazonS3ServiceImpl.deleteFile(URL);

        verify(amazonS3Client).deleteObject(BUCKET_NAME, KEY);
    }

    @Test
    void testDeleteFileNotExisting() {

        doThrow(new SdkClientException("TestException")).when(amazonS3Client).deleteObject(BUCKET_NAME, KEY);

        try {
            amazonS3ServiceImpl.deleteFile(URL);
        } catch (SdkClientException e) {
            fail("No exception should be thrown by this method");
        }

        verify(amazonS3Client).deleteObject(BUCKET_NAME, KEY);
    }
}
