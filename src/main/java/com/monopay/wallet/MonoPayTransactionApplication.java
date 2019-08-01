package com.monopay.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class MonoPayTransactionApplication {

  public static void main(String[] args) {
    SpringApplication.run(MonoPayTransactionApplication.class, args);
  }

}
