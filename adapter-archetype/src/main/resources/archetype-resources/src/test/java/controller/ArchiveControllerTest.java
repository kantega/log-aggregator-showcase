package ${package}.controller;

import ${package}.model.ArchiveResult;
import ${package}.service.ArchiveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .andExpect(jsonPath("$.adapter").value("${providerSlug}"));
    }

    @Test
    void archive_returns502OnFailure() throws Exception {
        when(archiveService.archive(any())).thenReturn(ArchiveResult.fail("timeout"));

        mockMvc.perform(post("/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":1,\"groupName\":\"Test\",\"entries\":[]}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("timeout"));
    }
}
