package uk.gov.hmcts.darts.audio.component;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

@FunctionalInterface
public interface OutboundDocumentGenerator<T> {
    Path generateAndWriteXmlFile(Object documentData, Path outboundFilePath)
        throws ParserConfigurationException, IOException, TransformerException;
}
