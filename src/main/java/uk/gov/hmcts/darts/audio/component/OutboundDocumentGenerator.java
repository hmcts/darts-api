package uk.gov.hmcts.darts.audio.component;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public interface OutboundDocumentGenerator<T> {

    Path generateAndWriteXmlFile(Object documentData, String outboundDirectory) throws ParserConfigurationException, IOException, TransformerException;
}
