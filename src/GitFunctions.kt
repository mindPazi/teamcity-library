package src

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashMap

import org.json.JSONArray
import org.json.JSONObject

/**
 * Utility class with static methods for Git-related operations
 */
object GitFunctions {

    /**
     * Find the common ancestor commit between two branches
     */
    @JvmStatic
    fun getMergeBase(localRepoPath: String, branchA: String, branchB: String): String {
        try {
            val cmd = ArrayList<String>()
            cmd.add("git")
            cmd.add("-C")
            cmd.add(localRepoPath)
            cmd.add("merge-base")
            cmd.add(branchA)
            cmd.add(branchB)

            val processBuilder = ProcessBuilder(cmd)
            val process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val sha = reader.readLine() ?: throw RuntimeException("Could not find merge base between branches")
            
            return sha.trim()
        } catch (e: IOException) {
            System.err.println("Error getting merge base: " + e.message)
            throw RuntimeException("Failed to get merge base", e)
        }
    }

    /**
     * Get the SHAs of all files in a local commit or branch
     */
    @JvmStatic
    fun getLocalFileShas(localRepoPath: String, commitId: String): Map<String, String> {
        try {
            val cmd = ArrayList<String>()
            cmd.add("git")
            cmd.add("-C")
            cmd.add(localRepoPath)
            cmd.add("ls-tree")
            cmd.add("-r")
            cmd.add(commitId)

            val processBuilder = ProcessBuilder(cmd)
            val process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))

            // Create a dictionary to store file paths and their SHAs
            val shaMap = HashMap<String, String>()

            // Iterate through each line of the output
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) {
                    continue
                }
                val parts = line!!.split("\\s+".toRegex()).toTypedArray() // array: ["mode", "type", "sha", "filename"]
                if (parts.size >= 4) {
                    // Add the file path and SHA to the dictionary
                    shaMap[parts[3]] = parts[2]
                }
            }

            return shaMap // Map: {"file1.txt": "sha1", "file2.txt": "sha2", ...}
        } catch (e: IOException) {
            System.err.println("Error getting local file SHAs: " + e.message)
            throw RuntimeException("Failed to get local file SHAs", e)
        }
    }

    /**
     * Get the SHAs of all files in a remote commit or branch
     */
    @JvmStatic
    fun getRemoteFileShas(owner: String, repo: String, commitId: String,
            headers: Map<String, String>): Map<String, String> {
        try {
            val urlStr = "https://api.github.com/repos/$owner/$repo/git/trees/$commitId?recursive=1"
            val url = URL(urlStr)

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            // Set headers
            for ((key, value) in headers) {
                connection.setRequestProperty(key, value)
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                throw RuntimeException("HTTP error: $responseCode")
            }

            // Read response
            val reader = BufferedReader(
                    InputStreamReader(connection.inputStream, StandardCharsets.UTF_8))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            // Parse JSON
            val data = JSONObject(response.toString())

            // Create a dictionary to store file paths and their SHAs
            val shaMap = HashMap<String, String>()

            // Iterate through each item in the tree
            val tree = data.getJSONArray("tree")
            for (i in 0 until tree.length()) {
                val item = tree.getJSONObject(i)
                // Only include blob items (files, not directories)
                if ("blob" == item.getString("type")) {
                    // Add the file path and SHA to the dictionary
                    shaMap[item.getString("path")] = item.getString("sha")
                }
            }

            // Return the dictionary of file SHAs
            return shaMap // Map: {"file1.txt": "sha1", "file2.txt": "sha2", ...}
        } catch (e: IOException) {
            System.err.println("Error getting remote file SHAs: " + e.message)
            throw RuntimeException("Failed to get remote file SHAs", e)
        }
    }

    /**
     * Get all files at a specific commit
     */
    @JvmStatic
    fun getFilesAtCommit(localRepoPath: String, commitSha: String): Map<String, String> {
        // Get all files and their SHA hashes at the specified commit
        // Returns a map of file paths to their SHA hashes
        val filesAtCommit = HashMap<String, String>()

        try {
            // Execute git command to list all files at the commit
            val pb = ProcessBuilder("git", "ls-tree", "-r", commitSha)
            pb.directory(File(localRepoPath))
            val process = pb.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            // Parse the output to extract file paths and their SHA hashes
            while (reader.readLine().also { line = it } != null) {
                // Format: <mode> <type> <object> <file>
                val parts = line!!.split("\\s+".toRegex(), 4)
                if (parts.size == 4) {
                    val sha = parts[2]
                    val filePath = parts[3]
                    filesAtCommit[filePath] = sha
                }
            }

            process.waitFor()
        } catch (e: IOException) {
            System.err.println("Error getting files at commit $commitSha: ${e.message}")
        } catch (e: InterruptedException) {
            System.err.println("Error getting files at commit $commitSha: ${e.message}")
        }

        return filesAtCommit
    }
} 