package fr.insa.toto.webui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@Theme("default")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(String[] args) {
    try {
        java.sql.Connection con = fr.insa.beuvron.utils.database.ConnectionSimpleSGBD.defaultCon();
        fr.insa.toto.model.GestionBDD.razBdd(con); 
        con.close();
    } catch (Exception ex) {
        ex.printStackTrace();
    }

    SpringApplication.run(Application.class, args);     
}

}
