package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    @Mock
    private ItemRepository repo;

    @InjectMocks
    private ItemService service;

    private Item item1, item2, saved1, saved2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Wire the mock into the private field
        ReflectionTestUtils.setField(service, "itemRepository", repo);

        item1  = new Item(1L, "name1", "description1", "QUEUED", "email1@test.com");
        item2  = new Item(2L, "name2", "description2", "QUEUED", "email2@test.com");
        saved1  = new Item(1L, "name1", "description1", "PROCESSED", "email1@test.com");
        saved2  = new Item(2L, "name2", "description2", "PROCESSED", "email2@test.com");

        // Clear any previous state
        service.processedItems.clear();
        service.processedCount.set(0);
    }

    @Test
    void findAll_returnsAllItems() throws Exception {
        List<Item> all = List.of(item1, item2);
        when(repo.findAll()).thenReturn(all);

        List<Item> result = service.findAll().get(1, TimeUnit.SECONDS);

        assertThat(result).containsExactlyElementsOf(all);
        verify(repo).findAll();
    }

    @Test
    void findById_found() throws Exception {
        when(repo.findById(1L)).thenReturn(Optional.of(item1));

        Optional<Item> opt = service.findById(1L).get();
        assertThat(opt).contains(item1);
        verify(repo).findById(1L);
    }

    @Test
    void save_savesAndReturns() throws Exception {
        when(repo.save(item1)).thenReturn(saved1);

        Item out = service.save(item1).get();
        assertThat(out.getStatus()).isEqualTo("PROCESSED");
        verify(repo).save(item1);
    }

    @Test
    void deleteById_invokesRepository() throws Exception {
        CompletableFuture<Void> f = service.deleteById(99L);
        // should complete without error
        f.get(1, TimeUnit.SECONDS);
        verify(repo).deleteById(99L);
    }

    @Test
    void processItemsAsync_allProcessed() throws Exception {
        when(repo.findAllIds()).thenReturn(List.of(1L, 2L));
        when(repo.findById(1L)).thenReturn(Optional.of(item1));
        when(repo.findById(2L)).thenReturn(Optional.of(item2));

        when(repo.save(any(Item.class)))
                .thenAnswer(inv -> {
                    Item arg = inv.getArgument(0);
                    return new Item(arg.getId(), arg.getName(), arg.getDescription(), arg.getStatus(), arg.getEmail());
                });

        List<Item> processed = service.processItemsAsync().get(5, TimeUnit.SECONDS);

        // verify two items, both with status PROCESSED
        assertThat(processed)
                .hasSize(2)
                .allMatch(it -> "PROCESSED".equals(it.getStatus()));

        // verify the counter incremented twice
        assertThat(service.processedCount.get()).isEqualTo(2);

        verify(repo, times(2)).save(any(Item.class));
    }
    @Test
    void processItemsAsync_repositoryThrows_failsFuture() {
        when(repo.findAllIds()).thenReturn(List.of(1L));
        when(repo.findById(1L)).thenThrow(new RuntimeException("DB down"));

        CompletableFuture<List<Item>> future = service.processItemsAsync();
        assertThatThrownBy(future::join)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB down");
    }
}
