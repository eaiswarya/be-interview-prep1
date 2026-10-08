package com.interviewprep.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Seeds 100 products on startup when the catalog is empty. A fixed random seed keeps the data the same every run. */
@Component
public class ProductSeeder implements ApplicationRunner {

    static final int PRODUCT_COUNT = 100;

    private static final List<String> CATEGORIES = List.of("Electronics", "Books", "Home", "Sports", "Toys");
    private static final List<String> ADJECTIVES = List.of("Classic", "Smart", "Compact", "Deluxe", "Eco", "Pro");
    private static final List<String> NOUNS = List.of("Lamp", "Speaker", "Notebook", "Bottle", "Backpack", "Puzzle",
            "Headphones", "Chair", "Ball", "Kettle");

    private final ProductRepository repository;

    public ProductSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        Random random = new Random(42);
        List<Product> products = new ArrayList<>(PRODUCT_COUNT);
        for (int i = 1; i <= PRODUCT_COUNT; i++) {
            String name = ADJECTIVES.get(random.nextInt(ADJECTIVES.size())) + " "
                    + NOUNS.get(random.nextInt(NOUNS.size())) + " " + i;
            String category = CATEGORIES.get(random.nextInt(CATEGORIES.size()));
            BigDecimal price = BigDecimal.valueOf(5 + random.nextDouble() * 495).setScale(2, RoundingMode.HALF_UP);
            int stock = random.nextInt(5) == 0 ? 0 : random.nextInt(200); // about 1 in 5 out of stock
            double rating = Math.round((1 + random.nextDouble() * 4) * 10) / 10.0;
            products.add(new Product(name, category, price, stock, rating));
        }
        repository.saveAll(products);
    }
}
