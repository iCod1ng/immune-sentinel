package com.immunesentinel;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
@MapperScan("com.immunesentinel.infrastructure.persistence.mapper")
public class ImmuneSentinelApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImmuneSentinelApplication.class, args);
    }
}
