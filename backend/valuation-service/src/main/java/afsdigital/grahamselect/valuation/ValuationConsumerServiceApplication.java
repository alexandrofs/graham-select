package afsdigital.grahamselect.valuation;

import afsdigital.grahamselect.common.config.CommonRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication(scanBasePackages = { "afsdigital.grahamselect.common", "afsdigital.grahamselect.valuation" })
@ImportRuntimeHints(CommonRuntimeHints.class)
public class ValuationConsumerServiceApplication {

        public static void main(String[] args) {
                SpringApplication.run(ValuationConsumerServiceApplication.class, args);
        }

}
