## Instruction to setup sns sqs for local testing

### create sns topics 

soh-order-created:  
```
aws --endpoint-url http://localhost:4566 sns create-topic --name soh-order-created
```

test sqs to receive the message from topic:  
```
aws --endpoint-url http://localhost:4566 sqs create-queue --queue-name soh-queue-order-created
```

connect sns and sqs:   
```
aws --endpoint-url http://localhost:4566 sns subscribe --topic arn:aws:sns:eu-central-1:000000000000:soh-order-created --protocol sqs --notification-endpoint arn:aws:sqs:eu-central-1:000000000000:soh-test-queue
```