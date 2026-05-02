package com.frauddetection.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notification")
@Data
public class NotificationProperties {

    private boolean simulate = true;
    private String opsEmail = "fraud-ops@bank.com";
    private String customerEmailDomain = "@bank.com";
}