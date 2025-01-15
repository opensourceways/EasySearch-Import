package com.openeuler.collect.service;

import com.openeuler.collect.dto.MailWebDto;

import java.util.List;
import java.util.Map;

public interface IEsDataService {
    Boolean createEsIndex(String index, String mappingPath);

    Boolean deleteIndex(String index);

    Boolean checkEsIndexExist(String index);
    void insertEsData(String index, List<MailWebDto> datas);

    List<Map> getEsData(String index);


    Boolean deleteByQuery(String index,String type);
}
