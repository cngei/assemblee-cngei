package it.cngei.assemblee.configuration;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;

@Configuration
public class LayoutConfiguration {
  @Bean
  public SpringTemplateEngine templateEngine(SpringResourceTemplateResolver resolver) {
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(resolver);
    templateEngine.addDialect(new LayoutDialect());
    return templateEngine;
  }
}
