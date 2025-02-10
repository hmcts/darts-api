package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "object_state_record")
@Getter
@Setter
@EqualsAndHashCode
public class ObjectStateRecordEntity {
    @Id
    @Column(name = "osr_uuid")
    private Long uuid;

    @Column(name = "eod_id")
    private String eodId;

    @Column(name = "arm_eod_id")
    private String armEodId;

    @Column(name = "parent_object_id")
    private String parentObjectId;

    @Column(name = "content_object_id")
    private String contentObjectId;

    @Column(name = "id_clip")
    private String idClip;

    @Column(name = "dets_location")
    private String detsLocation;

    @Column(name = "flag_file_transfer_to_dets")
    private Boolean flagFileTransferToDets;

    @Column(name = "date_file_transfer_to_dets")
    private OffsetDateTime dateFileTransferToDets;

    @Column(name = "md5_doc_transfer_to_dets")
    private String md5DocTransferToDets;

    @Column(name = "file_size_bytes_centera")
    private Long fileSizeBytesCentera;

    @Column(name = "file_size_bytes_dets")
    private Long fileSizeBytesDets;

    @Column(name = "flag_file_av_scan_pass")
    private Boolean flagFileAvScanPass;

    @Column(name = "date_file_av_scan_pass")
    private OffsetDateTime dateFileAvScanPass;

    @Column(name = "flag_file_transf_toarml")
    private Boolean flagFileTransfToarml;

    @Column(name = "date_file_transf_toarml")
    private OffsetDateTime dateFileTransfToarml;

    @Column(name = "md5_file_transf_arml")
    private String md5FileTransfArml;

    @Column(name = "file_size_bytes_arml")
    private Long fileSizeBytesArml;

    @Column(name = "flag_file_mfst_created")
    private Boolean flagFileMfstCreated;

    @Column(name = "date_file_mfst_created")
    private OffsetDateTime dateFileMfstCreated;

    @Column(name = "id_manifest_file")
    private String idManifestFile;

    @Column(name = "flag_mfst_transf_to_arml")
    private Boolean flagMfstTransfToArml;

    @Column(name = "date_mfst_transf_to_arml")
    private OffsetDateTime dateMfstTransfToArml;

    @Column(name = "flag_rspn_recvd_from_arml")
    private Boolean flagRspnRecvdFromArml;

    @Column(name = "date_rspn_recvd_from_arml")
    private OffsetDateTime dateRspnRecvdFromArml;

    @Column(name = "flag_file_ingest_status")
    private Boolean flagFileIngestStatus;

    @Column(name = "date_file_ingest_to_arm")
    private OffsetDateTime dateFileIngestToArm;

    @Column(name = "md5_file_ingest_to_arm")
    private String md5FileIngestToArm;

    @Column(name = "file_size_ingest_to_arm")
    private Long fileSizeIngestToArm;

    @Column(name = "id_response_file")
    private String idResponseFile;

    @Column(name = "id_response_cr_file")
    private String idResponseCrFile;

    @Column(name = "id_response_uf_file")
    private String idResponseUfFile;

    @Column(name = "flag_file_dets_cleanup_status")
    private Boolean flagFileDetsCleanupStatus;

    @Column(name = "date_file_dets_cleanup")
    private OffsetDateTime dateFileDetsCleanup;

    @Column(name = "object_status")
    private String objectStatus;

    @Column(name = "storage_id")
    private String storageId;

    @Column(name = "data_ticket")
    private Integer dataTicket;

}