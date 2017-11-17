/*
 * Copyright @ 2017, Oracle and/or its affiliates. All rights reserved.
 */

package oracle.paas.accs.deployer.spi.app;


import java.util.Map;

import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

public class AppInstanceStatusImpl implements AppInstanceStatus{

    private String id;
    private DeploymentState state;
    private Map<String, String> attr;

    public AppInstanceStatusImpl(String id, DeploymentState state, Map<String, String> attr) {
        this.id = id;
        this.state = state;
        this.attr = attr;
    }

    public String getId() {
        return id;
    }

    public DeploymentState getState() {
        return state;
    }

    public Map<String, String> getAttributes() {
        return attr;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setState(DeploymentState state) {
        this.state = state;
    }

    public void setAttributes(Map<String, String> attr) {
        this.attr = attr;
    }

}
