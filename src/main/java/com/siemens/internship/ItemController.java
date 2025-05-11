package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    /*
    Modified structure to process the data asynchronously
     */

    @GetMapping
    public CompletableFuture<ResponseEntity<List<Item>>> getAllItems() {
        return itemService.findAll().thenApply(itemsList -> new ResponseEntity<>(itemsList, HttpStatus.OK));
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<Item>> createItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            return CompletableFuture.completedFuture(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));
        }
        return itemService.save(item).thenApply(savedItem -> new ResponseEntity<>(savedItem, HttpStatus.CREATED))
                .exceptionally(ex -> new ResponseEntity<>(HttpStatus.CONFLICT));
    }

    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<Item>> getItemById(@PathVariable Long id) {
        return itemService.findById(id).thenApply(maybeItem ->
                maybeItem
                        .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                        .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
    }

    // Added result to verify if parsing errors
    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<Item>> updateItem(
            @PathVariable Long id,
            @RequestBody Item item,
            BindingResult result
    ) {
        if (result.hasErrors()) {
            return CompletableFuture.completedFuture(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));
        }
        return itemService.findById(id)
                .thenCompose(maybeItem -> {
                    if (maybeItem.isEmpty()) {
                        return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
                    }

                    item.setId(id);
                    return itemService.save(item)
                            .thenApply(savedItem -> new ResponseEntity<>(savedItem, HttpStatus.OK))
                            .exceptionally(ex -> new ResponseEntity<>(HttpStatus.CONFLICT));
                });
    }

    // Modified return type to fit the error case
    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Object>> deleteItem(@PathVariable Long id) {
        return itemService.deleteById(id).thenApply(voidResult -> new ResponseEntity<>(HttpStatus.NO_CONTENT))
                .exceptionally(ex -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/process")
    public CompletableFuture<ResponseEntity<List<Item>>> processItems() {
        return itemService.processItemsAsync().thenApply(processedItems ->
                new ResponseEntity<>(processedItems, HttpStatus.OK));
    }
}
