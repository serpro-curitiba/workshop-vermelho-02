package br.gov.client.sifap;

import org.springframework.boot.SpringApplication;

public class TestSifapBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(SifapBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
