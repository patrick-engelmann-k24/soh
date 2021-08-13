package de.kfzteile24.salesOrderHub.domain;

import de.kfzteile24.salesOrderHub.domain.converter.OrderJsonConverter;
import de.kfzteile24.soh.order.dto.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "sales_order", schema = "public", catalog = "soh")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class SalesOrder extends AbstractBaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "process_id")
    private String processId;

    @Column(name = "original_order", columnDefinition = "json", updatable = false)
    @Convert(converter = OrderJsonConverter.class)
    private Object originalOrder;

    @Column(name = "latest_json", columnDefinition = "json")
    @Convert(converter = OrderJsonConverter.class)
    private Order latestJson;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "recurring_order")
    private boolean recurringOrder;

    @Column(name = "sales_channel")
    private String salesChannel;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "salesOrder", targetEntity = SalesOrderInvoice.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<SalesOrderInvoice> salesOrderInvoiceList;

}
