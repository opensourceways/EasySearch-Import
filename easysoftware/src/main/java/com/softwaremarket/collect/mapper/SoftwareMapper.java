package com.softwaremarket.collect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.softwaremarket.collect.dto.SoftwareBaseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SoftwareMapper extends BaseMapper<SoftwareBaseDto> {

    List<SoftwareBaseDto> getRmpSoftwareInfo( Map param);


    List<SoftwareBaseDto> getApplicationSoftwareInfo( Map param);

    List<SoftwareBaseDto> getEpkgSoftwareInfo( Map param);



}