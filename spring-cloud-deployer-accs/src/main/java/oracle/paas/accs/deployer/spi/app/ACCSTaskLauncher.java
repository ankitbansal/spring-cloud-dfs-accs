package oracle.paas.accs.deployer.spi.app;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import oracle.paas.accs.deployer.spi.accs.client.ACCSClient;
import oracle.paas.accs.deployer.spi.accs.client.StorageClient;
import oracle.paas.accs.deployer.spi.accs.model.Application;
import oracle.paas.accs.deployer.spi.accs.model.ApplicationStatus;
import oracle.paas.accs.deployer.spi.accs.util.ACCSUtil;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ACCSTaskLauncher implements TaskLauncher {

    private static final int TIME_OUT_IN_MIN = 5;
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

        String appId = appId(appDeploymentRequest);

        try {

            logger.log(Level.INFO, String.format("Checking if app already exists = {%s}", appId));
            if(!applicationExists(appId)) {
                logger.log(Level.INFO, "Application doesn't exists. Creating new application");
//                deployApplication(appDeploymentRequest, appId); -- This will be taken care later
            }
            if(!applicationInRunningState(appId)) {
                waitForApplicationToFinish(appId);
            }
            launchTask(appDeploymentRequest, appId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Unable to launch task. " + e.getMessage());
        }

        String deploymentId = deploymentId(appDeploymentRequest);
        logger.log(Level.INFO, String.format("Exiting deploy().  Deployment Id = {%s}", deploymentId));
        return deploymentId;
    }


    private void launchTask(AppDeploymentRequest appDeploymentRequest, String appId) {
        try {
            String filename = appDeploymentRequest.getResource().getFilename();
            String deploymentId = deploymentId(appDeploymentRequest);
            AppBuilder appBuilder = new AppBuilder(appDeploymentRequest, deploymentId);
            String command = appBuilder.buildCommand(filename);
            String mavenCoordinates = appDeploymentRequest.getResource().getURI().toString();
            Client client = getClient();
            Response response = null;
            Task task = new Task(command, mavenCoordinates);
            WebTarget webTarget = client.target(getUrl(appId) + "/tasks");
            response = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(task, MediaType.APPLICATION_JSON_TYPE), Response.class);
            logger.log(Level.INFO, response.getStatus() + " "
                    + response.getStatusInfo() + " " + response);
            if (!response.getStatusInfo().toString().equals(Response.Status.ACCEPTED.toString()) &&
                    !response.getStatusInfo().toString().equals(Response.Status.OK.toString())) {
                String outputString = response.readEntity(String.class);
                throw new RuntimeException("Unable to launch task. " +  outputString);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to launch task", e);
            throw new RuntimeException(e);
        }
    }

    private static class Task {
        private String mavenCoordinates;
        private String command;

        public Task(String command, String mavenCoordinates) {
            this.command = command;
            this.mavenCoordinates = mavenCoordinates;
        }

        public String getMavenCoordinates() {
            return mavenCoordinates;
        }

        public void setMavenCoordinates(String mavenCoordinates) {
            this.mavenCoordinates = mavenCoordinates;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }

    private Client getClient() {
        final ClientConfig config = new ClientConfig().register(JacksonJsonProvider.class);
        return ClientBuilder.newClient(config);
    }

    private String appId(AppDeploymentRequest request) {
        if(request.getDefinition() != null) {
            Map<String, String> properties = request.getDefinition().getProperties();
            return properties.get("spring.cloud.task.name") + "Launcher";
        }
        throw new RuntimeException("Request invalid");
    }

    private String deploymentId(AppDeploymentRequest request) {
        if(request.getDefinition() != null) {
            Map<String, String> properties = request.getDefinition().getProperties();
            return properties.get("spring.cloud.task.name") + properties.get("spring.cloud.task.executionid");
        }
        throw new RuntimeException("Request invalid");
    }

    private boolean applicationExists(String appId) {
        return accsClient.applicationExists(appId);
    }

    private boolean applicationInRunningState(String appId) {
        ApplicationStatus applicationStatus =  accsClient.getApplication(appId);
        if(applicationStatus != null) {
            return applicationStatus.getStatus().equalsIgnoreCase("RUNNING");
        }
        return false;
    }

    private String getUrl(String appId) {
        ApplicationStatus applicationStatus =  accsClient.getApplication(appId);
        if(applicationStatus != null) {
            return applicationStatus.getWebURL();
        }
        return null;
    }

    private void waitForApplicationToFinish(String appId) throws  Exception {
        Date currentTime = Calendar.getInstance().getTime();
        long maxTimeToWait = currentTime.getTime() + TIME_OUT_IN_MIN * 60 * 1000;
        boolean appRunning = false;
        while(!appRunning && maxTimeToWait > Calendar.getInstance().getTime().getTime()) {
            Thread.sleep(15000);
            appRunning = applicationInRunningState(appId);
        }
        if(!appRunning) {
            throw new Exception("Unable to get launcher app running. Can't Launch task.");
        }
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
