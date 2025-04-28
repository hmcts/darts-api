package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
@ExtendWith(MockitoExtension.class)
class BaseArchiveRecordMapperTest {

    private BaseArchiveRecordMapper baseArchiveRecordMapper;
    private RecordMetadata metadata;

    @BeforeEach
    void setUp() {
        baseArchiveRecordMapper = new BaseArchiveRecordMapper();
        metadata = RecordMetadata.builder().build();
    }

    @Test
    void processStringMetadataProperties_setsCorrectProperty() {
        // given
        assertAllFieldsNull();
        // when
        setStringProperties();
        // then
        assertStringProperties();
    }

    @Test
    void processIntMetadataProperties_setsCorrectProperty() {
        // given
        assertAllFieldsNull();
        // when
        setIntProperties();
        // then
        assertIntProperties();
    }

    private void assertAllFieldsNull() {
        assertNull(metadata.getBf001());
        assertNull(metadata.getBf002());
        assertNull(metadata.getBf003());
        assertNull(metadata.getBf004());
        assertNull(metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertNull(metadata.getBf010());
        assertNull(metadata.getBf011());
        assertNull(metadata.getBf012());
        assertNull(metadata.getBf013());
        assertNull(metadata.getBf014());
        assertNull(metadata.getBf015());
        assertNull(metadata.getBf016());
        assertNull(metadata.getBf017());
        assertNull(metadata.getBf018());
        assertNull(metadata.getBf019());
        assertNull(metadata.getBf020());
    }

    private void setStringProperties() {
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_001", "value1");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_002", "value2");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_003", "value3");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_004", "value4");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_005", "value5");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_006", "value6");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_007", "value7");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_008", "value8");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_009", "value9");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_010", "value10");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_011", "value11");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_012", "value12");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_013", "value13");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_014", "value14");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_015", "value15");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_016", "value16");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_017", "value17");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_018", "value18");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_019", "value19");
        baseArchiveRecordMapper.processStringMetadataProperties(metadata, "bf_020", "value20");
    }

    private void setIntProperties() {
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_001", 1L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_002", 2L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_003", 3L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_004", 4L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_005", 5L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_006", 6L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_007", 7L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_008", 8L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_009", 9L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_010", 10L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_011", 11L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_012", 12L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_013", 13L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_014", 14L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_015", 15L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_016", 16L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_017", 17L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_018", 18L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_019", 19L);
        baseArchiveRecordMapper.processIntMetadataProperties(metadata, "bf_020", 20L);
    }

    private void assertStringProperties() {
        assertEquals("value1", metadata.getBf001());
        assertEquals("value2", metadata.getBf002());
        assertEquals("value3", metadata.getBf003());
        assertEquals("value4", metadata.getBf004());
        assertEquals("value5", metadata.getBf005());
        assertEquals("value6", metadata.getBf006());
        assertEquals("value7", metadata.getBf007());
        assertEquals("value8", metadata.getBf008());
        assertEquals("value9", metadata.getBf009());
        assertEquals("value10", metadata.getBf010());
        assertEquals("value11", metadata.getBf011());
        assertNull(metadata.getBf012());
        assertNull(metadata.getBf013());
        assertNull(metadata.getBf014());
        assertNull(metadata.getBf015());
        assertEquals("value16", metadata.getBf016());
        assertEquals("value17", metadata.getBf017());
        assertEquals("value18", metadata.getBf018());
        assertEquals("value19", metadata.getBf019());
        assertEquals("value20", metadata.getBf020());
    }

    private void assertIntProperties() {
        assertNull(metadata.getBf001());
        assertNull(metadata.getBf002());
        assertNull(metadata.getBf003());
        assertNull(metadata.getBf004());
        assertNull(metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertNull(metadata.getBf010());
        assertNull(metadata.getBf011());
        assertEquals(12, metadata.getBf012());
        assertEquals(13, metadata.getBf013());
        assertEquals(14, metadata.getBf014());
        assertEquals(15, metadata.getBf015());
        assertNull(metadata.getBf016());
        assertNull(metadata.getBf017());
        assertNull(metadata.getBf018());
        assertNull(metadata.getBf019());
        assertNull(metadata.getBf020());
    }

}