package afsdigital.grahamselect.common.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;

/**
 * Common AOT runtime hints necessary for GraalVM native images to run
 * correctly.
 * <p>
 * This class ensures that JDBC drivers, logging mechanisms, and Liquibase
 * configuration
 * that are normally discovered dynamically via reflection are correctly
 * registered
 * to the Native Image ahead-of-time compiler without relying on brittle
 * metadata conditions.
 */
public class CommonRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
                // Register MySQL Driver & Internal Utilities
                hints.reflection().registerType(TypeReference.of("com.mysql.cj.jdbc.Driver"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("com.mysql.cj.log.StandardLogger"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("com.mysql.cj.conf.url.SingleConnectionUrl"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("com.mysql.cj.exceptions.CJException"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("com.mysql.cj.jdbc.exceptions.SQLError"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

                // Register Liquibase Configuration
                hints.reflection().registerType(TypeReference.of("liquibase.ui.LoggerUIService"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("liquibase.ui.ConsoleUIService"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

                // Note: Liquibase metadata is loaded globally from
                // resources/META-INF/native-image/org.liquibase/liquibase-core

                // Register MySQL Resource Bundles
                hints.resources().registerResourceBundle("com.mysql.cj.LocalizedErrorMessages");

                // Register Jackson Custom Serializers/Deserializers for Native Image Reflection
                hints.reflection().registerType(TypeReference.of("afsdigital.grahamselect.common.domain.entities.FinancialDataKeySerializer"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("afsdigital.grahamselect.common.domain.entities.FinancialDataKeyDeserializer"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("afsdigital.grahamselect.common.domain.entities.FinancialDataKey"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS);
                hints.reflection().registerType(TypeReference.of("afsdigital.grahamselect.common.domain.entities.FinancialDataEvent"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS);
                hints.reflection().registerType(TypeReference.of("afsdigital.grahamselect.model.RankedCompany"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS);
        }
}
