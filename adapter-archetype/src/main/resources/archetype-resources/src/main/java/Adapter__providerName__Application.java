package ${package};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Adapter${providerName}Application {

    public static void main(String[] args) {
        SpringApplication.run(Adapter${providerName}Application.class, args);
    }
}
