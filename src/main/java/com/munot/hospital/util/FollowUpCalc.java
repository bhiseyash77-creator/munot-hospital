//package com.munot.hospital.util;
//
//import com.munot.hospital.entity.FollowUpOption;
//import java.time.LocalDate;
//
//public final class FollowUpCalc {
//    private FollowUpCalc() {}
//
//    /** Returns null when no follow-up is needed. */
//    public static LocalDate targetDate(LocalDate visitDate, FollowUpOption option, LocalDate custom) {
//        if (option == null || option == FollowUpOption.NONE) return null;
//        if (option == FollowUpOption.CUSTOM) {
//            if (custom == null || !custom.isAfter(visitDate)) throw new IllegalArgumentException("Custom follow-up date must be after today");
//            return custom;
//        }
//        return visitDate.plusDays(option.getDays());
//    }
//}
package com.munot.hospital.util;

import com.munot.hospital.entity.FollowUpOption;
import java.time.LocalDate;

public final class FollowUpCalc {
    private FollowUpCalc() {}

    /** Returns null when no follow-up is needed. */
    public static LocalDate targetDate(LocalDate visitDate, FollowUpOption option, LocalDate custom) {
        return targetDate(visitDate, option, custom, null);
    }

    /** As above; {@code days} is used only for {@link FollowUpOption#DAYS} (any whole number of days from 1 to 365). */
    public static LocalDate targetDate(LocalDate visitDate, FollowUpOption option, LocalDate custom, Integer days) {
        if (option == null || option == FollowUpOption.NONE) return null;
        if (option == FollowUpOption.DAYS) {
            if (days == null || days < 1 || days > 365) throw new IllegalArgumentException("Follow-up days must be a whole number from 1 to 365");
            return visitDate.plusDays(days);
        }
        if (option == FollowUpOption.CUSTOM) {
            if (custom == null || !custom.isAfter(visitDate)) throw new IllegalArgumentException("Custom follow-up date must be after today");
            return custom;
        }
        return visitDate.plusDays(option.getDays());
    }
}
