package com.dms.liteflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DMS LiteFlow 应用程序入口
 */
@SpringBootApplication
public class DmsLiteflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DmsLiteflowApplication.class, args);
        System.out.println("""

                ___   _   _     _       _      _           ___    ___  _
               / __| | | | |   (_)     | |    | |         / __|  / _ \\| |
              | (__  | |_| |__  _ _ __ | |_ __| |_  __    \\__ \\| | | | |
               \\___| \\___/|_| (_|(_|  \\__/|_| \\__|  \\___||_| |_| |_|_|
                   |___/

               DMS LiteFlow Rule Engine v1.0.0
        """);
    }
}
