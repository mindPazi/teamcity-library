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
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class with static methods for Git-related operations
 */
public class GitFunctions {

    /**
     * Find the common ancestor commit between two branches
     */
    public static String getMergeBase(String localRepoPath, String branchA, String branchB) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            cmd.add("-C");
            cmd.add(localRepoPath);
            cmd.add("merge-base");
            cmd.add(branchA);
            cmd.add(branchB);

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String sha = reader.readLine();
            if (sha == null) {
                throw new RuntimeException("Could not find merge base between branches");
            }
            sha = sha.trim();

            return sha;
        } catch (IOException e) {
            System.err.println("Error getting merge base: " + e.getMessage());
            throw new RuntimeException("Failed to get merge base", e);
        }
    }

    /**
     * Get the SHAs of all files in a local commit or branch
     */
    public static Map<String, String> getLocalFileShas(String localRepoPath, String commitId) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            cmd.add("-C");
            cmd.add(localRepoPath);
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
    public static Map<String, String> getRemoteFileShas(String owner, String repo, String commitId,
            Map<String, String> headers) {
        try {
            String urlStr = "https://api.github.com/repos/" + owner + "/" + repo + "/git/trees/" + commitId
                    + "?recursive=1";
            URL url = new URL(urlStr);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Set headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
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
     * Get all files at a specific commit
     */
    public static Map<String, String> getFilesAtCommit(String localRepoPath, String commitSha) {
        // Get all files and their SHA hashes at the specified commit
        // Returns a map of file paths to their SHA hashes
        Map<String, String> filesAtCommit = new HashMap<>();

        try {
            // Execute git command to list all files at the commit
            ProcessBuilder pb = new ProcessBuilder("git", "ls-tree", "-r", commitSha);
            pb.directory(new File(localRepoPath));
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
}