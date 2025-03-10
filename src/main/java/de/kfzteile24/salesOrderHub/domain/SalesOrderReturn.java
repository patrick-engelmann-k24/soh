package de.kfzteile24.salesOrderHub.domain;

import de.kfzteile24.salesOrderHub.domain.converter.CreditNoteMessageConverter;
import de.kfzteile24.salesOrderHub.domain.converter.OrderJsonConverter;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.soh.order.dto.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "return_order", schema = "public", catalog = "soh")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SalesOrderReturn {

    @Id
    @GeneratedValue
    private UUID id;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private SalesOrder salesOrder;

    private String orderNumber;

    private String orderGroupId;

    @Column(columnDefinition = "json")
    @Convert(converter = OrderJsonConverter.class)
    private Order returnOrderJson;

    @Column(columnDefinition = "json", name = "credit_note_event")
    @Convert(converter = CreditNoteMessageConverter.class)
    private SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage;

    @Basic
    @Column(name = "url")
    private String url;
}
