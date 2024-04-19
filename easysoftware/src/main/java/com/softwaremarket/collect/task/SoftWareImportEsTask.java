package com.softwaremarket.collect.task;

import com.softwaremarket.collect.config.SoftwareConfig;
import com.softwaremarket.collect.handler.SoftwareImportHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class SoftWareImportEsTask {
    private final SoftwareConfig collectConfig;
    private final SoftwareImportHandler softwareImportHandler;

    @Scheduled(cron = "${softwareconfig.schedule}")
    public void softWareRpmImportTask() {
        log.info("开始数据导入ES");
        softwareImportHandler.applicationSoftwareHandle();
    }
}

