package oracle.paas.accs.deployer.spi.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;

import oracle.paas.accs.deployer.spi.accs.client.ACCSClient;
import oracle.paas.accs.deployer.spi.accs.client.StorageClient;
import oracle.paas.accs.deployer.spi.accs.model.Application;
import oracle.paas.accs.deployer.spi.accs.model.ApplicationStatus;
import oracle.paas.accs.deployer.spi.accs.model.ApplicationStatus.Instances;
import oracle.paas.accs.deployer.spi.accs.util.ACCSUtil;

public class ACCSAppDeployer implements AppDeployer {
    private RuntimeEnvironmentInfo runtimeEnvironmentInfo;
    private StorageClient storageClient;
    private ACCSClient accsClient;
    
    private static final LogManager logManager = LogManager.getLogManager();
    private static Logger logger = Logger.getLogger(ACCSAppDeployer.class.getName());

    public ACCSAppDeployer(RuntimeEnvironmentInfo runtimeEnvironmentInfo, ACCSClient accsClient, StorageClient storageClient) {
        this.runtimeEnvironmentInfo = runtimeEnvironmentInfo;
        this.accsClient = accsClient;
        this.storageClient = storageClient;
    }

    public String deploy(AppDeploymentRequest appDeploymentRequest) {
        logger.log(Level.INFO, String.format("Entered deploy: Deploying AppDeploymentRequest: AppDefinition = {%s}, Resource = {%s}, Deployment Properties = {%s}",
                appDeploymentRequest.getDefinition(), appDeploymentRequest.getResource(), appDeploymentRequest.getDeploymentProperties()));

        String deploymentId = deploymentId(appDeploymentRequest);
        logger.log(Level.INFO, String.format("deploy: Getting Status for Deployment Id = {%s}", deploymentId));

        try {
            deployApplication(appDeploymentRequest, deploymentId);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to deploy application. " + e.getMessage());
        }
        logger.log(Level.INFO, String.format("Exiting deploy().  Deployment Id = {%s}", deploymentId));
        return deploymentId;
    }

    public void undeploy(String deploymentId) {
        String appName = ACCSUtil.getSanitizedApplicationName(deploymentId);
        try {
            accsClient.deleteApplication(appName);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to undeploy application. " + e.getMessage());
        }
    }

    public AppStatus status(String deploymentId) {
        String appName = ACCSUtil.getSanitizedApplicationName(deploymentId);
        ApplicationStatus application = accsClient.getApplication(appName);
        DeploymentState state = DeploymentState.unknown;
        if (application == null) {
            state = DeploymentState.undeployed;
        } else if (application.getCurrentOngoingActivity() != null) {
            state = DeploymentState.deploying;
        } else if(application.getStatus().equalsIgnoreCase("RUNNING")) {
            if(application.isLastDeploymentFailed()) {
                state = DeploymentState.failed;
            } else {
                state = DeploymentState.deployed;
            }
        }
        AppStatus.Builder builder = AppStatus.of(appName);
        if (application != null) {
            Instances[] instances = application.getInstances();
            if (instances != null) {
                for (Instances instance : instances) {
                    Map<String, String> attr = new HashMap<>();
                    attr.put("state", instance.getStatus());
                    builder = builder.with(new AppInstanceStatusImpl(instance.getName(), state, attr));
                }
            } else {
                builder = builder.with(new AppInstanceStatusImpl(deploymentId, state, new HashMap<String, String>()));
            }
        } else {
            builder = builder.with(new AppInstanceStatusImpl(deploymentId, state, new HashMap<String, String>()));
        }
        return builder.build();
    }

    public RuntimeEnvironmentInfo environmentInfo() {
        return runtimeEnvironmentInfo;
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
            String appLabel = properties.get("spring.cloud.dataflow.stream.app.label");
            String streamGroup = properties.get("spring.cloud.dataflow.stream.name");
            return streamGroup + "-" + appLabel;
        }
        throw new RuntimeException("Request invalid");
    }
}
