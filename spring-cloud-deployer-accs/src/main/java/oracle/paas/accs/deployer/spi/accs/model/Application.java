package oracle.paas.accs.deployer.spi.accs.model;

public class Application {
    private String name;
    private String runtime = "Java";
    private String notes = "App created using accs dataflow server";
    private Manifest manifest;
    private Deployment deployment;
    private String archiveURL;
    private String archiveFileName;
    private String subscription = "HOURLY";

    public String getSubscription() {
        return subscription;
    }

    public String getName() {
        return name;
    }

    public String getRuntime() {
        return runtime;
    }

    public String getNotes() {
        return notes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }

    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }

    public void setArchiveURL(String archiveURL) {
        this.archiveURL = archiveURL;
    }

    public void setArchiveFileName(String archiveFileName) {
        this.archiveFileName = archiveFileName;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public String getArchiveURL() {
        return archiveURL;
    }

    public String getArchiveFileName() {
        return archiveFileName;
    }

}



