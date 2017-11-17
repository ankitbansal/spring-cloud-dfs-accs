package oracle.paas.accs.deployer.spi.app;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;

public class ACCSTaskLauncher implements TaskLauncher {

    private RuntimeEnvironmentInfo runtimeEnvironmentInfo;

    public ACCSTaskLauncher(RuntimeEnvironmentInfo runtimeEnvironmentInfo) {
        this.runtimeEnvironmentInfo = runtimeEnvironmentInfo;
    }

    public String launch(AppDeploymentRequest appDeploymentRequest) {
        return null;
    }

    public void cancel(String s) {

    }

    public TaskStatus status(String s) {
        return null;
    }

    public void cleanup(String s) {

    }

    public void destroy(String s) {

    }

    public RuntimeEnvironmentInfo environmentInfo() {
        return runtimeEnvironmentInfo;
    }
}
