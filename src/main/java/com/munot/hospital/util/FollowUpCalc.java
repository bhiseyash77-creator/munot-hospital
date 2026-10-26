package com.munot.hospital.util;

import com.munot.hospital.entity.FollowUpOption;
import java.time.LocalDate;

public final class FollowUpCalc {
    private FollowUpCalc() {}

    /** Returns null when no follow-up is needed. */
    public static LocalDate targetDate(LocalDate visitDate, FollowUpOption option, LocalDate custom) {
        if (option == null || option == FollowUpOption.NONE) return null;
        if (option == FollowUpOption.CUSTOM) {
            if (custom == null || !custom.isAfter(visitDate)) throw new IllegalArgumentException("Custom follow-up date must be after today");
            return custom;
        }
        return visitDate.plusDays(option.getDays());
    }
}
