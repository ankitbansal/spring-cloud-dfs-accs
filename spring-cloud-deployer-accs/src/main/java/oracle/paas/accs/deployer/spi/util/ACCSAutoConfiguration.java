package oracle.paas.accs.deployer.spi.util;

import oracle.paas.accs.deployer.spi.app.ACCSAppDeployer;
import oracle.paas.accs.deployer.spi.app.ACCSTaskLauncher;
import oracle.paas.accs.deployer.spi.accs.client.ACCSInfo;
import oracle.paas.accs.deployer.spi.accs.client.ACCSClient;
import oracle.paas.accs.deployer.spi.accs.client.StorageClient;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.util.RuntimeVersionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class ACCSAutoConfiguration {

    private RuntimeEnvironmentInfo runtime(Class spiClass, Class implementationClass) {
        ACCSInfo client = ACCSInfo.getInstance();
        return new RuntimeEnvironmentInfo.Builder()
                .implementationName(implementationClass.getSimpleName())
                .spiClass(spiClass)
                .implementationVersion(RuntimeVersionUtils.getVersion(ACCSAppDeployer.class))
                .platformType(client.getPlatformType())
                .platformClientVersion(client.getVersion())
                .platformApiVersion(client.getApiVersion())
                .platformHostVersion(client.getHostVersion())
                .addPlatformSpecificInfo("API Endpoint", client.getEndPoint())
                .build();
    }

    private ACCSClient accsClient() {
        if(System.getenv("ACCS_USERNAME") != null && System.getenv("ACCS_PASSWORD") != null &&
                System.getenv("ACCS_DOMAIN") !=null && System.getenv("ACCS_URL") != null) {

            String username = System.getenv("ACCS_USERNAME");
            String password = System.getenv("ACCS_PASSWORD");
            String url = System.getenv("ACCS_URL");
            String identityDomain = System.getenv("ACCS_DOMAIN");
            return new ACCSClient(username, password, identityDomain, url);

        }
        throw new RuntimeException("ACCS Properties not correctly configured. ");
    }

    private StorageClient storageClient() {
        String username = null;
        String password = null;
        if(System.getenv("ACCS_STORAGE_URI") != null) {
            if(System.getenv("ACCS_STORAGE_USERNAME") != null) {
                username = System.getenv("ACCS_STORAGE_USERNAME");
            }else {
                username = System.getenv("ACCS_USERNAME");
            }

            if(System.getenv("ACCS_STORAGE_PASSWORD") != null) {
                password = System.getenv("ACCS_STORAGE_PASSWORD");
            }else {
                password = System.getenv("ACCS_PASSWORD");
            }
            String url = System.getenv("ACCS_STORAGE_URI");
            return new StorageClient(username, password, url);

        }
        throw new RuntimeException(("Storage Properties not correctly configured. "));

    }

    @Bean
    @ConditionalOnMissingBean(TaskLauncher.class)
    public TaskLauncher taskLauncher() {
        return new ACCSTaskLauncher(runtime(TaskLauncher.class, ACCSTaskLauncher.class));
    }

    @Bean
    @ConditionalOnMissingBean(AppDeployer.class)
    public AppDeployer appDeployer()
    {
        return new ACCSAppDeployer(runtime(AppDeployer.class, ACCSAppDeployer.class), accsClient(), storageClient());
    }
}
