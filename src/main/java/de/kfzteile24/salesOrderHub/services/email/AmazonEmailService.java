package de.kfzteile24.salesOrderHub.services.email;

import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderHeader;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmazonEmailService {

    @Value("${kfzteile.email.source}")
    protected String sourceEmail;

    @Value("${kfzteile.email.destination}")
    protected String destinationEmail;

    @Value("${kfzteile.email.cc}")
    protected String ccEmail;

    @Value("${kfzteile.email.password}")
    protected String password;

    @Value("${infrastructureEnvironment}")
    protected String environment;

    public void sendOrderCancelledEmail(Order order) {
        String orderNumber = order.getOrderHeader().getOrderNumber();
        log.info("Sending dropshipment order cancellation email for order number {}", orderNumber);
        Properties props = getProperties();
        Session session = Session.getDefaultInstance(props);

        try (Transport transport = session.getTransport()) {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(sourceEmail, sourceEmail));
            msg.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(destinationEmail));
            msg.setRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(ccEmail));
            msg.setSubject(getEnvironmentPrefix() + " Auftragsstorno -  " + orderNumber);
            msg.setContent(createEmailBody(order), "text/plain");

            transport.connect("smtp.gmail.com", sourceEmail, password);
            transport.sendMessage(msg, msg.getAllRecipients());

            log.info("Sent dropshipment order cancellation email for order number {}", orderNumber);
        } catch (Exception ex) {
            log.error("Could not send dropshipment order cancellation email for order number {}", orderNumber, ex);
        }
    }

    private String getEnvironmentPrefix() {
        return environment.equals("prod") ? "" : "[" + environment + "]";
    }

    private Properties getProperties() {
        Properties props = System.getProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.port", 587);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        return props;
    }

    String createEmailBody(Order order) {
        OrderHeader orderHeader = order.getOrderHeader();
        StringBuilder sb = new StringBuilder();
        sb.append("Dieser Dropshipment Auftrag wurde automatisch storniert Aufgrund einer Ablehnung durch delticom.\r\n");
        sb.append("This order was automatically cancelled due to a rejection by delticom.\r\n");

        sb.append("Kundenummer: ").append(orderHeader.getCustomer().getCustomerNumber()).append("\r\n");
        sb.append("Auftragsnummer: ").append(orderHeader.getOrderGroupId()).append("\r\n");
        sb.append("Auftragsgruppe: ").append(orderHeader.getOrderNumber()).append("\r\n");
        sb.append("Stornierte Artikel: \r\n");
        for (OrderRows row : order.getOrderRows()) {
            sb.append("sku: ").append(row.getSku()).append("\r\n");
        }
        return sb.toString();
    }
}
