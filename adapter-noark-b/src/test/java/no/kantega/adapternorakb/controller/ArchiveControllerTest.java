package no.kantega.adapternorakb.controller;

import no.kantega.adapternorakb.model.ArchiveResult;
import no.kantega.adapternorakb.service.ArchiveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArchiveController.class)
class ArchiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArchiveService archiveService;

    @Test
    void archive_returnsOkOnSuccess() throws Exception {
        when(archiveService.archive(any())).thenReturn(ArchiveResult.ok());

        mockMvc.perform(post("/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":1,\"groupName\":\"Test\",\"entries\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.adapter").value("noark-b"));
    }

    @Test
    void archive_returns502OnFailure() throws Exception {
        when(archiveService.archive(any())).thenReturn(ArchiveResult.fail("error"));

        mockMvc.perform(post("/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":1,\"groupName\":\"Test\",\"entries\":[]}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));
    }
}
