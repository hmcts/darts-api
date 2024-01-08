package uk.gov.hmcts.darts.audio.model;

import com.azure.storage.blob.BlobClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedAudioFileInfo {
    Path path;
    BlobClient blobClient;
}
