package de.kfzteile24.salesOrderHub.configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.config.annotation.SqsConfiguration;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import java.util.Collections;

@Configuration
@Import(SqsConfiguration.class)
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

    @Bean
    public AmazonSQSAsync amazonSQSAsync() {
        // check if local development
        if (endpoint != null) {
            AWSCredentials awsCredentials = new BasicAWSCredentials(
                    awsAccessKey,
                    awsSecretKey
            );

            AmazonSQSAsyncClientBuilder builder = AmazonSQSAsyncClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials));

            AwsClientBuilder.EndpointConfiguration localEndpoint = new AwsClientBuilder.EndpointConfiguration(endpoint, awsRegion);
            builder.withEndpointConfiguration(localEndpoint);
            return builder.build();
        } else {
            ClientConfiguration clientConfiguration = new ClientConfiguration().withMaxConnections(MAX_CONNECTIONS);
            return AmazonSQSAsyncClientBuilder.standard().withRegion(awsRegion).
                    withClientConfiguration(clientConfiguration).build();
        }
    }

    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(AmazonSQSAsync amazonSQSAsync) {
        return new QueueMessagingTemplate(amazonSQSAsync);
    }

    @Bean
    public QueueMessageHandlerFactory queueMessageHandlerFactory() {
        QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setStrictContentTypeMatch(false);
        messageConverter.getObjectMapper().registerModule(new JavaTimeModule());
        factory.setMessageConverters(Collections.singletonList(messageConverter));
        return factory;
    }

    @Bean
    public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(AmazonSQSAsync amazonSqs) {
        SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
        factory.setAmazonSqs(amazonSqs);
        factory.setMaxNumberOfMessages(10);
        factory.setWaitTimeOut(10);
        return factory;
    }
}
