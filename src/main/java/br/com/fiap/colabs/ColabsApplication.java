package br.com.fiap.colabs;

import jdk.jfr.Enabled;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCaching
public class ColabsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ColabsApplication.class, args);
	}

}
