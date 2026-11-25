package org.bazar.api.gateway.config

import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.boot.env.OriginTrackedMapPropertySource
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.ConfigurableEnvironment

/**
 * Fails startup when a `${VAR}` placeholder without a default (`${VAR:default}`) used in the
 * application config files cannot be resolved.
 *
 * `@ConfigurationProperties` binding silently keeps unresolved placeholders as literal strings
 * (https://github.com/spring-projects/spring-boot/issues/18816), so without this check a missing
 * env variable may surface only at runtime (or never).
 *
 * Registered in `META-INF/spring.factories`, so it applies to every context, including `@SpringBootTest`.
 * The scan runs as the first bean definition registry post-processor (before any configuration class is
 * parsed or bean is created) rather than in
 * [initialize], because some property sources (e.g. `@DynamicPropertySource`) are added after initializers run.
 *
 * Only the effective (highest-priority) value of each key is inspected, so a key overridden
 * by another source (profile file, env variable, test properties) does not require the original placeholder.
 * All missing variables are reported at once via `MissingRequiredPropertiesException`.
 */
class RequiredEnvironmentVariablesInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        if (System.getProperty(AOT_PROCESSING_PROPERTY).toBoolean()) return

        applicationContext.addBeanFactoryPostProcessor(object : BeanDefinitionRegistryPostProcessor {
            override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) =
                validate(applicationContext.environment)
        })
    }

    private fun validate(environment: ConfigurableEnvironment) {
        val required = findPlaceholdersWithoutDefault(environment)
        if (required.isNotEmpty()) {
            environment.setRequiredProperties(*required.toTypedArray())
            environment.validateRequiredProperties()
        }
    }

    private fun findPlaceholdersWithoutDefault(environment: ConfigurableEnvironment): Set<String> {
        val configKeys = environment.propertySources
            .filterIsInstance<OriginTrackedMapPropertySource>()
            .flatMap { it.propertyNames.asIterable() }
            .toSet()

        return configKeys
            .mapNotNull { key -> effectiveRawValue(environment, key) }
            .flatMap { value -> PLACEHOLDER_WITHOUT_DEFAULT.findAll(value).map { it.groupValues[1].trim() } }
            .toSortedSet()
    }

    private fun effectiveRawValue(environment: ConfigurableEnvironment, key: String): String? =
        environment.propertySources
            .firstOrNull { it.containsProperty(key) }
            ?.getProperty(key)
            ?.toString()

    private companion object {
        /** Set to `true` by Spring's `AbstractAotProcessor` while generating AOT sources. */
        const val AOT_PROCESSING_PROPERTY = "spring.aot.processing"

        /** Matches `${NAME}` but not `${NAME:default}`. */
        val PLACEHOLDER_WITHOUT_DEFAULT = Regex("""\$\{([^:}]+)}""")
    }
}
