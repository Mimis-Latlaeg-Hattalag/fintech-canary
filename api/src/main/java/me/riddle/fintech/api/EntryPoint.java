package me.riddle.fintech.api;

import java.util.Scanner;

public class EntryPoint {

    public static void main(String[] args) {
        String apiToken = System.getenv("PAGERDUTY_API_TOKEN");

        if (apiToken == null || apiToken.isBlank()) {
            System.err.println("""
                    
                    PAGERDUTY_API_TOKEN is not set.
                    
                    Please set it with: export PAGERDUTY_API_TOKEN=your_token_here
                    As in (export PAGERDUTY_API_TOKEN=your_token_here gradle api:run)
                    
                    
                    IMPORTANT: Are you testing in the IDE?
                    RunConfig is stored with the project.
                    Examine the `EntryPoint` runconfig and set the environment variables value for your token.
                    
                    Navigate here:
                    
                    https://developer.pagerduty.com/api-reference/c96e889522dd6-list-users
                    
                    To retrieve your testing token.
                    
                    """);
            System.exit(1);
        }

        try {
            // Check for command line arguments
            boolean interactiveMode = args.length > 0 &&
                    (args[0].equalsIgnoreCase("-i") || args[0].equalsIgnoreCase("--interactive"));

            PagingUserCanary canary = new PagingUserCanary(apiToken);

            if (interactiveMode) {
                System.out.println("Starting interactive mode...");
                canary.runInteractive(apiToken);
            } else {
                // Check if running in a terminal that supports interaction
                if (System.console() != null) {
                    System.out.println("PagerDuty API Canary");
                    System.out.println("====================");
                    System.out.println("1. Run simple demo");
                    System.out.println("2. Run interactive explorer");
                    System.out.print("\nChoice (1-2): ");

                    Scanner scanner = new Scanner(System.in);
                    String choice = scanner.nextLine().trim();

                    if ("2".equals(choice)) {
                        canary.runInteractive(apiToken);
                    } else {
                        canary.runDemo();
                    }
                } else {
                    // Non-interactive environment (like IDE), run simple demo
                    canary.runDemo();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to run: " + e.getMessage());
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            System.exit(1);
        }
    }
}