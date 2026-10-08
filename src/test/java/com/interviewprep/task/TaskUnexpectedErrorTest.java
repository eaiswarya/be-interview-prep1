package com.interviewprep.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@WithMockUser
class TaskUnexpectedErrorTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TaskService service;

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        when(service.list(any())).thenThrow(new IllegalStateException("connection refused to db-host:5432"));

        mvc.perform(get("/api/tasks"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected error"))
                .andExpect(jsonPath("$.path").value("/api/tasks"));
    }
}
