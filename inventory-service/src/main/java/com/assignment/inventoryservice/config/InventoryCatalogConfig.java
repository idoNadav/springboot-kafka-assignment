package com.assignment.inventoryservice.config;

import com.assignment.commonmodel.model.Category;
import com.assignment.inventoryservice.model.ProductDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class InventoryCatalogConfig {
    @Bean
    public Map<String, ProductDetails> productCatalog() {
        Map<String, ProductDetails> map = new HashMap<>();
        map.put("P1001", new ProductDetails("P1001", Category.STANDARD, 10, null));
        map.put("P1002", new ProductDetails("P1002", Category.PERISHABLE, 3, LocalDate.of(2025, 9, 1)));
        map.put("P1003", new ProductDetails("P1003", Category.DIGITAL, 0, null));
        map.put("P1005", new ProductDetails("P1005", Category.PERISHABLE, 5, LocalDate.now().plusDays(10)));
        map.put("P1006", new ProductDetails("P1006", Category.PERISHABLE, 2, LocalDate.now().minusDays(2)));
        map.put("P1007", new ProductDetails("P1007", Category.DIGITAL, 9999, null));
        map.put("P1008", new ProductDetails("P1008", Category.STANDARD, 0, null));
        return map;
    }
}
