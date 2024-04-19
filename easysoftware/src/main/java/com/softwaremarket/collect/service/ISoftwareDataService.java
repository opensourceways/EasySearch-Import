package com.softwaremarket.collect.service;


import com.softwaremarket.collect.dto.SoftwareBaseDto;
import org.apache.ibatis.annotations.Param;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ISoftwareDataService {
    List<SoftwareBaseDto> getRmpSoftwareData(Map param);

    List<SoftwareBaseDto> getApplicationSoftwareData(Map param);

    List<SoftwareBaseDto> getEpkgSoftwareData(Map param);

    List<SoftwareBaseDto> getAllSoftwareData(Map param);
}
