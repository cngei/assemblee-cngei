package it.cngei.assemblee.configuration;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

@Configuration
public class LayoutConfiguration {
  @Bean
  public SpringTemplateEngine templateEngine(SpringResourceTemplateResolver resolver) {
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(resolver);
    templateEngine.addDialect(new Java8TimeDialect());
    templateEngine.addDialect(new LayoutDialect());
    return templateEngine;
  }
}
