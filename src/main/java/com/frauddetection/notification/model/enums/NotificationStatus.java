package com.frauddetection.notification.model.enums;

public enum NotificationStatus {
    SENT,       // notification dispatched successfully
    FAILED,     // dispatch failed — logged for ops review
    SKIPPED     // APPROVE decisions — no customer notification needed
}
