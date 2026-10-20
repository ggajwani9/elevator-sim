package com.elevatorsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * One jar, two modes:
 * <ul>
 *   <li>with {@code --input FILE} or {@code --generate N}: run once on the command line and exit;</li>
 *   <li>with no such argument: start the REST API on port 8080.</li>
 * </ul>
 */
@SpringBootApplication
public class ElevatorSimApplication {

    public static void main(String[] args) {
        if (Cli.isCliRun(args)) {
            System.exit(Cli.run(args, System.out, System.err));
        }
        SpringApplication.run(ElevatorSimApplication.class, args);
    }
}
