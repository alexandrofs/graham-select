package afsdigital.grahamselect.valuation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@SpringBootApplication
@RegisterReflectionForBinding({
        liquibase.configuration.LiquibaseConfiguration.class,
        liquibase.logging.core.LogServiceFactory.class
})
public class ValuationConsumerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValuationConsumerServiceApplication.class, args);
    }

}
