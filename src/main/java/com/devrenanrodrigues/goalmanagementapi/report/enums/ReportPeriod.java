package com.devrenanrodrigues.goalmanagementapi.report.enums;

public enum ReportPeriod {
    THIRTY_DAYS,
    QUARTER,
    YEAR;

    public static ReportPeriod fromString(String value) {
        if (value == null) {
            return THIRTY_DAYS;
        }
        return switch (value.toLowerCase().trim()) {
            case "quarter", "trimestre" -> QUARTER;
            case "year", "ano" -> YEAR;
            default -> THIRTY_DAYS;
        };
    }
}
