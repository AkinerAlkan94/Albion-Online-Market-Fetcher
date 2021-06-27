package com.albionmaster.albionmarketfetcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    @JsonProperty("item_id")
    public String itemId;

    public String city;
    public int quality;

    @JsonProperty("sell_price_max")
    public double sellPriceMax;

    @JsonProperty("sell_price_min")
    public double sellPriceMin;

    @JsonProperty("buy_price_max")
    public double buyPriceMax;

    @JsonProperty("sell_price_max_date")
    public String sellPriceMaxDate;

    @JsonProperty("sell_price_min_date")
    public String sellPriceMinDate;
}
