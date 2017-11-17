package oracle.paas.accs.deployer.spi.app;

import com.google.gson.reflect.TypeToken;
import oracle.paas.accs.deployer.spi.accs.model.Application;
import oracle.paas.accs.deployer.spi.accs.model.Deployment;
import oracle.paas.accs.deployer.spi.accs.model.Manifest;
import oracle.paas.accs.deployer.spi.accs.model.ServiceBinding;
import oracle.paas.accs.deployer.spi.accs.util.ACCSUtil;
import oracle.paas.accs.deployer.spi.util.GsonUtil;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.cloud.deployer.spi.app.AppDeployer.GROUP_PROPERTY_KEY;

public class AppBuilder {

    private static final String JMX_DEFAULT_DOMAIN_KEY = "spring.jmx.default-domain";

    private static final String ENDPOINTS_SHUTDOWN_ENABLED_KEY = "endpoints.shutdown.enabled";

    public static final String PREFIX = "spring.cloud.deployer.local";
    public static final String DYNAMIC_VALUE_PATTERN = ".*\\$\\{(.*)\\}";
    public static final String ACCS = "app.accs";
    private AppDeploymentRequest appDeploymentRequest;
    private String deploymentId;
    private Map<String, String> appDefinitionProperties = new HashMap<String, String>();

    private static Logger logger = Logger.getLogger(AppBuilder.class.getName());

    public AppBuilder(AppDeploymentRequest appDeploymentRequest, String deploymentId) {

        this.appDeploymentRequest = appDeploymentRequest;
        this.deploymentId = deploymentId;
        initialize();
    }

    private void initialize() {
        appDefinitionProperties.putAll(appDeploymentRequest.getDefinition().getProperties());
        String group = appDeploymentRequest.getDeploymentProperties().get(GROUP_PROPERTY_KEY);

        appDefinitionProperties.put(JMX_DEFAULT_DOMAIN_KEY, deploymentId);
        if (!appDeploymentRequest.getDefinition().getProperties().containsKey(ENDPOINTS_SHUTDOWN_ENABLED_KEY)) {
            appDefinitionProperties.put(ENDPOINTS_SHUTDOWN_ENABLED_KEY, "true");
        }
        appDefinitionProperties.put("endpoints.jmx.unique-names", "true");
        appDefinitionProperties.put("spring.cloud.application.guid", "1");
        if (group != null) {
            appDefinitionProperties.put("spring.cloud.application.group", group);
        }
        appDefinitionProperties.remove("server.port");

    }

    public Application getApplicationData(String zipName) {
        Map<String, String> accsProperties = getACCSProperties(appDeploymentRequest);
        String appName = ACCSUtil.getSanitizedApplicationName(deploymentId);
        Application application = new Application();
        application.setDeployment(buildDeployment(accsProperties));
        application.setManifest(buildManifest(accsProperties));
        application.setArchiveURL("_apaas/" + zipName);
        application.setArchiveFileName(zipName);
        application.setName(appName);
        return application;
    }
    private Deployment buildDeployment(Map<String, String> accsProperties) {
        Deployment deployment = new Deployment();
        if(accsProperties.get("app.ccs.deployment.instances") != null) {
            deployment.setInstances(Integer.parseInt(accsProperties.get("app.accs.deployment.instances")));
        }
        if(accsProperties.get("app.accs.deployment.memory") != null) {
            deployment.setMemory(accsProperties.get("app.accs.deployment.memory"));
        }
        if(accsProperties.get("app.accs.deployment.services") != null) {
            deployment.setServices(buildServiceBindings(accsProperties.get("app.accs.deployment.services")));
        }
        if(accsProperties.get("app.accs.deployment.environment") != null) {
            deployment.setEnvironment(buildEnvironmentVariables(accsProperties.get("app.accs.deployment.environment")));
        }
        return deployment;
    }

    private Map<String, String> buildEnvironmentVariables(String envVariables) {
        try {
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            return GsonUtil.gson().fromJson(envVariables, type);
        } catch (Exception e ) {
            logger.log(Level.SEVERE, "unable to parse env variables ", e);
            e.printStackTrace();
        }

        return new HashMap<String, String>();
    }

    private List<ServiceBinding> buildServiceBindings(String serviceBindings) {
        try {
            Type type = new TypeToken<List<ServiceBinding>>() {}.getType();
            return GsonUtil.gson().fromJson(serviceBindings, type);
        }catch (Exception e ) {
            logger.log(Level.SEVERE, "unable to parse service bindings", e);
            e.printStackTrace();
        }

        return new ArrayList<ServiceBinding>();
    }

    private Manifest buildManifest(Map<String, String> accsProperties) {
        Manifest manifest = new Manifest();
        if(accsProperties.get("app.accs.manifest.type") != null) {
            manifest.setType(accsProperties.get("app.accs.manifest.type"));
        }
        if(accsProperties.get("app.accs.manifest.startupTime") != null) {
            manifest.setStartupTime(Integer.parseInt(accsProperties.get("app.accs.manifest.startupTime")));
        }
        manifest.setCommand("sh " + ACCSUtil.APP_RUNNER);
        return manifest;
    }

    private Map<String, String> getACCSProperties(AppDeploymentRequest appDeploymentRequest) {
        Map<String, String> accsProperties = new HashMap<String, String>();
        Map<String, String> deploymentProperties = appDeploymentRequest.getDeploymentProperties();
        for(String prop : deploymentProperties.keySet()) {
            if(prop.startsWith(ACCS)) {
                accsProperties.put(prop, deploymentProperties.get(prop));
            }
        }

        Map<String, String> definitionProperties = appDeploymentRequest.getDefinition().getProperties();
        for(String prop : definitionProperties.keySet()) {
            if(prop.startsWith(ACCS)) {
                accsProperties.put(prop, definitionProperties.get(prop));
            }
        }
        return accsProperties;
    }


    public String buildCommand(String jarName) {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("java -jar " +jarName);

        addDefinitionProperties(commands);
        addDeploymentProperties(commands);
        commands.addAll(appDeploymentRequest.getCommandlineArguments());
        logger.log(Level.INFO, "Java Command = " + StringUtils.collectionToDelimitedString(commands, " "));
        return StringUtils.collectionToDelimitedString(commands, " ");
    }

    private void addDefinitionProperties(List<String> commands) {
        for (String prop : appDefinitionProperties.keySet()) {
            if(!prop.startsWith(ACCS)) {
                addToCommand(commands, appDefinitionProperties, prop);
            }
        }
    }

    private static void addToCommand(List<String> commands, Map<String, String> args, String prop) {
        if(isDynamicValue(args, prop)) {
            commands.add(String.format("--%s=%s", prop, replaceDynamicValue(args, prop)));
        } else {
            commands.add(String.format("--%s=%s", prop, args.get(prop)));
        }
    }

    private static String replaceDynamicValue(Map<String, String> args, String prop) {
        String value = args.get(prop);

        Pattern pattern = Pattern.compile(DYNAMIC_VALUE_PATTERN);
        Matcher matcher = pattern.matcher(args.get(prop));
        int count=1;
        while(matcher.find()) {
            String match = matcher.group(count);
            logger.log(Level.INFO, "Match : " + match);
            if(args.get(match) != null) {
                value = value.replace("${"  + match + "}", args.get(match));
            } else {
                value = value.replace("${"  + match + "}", "");
            }
            count++;
        }
        logger.log(Level.INFO, "Final value : " + value);
        return value;
    }

    private static boolean isDynamicValue(Map<String, String> args, String prop) {
        Pattern pattern = Pattern.compile(DYNAMIC_VALUE_PATTERN);
        Matcher matcher = pattern.matcher(args.get(prop));

        if(matcher.find()) {
            return true;
        }

            return false;
    }

    private void addDeploymentProperties(List<String> commands) {
        Map<String, String> deploymentProperties = appDeploymentRequest.getDeploymentProperties();
        for(String prop: deploymentProperties.keySet()) {
            if(prop.equalsIgnoreCase(PREFIX + "." + "javaOpts")) {
                addJavaOptions(commands, deploymentProperties.get(prop));
            } else {
                if(!prop.startsWith(ACCS)) {
                    addToCommand(commands, deploymentProperties, prop);
                }
            }
        }
    }

    private void addJavaOptions(List<String> commands, String javaOptsString) {

        if (javaOptsString != null) {
            String[] javaOpts = StringUtils.tokenizeToStringArray(javaOptsString, " ");
            commands.addAll(Arrays.asList(javaOpts));
        }
    }

}
