package uk.gov.hmcts.darts.audio.model.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class IncrementingElementNameAdapter extends XmlAdapter<ViqAnnotationItem, ViqAnnotationItem> {

    private int annotationItemIndex = 0;

    @Override
    public ViqAnnotationItem unmarshal(ViqAnnotationItem v) throws Exception {
        return v;
    }

    @Override
    public ViqAnnotationItem marshal(ViqAnnotationItem v) throws Exception {

        ViqAnnotationItem annotationItem = new ViqAnnotationItem();
        annotationItem.setLabel(v.getLabel());
        annotationItem.setEventText(v.getEventText());
        annotationItem.setStartTimeInMillis(v.getStartTimeInMillis());
        annotationItem.setStartTimeYear(v.getStartTimeYear());
        annotationItem.setStartTimeMonth(v.getStartTimeMonth());
        annotationItem.setStartTimeDate(v.getStartTimeDate());
        annotationItem.setStartTimeHour(v.getStartTimeHour());
        annotationItem.setStartTimeMinutes(v.getStartTimeMinutes());
        annotationItem.setStartTimeSeconds(v.getStartTimeSeconds());
        annotationItem.setRestricted(v.getRestricted());
        annotationItem.setLapsed(v.getLapsed());

        String incrementedElementName = "a" + annotationItemIndex++;
        annotationItem.setXmlElementName(incrementedElementName);

        return annotationItem;
    }
}
