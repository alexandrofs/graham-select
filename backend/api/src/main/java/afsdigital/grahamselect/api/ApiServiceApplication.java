package afsdigital.grahamselect.api;

import afsdigital.grahamselect.common.config.CommonRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = { "afsdigital.grahamselect.common", "afsdigital.grahamselect.api" })
@EntityScan(basePackages = { "afsdigital.grahamselect.common", "afsdigital.grahamselect.api" })
@EnableJpaRepositories(basePackages = { "afsdigital.grahamselect.common", "afsdigital.grahamselect.api" })
@org.springframework.data.jpa.repository.config.EnableJpaAuditing
@ImportRuntimeHints(CommonRuntimeHints.class)
public class ApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiServiceApplication.class, args);
    }

}
