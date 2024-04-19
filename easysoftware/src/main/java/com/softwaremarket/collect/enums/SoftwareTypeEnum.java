package com.softwaremarket.collect.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SoftwareTypeEnum {
    APPLICATION("application","IMAGE", "容器镜像"),
    EKPG("epkg", "EPKG","openeuler软件包"),
    RPMPKG("rpmpkg", "RPM","rpm软件包"),
    ALL("all","","epkg rpmpkg 的category不为其他类型和application ");
    private final String type;
    private final String tag;
    private final String message;
}
