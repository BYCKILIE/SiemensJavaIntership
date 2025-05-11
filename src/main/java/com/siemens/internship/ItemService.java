package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Bean(name = "executor")
    public Executor taskExecutor() {
        return Executors.newFixedThreadPool(10);
    }

    // Updated collection types to Thread Safe ones
    ConcurrentLinkedQueue<Item> processedItems = new ConcurrentLinkedQueue<>();
    AtomicInteger processedCount = new AtomicInteger(0);

    // Modified the methods to work async
    @Async("executor")
    public CompletableFuture<List<Item>> findAll() {
        return CompletableFuture.supplyAsync(() -> itemRepository.findAll());
    }

    @Async("executor")
    public CompletableFuture<Optional<Item>> findById(Long id) {
        return CompletableFuture.supplyAsync(() -> itemRepository.findById(id));
    }

    @Async("executor")
    public CompletableFuture<Item> save(Item item) {
        return CompletableFuture.supplyAsync(() -> itemRepository.save(item));
    }

    @Async("executor")
    public CompletableFuture<Void> deleteById(Long id) {
        return CompletableFuture.runAsync(() -> itemRepository.deleteById(id));
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     * <p>
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async("executor")
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        // Declaring a list of async tasks for each processing
        List<CompletableFuture<Void>> futures = itemIds.stream()
                .map(id -> CompletableFuture.runAsync(() ->
                        itemRepository.findById(id).ifPresent(item -> {
                            item.setStatus("PROCESSED");
                            Item saved = itemRepository.save(item);
                            processedItems.add(saved);
                            processedCount.incrementAndGet();
                        })))
                .toList();

        // Traverse-like processing to await every async extract + map to finish
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> new ArrayList<>(processedItems));
    }

}

