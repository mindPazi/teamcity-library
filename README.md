# TeamCity GitHub Diff Library

A Java library that identifies divergent files between branches in a GitHub repository.

## Features

Find:

- files modified in both branches
- files deleted in one branch but modified in another
- files renamed in one branch and edited in another
- files renamed in one branch and deleted in another

## Installation

### Gradle

```gradle
dependencies {
    implementation 'io.github.teamcity:teamcity-github-diff:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.teamcity</groupId>
    <artifactId>teamcity-github-diff</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Manual

1. Download the latest JAR from the [releases](https://github.com/yourusername/teamcity-library/releases)
2. Add it to your project's classpath

## Usage

```java
// Initialize the finder
DivergentFilesFinder finder = new DivergentFilesFinder(
    "github-token",
    "/path/to/local/repo",
    "main",
    "feature-branch"
);

// Get the merge base and files
String baseSha = finder.getMergeBase();
Map<String, String> baseFiles = GitFunctions.getFilesAtCommit(
    "/path/to/local/repo", baseSha);

// Set up GitHub API headers
Map<String, String> headers = new HashMap<>();
headers.put("Authorization", "token github-token");
headers.put("Accept", "application/vnd.github.v3+json");

// Get files from branches
Map<String, String> remoteShas = GitFunctions.getRemoteFileShas(
    "username", "repository", "main", headers);
Map<String, String> localShas = GitFunctions.getLocalFileShas(
    "/path/to/local/repo", "feature-branch");

// Find divergences
finder.editedOnly(baseFiles, remoteShas, localShas);
finder.deletedToEdited(baseFiles, remoteShas, localShas);
finder.renamedToEdited(baseFiles, remoteShas, localShas);
finder.renamedToDeleted(baseFiles, remoteShas, localShas);
```

## API Reference

### DivergentFilesFinder

```java
DivergentFilesFinder(String accessToken, String localRepoPath, String branchA, String branchB)
String getMergeBase()
void editedOnly(Map<String, String> baseFiles, Map<String, String> remoteShas, Map<String, String> localShas)
void deletedToEdited(Map<String, String> baseFiles, Map<String, String> remoteShas, Map<String, String> localShas)
void renamedToEdited(Map<String, String> baseFiles, Map<String, String> remoteShas, Map<String, String> localShas)
void renamedToDeleted(Map<String, String> baseFiles, Map<String, String> remoteShas, Map<String, String> localShas)
```

### GitFunctions

```java
static String getMergeBase(String localRepoPath, String branchA, String branchB)
static Map<String, String> getLocalFileShas(String localRepoPath, String commitId)
static Map<String, String> getRemoteFileShas(String owner, String repo, String commitId, Map<String, String> headers)
static Map<String, String> getFilesAtCommit(String localRepoPath, String commitSha)
```

## Building from Source

```bash
./gradlew build
```

Generates:

- `teamcity-github-diff-1.0.0.jar` - Standard JAR
- `teamcity-github-diff-1.0.0-all.jar` - Fat JAR with dependencies
- `teamcity-github-diff-1.0.0-sources.jar` - Sources
- `teamcity-github-diff-1.0.0-javadoc.jar` - Javadoc

## License

[MIT License](LICENSE)
