package com.softwaremarket.collect.dto;

import lombok.Data;

@Data
public class SoftwareBaseDto {
    String name;
    String version;


    String os;
    String arch;
    String binDownloadUrl;

    String srcRepo;

    String description;
    String category;


    String htmlurl;
    String dataType;
    String id;

    String installation;

    String downloadCount;

    String pkgId;
    String tagsText;
}
