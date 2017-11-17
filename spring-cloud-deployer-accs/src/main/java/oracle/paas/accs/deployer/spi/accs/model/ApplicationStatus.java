package oracle.paas.accs.deployer.spi.accs.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


public class ApplicationStatus {
    private String identityDomain;
    private String name;
    private String status;
    private String type;
    private String currentOngoingActivity;
    private String webURL;
    private String[] message;
    private LastestDeployment lastestDeployment;
    private Instances[] instances;

    public String getIdentityDomain() {
        return identityDomain;
    }

    public void setIdentityDomain(String identityDomain) {
        this.identityDomain = identityDomain;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCurrentOngoingActivity() {
        return currentOngoingActivity;
    }

    public void setCurrentOngoingActivity(String currentOngoingActivity) {
        this.currentOngoingActivity = currentOngoingActivity;
    }

    public String getWebURL() {
        return webURL;
    }

    public void setWebURL(String webURL) {
        this.webURL = webURL;
    }

    public String[] getMessage() {
        return message;
    }

    public void setMessage(String[] message) {
        this.message = message;
    }

    public LastestDeployment getLastestDeployment() {
        return lastestDeployment;
    }

    public void setLastestDeployment(LastestDeployment lastestDeployment) {
        this.lastestDeployment = lastestDeployment;
    }

    public Instances[] getInstances() {
        return instances;
    }

    public void setInstances(Instances[] instances) {
        this.instances = instances;
    }

    public boolean isLastDeploymentFailed() {
        if(this.getLastestDeployment() == null || this.getLastestDeployment().getDeploymentStatus() == null) {
            return false;
        }
        String deploymentStatus = this.getLastestDeployment().getDeploymentStatus();
        if(!deploymentStatus.equalsIgnoreCase("ERROR") && !deploymentStatus.equalsIgnoreCase("FAILED")) {
            return false;
        }

        return true;
    }

    @XmlType(propOrder = {"name", "status","memory","instanceURI"})
    public static class Instances {

        private String name;
        private String status;
        private String shape;
        private String instanceURI;

        public Instances() {
        }
        
        public Instances(String name, String status, String shape, String jobId) {
            this.name = name;
            this.status = status;
            this.shape = shape;
            this.instanceURI = jobId;
        }

        @XmlElement(name = "name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement(name = "status")
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @XmlElement(name = "instanceURI")
        public String getInstanceURI() {
            return instanceURI;
        }

        public void setInstanceURI(String uri) {
            this.instanceURI = uri;
        }

        @XmlElement(name = "memory")
        public String getShape() {
            return shape;
        }

        public void setShape(String shape) {
            this.shape = shape;
        }
    
    }

    public static class LastestDeployment {
        private String deploymentId;
        private String deploymentStatus;

        public String getDeploymentId() {
            return deploymentId;
        }

        public void setDeploymentId(String deploymentId) {
            this.deploymentId = deploymentId;
        }

        public String getDeploymentStatus() {
            return deploymentStatus;
        }

        public void setDeploymentStatus(String deploymentStatus) {
            this.deploymentStatus = deploymentStatus;
        }
    }
    
    

}
