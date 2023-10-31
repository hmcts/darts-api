package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.exception.OutboundDeleterException;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;
import uk.gov.hmcts.darts.common.service.bankholidays.Event;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.MARKED_FOR_DELETION;

@Service
public class OutboundAudioDeleterProcessorImpl implements OutboundAudioDeleterProcessor {
    private final MediaRequestRepository mediaRequestRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final BankHolidaysService bankHolidaysService;
    private final UserAccountRepository userAccountRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final Clock clock;
    private final long lastAccessedDeletionDays;

    public OutboundAudioDeleterProcessorImpl(MediaRequestRepository mediaRequestRepository,
                                             TransientObjectDirectoryRepository transientObjectDirectoryRepository,
                                             BankHolidaysService bankHolidaysService, UserAccountRepository userAccountRepository,
                                             ObjectDirectoryStatusRepository objectDirectoryStatusRepository,
                                             Clock clock,
                                             @Value("${darts.audio.outbounddeleter.last-accessed-deletion-day:2}") long lastAccessedDeletionDays) {
        this.mediaRequestRepository = mediaRequestRepository;
        this.transientObjectDirectoryRepository = transientObjectDirectoryRepository;
        this.bankHolidaysService = bankHolidaysService;
        this.userAccountRepository = userAccountRepository;
        this.objectDirectoryStatusRepository = objectDirectoryStatusRepository;
        this.clock = clock;
        this.lastAccessedDeletionDays = lastAccessedDeletionDays;
    }


    private static boolean isBankHolidayBetweenDates(Event bankHoliday, LocalDate cutOff, LocalDate today) {
        return !bankHoliday.getDate().isBefore(cutOff) && !bankHoliday.getDate().isAfter(today);
    }

    private static long calculateWeekendDays(LocalDate fromDate, LocalDate toDate) {
        Set<DayOfWeek> weekend = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        return fromDate.datesUntil(toDate)
            .filter(d -> weekend.contains(d.getDayOfWeek()))
            .count();
    }


    @Transactional
    public List<MediaRequestEntity> delete() {
        List<MediaRequestEntity> deletedValues = new ArrayList<>();
        OffsetDateTime lastAccessedDateTime = OffsetDateTime.now(clock)
            .truncatedTo(ChronoUnit.DAYS)
            .minusDays(getLastAccessedDeletionDays());


        List<Integer> mediaRequests = mediaRequestRepository.findAllIdsByLastAccessedTimeBeforeAndStatus(
            lastAccessedDateTime,
            AudioRequestStatus.COMPLETED
        );

        mediaRequests.addAll(mediaRequestRepository.findAllByCreatedDateTimeBeforeAndStatusNotAndLastAccessedDateTimeIsNull(
            lastAccessedDateTime,
            AudioRequestStatus.PROCESSING
        ));

        List<TransientObjectDirectoryEntity> transientObjectDirectoryEntities = transientObjectDirectoryRepository.findByMediaRequest_idIn(
            mediaRequests);

        Optional<UserAccountEntity> systemUser = userAccountRepository.findById(0);

        if (systemUser.isEmpty()) {
            throw new DartsApiException(OutboundDeleterException.MISSING_SYSTEM_USER);
        }
        ObjectDirectoryStatusEntity deletionStatus = objectDirectoryStatusRepository.getReferenceById(
            MARKED_FOR_DELETION.getId());


        for (TransientObjectDirectoryEntity entity : transientObjectDirectoryEntities) {
            entity.getMediaRequest().setStatus(AudioRequestStatus.EXPIRED);
            entity.getMediaRequest().setLastModifiedBy(systemUser.get());

            entity.setLastModifiedBy(systemUser.get());
            entity.setStatus(deletionStatus);

            deletedValues.add(entity.getMediaRequest());
        }


        return deletedValues;
    }

    /**
     * Returns how many of the days since last access were bank holidays or weekends.
     *
     * @return long number of days
     */
    private long getLastAccessedDeletionDays() {
        return lastAccessedDeletionDays + howManyOfPreviousDaysAreBankHolidaysOrWeekends();
    }

    private long howManyOfPreviousDaysAreBankHolidaysOrWeekends() {
        long bankHolidayOrWeekendCount = 0;
        LocalDate today = LocalDate.now(clock);
        LocalDate cutOff = LocalDate.now(clock).minusDays(lastAccessedDeletionDays);
        List<Event> bankHolidays = bankHolidaysService.getBankHolidaysFor(OffsetDateTime.now().getYear());

        for (Event bankHoliday : bankHolidays) {
            if (isBankHolidayBetweenDates(bankHoliday, cutOff, today)) {
                bankHolidayOrWeekendCount++;
            }
        }
        bankHolidayOrWeekendCount += calculateWeekendDays(cutOff, today);
        return bankHolidayOrWeekendCount;
    }

}
