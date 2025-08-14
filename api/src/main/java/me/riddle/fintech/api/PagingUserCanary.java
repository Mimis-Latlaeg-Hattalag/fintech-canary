package me.riddle.fintech.api;

import me.riddle.fintech.application.service.dto.PagerDutyUserService;
import me.riddle.fintech.domain.model.dto.PagedResponse;
import me.riddle.fintech.domain.model.dto.PagerDutyUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates paging through PagerDuty users API.
 */
public class PagingUserCanary {

    private final PagerDutyUserService userService;

    public PagingUserCanary(String apiToken) {
        this.userService = new PagerDutyUserService(apiToken);
    }

    /**
     * Fetch and display a single page of users.
     */
    public void demonstrateSinglePage() throws IOException, InterruptedException {
        System.out.println("=== Fetching Single Page of Users ===");

        PagedResponse<PagerDutyUser> page = userService.getUsersPage(0, 10);

        System.out.printf("Retrieved %d users%n", page.itemCount());
        System.out.printf("Has more pages: %s%n", page.hasMorePages());
        System.out.printf("Total users: %s%n", page.total());

        System.out.println("\nFirst few users:");
        page.data().stream()
                .limit(3)
                .forEach(user -> System.out.printf("  - %s (%s)%n", user.name(), user.email()));
    }

    /**
     * Fetch all users across multiple pages.
     * FixMe: Implement in next refactoring.
     */
    @SuppressWarnings("unused")
    public void demonstrateFullPagination() throws IOException, InterruptedException {
        System.out.println("\n=== Fetching All Users with Pagination ===");

        List<PagerDutyUser> allUsers = new ArrayList<>();
        int offset = 0;
        int pageSize = 25;
        int pageNumber = 0;

        while (true) {
            pageNumber++;
            System.out.printf("Fetching page %d (offset=%d)...%n", pageNumber, offset);

            PagedResponse<PagerDutyUser> page = userService.getUsersPage(offset, pageSize);
            allUsers.addAll(page.data());

            System.out.printf("  Retrieved %d users (total so far: %d)%n",
                    page.itemCount(), allUsers.size());

            if (!page.hasMorePages()) {
                break;
            }

            offset = page.nextOffset();
        }

        System.out.printf("\nTotal users retrieved: %d%n", allUsers.size());

        // Show some statistics
        long activeUsers = allUsers.stream()
                .filter(user -> !Boolean.FALSE.equals(user.invitationSent()))
                .count();

        System.out.printf("Active users (invitation sent): %d%n", activeUsers);
    }

    /**
     * Fetch a specific user by ID.
     * FixMe: Implement in next refactoring.
     */
    @SuppressWarnings("unused")
    public void demonstrateSingleUser(String userId) throws IOException, InterruptedException {
        System.out.printf("\n=== Fetching User: %s ===%n", userId);

        PagerDutyUser user = userService.getUser(userId);

        System.out.printf("Name: %s%n", user.name());
        System.out.printf("Email: %s%n", user.email());
        System.out.printf("Role: %s%n", user.role());
        System.out.printf("Time Zone: %s%n", user.timeZone());
        System.out.printf("Job Title: %s%n", user.jobTitle());

        if (user.hasUnknownFields()) {
            System.out.println("Unknown fields detected: " + user.unknownFields());
        }
    }

    /**
     * Run all demonstrations.
     */
    public void runDemo() throws IOException, InterruptedException {
        try {
            demonstrateSinglePage();

            // Uncomment to test full pagination (be careful with rate limits!)
            // demonstrateFullPagination();

            // To test single user fetch, you need a valid user ID
            // demonstrateSingleUser("PXXXXXX");

        } catch (IOException e) {
            System.err.println("API Error: " + e.getMessage());
            throw e;
        }
    }
}