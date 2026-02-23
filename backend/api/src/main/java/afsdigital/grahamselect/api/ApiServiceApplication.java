package afsdigital.grahamselect.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@SpringBootApplication
@RegisterReflectionForBinding({
        liquibase.configuration.LiquibaseConfiguration.class,
        liquibase.logging.core.LogServiceFactory.class,
        liquibase.configuration.ConfiguredValueModifierFactory.class
})
public class ApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiServiceApplication.class, args);
    }

}
