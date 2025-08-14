package me.riddle.fintech.api;

import me.riddle.fintech.application.service.dto.PagerDutyUserService;
import me.riddle.fintech.domain.model.dto.PagedResponse;
import me.riddle.fintech.domain.model.dto.PagerDutyUser;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Full-featured interactive PagerDuty API canary with visual pagination.
 */
public class InteractivePagerDutyCanary {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RED = "\u001B[31m";

    private final PagerDutyUserService service;
    private final Scanner scanner;

    // Pagination state
    private int currentOffset = 0;
    private int pageSize = 10;
    private PagedResponse<PagerDutyUser> currentPage;
    private final List<PagerDutyUser> allLoadedUsers = new ArrayList<>();

    // Statistics
    private int totalApiCalls = 0;
    private long totalApiTime = 0;
    private final Map<String, Integer> timeZoneStats = new HashMap<>();
    private final Map<String, Integer> roleStats = new HashMap<>();

    public InteractivePagerDutyCanary(String apiToken) {
        this.service = new PagerDutyUserService(apiToken);
        this.scanner = new Scanner(System.in);
    }

    public void run() throws IOException, InterruptedException {
        printWelcome();

        while (true) {
            printMenu();
            var choice = scanner.nextLine().trim().toLowerCase();

            try {
                switch (choice) {
                    case "1" -> viewCurrentPage();
                    case "2" -> nextPage();
                    case "3" -> previousPage();
                    case "4" -> jumpToPage();
                    case "5" -> changePageSize();
                    case "6" -> searchUser();
                    case "7" -> loadAllUsers();
                    case "8" -> showStatistics();
                    case "9" -> exportData();
                    case "q", "quit", "exit" -> {
                        printGoodbye();
                        return;
                    }
                    default -> printError("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                printError("Error: " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    printWarning("Rate limit hit! Waiting 30 seconds...");
                    //noinspection BusyWait
                    Thread.sleep(30000);
                }
            }

            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void printWelcome() {
        clearScreen();
        System.out.println(ANSI_BOLD + ANSI_BLUE + """
            ╔══════════════════════════════════════════════════════╗
            ║      PagerDuty User Explorer - Interactive Canary    ║
            ╚══════════════════════════════════════════════════════╝
            """ + ANSI_RESET);
        System.out.println("Loading initial data...\n");
    }

    private void printMenu() throws IOException, InterruptedException {
        clearScreen();

        // Load current page if not loaded
        if (currentPage == null) {
            loadPage(currentOffset);
        }

        System.out.println(ANSI_BOLD + ANSI_CYAN + "═══ PagerDuty User Explorer ═══" + ANSI_RESET);
        System.out.printf("Page %d | Showing %d-%d of %s users%n",
                getCurrentPageNumber(),
                currentOffset + 1,
                currentOffset + currentPage.itemCount(),
                currentPage.total() != null ? currentPage.total().toString() : "?"
        );
        System.out.println("─".repeat(50));

        // Show current page preview
        if (!currentPage.isEmpty()) {
            System.out.println(ANSI_YELLOW + "Current page preview:" + ANSI_RESET);
            currentPage.data().stream()
                    .limit(3)
                    .forEach(user -> System.out.printf("  • %s (%s)%n",
                            user.name() != null ? user.name() : "Unknown",
                            user.email() != null ? user.email() : "No email"));
            if (currentPage.itemCount() > 3) {
                System.out.println("  ... and " + (currentPage.itemCount() - 3) + " more");
            }
        }

        System.out.println("\n" + ANSI_BOLD + "Options:" + ANSI_RESET);
        System.out.println("  1. View current page (detailed)");
        System.out.println("  2. Next page →");
        System.out.println("  3. Previous page ←");
        System.out.println("  4. Jump to page");
        System.out.println("  5. Change page size (current: " + pageSize + ")");
        System.out.println("  6. Search for user");
        System.out.println("  7. Load all users");
        System.out.println("  8. Show statistics");
        System.out.println("  9. Export data");
        System.out.println("  Q. Quit");
        System.out.print("\nChoice: ");
    }

    private void viewCurrentPage() {
        if (currentPage == null || currentPage.isEmpty()) {
            printWarning("No users on current page");
            return;
        }

        clearScreen();
        System.out.println(ANSI_BOLD + ANSI_GREEN +
                String.format("═══ Page %d - Detailed View ═══", getCurrentPageNumber()) +
                ANSI_RESET);
        System.out.println();

        int index = 1;
        for (var user : currentPage.data()) {
            printUserDetailed(index++, user);
            System.out.println("─".repeat(70));
        }

        printPageNavigation();
    }

    private void printUserDetailed(int index, PagerDutyUser user) {
        System.out.printf(ANSI_BOLD + "%d. %s" + ANSI_RESET + " (ID: %s)%n",
                index,
                user.name() != null ? user.name() : "Unknown User",
                user.id());

        System.out.printf("   Email: %s%n", user.email() != null ? user.email() : "N/A");
        System.out.printf("   Role: %s | Type: %s%n",
                user.role() != null ? user.role() : "N/A",
                user.type());

        if (user.jobTitle() != null) {
            System.out.printf("   Job Title: %s%n", user.jobTitle());
        }

        if (user.timeZone() != null) {
            System.out.printf("   Time Zone: %s%n", user.timeZone());
        }

        if (user.invitationSent() != null) {
            System.out.printf("   Status: %s%n",
                    user.invitationSent() ? "Active" : "Invitation Pending");
        }

        if (user.hasUnknownFields()) {
            System.out.printf("   " + ANSI_YELLOW + "Unknown fields: %s" + ANSI_RESET + "%n",
                    user.unknownFields().keySet());
        }
    }

    private void nextPage() throws IOException, InterruptedException {
        if (currentPage != null && currentPage.hasMorePages()) {
            currentOffset = currentPage.nextOffset();
            loadPage(currentOffset);
            printSuccess("Moved to page " + getCurrentPageNumber());
        } else {
            printWarning("Already on last page");
        }
    }

    private void previousPage() throws IOException, InterruptedException {
        if (currentOffset > 0) {
            currentOffset = Math.max(0, currentOffset - pageSize);
            loadPage(currentOffset);
            printSuccess("Moved to page " + getCurrentPageNumber());
        } else {
            printWarning("Already on first page");
        }
    }

    private void jumpToPage() throws IOException, InterruptedException {
        System.out.print("Enter page number: ");
        try {
            int pageNum = Integer.parseInt(scanner.nextLine().trim());
            if (pageNum < 1) {
                printError("Page number must be positive");
                return;
            }

            currentOffset = (pageNum - 1) * pageSize;
            loadPage(currentOffset);
            printSuccess("Jumped to page " + pageNum);
        } catch (NumberFormatException e) {
            printError("Invalid page number");
        }
    }

    private void changePageSize() throws IOException, InterruptedException {
        System.out.print("Enter new page size (1-100): ");
        try {
            int newSize = Integer.parseInt(scanner.nextLine().trim());
            if (newSize < 1 || newSize > 100) {
                printError("Page size must be between 1 and 100");
                return;
            }

            pageSize = newSize;
            currentOffset = 0; // Reset to first page
            loadPage(currentOffset);
            printSuccess("Page size changed to " + pageSize);
        } catch (NumberFormatException e) {
            printError("Invalid page size");
        }
    }

    private void searchUser() {
        System.out.print("Enter search term (name or email): ");
        var searchTerm = scanner.nextLine().trim().toLowerCase();

        if (searchTerm.isEmpty()) {
            printWarning("Search term cannot be empty");
            return;
        }

        System.out.println("\nSearching...");

        // Search in loaded users first
        var localResults = allLoadedUsers.stream()
                .filter(user ->
                        (user.name() != null && user.name().toLowerCase().contains(searchTerm)) ||
                                (user.email() != null && user.email().toLowerCase().contains(searchTerm))
                )
                .toList();

        if (!localResults.isEmpty()) {
            System.out.println("\nFound in loaded users:");
            localResults.forEach(user ->
                    System.out.printf("  • %s (%s) - ID: %s%n",
                            user.name(), user.email(), user.id()));
        }

        System.out.println("\nNote: Full search requires loading all users (option 7)");
    }

    private void loadAllUsers() throws IOException, InterruptedException {
        System.out.println(ANSI_YELLOW + "Loading all users... This may take a while." + ANSI_RESET);
        System.out.println("Press Ctrl+C to cancel\n");

        allLoadedUsers.clear();
        var offset = 0;
        var limit = 100; // Max for faster loading
        var pageNum = 0;

        while (true) {
            pageNum++;
            System.out.printf("\rLoading page %d... ", pageNum);
            System.out.flush();

            var page = fetchPage(offset, limit);
            allLoadedUsers.addAll(page.data());

            updateStatistics(page.data());

            if (!page.hasMorePages()) {
                break;
            }

            offset = page.nextOffset();
            //noinspection BusyWait
            Thread.sleep(100); // Rate limiting
        }

        System.out.println(ANSI_GREEN + "\n✓ Loaded " + allLoadedUsers.size() +
                " users in " + pageNum + " pages" + ANSI_RESET);
    }

    private void showStatistics() {
        clearScreen();
        System.out.println(ANSI_BOLD + ANSI_CYAN + "═══ Statistics ═══" + ANSI_RESET);

        System.out.printf("\nAPI Calls: %d%n", totalApiCalls);
        if (totalApiCalls > 0) {
            System.out.printf("Average Response Time: %d ms%n", totalApiTime / totalApiCalls);
        }

        System.out.printf("\nTotal Users Loaded: %d%n", allLoadedUsers.size());

        if (!timeZoneStats.isEmpty()) {
            System.out.println("\n" + ANSI_YELLOW + "Users by Time Zone:" + ANSI_RESET);
            timeZoneStats.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry ->
                            System.out.printf("  %-30s: %d%n", entry.getKey(), entry.getValue()));
        }

        if (!roleStats.isEmpty()) {
            System.out.println("\n" + ANSI_YELLOW + "Users by Role:" + ANSI_RESET);
            roleStats.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry ->
                            System.out.printf("  %-20s: %d%n", entry.getKey(), entry.getValue()));
        }
    }

    private void exportData() {
        if (allLoadedUsers.isEmpty()) {
            printWarning("No data to export. Load users first (option 7)");
            return;
        }

        System.out.print("Export format ((c)sv/(j)son): ");
        var format = scanner.nextLine().trim().toLowerCase();

        try {
            var filename = "pagerduty_users_" + Instant.now().getEpochSecond();

            switch (format) {
                case "csv", "c" -> exportToCsv(filename + ".csv");
                case "json", "j" -> exportToJson(filename + ".json");
                default -> printError("Invalid format. Choose 'csv' or 'json'");
            }
        } catch (Exception e) {
            printError("Export failed: " + e.getMessage());
        }
    }

    private void exportToCsv(String filename) throws IOException {
        try (var writer = new java.io.PrintWriter(filename)) {
            writer.println("ID,Name,Email,Role,TimeZone,Status,JobTitle");

            for (var user : allLoadedUsers) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                        escapeCsv(user.id()),
                        escapeCsv(user.name()),
                        escapeCsv(user.email()),
                        escapeCsv(user.role()),
                        escapeCsv(user.timeZone()),
                        user.invitationSent() != null ? user.invitationSent() : "",
                        escapeCsv(user.jobTitle())
                );
            }
        }

        printSuccess("Exported " + allLoadedUsers.size() + " users to " + filename);
    }

    private void exportToJson(String filename) throws IOException {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new java.io.File(filename), allLoadedUsers);

        printSuccess("Exported " + allLoadedUsers.size() + " users to " + filename);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void loadPage(int offset) throws IOException, InterruptedException {
        var startTime = System.currentTimeMillis();
        currentPage = fetchPage(offset, pageSize);
        var duration = System.currentTimeMillis() - startTime;

        totalApiCalls++;
        totalApiTime += duration;

        System.out.printf(ANSI_CYAN + "  [API call took %d ms]" + ANSI_RESET + "%n", duration);
    }

    private PagedResponse<PagerDutyUser> fetchPage(int offset, int limit)
            throws IOException, InterruptedException {
        return service.getUsersPage(offset, limit);
    }

    private void updateStatistics(List<PagerDutyUser> users) {
        for (var user : users) {
            if (user.timeZone() != null) {
                timeZoneStats.merge(user.timeZone(), 1, Integer::sum);
            }
            if (user.role() != null) {
                roleStats.merge(user.role(), 1, Integer::sum);
            }
        }
    }

    private int getCurrentPageNumber() {
        return (currentOffset / pageSize) + 1;
    }

    private void printPageNavigation() {
        System.out.println("\n" + ANSI_CYAN);
        if (currentOffset > 0) {
            System.out.print("[← Previous] ");
        }
        System.out.print("Page " + getCurrentPageNumber());
        if (currentPage != null && currentPage.hasMorePages()) {
            System.out.print(" [Next →]");
        }
        System.out.println(ANSI_RESET);
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void printSuccess(String message) {
        System.out.println(ANSI_GREEN + "✓ " + message + ANSI_RESET);
    }

    private void printWarning(String message) {
        System.out.println(ANSI_YELLOW + "⚠ " + message + ANSI_RESET);
    }

    private void printError(String message) {
        System.out.println(ANSI_RED + "✗ " + message + ANSI_RESET);
    }

    private void printGoodbye() {
        clearScreen();
        System.out.println(ANSI_BOLD + ANSI_BLUE + """
            
            Thank you for using PagerDuty User Explorer!
            
            Statistics for this session:
            """ + ANSI_RESET);

        System.out.printf("  • API calls made: %d%n", totalApiCalls);
        System.out.printf("  • Users examined: %d%n", allLoadedUsers.size());
        if (totalApiCalls > 0) {
            System.out.printf("  • Avg response time: %d ms%n", totalApiTime / totalApiCalls);
        }

        System.out.println("\nGoodbye! 👋\n");
    }
}