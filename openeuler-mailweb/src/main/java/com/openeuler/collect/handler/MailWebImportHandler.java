package com.openeuler.collect.handler;

import com.alibaba.fastjson.JSONObject;
import com.openeuler.collect.dto.MailWebDto;
import com.openeuler.collect.service.IEsDataService;
import com.openeuler.collect.service.IOpeneulerMailWebDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MailWebImportHandler {
    private final IOpeneulerMailWebDataService openeulerMailWebDataService;

    private final IEsDataService esDataService;


    @Value("${elasticsearch.index}")
    public String index;

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;


    public void dbDataImportToEs() {
        List<String> excludeTitle = Arrays.asList("mail test", "test mail", "测试邮件", "邮件测试", "[Test]");
        HashSet<String> idSet = new HashSet<String>();
        int start = 0;
        int lastCount = 0;
        do {
            HashMap paramMap = new HashMap();
            paramMap.put("LIMIT", 500);
            paramMap.put("OFFSET", start * 500);
            System.out.println(paramMap);
            List<MailWebDto> mailData = openeulerMailWebDataService.getMailData(paramMap);
            lastCount = mailData.size();
            mailData = mailData.stream().filter(mailWebDto -> "2".equals(mailWebDto.getArchivePolicy()) && idSet.add(mailWebDto.getPath()))
                    .collect(Collectors.toList());
           /* for (int i = 0; i < mailData.size(); i++) {
                MailWebDto mailWebDto = mailData.get(i);
                String title = mailWebDto.getTitle();
                for (String s : excludeTitle) {
                    if (title.contains(s)) {
                        mailData.remove(i);
                        i--;
                    }

                }
            }*/
            esDataService.insertEsData(index, mailData);
            mailData.clear();
            System.gc();
            start++;
        } while (lastCount >= 500);

    }
}
