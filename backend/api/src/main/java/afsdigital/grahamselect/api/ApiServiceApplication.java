package afsdigital.grahamselect.api;

import afsdigital.grahamselect.common.config.CommonRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication(scanBasePackages = { "afsdigital.grahamselect.common", "afsdigital.grahamselect.api" })
@ImportRuntimeHints(CommonRuntimeHints.class)
public class ApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiServiceApplication.class, args);
    }

}
