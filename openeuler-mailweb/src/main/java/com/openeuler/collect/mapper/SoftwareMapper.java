package com.openeuler.collect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openeuler.collect.dto.MailWebDto;
import org.apache.ibatis.annotations.Mapper;


import java.util.List;
import java.util.Map;

@Mapper
public interface SoftwareMapper extends BaseMapper<MailWebDto> {

    List<MailWebDto> getOpeneulerMailWebData(Map param);




}