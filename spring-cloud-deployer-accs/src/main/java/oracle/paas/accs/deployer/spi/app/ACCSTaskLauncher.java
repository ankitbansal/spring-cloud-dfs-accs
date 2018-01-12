package oracle.paas.accs.deployer.spi.app;

import oracle.paas.accs.deployer.spi.accs.client.ACCSClient;
import oracle.paas.accs.deployer.spi.accs.client.StorageClient;
import oracle.paas.accs.deployer.spi.accs.model.Application;
import oracle.paas.accs.deployer.spi.accs.util.ACCSUtil;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ACCSTaskLauncher implements TaskLauncher {

    private RuntimeEnvironmentInfo runtimeEnvironmentInfo;
    private StorageClient storageClient;
    private ACCSClient accsClient;

    private static Logger logger = Logger.getLogger(ACCSTaskLauncher.class.getName());

    public ACCSTaskLauncher(RuntimeEnvironmentInfo runtimeEnvironmentInfo, ACCSClient accsClient, StorageClient storageClient) {
        this.runtimeEnvironmentInfo = runtimeEnvironmentInfo;
        this.accsClient = accsClient;
        this.storageClient = storageClient;
    }

    public String launch(AppDeploymentRequest appDeploymentRequest) {
        logger.log(Level.INFO, String.format("Entered deploy: Deploying AppDeploymentRequest: AppDefinition = {%s}, Resource = {%s}, Deployment Properties = {%s}",
                appDeploymentRequest.getDefinition(), appDeploymentRequest.getResource(), appDeploymentRequest.getDeploymentProperties()));

        String deploymentId = deploymentId(appDeploymentRequest);
        logger.log(Level.INFO, String.format("deploy: Getting Status for Deployment Id = {%s}", deploymentId));

        try {
            deployApplication(appDeploymentRequest, deploymentId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Unable to launch task. " + e.getMessage());
        }
        logger.log(Level.INFO, String.format("Exiting deploy().  Deployment Id = {%s}", deploymentId));
        return deploymentId;
    }

    private void deployApplication(AppDeploymentRequest appDeploymentRequest, String deploymentId) {
        File file = null;
        try {
            file = appDeploymentRequest.getResource().getFile();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception while retrieving file", e);
        }

        if(file != null) {
            AppBuilder appBuilder = new AppBuilder(appDeploymentRequest, deploymentId);
            String command = appBuilder.buildCommand(file.getName());

            File zipFile = null;
            try {
                zipFile = ACCSUtil.convertToZipFile(file, command);
                logger.log(Level.INFO, "Created zip file : " +zipFile.getAbsolutePath());
                storageClient.pushFileToStorage(zipFile);
                String appName = ACCSUtil.getSanitizedApplicationName(deploymentId);
                if(!accsClient.applicationExists(appName)) {
                    Application application = appBuilder.getApplicationData(zipFile.getName());
                    accsClient.createApplication(application);
                } else {
                    Application application = appBuilder.getApplicationData(zipFile.getName());
                    accsClient.updateApplication(application);
                }
            } catch (Exception se){
                logger.log(Level.SEVERE, "Exception during file operation: " + zipFile.getAbsolutePath(), se);
            }
            ACCSUtil.deleteFile(zipFile);
            ACCSUtil.deleteCommonFiles();
        }
    }

    private String deploymentId(AppDeploymentRequest request) {
        if(request.getDefinition() != null) {
            Map<String, String> properties = request.getDefinition().getProperties();
            String taskName = properties.get("spring.cloud.task.name");
            return taskName;
        }
        throw new RuntimeException("Request invalid");
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
