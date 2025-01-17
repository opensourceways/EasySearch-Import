package com.openeuler.collect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openeuler.collect.dto.MailWebDto;
import com.openeuler.collect.mapper.SoftwareMapper;
import com.openeuler.collect.service.IOpeneulerMailWebDataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class OpeneulerMailWebDataService extends  ServiceImpl<SoftwareMapper, MailWebDto>   implements IOpeneulerMailWebDataService {


    @Override
    public List<MailWebDto> getMailData(Map param) {
       return baseMapper.getOpeneulerMailWebData(param);
    }
}
