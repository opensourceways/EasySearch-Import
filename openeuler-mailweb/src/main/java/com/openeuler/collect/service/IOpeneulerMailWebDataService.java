package com.openeuler.collect.service;


import com.openeuler.collect.dto.MailWebDto;
import java.util.List;
import java.util.Map;

public interface IOpeneulerMailWebDataService {
    List<MailWebDto> getMailData(Map param);

}
