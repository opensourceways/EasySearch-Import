package com.openeuler.collect.task;

import com.openeuler.collect.handler.MailWebImportHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class MailWebImportEsTask {

    private final MailWebImportHandler mailWebImportHandler;


    public void softWareRpmImportTask() {
        log.info("开始数据导入ES");
        mailWebImportHandler.dbDataImportToEs();
    }
}

