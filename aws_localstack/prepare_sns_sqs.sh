#!/bin/bash

#set -x # print commands for debugging purposes
export AWS_PAGER=""

TOPICS="soh-order-payment-secured soh-order-invoice-created soh-order-created soh-order-completed soh-order-cancelled soh-order-item-cancelled soh-invoice-address-changed soh-delivery-address-changed order-item-transmitted-to-logistics order-item-packing-started order-item-tracking-id-received order-item-delivered order-item-tour-started"

QUEUES="soh-ecp-shop-orders soh-order-item-shipped soh-order-payment-secured soh-order-item-transmitted-to-logistic soh-order-item-packing-started soh-order-item-tracking-id-received"

INT_AWS_HOST=${AWS_HOST:-http://localhost:4566}

createSQSSNS () {
  aws --endpoint-url ${INT_AWS_HOST} sns create-topic --name $1
  aws --endpoint-url ${INT_AWS_HOST} sqs create-queue --queue-name $1"-queue"
  aws --endpoint-url ${INT_AWS_HOST} sns subscribe --topic arn:aws:sns:eu-central-1:000000000000:$1 --protocol sqs --notification-endpoint arn:aws:sqs:eu-central-1:000000000000:$1"-queue"
}

for CURRENT_TOPICS in ${TOPICS}
do
  echo "create SQS/SNS ${CURRENT_TOPICS}"
  createSQSSNS "$CURRENT_TOPICS"
  echo " .. done"; echo ""
done;

for CURRENT_QUEUE in ${QUEUES}
do
  echo "create SQS ${CURRENT_QUEUE}"
  aws --endpoint-url ${INT_AWS_HOST} sqs create-queue --queue-name ${CURRENT_QUEUE}"-queue"
  echo " .. done "; echo ""
done;
