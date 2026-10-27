package com.campus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.cloud.netflix.eureka.server.EnableEurekaServer
@SpringBootApplication
public class RegistryApplication {

  public static void main(String[] args) {
    SpringApplication.run(RegistryApplication.class, args);
  }
}
