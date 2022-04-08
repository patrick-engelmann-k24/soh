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
    soh-order-created-v2
    soh-sales-order-completed-v1
    soh-order-cancelled
    soh-order-item-cancelled
    soh-order-rows-cancelled-v1
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
    soh-bc-shop-orders
    soh-core-shop-orders
    soh-sales-order-row-cancelled
    soh-sales-order-cancelled
    soh-core-cancellation
    soh-subsequent-delivery-received
    soh-order-invoice-created-v1
    soh-d365-order-payment-secured
    soh-dropshipment-shipment-confirmed
    soh-shipment-confirmed-v1
    soh-dropshipment-purchase-order-booked
    soh-core-return-delivery-note-printed
    soh-return-receipt-calculated-v1
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
