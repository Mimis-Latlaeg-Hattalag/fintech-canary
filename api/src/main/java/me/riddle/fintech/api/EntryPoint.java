package me.riddle.fintech.api;

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
                    
                    Navigate her:
                    
                    https://developer.pagerduty.com/api-reference/c96e889522dd6-list-users
                    
                    To retrieve your testing token.
                    
                    """);
            System.exit(1);
        }

        try {
            PagingUserCanary canary = new PagingUserCanary(apiToken);
            canary.runDemo();
        } catch (Exception e) {
            System.err.println("Failed to run demo: " + e.getMessage());
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            System.exit(1);
        }
    }
}