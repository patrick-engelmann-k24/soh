package de.kfzteile24.salesOrderHub.domain;

import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.domain.converter.SalesOrderInvoiceSourceConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Convert;

@Entity
@Table(name = "sales_order_invoice", schema = "public", catalog = "soh")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SalesOrderInvoice extends AbstractBaseEntity {

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "sales_order_id")
    private SalesOrder salesOrder;

    @Column(name = "order_number")
    private String orderNumber;

    @Basic
    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Basic
    @Column(name = "url")
    private String url;

    @Basic
    @Column(name = "customer_access_token")
    private String customerAccessToken;

    @Column(name = "source")
    @Convert(converter = SalesOrderInvoiceSourceConverter.class)
    private InvoiceSource source;
}
