package com.softwaremarket.collect.handler;

import com.alibaba.fastjson.JSON;
import com.softwaremarket.collect.config.SoftwareConfig;
import com.softwaremarket.collect.dto.SoftwareBaseDto;
import com.softwaremarket.collect.enums.SoftwareTypeEnum;
import com.softwaremarket.collect.service.IEsDataService;
import com.softwaremarket.collect.service.ISoftwareDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class SoftwareImportHandler {
    private final ISoftwareDataService softwareDataService;

    private final IEsDataService esDataService;

    private final SoftwareConfig softwareConfig;

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void applicationSoftwareHandle() {
        Boolean esIndexIsCreated = esDataService.createEsIndex(softwareConfig.getImportEsIndex(), softwareConfig.getMappingPath());
        if (!esIndexIsCreated)
            return;


        threadPoolTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    dbDataImportToEs(SoftwareTypeEnum.APPLICATION);
                    log.info("application Data import end ");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        threadPoolTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    dbDataImportToEs(SoftwareTypeEnum.EKPG);
                    log.info("epkg Data import end ");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        threadPoolTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    dbDataImportToEs(SoftwareTypeEnum.RPMPKG);
                    log.info("rpmpkg Data import end ");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

    }

    public void dbDataImportToEs(SoftwareTypeEnum typeEnume) {
        int start = 0;
        int lastCount = 0;
        do {
            List<SoftwareBaseDto> softwareData = new ArrayList<>();
            HashMap paramMap = new HashMap();
            paramMap.put("start", start * softwareConfig.getPagesize());
            paramMap.put("pagesize", softwareConfig.getPagesize());
            System.out.println(paramMap);
            switch (typeEnume) {
                case APPLICATION:
                    softwareData = softwareDataService.getApplicationSoftwareData(paramMap);
                    break;

                case RPMPKG:
                    softwareData = softwareDataService.getRmpSoftwareData(paramMap);
                    break;

                case EKPG:
                    softwareData = softwareDataService.getEpkgSoftwareData(paramMap);


                    break;
            }
            if (!CollectionUtils.isEmpty(softwareData)) {
                 esDataService.insertEsData(softwareConfig.getImportEsIndex(), softwareData);
                log.info("import data {}{}条 ", typeEnume.getType(), softwareData.size());
                List<SoftwareBaseDto> all = softwareData.stream().filter(softwareBaseDto -> SoftwareTypeEnum.APPLICATION.getType().equals(softwareBaseDto.getDataType())
                        || (!SoftwareTypeEnum.APPLICATION.getType().equals(softwareBaseDto.getDataType()) && softwareBaseDto.getCategory() != null && !"其他".equals(softwareBaseDto.getCategory()))).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(all)) {
                    all.stream().forEach(a -> {
                                a.setDataType(SoftwareTypeEnum.ALL.getType());
                                if (StringUtils.isEmpty(a.getTagsText()))
                                    a.setTagsText(typeEnume.getTag());
                            }
                    );
                    esDataService.insertEsData(softwareConfig.getImportEsIndex(), all);
                }
                log.info(JSON.toJSONString(all));
            }
            start++;
            lastCount = softwareData.size();
        } while (lastCount >= softwareConfig.getPagesize());

    }
}
