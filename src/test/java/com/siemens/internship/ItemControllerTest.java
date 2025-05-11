package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ItemService service;

    private Item item1, item2;

    @BeforeEach
    void setup() {
        item1  = new Item(1L, "name1", "description1", "QUEUED", "email1@test.com");
        item2  = new Item(2L, "name2", "description2", "QUEUED", "email2@test.com");
    }

    @Test
    void getAllItems() throws Exception {
        List<Item> all = List.of(item1, item2);
        Mockito.when(service.findAll())
                .thenReturn(CompletableFuture.completedFuture(all));

        MvcResult mvcResult = mvc.perform(get("/api/items"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(all)));
    }

    @Test
    void getItemById_found() throws Exception {
        Mockito.when(service.findById(1L))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(item1)));

        MvcResult mvcResult = mvc.perform(get("/api/items/1"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(item1)));
    }

    @Test
    void getItemById_notFound() throws Exception {
        Mockito.when(service.findById(99L))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        MvcResult mvcResult = mvc.perform(get("/api/items/99"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound());
    }

    @Test
    void createItem_success() throws Exception {
        Item toCreate = new Item(null, "name3", "description3", "QUEUED", "email3@test.com");
        Item created  = new Item(3L, "name3", "description3", "QUEUED", "email3@test.com");

        Mockito.when(service.save(any()))
                .thenReturn(CompletableFuture.completedFuture(created));

        MvcResult mvcResult = mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(toCreate)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(content().json(mapper.writeValueAsString(created)));
    }

    @Test
    void createItem_conflict() throws Exception {
        Item toCreate = new Item(null, "name3", "description3", "QUEUED", "email3@test.com");

        Mockito.when(service.save(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("dup")));

        MvcResult mvcResult = mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(toCreate)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isConflict());
    }

    @Test
    void updateItem_notFound() throws Exception {
        Item update = new Item(null, "name5", "description5", "QUEUED", "email5@test.com");

        Mockito.when(service.findById(5L))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        MvcResult mvcResult = mvc.perform(put("/api/items/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(update)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItem_success() throws Exception {
        Item existing = new Item(2L, "name5", "description5", "QUEUED", "email5@test.com");
        Item updated  = new Item(2L, "name5-upd", "description5", "QUEUED", "email5@test.com");

        Mockito.when(service.findById(6L))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));
        Mockito.when(service.save(any()))
                .thenReturn(CompletableFuture.completedFuture(updated));

        MvcResult mvcResult = mvc.perform(put("/api/items/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(updated)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(updated)));
    }

    @Test
    void deleteItem_noContent() throws Exception {
        Mockito.when(service.deleteById(7L))
                .thenReturn(CompletableFuture.completedFuture(null));

        MvcResult mvcResult = mvc.perform(delete("/api/items/7"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteItem_notFound() throws Exception {
        Mockito.when(service.deleteById(8L))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("not found")));

        MvcResult mvcResult = mvc.perform(delete("/api/items/8"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound());
    }

    @Test
    void processItems() throws Exception {
        List<Item> processed = List.of(item1, item2);
        Mockito.when(service.processItemsAsync())
                .thenReturn(CompletableFuture.completedFuture(processed));

        MvcResult mvcResult = mvc.perform(get("/api/items/process"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(processed)));
    }
}
