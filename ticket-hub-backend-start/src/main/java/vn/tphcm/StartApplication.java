/*
 * @ (#) StartApplication.java       1.0     7/30/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm;
 /*
  * @author: Luong Tan Dat
  * @date: 7/30/2026
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class StartApplication {
    static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
