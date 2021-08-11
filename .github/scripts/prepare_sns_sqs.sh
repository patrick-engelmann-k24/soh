#!/bin/bash

# exit on error
set -e

#set -x # print commands for debugging purposes
export AWS_PAGER=""
export AWS_DEFAULT_REGION=eu-central-1
export AWS_SECRET_ACCESS_KEY=000000000000
export AWS_ACCESS_KEY_ID=000000000000

TOPICS="
    soh-delivery-address-changed
    soh-order-created
    soh-order-completed
    soh-order-cancelled
    soh-order-item-cancelled
    soh-order-item-delivered
    soh-order-item-packing-started
    soh-order-item-tour-started
    soh-order-item-tracking-id-received
    soh-order-item-transmitted-to-logistic
    soh-order-item-shipped
    soh-order-payment-secured
    soh-invoice-address-changed
    soh-invoices-from-core
    soh-ecp-shop-orders
    "

# Outdated topics
#    soh-order-invoice-created

QUEUES="
    soh-cdr-own-delivery-picklist-shipped
    "

INT_AWS_HOST="http://localhost:4566"

createSQSSNS () {
  aws --cli-connect-timeout 60 --cli-read-timeout 60 --endpoint-url ${INT_AWS_HOST} sns create-topic --name $1
  aws --cli-connect-timeout 60 --cli-read-timeout 60 --endpoint-url ${INT_AWS_HOST} sqs create-queue --queue-name $1"-queue"
  aws --cli-connect-timeout 60 --cli-read-timeout 60 --endpoint-url ${INT_AWS_HOST} sns subscribe --topic arn:aws:sns:eu-central-1:000000000000:$1 --protocol sqs --notification-endpoint "arn:aws:sqs:eu-central-1:000000000000:$1-queue"
}

for CURRENT_TOPICS in ${TOPICS}
do
  echo "creating SQS/SNS ${CURRENT_TOPICS}"
  createSQSSNS "$CURRENT_TOPICS"
  echo " .. done"; echo ""
done;

for CURRENT_QUEUE in ${QUEUES}
do
  echo "creating SQS ${CURRENT_QUEUE}"
  aws --cli-connect-timeout 60 --cli-read-timeout 60 --endpoint-url ${INT_AWS_HOST} sqs create-queue --queue-name ${CURRENT_QUEUE}"-queue"
  echo " .. done "; echo ""
done;
