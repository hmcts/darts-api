package uk.gov.hmcts.darts.Events.controller;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

    @WebMvcTest
    @ActiveProfiles("local")
    class EventsControllerTest {
        @MockBean
        private NotificationRepository notificationRepository;

        @Autowired
        private transient MockMvc mockMvc;

        @Test
        void dailyListAddDailyListEndpoint() throws Exception {
            String requestBody = "{}"; //getContentsFromFile("Tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");
            MockHttpServletRequestBuilder requestBuilder = post("/event/addDocument")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("message_id", "400")
                .queryParam("type", "1")
                .queryParam("sub_type", "1")
                .content(requestBody);
            MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();

            assertThat(response.getResponse().getContentAsString()).isEqualTo("");
        }

//        @Test
//        void dailyListGetCasesEndpoint() throws Exception {
//            MockHttpServletRequestBuilder requestBuilder = get("/dailylist/getCases")
//                .contentType(MediaType.APPLICATION_JSON_VALUE)
//                .queryParam("court_house_code", "457")
//                .queryParam("court_room_number", "1")
//                .queryParam("hearing_date", "2023-02-02");
//            MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();
//
//            assertThat(response.getResponse().getContentAsString()).isEqualTo("");
//        }
//
//        private String getContentsFromFile(String filelocation) throws IOException {
//            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//            File file = new File(classLoader.getResource(filelocation).getFile());
//            return FileUtils.readFileToString(file, "UTF-8");
//        }
//    }

}
