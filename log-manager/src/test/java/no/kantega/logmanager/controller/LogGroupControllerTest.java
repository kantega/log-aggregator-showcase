package no.kantega.logmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.kantega.logmanager.model.LogEntry;
import no.kantega.logmanager.model.LogGroup;
import no.kantega.logmanager.service.LogManagerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogGroupController.class)
class LogGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogManagerService logManagerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createGroup_returns201() throws Exception {
        LogGroup group = LogGroup.builder()
                .id(1L)
                .name("Test Group")
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(logManagerService.createGroup("Test Group")).thenReturn(group);

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Test Group"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Group"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getAllGroups_returnsList() throws Exception {
        LogGroup group = LogGroup.builder()
                .id(1L)
                .name("Test Group")
                .status("OPEN")
                .build();
        when(logManagerService.getAllGroups()).thenReturn(List.of(group));

        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Group"));
    }

    @Test
    void getGroup_returnsGroup() throws Exception {
        LogGroup group = LogGroup.builder()
                .id(1L)
                .name("Test Group")
                .status("OPEN")
                .build();
        when(logManagerService.getGroup(1L)).thenReturn(group);

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Group"));
    }

    @Test
    void addEntry_returns201() throws Exception {
        LogEntry entry = LogEntry.builder()
                .id(1L)
                .content("Test entry")
                .createdAt(LocalDateTime.now())
                .build();
        when(logManagerService.addEntry(eq(1L), eq("Test entry"))).thenReturn(entry);

        mockMvc.perform(post("/api/groups/1/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Test entry"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Test entry"));
    }

    @Test
    void closeGroup_returns200() throws Exception {
        LogGroup group = LogGroup.builder()
                .id(1L)
                .name("Test Group")
                .status("CLOSED")
                .build();
        when(logManagerService.closeGroup(1L)).thenReturn(group);

        mockMvc.perform(post("/api/groups/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }
}
