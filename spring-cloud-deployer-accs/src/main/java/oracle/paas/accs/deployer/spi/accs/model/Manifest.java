package oracle.paas.accs.deployer.spi.accs.model;

public class Manifest {
    private String command;
    private String type = "web";
    private Integer startupTime = 120;

    public String getCommand() {
        return command;
    }

    public String getType() {
        return type;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(Integer startupTime) {
        this.startupTime = startupTime;
    }
}
