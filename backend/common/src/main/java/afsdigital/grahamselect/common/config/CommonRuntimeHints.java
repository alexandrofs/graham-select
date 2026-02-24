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
                hints.reflection().registerType(TypeReference.of("liquibase.configuration.LiquibaseConfiguration"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("liquibase.logging.core.JavaLogService"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(
                                TypeReference.of("liquibase.logging.core.composite.CompositeLogService"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

                hints.reflection().registerType(TypeReference.of("liquibase.logging.core.LogServiceFactory"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("liquibase.logging.mdc.MdcManagerFactory"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("liquibase.change.ChangeFactory"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("liquibase.command.CommandFactory"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
                hints.reflection().registerType(TypeReference.of("liquibase.plugin.AbstractPluginFactory"),
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

                // Liquibase ServiceLoaders
                hints.resources().registerPattern("META-INF/services/liquibase.*");
                hints.resources().registerPattern("db/changelog/*");
                hints.resources().registerPattern("db/changelog/changesets/*");
        }
}
