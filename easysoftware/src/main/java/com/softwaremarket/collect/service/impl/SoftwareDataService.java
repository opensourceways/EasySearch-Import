package com.softwaremarket.collect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.softwaremarket.collect.config.SoftwareConfig;
import com.softwaremarket.collect.dto.SoftwareBaseDto;
import com.softwaremarket.collect.enums.SoftwareTypeEnum;
import com.softwaremarket.collect.mapper.SoftwareMapper;
import com.softwaremarket.collect.service.ISoftwareDataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class SoftwareDataService extends ServiceImpl<SoftwareMapper, SoftwareBaseDto> implements ISoftwareDataService {
    private final SoftwareConfig softwareConfig;

    @Override
    public List<SoftwareBaseDto> getRmpSoftwareData(Map param) {
        List<SoftwareBaseDto> rmpSoftwareData = baseMapper.getRmpSoftwareInfo(param);
        rmpSoftwareData.stream().forEach(r -> {
            r.setDataType(SoftwareTypeEnum.RPMPKG.getType());
            r.setHtmlurl(softwareConfig.getSotfwareDetailUrl() + r.getName());
        });
        return rmpSoftwareData;
    }

    @Override
    public List<SoftwareBaseDto> getApplicationSoftwareData(Map param) {
        List<SoftwareBaseDto> appSoftwareData = baseMapper.getApplicationSoftwareInfo(param);
        appSoftwareData.stream().forEach(r -> {
            r.setDataType(SoftwareTypeEnum.APPLICATION.getType());
            r.setHtmlurl(softwareConfig.getSotfwareDetailUrl() + r.getName());
        });

        return appSoftwareData;
    }

    @Override
    public List<SoftwareBaseDto> getEpkgSoftwareData(Map param) {
        List<SoftwareBaseDto> epkgSoftwareData = baseMapper.getEpkgSoftwareInfo(param);
        epkgSoftwareData.stream().forEach(r -> {
            r.setDataType(SoftwareTypeEnum.EKPG.getType());
            r.setHtmlurl(softwareConfig.getSotfwareDetailUrl() + r.getName());
        });
        return epkgSoftwareData;
    }

    @Override
    public List<SoftwareBaseDto> getAllSoftwareData(Map param) {


        return null;
    }
}
