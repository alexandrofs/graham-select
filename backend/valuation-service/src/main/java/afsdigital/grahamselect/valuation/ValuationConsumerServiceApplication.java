package afsdigital.grahamselect.valuation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;

class ValuationRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerResourceBundle("com.mysql.cj.LocalizedErrorMessages");
        hints.resources().registerPattern("com/mysql/cj/LocalizedErrorMessages*.properties");
    }
}

@SpringBootApplication
@ImportRuntimeHints(ValuationRuntimeHints.class)
@RegisterReflectionForBinding({
        liquibase.configuration.LiquibaseConfiguration.class,
        liquibase.logging.core.LogServiceFactory.class,
        liquibase.configuration.ConfiguredValueModifierFactory.class,
        liquibase.ui.LoggerUIService.class
})
public class ValuationConsumerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValuationConsumerServiceApplication.class, args);
    }

}
