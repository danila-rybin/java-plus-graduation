package ru.practicum.web.validation;

public final class ValidationConstants {

    private ValidationConstants() {
    }

    // Event validation
    public static final int EVENT_TITLE_MIN = 3;
    public static final int EVENT_TITLE_MAX = 120;

    public static final int EVENT_ANNOTATION_MIN = 20;
    public static final int EVENT_ANNOTATION_MAX = 2000;

    public static final int EVENT_DESCRIPTION_MIN = 20;
    public static final int EVENT_DESCRIPTION_MAX = 7000;

    public static final int EVENT_PARTICIPANT_LIMIT_MIN = 0;

    public static final int EVENT_HOURS_BEFORE_START = 2;
    public static final int EVENT_PUBLISH_HOURS_BEFORE = 1;

    // User validation
    public static final int USER_NAME_MIN = 2;
    public static final int USER_NAME_MAX = 250;

    public static final int USER_EMAIL_MIN = 6;
    public static final int USER_EMAIL_MAX = 254;

    // Pagination
    public static final int PAGE_DEFAULT_FROM = 0;
    public static final int PAGE_DEFAULT_SIZE = 10;
    public static final int PAGE_MIN_FROM = 0;
    public static final int PAGE_MIN_SIZE = 1;

    // Category validation
    public static final int CATEGORY_NAME_MAX = 50;

    // Compilation validation
    public static final int COMPILATION_TITLE_MIN = 1;
    public static final int COMPILATION_TITLE_MAX = 50;

    // Default values
    public static final long DEFAULT_ID = 0L;
    public static final String DEFAULT_NAME = "Unknown";
    public static final long DEFAULT_VIEWS = 0L;
    public static final long DEFAULT_CONFIRMED_REQUESTS = 0L;

    // Date format
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
}