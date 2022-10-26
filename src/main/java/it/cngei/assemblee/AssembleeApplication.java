package it.cngei.assemblee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AssembleeApplication {

  public static void main(String[] args) {
    SpringApplication.run(AssembleeApplication.class, args);
  }

}
