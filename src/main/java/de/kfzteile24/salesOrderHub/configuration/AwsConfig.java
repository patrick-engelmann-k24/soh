package de.kfzteile24.salesOrderHub.configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {


    @Value("${cloud.aws.credentials.secret-key}")
    protected String awsSecretKey;

    @Value("${cloud.aws.credentials.access-key}")
    protected String awsAccessKey;

    @Value("${cloud.aws.endpoint.url:#{null}}")
    protected String endpoint;

    @Value("${cloud.aws.region.static}")
    protected String awsRegion;

    @Bean
    public NotificationMessagingTemplate notificationMessagingTemplate(AmazonSNS amazonSns) {
        return new NotificationMessagingTemplate(amazonSns);
    }

    @Bean
    public AmazonSNS amazonSNS() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(
                awsAccessKey,
                awsSecretKey
        );

        AmazonSNSClientBuilder builder = AmazonSNSClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials));

        if (endpoint != null) {
            AwsClientBuilder.EndpointConfiguration localEndpoint = new AwsClientBuilder.EndpointConfiguration(endpoint, awsRegion);
            builder.withEndpointConfiguration(localEndpoint);
        } else {
            builder.withRegion(awsRegion);
        }

        return builder.build();
    }

}
