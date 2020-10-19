package de.kfzteile24.salesOrderHub.configuration;

import com.amazonaws.ClientConfiguration;
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

    private static final int MAX_CONNECTIONS = 100;

    @Value("${cloud.aws.credentials.secret-key:#{null}}")
    protected String awsSecretKey;

    @Value("${cloud.aws.credentials.access-key:#{null}}")
    protected String awsAccessKey;

    @Value("${cloud.aws.endpoint.url:#{null}}")
    protected String endpoint;

    @Value("${cloud.aws.region.static}")
    protected String awsRegion;

    @Bean
    public NotificationMessagingTemplate notificationMessagingTemplate(AmazonSNS amazonSns) {
        return new NotificationMessagingTemplate(amazonSns);
    }

    /**
     * Amazon SNS bean.
     *
     * @return AmazonSNS
     */
    @Bean
    public AmazonSNS amazonSNS() {
        // check if local development
        if (endpoint != null) {
            AWSCredentials awsCredentials = new BasicAWSCredentials(
                    awsAccessKey,
                    awsSecretKey
            );

            AmazonSNSClientBuilder builder = AmazonSNSClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials));

            AwsClientBuilder.EndpointConfiguration localEndpoint = new AwsClientBuilder.EndpointConfiguration(endpoint, awsRegion);
            builder.withEndpointConfiguration(localEndpoint);
            return builder.build();
        } else {
            ClientConfiguration clientConfiguration = new ClientConfiguration().withMaxConnections(MAX_CONNECTIONS);
            return AmazonSNSClientBuilder.standard().withRegion(awsRegion).
                    withClientConfiguration(clientConfiguration).build();
        }
    }
}
