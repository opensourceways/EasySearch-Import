package com.openeuler.collect.dto;

import lombok.Data;

@Data
public class MailWebDto {
    String title;
    String textContent;
    String lang;

    String type;
    String path;

    String archivePolicy;
    String date;
}
