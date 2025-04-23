package uk.gov.hmcts.darts.audio.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchResponseItem;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.io.IOException;
import java.util.List;

import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

class PostAdminMediaSearchResponseMapperTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = TestUtils.getObjectMapper();
    }

    @Test
    void multipleEntities() throws IOException {
        MediaEntity media = CommonTestDataUtil.createMedia("caseNumber1");
        MediaEntity media2 = CommonTestDataUtil.createMedia(media.getHearing());
        media2.setStart(media2.getStart().plusMinutes(1));
        MediaEntity media3 = CommonTestDataUtil.createMedia(media.getHearing());
        media3.setStart(media3.getStart().plusMinutes(2));
        List<PostAdminMediasSearchResponseItem> responseItemList = PostAdminMediaSearchResponseMapper.createResponseItemList(List.of(media, media2, media3));

        String actualResponse = objectMapper.writeValueAsString(responseItemList);
        String expectedResponse = getContentsFromFile(
            "Tests/audio/PostAdminMediaSearchResponseMapperTest/multipleEntities/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }
}