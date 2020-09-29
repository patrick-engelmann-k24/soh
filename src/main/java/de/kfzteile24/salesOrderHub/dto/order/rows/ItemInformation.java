package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

@Data
public class ItemInformation {
    String imageUrl;
    String thumbnailUrl;
    String brand;
    Boolean isBulkyGood;
    Boolean isRiskyGood;
    Boolean isHazardousGood;
    String legalNotice;
    Boolean pricehammer;
    ArticleInfo offer;
    ArticleInfo order;
    ArticleInfo invoice;
}
