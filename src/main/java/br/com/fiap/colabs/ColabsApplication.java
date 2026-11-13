package br.com.fiap.colabs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ColabsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ColabsApplication.class, args);
	}

}
