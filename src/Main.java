package src;
import java.time.Instant;

public class Main {
    public static void main(String[] args) {
        long startTime = Instant.now().toEpochMilli();
        DivergentFilesFinder finder = new DivergentFilesFinder(
                "mindPazi",
                "test-diff",
                "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",
                "C:/Users/Andrea/Desktop/test-diff",
                "branchA",
                "branchB");

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
