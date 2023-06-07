package uk.gov.hmcts.darts.dailylist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Date;

@Entity
@Table(name = DailyListEntity.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyListEntity {

    public static final String ID = "moj_dal_id";
    public static final String COURTHOUSE_ID = "moj_crt_id";
    public static final String DAILY_LIST_OBJECT_ID = "r_daily_list_object_id";
    public static final String UNIQUE_ID = "unique_id";
    public static final String STATUS = "status";
    public static final String PUBLISHED_TIME = "published_time";
    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";
    public static final String SOURCE = "source";
    public static final String CONTENT = "content";
    public static final String CREATED_DATE_TIME = "created_date_time";
    public static final String LAST_UPDATED_DATE_TIME = "last_updated_date_time";
    public static final String TABLE_NAME = "moj_daily_list";

    @Id
    @Column(name = ID)
    private Integer id;

    @Column(name = COURTHOUSE_ID)
    private Integer crtId;

    @Column(name = DAILY_LIST_OBJECT_ID)
    private String dailyListObjectId;

    @Column(name = UNIQUE_ID)
    private String uniqueID;

    @Column(name = STATUS)
    private String status;

    @Column(name = PUBLISHED_TIME)
    private Date publishedTime;

    @Column(name = START_DATE)
    private Date startDate;

    @Column(name = END_DATE)
    private Date endDate;

    @Column(name = SOURCE)
    private String source;

    @Column(name = CONTENT)
    private String content;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private OffsetDateTime lastUpdatedDateTime;

}
