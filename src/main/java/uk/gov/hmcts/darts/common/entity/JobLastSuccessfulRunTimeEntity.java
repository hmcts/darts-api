package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "job_last_successful_run_time")
@Getter
@Setter
public class JobLastSuccessfulRunTimeEntity {

    @Id
    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "last_successful_run_timestamp_utc", nullable = false)
    private Instant lastSuccessfulRunTimestampUtc;
}
