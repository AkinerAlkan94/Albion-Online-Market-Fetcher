package com.albionmaster.albionmarketfetcher;

import lombok.Data;

@Data
public class ProfitableItem {
    String minCity;
    String maxCity;
    String itemId;
    String itemName;
    double minPrice;
    double maxPrice;
    double profitPercentage;
    double otherCitiesAverage;

    public String toString() {
        return String.format("%s , Min Price: %2f, Max Price: %2f", itemName, minPrice, maxPrice);
    }
}
