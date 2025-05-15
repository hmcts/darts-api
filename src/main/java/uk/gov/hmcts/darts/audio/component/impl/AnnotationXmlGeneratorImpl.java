package uk.gov.hmcts.darts.audio.component.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.ViqAnnotationData;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

@Component
@Qualifier("annotationXmlGenerator")
public class AnnotationXmlGeneratorImpl extends AbstractDocumentGenerator {

    private static final String ANNOTATION_ROOT_ELEMENT_NAME = "cfMetaFile";
    private static final String ANNOTATION_ANNOTATION_ELEMENT_NAME = "annotations";
    private static final String ANNOTATION_COUNT_ATTRIBUTE_NAME = "count";
    private static final String ANNOTATION_EVENT_ELEMENT_NAME = "a";
    private static final String ANNOTATION_EVENT_DEFAULT_LABEL = "operator";
    private static final String ANNOTATION_EVENT_DEFAULT_RESTRICTION = "0";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_LABEL = "L";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_TEXT = "N";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_START_TIME_MILLIS = "T";

    private static final String ANNOTATION_EVENT_ATTRIBUTE_YEAR = "Y";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_MONTH = "M";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_DAY = "D";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_HOUR = "H";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_MINUTES = "MIN";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_SECONDS = "S";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_RESTRICTED = "R";
    private static final String ANNOTATION_EVENT_ATTRIBUTE_LAPSED = "P";

    public AnnotationXmlGeneratorImpl() throws ParserConfigurationException {
        super();
    }

    @Override
    public Path generateAndWriteXmlFile(Object data, Path outboundFilePath)
        throws ParserConfigurationException, IOException, TransformerException {

        if (!(data instanceof ViqAnnotationData)) {
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST);
        }

        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        setDocumentBuilder(documentFactory.newDocumentBuilder());
        Document document = getDocumentBuilder().newDocument();

        ViqAnnotationData annotationData = (ViqAnnotationData) data;
        int eventCounter = 0;

        // Root element
        Element root = document.createElement(ANNOTATION_ROOT_ELEMENT_NAME);
        document.appendChild(root);
        // Annotation element
        Element annotations = document.createElement(ANNOTATION_ANNOTATION_ELEMENT_NAME);
        root.appendChild(annotations);
        annotations.setAttribute(ANNOTATION_COUNT_ATTRIBUTE_NAME, String.valueOf(annotationData.getEvents().size()));

        // Events
        for (EventEntity event : annotationData.getEvents()) {

            ZonedDateTime localEventTimestamp = DateConverterUtil.toZonedDateTime(event.getTimestamp());

            Element eventElement = document.createElement(String.format(
                "%s%d",
                ANNOTATION_EVENT_ELEMENT_NAME,
                eventCounter
            ));
            eventElement.setAttribute(ANNOTATION_EVENT_ATTRIBUTE_LABEL, ANNOTATION_EVENT_DEFAULT_LABEL);
            eventElement.setAttribute(ANNOTATION_EVENT_ATTRIBUTE_TEXT, event.getEventText());
            eventElement.setAttribute(
                ANNOTATION_EVENT_ATTRIBUTE_START_TIME_MILLIS,
                String.valueOf(localEventTimestamp.toInstant().toEpochMilli())
            );
            eventElement.setAttribute(ANNOTATION_EVENT_ATTRIBUTE_YEAR, String.valueOf(localEventTimestamp.getYear()));
            eventElement.setAttribute(
                ANNOTATION_EVENT_ATTRIBUTE_MONTH,
                String.valueOf(localEventTimestamp.getMonthValue())
            );
            eventElement.setAttribute(
                ANNOTATION_EVENT_ATTRIBUTE_DAY,
                String.valueOf(localEventTimestamp.getDayOfMonth())
            );
            eventElement.setAttribute(ANNOTATION_EVENT_ATTRIBUTE_HOUR, String.valueOf(localEventTimestamp.getHour()));
            eventElement.setAttribute(
                ANNOTATION_EVENT_ATTRIBUTE_MINUTES,
                String.valueOf(localEventTimestamp.getMinute())
            );
            eventElement.setAttribute(
                ANNOTATION_EVENT_ATTRIBUTE_SECONDS,
                String.valueOf(localEventTimestamp.getSecond())
            );
            eventElement.setAttribute(ANNOTATION_EVENT_ATTRIBUTE_RESTRICTED, ANNOTATION_EVENT_DEFAULT_RESTRICTION);
            eventElement.setAttribute(
                ANNOTATION_EVENT_ATTRIBUTE_LAPSED,
                String.valueOf(Duration.between(annotationData.getAnnotationsStartTime(), localEventTimestamp)
                                   .getSeconds())
            );

            eventElement.appendChild(document.createTextNode(event.getEventType().getEventName()));
            annotations.appendChild(eventElement);
            eventCounter++;
        }

        transformDocument(document, outboundFilePath);

        return outboundFilePath;
    }

}
