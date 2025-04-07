package src;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            return;
        }

        String username = properties.getProperty("username");
        String repository = properties.getProperty("repository");
        String token = properties.getProperty("token");
        String localPath = properties.getProperty("localPath");
        String branchA = properties.getProperty("branchA");
        String branchB = properties.getProperty("branchB");

        long startTime = Instant.now().toEpochMilli();
        DivergentFilesFinder finder = new DivergentFilesFinder(token,
                localPath,
                branchA,
                branchB);

        String baseSha = finder.getMergeBase();
        Map<String, String> baseFiles = GitFunctions.getFilesAtCommit(localPath, baseSha);
        System.out.println("Base SHA: " + baseSha);
        System.out.println();
        System.out.println("Base files: " + baseFiles);
        System.out.println();

        // Create headers map for GitHub API calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "token " + token);
        headers.put("Accept", "application/vnd.github.v3+json");

        Map<String, String> remoteShas = GitFunctions.getRemoteFileShas(username, repository, finder.getBranchA(),
                headers);
        Map<String, String> localShas = GitFunctions.getLocalFileShas(localPath, finder.getBranchB());
        System.out.println("Remote file SHAs: " + remoteShas);
        System.out.println();
        System.out.println("Local file SHAs: " + localShas);
        System.out.println();

        finder.editedOnly(baseFiles, remoteShas, localShas);
        finder.deletedToEdited(baseFiles, remoteShas, localShas);

        finder.renamedToEdited(baseFiles, remoteShas, localShas);
        finder.renamedToDeleted(baseFiles, remoteShas, localShas);

        long endTime = Instant.now().toEpochMilli();
        System.out.println("\nExecution time: " + (endTime - startTime) / 1000.0 + " seconds");
    }
}
