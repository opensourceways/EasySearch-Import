package com.softwaremarket.collect.dto;

import lombok.Data;

@Data
public class SoftwareEpkgDto extends  SoftwareBaseDto {
    String updatetime;
    String size;
    String summary;
    String requiresText;

    String providesText;
    String originPkg;
}
