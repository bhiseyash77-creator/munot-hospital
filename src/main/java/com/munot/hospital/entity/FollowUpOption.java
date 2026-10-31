//package com.munot.hospital.entity;
//
//public enum FollowUpOption {
//    NONE(0), DAYS_3(3), DAYS_7(7), DAYS_15(15), DAYS_30(30), CUSTOM(0);
//
//    private final int days;
//
//    FollowUpOption(int days) { this.days = days; }
//
//    public int getDays() { return days; }
//}
package com.munot.hospital.entity;

public enum FollowUpOption {
    NONE(0), DAYS_3(3), DAYS_7(7), DAYS_15(15), DAYS_30(30), CUSTOM(0),
    /** Any number of days, taken from the consultation's followUpDays. */
    DAYS(0);

    private final int days;

    FollowUpOption(int days) { this.days = days; }

    public int getDays() { return days; }
}
