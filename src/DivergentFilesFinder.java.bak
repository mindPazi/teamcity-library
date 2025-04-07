package src;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Two files with the same SHA could have different content in case of
 * collisions,
 * but the probability is negligible so we will avoid the check
 */
public class DivergentFilesFinder {
    private String accessToken;
    private String localRepoPath;
    private String branchA;
    private String branchB;
    private Map<String, String> headers;

    public DivergentFilesFinder(String accessToken, String localRepoPath, String branchA,
            String branchB) {
        this.accessToken = accessToken;
        this.localRepoPath = localRepoPath;
        this.branchA = branchA;
        this.branchB = branchB;
        this.headers = new HashMap<>();
        this.headers.put("Authorization", "token " + this.accessToken);
        this.headers.put("Accept", "application/vnd.github.v3+json");
    }

    /**
     * Find the common ancestor commit between two branches
     */
    public String getMergeBase() {
        return GitFunctions.getMergeBase(localRepoPath, branchA, branchB);
    }

    /**
     * Find files that were only edited in both branches but not deleted
     */
    public void editedOnly(Map<String, String> baseFiles, Map<String, String> remoteShas,
            Map<String, String> localShas) {
        // Find files that exist in both branches
        Set<String> commonFiles = new HashSet<>(remoteShas.keySet());
        commonFiles.retainAll(localShas.keySet());

        System.out.println("\nFiles modified both remotely (branchA) and locally (branchB), still existing in both:");

        // Iterate through files that exist in both branches
        for (String file : commonFiles) {
            // Get the SHA of the file in each branch
            String remoteSha = remoteShas.get(file);
            String localSha = localShas.get(file);
            String baseSha = baseFiles.get(file);

            // If the file exists in both branches, was in base, and has different content
            // in both branches
            if (remoteSha != null && localSha != null && baseSha != null && !remoteSha.equals(localSha)) {
                // Print the file path
                System.out.println(" - " + file);
            }
        }
    }

    /**
     * Find files that were deleted in one branch and edited in the other
     */
    public void deletedToEdited(Map<String, String> baseFiles, Map<String, String> remoteShas,
            Map<String, String> localShas) {
        // Get all files that were in the base commit
        Set<String> baseFilesSet = baseFiles.keySet();

        System.out.println("\nFiles deleted in one branch and modified in the other:");

        for (String file : baseFilesSet) {
            // Get the SHA of the file in each branch
            String remoteSha = remoteShas.get(file);
            String localSha = localShas.get(file);
            String baseSha = baseFiles.get(file);

            // If the file exists locally but not remotely
            if (localSha != null && remoteSha == null) {
                // Check if the file was possibly renamed in the remote branch
                boolean possiblyRenamed = false;
                for (Map.Entry<String, String> entry : remoteShas.entrySet()) {
                    if (!entry.getKey().equals(file) && entry.getValue().equals(baseSha)) {
                        possiblyRenamed = true;
                        break;
                    }
                }

                // If not renamed, it was deleted
                if (!possiblyRenamed) {
                    // Print the file path and status
                    System.out
                            .println(" - " + file + " -> deleted in " + this.branchA + ", modified in " + this.branchB);
                }
            }
            // If the file exists remotely but not locally
            else if (remoteSha != null && localSha == null) {
                // Check if the file was possibly renamed in the local branch
                boolean possiblyRenamed = false;
                for (Map.Entry<String, String> entry : localShas.entrySet()) {
                    if (!entry.getKey().equals(file) && entry.getValue().equals(baseSha)) {
                        possiblyRenamed = true;
                        break;
                    }
                }

                // If not renamed, it was deleted
                if (!possiblyRenamed) {
                    // Print the file path and status
                    System.out
                            .println(" - " + file + " -> deleted in " + this.branchB + ", modified in " + this.branchA);
                }
            }
        }
    }

    /**
     * Find files that were renamed in one branch and edited in the other
     */
    public void renamedToEdited(Map<String, String> baseFiles, Map<String, String> remoteShas,
            Map<String, String> localShas) {
        System.out.println("\nFiles renamed or added in one branch, but modified in the other:");

        for (Map.Entry<String, String> entry : baseFiles.entrySet()) {
            String basePath = entry.getKey();
            String baseSha = entry.getValue();

            // Get the SHA of the file in the local branch
            String localSha = localShas.get(basePath);

            // If the file exists locally and was modified
            if (localSha != null && !localSha.equals(baseSha)) {
                // Look for a file in the remote branch with the same SHA as the base file
                for (Map.Entry<String, String> remoteEntry : remoteShas.entrySet()) {
                    String candidateRemote = remoteEntry.getKey();
                    String candidateSha = remoteEntry.getValue();

                    // If found a different file with the same SHA, it was renamed
                    if (!candidateRemote.equals(basePath) && candidateSha.equals(baseSha)) {
                        // Print the file paths and status
                        System.out.println(" - " + basePath + " -> " + candidateRemote + ": renamed/added in " +
                                this.branchA + ", modified in " + this.branchB + " as " + basePath);
                    }
                }
            }

            // Get the SHA of the file in the remote branch
            String remoteSha = remoteShas.get(basePath);

            // If the file exists remotely and was modified
            if (remoteSha != null && !remoteSha.equals(baseSha)) {
                // Look for a file in the local branch with the same SHA as the base file
                for (Map.Entry<String, String> localEntry : localShas.entrySet()) {
                    String candidateLocal = localEntry.getKey();
                    String candidateSha = localEntry.getValue();

                    // If found a different file with the same SHA, it was renamed
                    if (!candidateLocal.equals(basePath) && candidateSha.equals(baseSha)) {
                        // Print the file paths and status
                        System.out.println(" - " + basePath + " -> " + candidateLocal + ": renamed/added in " +
                                this.branchB + ", modified in " + this.branchA + " as " + basePath);
                    }
                }
            }
        }
    }

    /**
     * Find files that were renamed in one branch and deleted in the other
     */
    public void renamedToDeleted(Map<String, String> baseFiles, Map<String, String> remoteShas,
            Map<String, String> localShas) {
        System.out.println("\nFiles renamed or added in one branch and deleted in the other:");

        // Iterate through files in the base commit
        for (Map.Entry<String, String> entry : baseFiles.entrySet()) {
            String basePath = entry.getKey();
            String baseSha = entry.getValue();

            // Check if the file exists in each branch
            boolean remoteHasFile = remoteShas.containsKey(basePath);
            boolean localHasFile = localShas.containsKey(basePath);

            // If the file doesn't exist locally
            if (!localHasFile) {
                // Look for a file in the remote branch with the same SHA as the base file
                for (Map.Entry<String, String> remoteEntry : remoteShas.entrySet()) {
                    String f = remoteEntry.getKey();
                    String sha = remoteEntry.getValue();

                    // If found a different file with the same SHA, it was renamed remotely
                    if (!f.equals(basePath) && sha.equals(baseSha)) {
                        // Print the file paths and status
                        System.out.println(" - " + basePath + " -> " + f + ": renamed/added in " +
                                this.branchA + ", deleted in " + this.branchB);
                    }
                }
            }

            // If the file doesn't exist remotely
            if (!remoteHasFile) {
                // Look for a file in the local branch with the same SHA as the base file
                for (Map.Entry<String, String> localEntry : localShas.entrySet()) {
                    String f = localEntry.getKey();
                    String sha = localEntry.getValue();

                    // If found a different file with the same SHA, it was renamed locally
                    if (!f.equals(basePath) && sha.equals(baseSha)) {
                        // Print the file paths and status
                        System.out.println(" - " + basePath + " -> " + f + ": renamed/added in " +
                                this.branchB + ", deleted in " + this.branchA);
                    }
                }
            }
        }
    }

    /**
     * Get the name of branch A
     * 
     * @return the name of branch A
     */
    public String getBranchA() {
        return this.branchA;
    }

    /**
     * Get the name of branch B
     * 
     * @return the name of branch B
     */
    public String getBranchB() {
        return this.branchB;
    }
}
