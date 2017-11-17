package oracle.paas.accs.deployer.spi.accs.client;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import oracle.paas.accs.deployer.spi.accs.model.Application;
import oracle.paas.accs.deployer.spi.accs.model.ApplicationStatus;
import oracle.paas.accs.deployer.spi.accs.util.ACCSUtil;
import oracle.paas.accs.deployer.spi.util.GsonUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static oracle.paas.accs.deployer.spi.util.GsonUtil.gson;

public class ACCSClient {
    private String username;
    private String password;
    private String identityDomain;
    private String uri;
    private static Logger logger = Logger.getLogger(ACCSClient.class.getName());


    public ACCSClient(String username, String password, String identityDomain, String uri) {

        this.username = username;
        this.password = password;
        this.identityDomain = identityDomain;
        this.uri = uri;
    }

    public void createApplication(Application application) {
        logger.log(Level.INFO, "Inside createApplication");
        Client client = getClient();
        Response response = null;
        InputStream is = null;
        File manifestFile = null;
        File deploymentFile = null;
        try {
            FormDataMultiPart uploadform = new FormDataMultiPart();
            uploadform.field("name", application.getName());
            uploadform.field("runtime", application.getRuntime());
            uploadform.field("notes", application.getNotes());
            uploadform.field("subscription", application.getSubscription());
            uploadform.field("archiveURL", application.getArchiveURL());
            uploadform.field("archiveFileName", application.getArchiveFileName());

            if (application.getManifest() != null) {
                logger.log(Level.INFO, "Upload manifest.json file");

                manifestFile = new File(ACCSUtil.MANIFEST_FILE);
                FileUtils.writeStringToFile(manifestFile, gson().toJson(application.getManifest()));
                uploadform.bodyPart(new FileDataBodyPart("manifest", manifestFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));
            }
            if (application.getDeployment() != null) {
                logger.log(Level.INFO, "Upload deployment.json file. : " +gson().toJson(application.getDeployment()));
                deploymentFile = new File(ACCSUtil.DEPLOYMENT_FILE);
                FileUtils.writeStringToFile(deploymentFile, gson().toJson(application.getDeployment()));
                uploadform.bodyPart(new FileDataBodyPart("deployment", deploymentFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));
            }


            WebTarget webTarget = client.target(uri + "/" + identityDomain);
            response = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
                    .header("Authorization", authHeader())
                    .header("X-ID-TENANT-NAME", identityDomain)
                    .post(Entity.entity(uploadform, MediaType.MULTIPART_FORM_DATA_TYPE), Response.class);
            logger.log(Level.INFO, response.getStatus() + " "
                    + response.getStatusInfo() + " " + response);
            if (!response.getStatusInfo().toString().equals(Response.Status.ACCEPTED.toString())) {
                String outputString = response.readEntity(String.class);
                FailedResponse failedResponse =
                        GsonUtil.gson().fromJson(outputString, FailedResponse.class);
                if (failedResponse.getDetails() != null &&
                        failedResponse.getDetails().getMessage() != null) {
                    throw new RuntimeException(failedResponse.getDetails().getMessage());
                } else {
                    throw new RuntimeException("Post Application provisioning failed with status: " +
                        response.getStatusInfo());
                }
            }
        } catch (Exception je) {
            logger.log(Level.SEVERE, "Application creation failed :", je);
            throw new RuntimeException(je);
        } finally {
//            ACCSUtil.deleteFile(manifestFile);
//            ACCSUtil.deleteFile(deploymentFile);
            try {
                if (is != null)
                    is.close();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Exception while cleaning up", ex);
            }
        }
    }

    public ApplicationStatus getApplication(String appName) {
        logger.log(Level.INFO, "Inside getApplication : " + appName);
        Client client = getClient();
        Response response = null;

        WebTarget webTarget = client.target(uri + "/" + identityDomain + "/" + appName);
        response = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", authHeader())
                .header("X-ID-TENANT-NAME", identityDomain)
                .get(Response.class);
        
        logger.log(Level.INFO, response.getStatus() + " "
                + response.getStatusInfo() + " " + response);
        if (!response.getStatusInfo().toString().equals(Response.Status.OK.toString())) {
            if(response.getStatusInfo().toString().equals(Response.Status.NOT_FOUND.toString())) {
                logger.log(Level.WARNING, "Application doesn't exists");
                return null;
            } else {
                logger.log(Level.WARNING, "Unable to retrieve app details. Error Response : " + response);
                throw new RuntimeException("Unable to retrieve app details. Error Response : " + response.getStatus());
            }
        } else {
            String output = response.readEntity(String.class);
            return GsonUtil.gson().fromJson(output, ApplicationStatus.class);
        }
    }

    public void deleteApplication(String appName) {
        logger.log(Level.INFO, "Inside deleteApplication : " + appName);
        Client client = getClient();
        Response response = null;
        WebTarget webTarget = client.target(uri + "/" + identityDomain + "/" + appName);
        response = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", authHeader())
                .header("X-ID-TENANT-NAME", identityDomain)
                .delete(Response.class);
        if (!response.getStatusInfo().toString().equals(Response.Status.OK.toString()) &&
                !response.getStatusInfo().toString().equals(Response.Status.ACCEPTED.toString())) {
            String outputString = response.readEntity(String.class);
            FailedResponse failedResponse =
                    GsonUtil.gson().fromJson(outputString, FailedResponse.class);
            if (failedResponse.getDetails() != null &&
                    failedResponse.getDetails().getMessage() != null) {
                throw new RuntimeException(failedResponse.getDetails().getMessage());
            } else {
                throw new RuntimeException("Delete Application failed with status: " +
                        response.getStatusInfo());
            }
        }
    }

    private  String authHeader() {
        byte[] encodedBytes = Base64.encodeBase64((username + ":" + password).getBytes());
        return "Basic " + new String(encodedBytes);
    }

    private Client getClient() {
        final ClientConfig config = new ClientConfig().register(JacksonJsonProvider.class);
        Client client = ClientBuilder.newClient(config).register(MultiPartFeature.class);
        return client;
    }

    public boolean applicationExists(String appName) {
        if(getApplication(appName) == null) {
            return false;
        }
        return true;
    }

    public void updateApplication(Application application) {
        logger.log(Level.INFO, "Inside updateApplication");
        Client client = getClient();
        Response response = null;
        InputStream is = null;
        File manifestFile = null;
        File deploymentFile = null;
        try {
            FormDataMultiPart uploadform = new FormDataMultiPart();
            uploadform.field("archiveURL", application.getArchiveURL());
            uploadform.field("archiveFileName", application.getArchiveFileName());

            if (application.getManifest() != null) {
                logger.log(Level.INFO, "Upload manifest.json file");

                manifestFile = new File(ACCSUtil.MANIFEST_FILE);
                FileUtils.writeStringToFile(manifestFile, gson().toJson(application.getManifest()));
                uploadform.bodyPart(new FileDataBodyPart("manifest", manifestFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));
            }
            if (application.getDeployment() != null) {
                logger.log(Level.INFO, "Upload deployment.json file. : " + gson().toJson(application.getDeployment()));
                deploymentFile = new File(ACCSUtil.DEPLOYMENT_FILE);
                FileUtils.writeStringToFile(deploymentFile, gson().toJson(application.getDeployment()));
                uploadform.bodyPart(new FileDataBodyPart("deployment", deploymentFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));
            }

            WebTarget webTarget = client.target(uri + "/" + identityDomain + "/" + application.getName());
            response = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
                    .header("Authorization", authHeader())
                    .header("X-ID-TENANT-NAME", identityDomain)
                    .put(Entity.entity(uploadform, MediaType.MULTIPART_FORM_DATA_TYPE), Response.class);
            logger.log(Level.INFO, response.getStatus() + " "
                    + response.getStatusInfo() + " " + response);
            if (!response.getStatusInfo().toString().equals(Response.Status.ACCEPTED.toString()) &&
                    !response.getStatusInfo().toString().equals(Response.Status.OK.toString())) {
                String outputString = response.readEntity(String.class);
                FailedResponse failedResponse =
                        GsonUtil.gson().fromJson(outputString, FailedResponse.class);
                if (failedResponse.getDetails() != null &&
                        failedResponse.getDetails().getMessage() != null) {
                    throw new RuntimeException(failedResponse.getDetails().getMessage());
                } else {
                    throw new RuntimeException("Update failed with status: " +
                            response.getStatusInfo());
                }
            }
        } catch (Exception je) {
            logger.log(Level.SEVERE, "Application updation failed :", je);
            throw new RuntimeException(je);
        } finally {
//            ACCSUtil.deleteFile(manifestFile);
//            ACCSUtil.deleteFile(deploymentFile);
            try {
                if (is != null)
                    is.close();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Exception while cleaning up ", ex);
            }
        }
    }

    public static class FailedResponse {
        public FailedResponse() {
            super();
        }

        private String status;
        private Details details;

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setDetails(FailedResponse.Details details) {
            this.details = details;
        }

        public FailedResponse.Details getDetails() {
            return details;
        }

        public static class Details {
            private String message;

            public void setMessage(String message) {
                this.message = message;
            }

            public String getMessage() {
                return message;
            }
        }

    }


}
