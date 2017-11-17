package oracle.paas.accs.dataflow.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;

@SpringBootApplication
@EnableDataFlowServer
public class ACCSDataFlowServer {

    public static void main(String[] args) {
        SpringApplication.run(ACCSDataFlowServer.class, args);
    }
}
