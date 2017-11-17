package oracle.paas.accs.deployer.spi.accs.client;


public class ACCSInfo {

    private static final String NOT_SET_STR = "NOT-SET";
    private String platformType = NOT_SET_STR;
    private String version = NOT_SET_STR;
    private String apiVersion = NOT_SET_STR;
    private String hostVersion = NOT_SET_STR;
    private String endPoint = NOT_SET_STR;
    
    private static ACCSInfo instance = new ACCSInfo(); 
    
    private ACCSInfo() {
        loadProperties();
    }
    
    public static ACCSInfo getInstance() {
        return instance;
    }

    private void loadProperties() {
        platformType = validate("ACCS");
        apiVersion = validate("");
        hostVersion = validate("");
        endPoint = validate(System.getenv("ACCS_URL")); // http://slc06cig.us.oracle.com:7001";
    }
    
    private String validate(String str) {
        if (str == null || str.trim().length() <= 0) {
            return NOT_SET_STR;
        }
        return str;
    }
    
    public String getPlatformType() {
        return platformType;
    }

    public void setPlatformType(String platformType) {
        this.platformType = platformType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getHostVersion() {
        return hostVersion;
    }

    public void setHostVersion(String hostVersion) {
        this.hostVersion = hostVersion;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }
}
