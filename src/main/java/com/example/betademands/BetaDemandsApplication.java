package com.example.betademands;

import com.example.betademands.config.ProblemTicketMetricsProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.example.betademands.mapper")
@EnableConfigurationProperties(ProblemTicketMetricsProperties.class)
public class BetaDemandsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BetaDemandsApplication.class, args);
    }
}
