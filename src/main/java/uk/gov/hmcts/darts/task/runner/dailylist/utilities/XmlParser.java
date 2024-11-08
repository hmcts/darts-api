package uk.gov.hmcts.darts.task.runner.dailylist.utilities;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.springframework.stereotype.Service;

import java.io.StringReader;

@Service
public class XmlParser {

    public <T> T unmarshal(String xml, Class<T> clazz) throws JAXBException {
        JAXBContext context;
        Object clazzInstance;
        context = JAXBContext.newInstance(clazz);
        clazzInstance = context
            .createUnmarshaller()
            .unmarshal(new StringReader(xml));


        return clazz.cast(clazzInstance);
    }
}
