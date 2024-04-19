package com.softwaremarket.collect.service;

import com.softwaremarket.collect.dto.SoftwareBaseDto;

import java.util.List;
import java.util.Map;

public interface IEsDataService {
    Boolean createEsIndex(String index, String mappingPath);

    Boolean deleteIndex(String index);

    Boolean checkEsIndexExist(String index);
    void insertEsData(String index, List<SoftwareBaseDto> datas);

    List<Map> getEsData(String index);
}
