package io.github.julianjupiter.springbatchandexcel;

import io.github.julianjupiter.springbatchandexcel.storage.StorageProperties;
import io.github.julianjupiter.springbatchandexcel.storage.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class SpringBatchAndExcelApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBatchAndExcelApplication.class, args);
	}

	@Bean
	CommandLineRunner init(StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}

}

