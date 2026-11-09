package com.examprep.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examprep.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class ApplicationRoutesTest extends AbstractIntegrationTest {

    @Test
    void unknownPagesRenderTheFriendlyErrorPage() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        mockMvc.perform(get("/no-such-page").session(session))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ExamPrep")));
    }

    @Test
    void documentAndStudyPagesRenderEmptyStates() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());

        mockMvc.perform(get("/documents").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nothing uploaded yet")));

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No documents yet")));
    }
}
