## Commands to work with localstack and aws cli

###List all your sns topics:
```shell
aws --endpoint-url http://localhost:4566 sns list-topics
```

###List all your sqs queues:
```shell
aws --endpoint-url http://localhost:4566 sqs list-queues
```

###Publish an message to an sns topic
```shell
aws --endpoint-url http://localhost:4566 sns publish --topic-arn "arn:aws:sns:eu-central-1:000000000000:soh-order-created" --message file://src/test/resources/examples/order.json5 --message-structure json
```
**_Hint:_** Run this command directly from the project root folder or change the message string!

###Read all messages of a queue
```shell
aws --endpoint-url http://localhost:4566 sqs receive-message --queue-url=http://localhost:4566/000000000000/soh-order-created-queue
```

