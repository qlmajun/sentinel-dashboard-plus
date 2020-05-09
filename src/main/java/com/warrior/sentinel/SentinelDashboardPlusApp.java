package com.warrior.sentinel;

import com.alibaba.csp.sentinel.init.InitExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author majun
 * @description sentinel dashboard plus 启动类
 * @date 2020/4/26
 */
@SpringBootApplication
@ComponentScan(value = {"com.warrior.sentinel", "com.alibaba.csp.sentinel.dashboard"})
public class SentinelDashboardPlusApp {
    public static void main(String[] args) {
        triggerSentinelInit();
        SpringApplication.run(SentinelDashboardPlusApp.class, args);
    }

    private static void triggerSentinelInit() {
        new Thread(() -> InitExecutor.doInit()).start();
    }
}
