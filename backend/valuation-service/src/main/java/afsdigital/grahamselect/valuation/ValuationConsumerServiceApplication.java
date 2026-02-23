package afsdigital.grahamselect.valuation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.MemberCategory;

class ValuationRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerResourceBundle("com.mysql.cj.LocalizedErrorMessages");
        hints.resources().registerPattern("com/mysql/cj/LocalizedErrorMessages*.properties");

        hints.reflection().registerType(TypeReference.of("com.mysql.cj.conf.url.SingleConnectionUrl"),
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        hints.reflection().registerType(TypeReference.of("com.mysql.cj.conf.url.FailoverConnectionUrl"),
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        hints.reflection().registerType(TypeReference.of("com.mysql.cj.exceptions.WrongArgumentException"),
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.reflection().registerType(TypeReference.of("com.mysql.cj.exceptions.UnableToConnectException"),
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.reflection().registerType(TypeReference.of("com.mysql.cj.exceptions.CJException"),
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
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
