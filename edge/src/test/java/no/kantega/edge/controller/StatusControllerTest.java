package no.kantega.edge.controller;

import no.kantega.edge.model.ArchiveGroup;
import no.kantega.edge.model.ArchiveGroup.ArchiveStatus;
import no.kantega.edge.repository.ArchiveGroupRepository;
import no.kantega.edge.service.ArchiveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatusController.class)
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArchiveGroupRepository repository;

    @MockitoBean
    private ArchiveService archiveService;

    @Test
    void getOverview_returnsCounts() throws Exception {
        List<ArchiveGroup> groups = List.of(
                createGroup(1L, ArchiveStatus.PENDING),
                createGroup(2L, ArchiveStatus.ARCHIVED),
                createGroup(3L, ArchiveStatus.ARCHIVED),
                createGroup(4L, ArchiveStatus.FAILED)
        );
        when(repository.findAll()).thenReturn(groups);

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.archived").value(2))
                .andExpect(jsonPath("$.failed").value(1));
    }

    @Test
    void getAllGroups_returnsList() throws Exception {
        when(repository.findAll()).thenReturn(List.of(createGroup(1L, ArchiveStatus.PENDING)));

        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getGroup_found() throws Exception {
        ArchiveGroup group = createGroup(1L, ArchiveStatus.ARCHIVED);
        when(repository.findByGroupId(1L)).thenReturn(Optional.of(group));

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(1));
    }

    @Test
    void getGroup_notFound() throws Exception {
        when(repository.findByGroupId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/groups/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retryFailed_triggersRetry() throws Exception {
        mockMvc.perform(post("/api/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("retry triggered"));
    }

    @Test
    void retryGroup_found_triggersRetry() throws Exception {
        ArchiveGroup group = createGroup(1L, ArchiveStatus.FAILED);
        when(repository.findByGroupId(1L)).thenReturn(Optional.of(group));

        mockMvc.perform(post("/api/groups/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("retry triggered for group 1"));

        verify(archiveService).retryGroup(group);
    }

    @Test
    void retryGroup_notFound() throws Exception {
        when(repository.findByGroupId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/groups/99/retry"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllGroups_clearsRepository() throws Exception {
        mockMvc.perform(delete("/api/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("all groups deleted"));

        verify(repository).deleteAll();
    }

    private ArchiveGroup createGroup(Long groupId, ArchiveStatus status) {
        ArchiveGroup group = new ArchiveGroup();
        group.setGroupId(groupId);
        group.setName("Group " + groupId);
        group.setStatus(status);
        group.setEntries(new ArrayList<>());
        group.setErrors(new ArrayList<>());
        group.setCreatedAt(Instant.now());
        group.setUpdatedAt(Instant.now());
        return group;
    }
}
