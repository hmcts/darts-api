package uk.gov.hmcts.darts.audio.service;

@FunctionalInterface
public interface FindFilesFromCvpAndCarService {

    void findFilesFromCvpAndCar(int batchSize);
}
