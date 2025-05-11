package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
@Import(GlobalValidationHandler.class)
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    private Item validItem() {
        return new Item(null, "Valid Name", "Valid Description", "ACTIVE", "test@example.com");
    }

    @Test
    void shouldCreateItemSuccessfully() throws Exception {
        Item input = validItem();
        Item saved = new Item(1L, input.getName(), input.getDescription(), input.getStatus(), input.getEmail());

        Mockito.when(itemService.save(any()))
                .thenReturn(CompletableFuture.completedFuture(new Item(1L, "Test", "Desc", "ACTIVE", "email@test.com")));

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturnBadRequestWhenEmailInvalid() throws Exception {
        Item invalid = validItem();
        invalid.setEmail("not-an-email");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Validation failed"));
    }

    @Test
    void shouldReturnItemById() throws Exception {
        Item item = validItem();
        item.setId(1L);

        Mockito.when(itemService.findById(eq(1L)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(item)));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturnNotFoundForMissingItem() throws Exception {
        Mockito.when(itemService.findById(eq(999L)))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        mockMvc.perform(get("/api/items/999"))
                .andExpect(status().isNotFound());
    }
}
