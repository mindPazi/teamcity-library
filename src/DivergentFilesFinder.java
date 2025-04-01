package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Two files with the same SHA could have different content in case of
 * collisions,
 * but the probability is negligible so we will avoid the check
 */
public class DivergentFilesFinder {
    private String owner;
    private String repo;
    private String accessToken;
    private String localRepoPath;
    private String branchA;
    private String branchB;
    private Map<String, String> headers;

    public DivergentFilesFinder(String owner, String repo, String accessToken, String localRepoPath, String branchA,
            String branchB) {
        this.owner = owner;
        this.repo = repo;
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
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            cmd.add("-C");
            cmd.add(this.localRepoPath);
            cmd.add("merge-base");
            cmd.add(this.branchA);
            cmd.add(this.branchB);

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String sha = reader.readLine().trim(); // str: "a1b2c3d4..."

            return sha;
        } catch (IOException e) {
            System.err.println("Error getting merge base: " + e.getMessage());
            throw new RuntimeException("Failed to get merge base", e);
        }
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
            String remoteSha = remoteShas.get(file); // str: "a1b2c3d4..." or null
            String localSha = localShas.get(file); // str: "e5f6g7h8..." or null
            String baseSha = baseFiles.get(file); // str: "x1y2z3..." or null

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
            String remoteSha = remoteShas.get(file); // str: "e5f6g7h8..." or null
            String localSha = localShas.get(file); // str: "i9j0k1l2..." or null
            String baseSha = baseFiles.get(file); // str: "a1b2c3d4..." or null

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
     * Get the SHAs of all files in a local commit or branch
     */
    public Map<String, String> getLocalFileShas(String commitId) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            cmd.add("-C");
            cmd.add(this.localRepoPath);
            cmd.add("ls-tree");
            cmd.add("-r");
            cmd.add(commitId);

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Create a dictionary to store file paths and their SHAs
            Map<String, String> shaMap = new HashMap<>();

            // Iterate through each line of the output
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\s+"); // array: ["mode", "type", "sha", "filename"]
                if (parts.length >= 4) {
                    // Add the file path and SHA to the dictionary
                    shaMap.put(parts[3], parts[2]);
                }
            }

            return shaMap; // Map: {"file1.txt": "sha1", "file2.txt": "sha2", ...}
        } catch (IOException e) {
            System.err.println("Error getting local file SHAs: " + e.getMessage());
            throw new RuntimeException("Failed to get local file SHAs", e);
        }
    }

    /**
     * Get the SHAs of all files in a remote commit or branch
     */
    public Map<String, String> getRemoteFileShas(String commitId) {
        try {
            String urlStr = "https://api.github.com/repos/" + this.owner + "/" + this.repo + "/git/trees/" + commitId
                    + "?recursive=1";
            URL url = new URL(urlStr);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Set headers
            for (Map.Entry<String, String> header : this.headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP error: " + responseCode);
            }

            // Read response
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON
            JSONObject data = new JSONObject(response.toString());

            // Create a dictionary to store file paths and their SHAs
            Map<String, String> shaMap = new HashMap<>();

            // Iterate through each item in the tree
            JSONArray tree = data.getJSONArray("tree");
            for (int i = 0; i < tree.length(); i++) {
                JSONObject item = tree.getJSONObject(i);
                // Only include blob items (files, not directories)
                if ("blob".equals(item.getString("type"))) {
                    // Add the file path and SHA to the dictionary
                    shaMap.put(item.getString("path"), item.getString("sha"));
                }
            }

            // Return the dictionary of file SHAs
            return shaMap; // Map: {"file1.txt": "sha1", "file2.txt": "sha2", ...}
        } catch (IOException e) {
            System.err.println("Error getting remote file SHAs: " + e.getMessage());
            throw new RuntimeException("Failed to get remote file SHAs", e);
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
            String localSha = localShas.get(basePath); // str: "a1b2c3d4..." or null

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
            String remoteSha = remoteShas.get(basePath); // str: "e5f6g7h8..." or null

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
            boolean remoteHasFile = remoteShas.containsKey(basePath); // boolean: true/false
            boolean localHasFile = localShas.containsKey(basePath); // boolean: true/false

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
     * Get all files at a specific commit
     */
    public Map<String, String> getFilesAtCommit(String commitSha) {
        // Get all files and their SHA hashes at the specified commit
        // Returns a map of file paths to their SHA hashes
        Map<String, String> filesAtCommit = new HashMap<>();

        try {
            // Execute git command to list all files at the commit
            ProcessBuilder pb = new ProcessBuilder("git", "ls-tree", "-r", commitSha);
            pb.directory(new File(this.localRepoPath));
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // Parse the output to extract file paths and their SHA hashes
            while ((line = reader.readLine()) != null) {
                // Format: <mode> <type> <object> <file>
                String[] parts = line.split("\\s+", 4);
                if (parts.length == 4) {
                    String sha = parts[2];
                    String filePath = parts[3];
                    filesAtCommit.put(filePath, sha);
                }
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error getting files at commit " + commitSha + ": " + e.getMessage());
        }

        return filesAtCommit;
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
