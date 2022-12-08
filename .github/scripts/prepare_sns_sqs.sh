#!/bin/bash

# exit on error
set -e

#set -x # print commands for debugging purposes
export AWS_PAGER=""
export AWS_DEFAULT_REGION=eu-central-1
export AWS_SECRET_ACCESS_KEY=000000000000
export AWS_ACCESS_KEY_ID=000000000000

QUEUES="
    soh-order-created-v2
    soh-sales-order-completed-v1
    soh-order-payment-secured
    soh-invoice-address-changed
    soh-invoices-from-core
    soh-ecp-shop-orders
    soh-bc-shop-orders
    soh-core-shop-orders
    soh-sales-order-row-cancelled
    soh-sales-order-cancelled
    soh-order-invoice-created-v1
    soh-d365-order-payment-secured
    soh-dropshipment-shipment-confirmed
    soh-shipment-confirmed-v1
    soh-dropshipment-purchase-order-booked
    soh-dropshipment-purchase-order-return-confirmed
    soh-core-sales-credit-note-created
    soh-dropshipment-purchase-order-return-notified
    soh-return-order-created-v1
    soh-credit-note-received-v1
    soh-credit-note-created-v1
    soh-credit-note-document-generated-v1
    soh-core-sales-invoice-created
    soh-core-invoice-received-v1
    migration-core-sales-order-created
    migration-core-sales-invoice-created
    migration-core-sales-credit-note-created
    migration-soh-order-created-v2
    migration-soh-sales-order-row-cancelled-v1
    migration-soh-sales-order-cancelled-v1
    migration-soh-return-order-created-v1
    soh-dropshipment-order-created-v1
    soh-dropshipment-order-return-notified-v1
    soh-parcel-shipped
    soh-paypal-refund-instruction-successful
    soh-payout-receipt-confirmation-received-v1
    soh-invoice-pdf-generation-triggered-v1
    soh-core-sales-order-cancelled
    "

INT_AWS_HOST="http://localhost:4566"

createSQSSNS () {
  aws --cli-connect-timeout 60 --cli-read-timeout 60 --endpoint-url ${INT_AWS_HOST} sqs create-queue --queue-name $1"-queue"
}

for CURRENT_QUEUE in ${QUEUES}
do
  echo "creating SQS/SNS ${CURRENT_QUEUE}"
  createSQSSNS "$CURRENT_QUEUE"
  echo " .. done"; echo ""
done;
