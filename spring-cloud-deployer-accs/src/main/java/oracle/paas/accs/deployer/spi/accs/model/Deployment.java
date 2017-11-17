package oracle.paas.accs.deployer.spi.accs.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Deployment {

    private Integer instances = 1;
    private String memory = "2G";
    private List<ServiceBinding> services = new ArrayList<ServiceBinding>();
    private Map<String, String> environment = new LinkedHashMap<String, String>();

    public Integer getInstances() {
        return instances;
    }

    public String getMemory() {
        return memory;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public List<ServiceBinding> getServices() {
        return services;
    }

    public void setInstances(Integer instances) {
        this.instances = instances;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public void setServices(List<ServiceBinding> services) {
        this.services = services;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }
}
