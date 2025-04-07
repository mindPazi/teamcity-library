package src

import java.util.HashMap
import java.util.HashSet

/**
 * Two files with the same SHA could have different content in case of
 * collisions,
 * but the probability is negligible so we will avoid the check
 */
class DivergentFilesFinder(
    private val accessToken: String,
    private val localRepoPath: String,
    private val branchA: String,
    private val branchB: String
) {
    private val headers: Map<String, String>

    init {
        val headersMap = HashMap<String, String>()
        headersMap["Authorization"] = "token $accessToken"
        headersMap["Accept"] = "application/vnd.github.v3+json"
        this.headers = headersMap
    }

    /**
     * Find the common ancestor commit between two branches
     */
    fun getMergeBase(): String {
        return GitFunctions.getMergeBase(localRepoPath, branchA, branchB)
    }

    /**
     * Find files that were only edited in both branches but not deleted
     */
    fun editedOnly(baseFiles: Map<String, String>, remoteShas: Map<String, String>,
            localShas: Map<String, String>) {
        // Find files that exist in both branches
        val commonFiles = HashSet(remoteShas.keys)
        commonFiles.retainAll(localShas.keys)

        println("\nFiles modified both remotely (branchA) and locally (branchB), still existing in both:")

        // Iterate through files that exist in both branches
        for (file in commonFiles) {
            // Get the SHA of the file in each branch
            val remoteSha = remoteShas[file]
            val localSha = localShas[file]
            val baseSha = baseFiles[file]

            // If the file exists in both branches, was in base, and has different content
            // in both branches
            if (remoteSha != null && localSha != null && baseSha != null && remoteSha != localSha) {
                // Print the file path
                println(" - $file")
            }
        }
    }

    /**
     * Find files that were deleted in one branch and edited in the other
     */
    fun deletedToEdited(baseFiles: Map<String, String>, remoteShas: Map<String, String>,
            localShas: Map<String, String>) {
        // Get all files that were in the base commit
        val baseFilesSet = baseFiles.keys

        println("\nFiles deleted in one branch and modified in the other:")

        for (file in baseFilesSet) {
            // Get the SHA of the file in each branch
            val remoteSha = remoteShas[file]
            val localSha = localShas[file]
            val baseSha = baseFiles[file]

            // If the file exists locally but not remotely
            if (localSha != null && remoteSha == null) {
                // Check if the file was possibly renamed in the remote branch
                var possiblyRenamed = false
                for (entry in remoteShas.entries) {
                    if (entry.key != file && entry.value == baseSha) {
                        possiblyRenamed = true
                        break
                    }
                }

                // If not renamed, it was deleted
                if (!possiblyRenamed) {
                    // Print the file path and status
                    println(" - $file -> deleted in $branchA, modified in $branchB")
                }
            }
            // If the file exists remotely but not locally
            else if (remoteSha != null && localSha == null) {
                // Check if the file was possibly renamed in the local branch
                var possiblyRenamed = false
                for (entry in localShas.entries) {
                    if (entry.key != file && entry.value == baseSha) {
                        possiblyRenamed = true
                        break
                    }
                }

                // If not renamed, it was deleted
                if (!possiblyRenamed) {
                    // Print the file path and status
                    println(" - $file -> deleted in $branchB, modified in $branchA")
                }
            }
        }
    }

    /**
     * Find files that were renamed in one branch and edited in the other
     */
    fun renamedToEdited(baseFiles: Map<String, String>, remoteShas: Map<String, String>,
            localShas: Map<String, String>) {
        println("\nFiles renamed or added in one branch, but modified in the other:")

        for (entry in baseFiles.entries) {
            val basePath = entry.key
            val baseSha = entry.value

            // Get the SHA of the file in the local branch
            val localSha = localShas[basePath]

            // If the file exists locally and was modified
            if (localSha != null && localSha != baseSha) {
                // Look for a file in the remote branch with the same SHA as the base file
                for (remoteEntry in remoteShas.entries) {
                    val candidateRemote = remoteEntry.key
                    val candidateSha = remoteEntry.value

                    // If found a different file with the same SHA, it was renamed
                    if (candidateRemote != basePath && candidateSha == baseSha) {
                        // Print the file paths and status
                        println(" - $basePath -> $candidateRemote: renamed/added in " +
                                "$branchA, modified in $branchB as $basePath")
                    }
                }
            }

            // Get the SHA of the file in the remote branch
            val remoteSha = remoteShas[basePath]

            // If the file exists remotely and was modified
            if (remoteSha != null && remoteSha != baseSha) {
                // Look for a file in the local branch with the same SHA as the base file
                for (localEntry in localShas.entries) {
                    val candidateLocal = localEntry.key
                    val candidateSha = localEntry.value

                    // If found a different file with the same SHA, it was renamed
                    if (candidateLocal != basePath && candidateSha == baseSha) {
                        // Print the file paths and status
                        println(" - $basePath -> $candidateLocal: renamed/added in " +
                                "$branchB, modified in $branchA as $basePath")
                    }
                }
            }
        }
    }

    /**
     * Find files that were renamed in one branch and deleted in the other
     */
    fun renamedToDeleted(baseFiles: Map<String, String>, remoteShas: Map<String, String>,
            localShas: Map<String, String>) {
        println("\nFiles renamed or added in one branch and deleted in the other:")

        // Iterate through files in the base commit
        for (entry in baseFiles.entries) {
            val basePath = entry.key
            val baseSha = entry.value

            // Check if the file exists in each branch
            val remoteHasFile = remoteShas.containsKey(basePath)
            val localHasFile = localShas.containsKey(basePath)

            // If the file doesn't exist locally
            if (!localHasFile) {
                // Look for a file in the remote branch with the same SHA as the base file
                for (remoteEntry in remoteShas.entries) {
                    val f = remoteEntry.key
                    val sha = remoteEntry.value

                    // If found a different file with the same SHA, it was renamed remotely
                    if (f != basePath && sha == baseSha) {
                        // Print the file paths and status
                        println(" - $basePath -> $f: renamed/added in " +
                                "$branchA, deleted in $branchB")
                    }
                }
            }

            // If the file doesn't exist remotely
            if (!remoteHasFile) {
                // Look for a file in the local branch with the same SHA as the base file
                for (localEntry in localShas.entries) {
                    val f = localEntry.key
                    val sha = localEntry.value

                    // If found a different file with the same SHA, it was renamed locally
                    if (f != basePath && sha == baseSha) {
                        // Print the file paths and status
                        println(" - $basePath -> $f: renamed/added in " +
                                "$branchB, deleted in $branchA")
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
    fun getBranchA(): String {
        return branchA
    }

    /**
     * Get the name of branch B
     *
     * @return the name of branch B
     */
    fun getBranchB(): String {
        return branchB
    }
} 