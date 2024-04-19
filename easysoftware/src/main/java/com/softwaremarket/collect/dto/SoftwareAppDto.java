package com.softwaremarket.collect.dto;

import lombok.Data;

@Data
public class SoftwareAppDto extends SoftwareBaseDto{
    String iconUrl;
    String EPKG;
    String IMAGE;
    String RPM;
    String epkgUpdate;
    String rpmUpdate;
}
