package src

import java.io.FileInputStream
import java.io.IOException
import java.time.Instant
import java.util.HashMap
import java.util.Properties

fun main() {
    val properties = Properties()
    try {
        FileInputStream("config.properties").use { fis ->
            properties.load(fis)
        }
    } catch (e: IOException) {
        System.err.println("Error loading config.properties: " + e.message)
        return
    }

    val username = properties.getProperty("username")
    val repository = properties.getProperty("repository")
    val token = properties.getProperty("token")
    val localPath = properties.getProperty("localPath")
    val branchA = properties.getProperty("branchA")
    val branchB = properties.getProperty("branchB")

    val startTime = Instant.now().toEpochMilli()
    val finder = DivergentFilesFinder(token,
            localPath,
            branchA,
            branchB)

    val baseSha = finder.getMergeBase()
    val baseFiles = GitFunctions.getFilesAtCommit(localPath, baseSha)
    println("Base SHA: $baseSha")
    println()
    println("Base files: $baseFiles")
    println()

    // Create headers map for GitHub API calls
    val headers = HashMap<String, String>()
    headers["Authorization"] = "token $token"
    headers["Accept"] = "application/vnd.github.v3+json"

    val remoteShas = GitFunctions.getRemoteFileShas(username, repository, finder.getBranchA(),
            headers)
    val localShas = GitFunctions.getLocalFileShas(localPath, finder.getBranchB())
    println("Remote file SHAs: $remoteShas")
    println()
    println("Local file SHAs: $localShas")
    println()

    finder.editedOnly(baseFiles, remoteShas, localShas)
    finder.deletedToEdited(baseFiles, remoteShas, localShas)

    finder.renamedToEdited(baseFiles, remoteShas, localShas)
    finder.renamedToDeleted(baseFiles, remoteShas, localShas)

    val endTime = Instant.now().toEpochMilli()
    println("\nExecution time: " + (endTime - startTime) / 1000.0 + " seconds")
} 