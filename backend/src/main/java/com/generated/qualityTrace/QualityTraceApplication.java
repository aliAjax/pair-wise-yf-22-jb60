package com.generated.qualityTrace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.generated.qualityTrace.repositories")
@EnableScheduling
public class QualityTraceApplication {
  public static void main(String[] args) {
    SpringApplication.run(QualityTraceApplication.class, args);
  }
}
