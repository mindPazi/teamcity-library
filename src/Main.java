package src;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
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
        DivergentFilesFinder finder = new DivergentFilesFinder(
                username,
                repository,
                token,
                localPath,
                branchA,
                branchB);

        String baseSha = finder.getMergeBase();
        java.util.Map<String, String> baseFiles = finder.getFilesAtCommit(baseSha);
        System.out.println("Base SHA: " + baseSha);
        System.out.println();
        System.out.println("Base files: " + baseFiles);
        System.out.println();
        java.util.Map<String, String> remoteShas = finder.getRemoteFileShas(finder.getBranchA());
        java.util.Map<String, String> localShas = finder.getLocalFileShas(finder.getBranchB());
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
