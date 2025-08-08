package com.yupi.yubibackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.yupi.yubibackend.mapper")
@EnableScheduling
public class YubiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(YubiBackendApplication.class, args);
	}

}
