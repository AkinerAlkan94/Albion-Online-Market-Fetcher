package com.albionmaster.albionmarketfetcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Component
public class MarketProfitMaker {

    private static final String BASE_URL = "https://www.albion-online-data.com/api/v1/stats/prices";
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();
    private HashMap<String, String> itemNames = new HashMap<>();
    private List<ProfitableItem> profitableItems = new ArrayList<>();

    private static List<Set<String>> splitSet(Set<String> original, int count) {
        ArrayList<Set<String>> result = new ArrayList<>(count);

        Iterator<String> it = original.iterator();

        int each = original.size() / count;

        for (int i = 0; i < count; i++) {
            HashSet<String> s = new HashSet<>(original.size() / count + 1);
            result.add(s);
            for (int j = 0; j < each && it.hasNext(); j++) {
                s.add(it.next());
            }
        }
        return result;
    }

    @PostConstruct
    private void iterateItems() throws IOException {
        List<String> itemKeys = Files.readAllLines(Paths.get("D:\\Repositories\\Personal\\albionmarketfetcher\\src\\main\\java\\com\\albionmaster\\albionmarketfetcher\\ItemKeys.txt"));
        List<String> itemValues = Files.readAllLines(Paths.get("D:\\Repositories\\Personal\\albionmarketfetcher\\src\\main\\java\\com\\albionmaster\\albionmarketfetcher\\ItemValues.txt"));
        String[] interestedCities = {"Bridgewatch", "Fort Sterling", "Lymhurst", "Martlock", "Thetford"};
        List<Item> marketItems = new ArrayList<>();
        for (int i = 0; i < itemKeys.size(); i++) {
            String key = itemKeys.get(i);
            String value = itemValues.get(i);
            itemNames.put(key, value);
        }

        Set<String> keys = itemNames.keySet();
        List<Set<String>> keyParts = splitSet(keys, 50);
        int i = 1;
        for (Set<String> part : keyParts) {
            StringBuilder builder = new StringBuilder("/");
            part.forEach(x -> builder.append(x).append(","));
            String pathParameter = builder.toString();
            pathParameter = pathParameter.substring(0, pathParameter.length() - 1);
            marketItems.addAll(fetch(pathParameter));
            System.out.println(String.format("Fetching Parts %s / %s", i++, keyParts.size()));
        }
        System.out.println("Finished Fetching..");
        System.out.println("Filtering the results");
        List<Item> filtered = marketItems.stream().filter(item -> {

            if (Arrays.asList(interestedCities).contains(item.city)) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String today = formatter.format(new Date());
                if ((item.sellPriceMax > 0.0 || item.sellPriceMin > 0.0)) {
                    return item.sellPriceMin < 10000.0 || item.sellPriceMax < 10000.0;
                }
            }
            return false;
        }).collect(Collectors.toList());
        System.out.println("Filtered..");

        System.out.println("Grouping The Results");
        Map<String, List<Item>> grouped = filtered.stream().collect(groupingBy(item -> item.itemId));
        System.out.println("Grouped..");

        System.out.println("Comparing Results to Find Best Profitable Items");
        int count = 0;


        for (Map.Entry<String, List<Item>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                if (count++ > 50) {
                    System.out.print(".");
                    count = 0;
                }
                String itemId = entry.getKey();
                List<Item> items = entry.getValue();

                items.sort(Comparator.comparingDouble(pi -> pi.sellPriceMax));

                String minCity = items.get(0).city;
                String maxCity = items.get(1).city;
                double minSellPrice = items.get(0).sellPriceMax;
                double maxSellPrice = items.get(1).sellPriceMax;

                double percentage = (maxSellPrice - minSellPrice) / minSellPrice;
                if (percentage > 0.05) {
                    ProfitableItem profitableItem = new ProfitableItem();
                    profitableItem.setItemId(itemId);
                    profitableItem.setItemName(itemNames.get(itemId));
                    profitableItem.setMaxCity(maxCity);
                    profitableItem.setMinCity(minCity);
                    profitableItem.setMaxPrice(maxSellPrice);
                    profitableItem.setMinPrice(minSellPrice);
                    profitableItem.setProfitPercentage(percentage);
                    profitableItem.setOtherCitiesAverage(0.0);
                    profitableItems.add(profitableItem);
                }
            }
        }

        System.out.println("Profits are calculated");
        profitableItems.sort((pi1, pi2) -> Double.compare(pi2.getProfitPercentage(), pi1.getProfitPercentage()));

        Arrays.stream(interestedCities).forEach(city -> {
            System.out.println("\n\n---- " + city + " Profits ---- ");
            profitableItems.stream().filter(pi -> pi.getMinCity().equalsIgnoreCase(city)).forEach(System.out::println);
        });
    }

    private List<Item> fetch(String items) throws JsonProcessingException {
        String uri = BASE_URL + items;
        String body = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(null), String.class).getBody();
        return Arrays.asList(objectMapper.readValue(body, Item[].class));
    }

}
