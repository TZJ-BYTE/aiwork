package org.manage.xiaozuzuoye;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.Banner;
import org.springframework.core.env.Environment;
import java.io.PrintStream;

@SpringBootApplication
public class XiaozuzuoyeApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(XiaozuzuoyeApplication.class);
        app.setBanner(new Banner() {
            @Override
            public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
                out.println("" +
                    "   _____ _                 _   _____ _____ \n" +
                    "  / ____| |               | | |_   _|_   _|\n" +
                    " | |    | |__   __ _ _ __ | |   | |   | |  \n" +
                    " | |    | '_ \\ / _` | '_ \\| |   | |   | |  \n" +
                    " | |____| | | | (_| | | | | |  _| |_ _| |_ \n" +
                    "  \\_____|_| |_|\\__,_|_| |_|_| |_____|_____|\n" +
                    "                                            \n" +
                    "  :: Chat AI ::                (v1.0.0)");
            }
        });
        app.run(args);
    }
}
