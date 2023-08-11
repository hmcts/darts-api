package uk.gov.hmcts.darts.audio.util;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.File;

public final class XmlUtil {

    private XmlUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String marshalToXmlFile(Object object, Class<?> type, String outputFileLocation, String filename) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(type);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        File xmlFile = new File(outputFileLocation, filename);
        marshaller.marshal(object, xmlFile);
        return xmlFile.getAbsolutePath();
    }
}
