package afsdigital.grahamselect.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;

class ApiRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerResourceBundle("com.mysql.cj.LocalizedErrorMessages");
        hints.resources().registerPattern("com/mysql/cj/LocalizedErrorMessages*.properties");
    }
}

@SpringBootApplication
@ImportRuntimeHints(ApiRuntimeHints.class)
@RegisterReflectionForBinding({
        liquibase.configuration.LiquibaseConfiguration.class,
        liquibase.logging.core.LogServiceFactory.class,
        liquibase.configuration.ConfiguredValueModifierFactory.class,
        liquibase.ui.LoggerUIService.class
})
public class ApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiServiceApplication.class, args);
    }

}
