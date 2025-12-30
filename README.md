# MyGit – A Minimal Git Implementation in Java

MyGit is a learning-oriented reimplementation of core Git concepts in pure Java.  
It provides:
- A **command-line interface (CLI)** that mimics a subset of `git` commands
- A **JavaFX graphical UI** for visualizing repository state, history, branches, and diffs

This project is intended both as a functional tool and as a reference for understanding how Git works internally: object storage, the index, references, and common porcelain commands.

---

## Features

### Core repository operations
- `init` – Initialize a new `.mygit` repository in the current directory
- `add` – Add files or directories to the index (staging area)
- `commit` – Create commits with author, message, and tree object
- `status` – Show staged, modified, and untracked files, plus branch/head state
- `log` – Show commit history for the current branch
- `config` – Configure `user.name` and `user.email` used for commits

### Branching and history
- `branch` – List branches or create/delete branches
- `checkout` – Check out branches or specific commits (supports `-create`)
- `switch` – Switch branches with safety checks for dirty working trees
- `merge` – Merge another branch/commit into the current branch
- `revert` – Create a new commit that reverts a given commit

### Working tree and index management
- `diff` – Show diffs for working tree or staged changes (`--staged`)
- `reset` – Reset index and/or working tree to `HEAD` (`--mixed`, `--hard`)
- `restore` – Restore files from `HEAD` or unstage files (`--staged`)
- `stash` – Stash current working changes and clean the tree, then apply or pop
- `gc` – Perform garbage-collection-like cleanup of stored objects

### JavaFX UI

The JavaFX desktop UI (see `com.mygit.UI.MainApp`) provides:

- **Startup / Repository detection**
  - If there is no `.mygit` directory in the current folder, shows an empty state with "No repository created yet" and a "Create Repo" button
  - After initializing the repository, automatically switches to the Home tab and prompts for username and email configuration
- **Home / Overview**
  - Repository path
  - Current branch
  - HEAD state (attached/detached)
  - Working tree state (Clean/Dirty)
  - Last commit SHA, message, author, and date/time
- **Status**
  - Staged, modified, and untracked files using `StatusCommand`
- **Log**
  - Commit history list
- **Branches**
  - Branch list
  - Create, switch, and delete branches with error dialogs
- **Diff**
  - File list and diff viewer for working and staged changes; for binary files, shows a placeholder message instead of crashing
- **Commit**
  - Prepare commit with message and stage summary
- **Merge**
  - Merge a branch or commit into the current branch via dialog
- **Repository menu**
  - Refresh repository overview
  - Stash changes
  - Apply stash

---

## Project Structure

Key source directories:

- `src/main/java/com/mygit`
  - `Main.java` – CLI entry point
  - `RefsUtil.java` – Helpers for reading/writing HEAD and branch refs
  - `storage/*` – Object store, trees, commits, packs
  - `index/*` – Git index implementation
  - `command/*` – Individual commands (`AddCommand`, `CommitCommand`, `StatusCommand`, etc.)
  - `pack/*` – Pack file and reachability logic
  - `ignore/IgnoreMatcher.java` – `.gitignore`-like behavior
  - `util/*` – Hashing, file stat, merge utilities, thread pool, configuration
- `src/main/java/com/mygit/UI`
  - `MainApp.java` – JavaFX application launcher
  - `controller/*` – Controllers for JavaFX views
  - `view/*` – FXML layouts (`main.fxml`, `status.fxml`, `log.fxml`, `branches.fxml`, `diff.fxml`, `commit.fxml`, etc.)
  - `model/*` – View models (`RepositoryState`, `CommitViewModel`, etc.)
  - `service/*` – UI-side services (`GitStateService`, `StatusService`, `GitLogService`, etc.)


## Getting Started

### Prerequisites

- **Java JDK 11+** (recommended 17+)
- JavaFX runtime (if your JDK distribution does not bundle it)
- A terminal / shell (PowerShell, CMD, bash, etc.)

There is no build tool configuration committed (no Maven/Gradle files), so compilation is done directly with `javac`.

---

## Building and Running (CLI)

### 1. Compile the sources

From the project root:

```bash
javac -d out $(find src/main/java -name "*.java")
```

On Windows PowerShell, an equivalent command would be:

```powershell
javac -d out (Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object FullName)
```

This compiles all Java sources into the `out` directory.

### 2. Run the CLI

The CLI entry point is `com.mygit.Main`:

```bash
java -cp out com.mygit.Main <command> [options]
```

Examples:

```bash
# Initialize a new repository
java -cp out com.mygit.Main init

# Configure author details
java -cp out com.mygit.Main config user.name "Your Name"
java -cp out com.mygit.Main config user.email "you@example.com"

# Stage and commit
java -cp out com.mygit.Main add src
java -cp out com.mygit.Main commit -m "Initial commit"

# Show status and log
java -cp out com.mygit.Main status
java -cp out com.mygit.Main log
```

---

## CLI Commands Reference

### Repository and configuration

- `init`  
  Initialize a new repository in the current working directory:

  ```bash
  java -cp out com.mygit.Main init
  ```

- `config user.name <value>`  
  Set user name:

  ```bash
  java -cp out com.mygit.Main config user.name "Your Name"
  ```

- `config user.email <value>`  
  Set user email:

  ```bash
  java -cp out com.mygit.Main config user.email "you@example.com"
  ```

### Staging and committing

- `add <path>`  
  Stage a file or directory:

  ```bash
  java -cp out com.mygit.Main add src/main/java
  ```

- `commit -m "message"`  

  ```bash
  java -cp out com.mygit.Main commit -m "Implement feature X"
  ```

### Status and history

- `status`  
  Show branch, staged, modified, and untracked files:

  ```bash
  java -cp out com.mygit.Main status
  ```

- `log`  
  Show commit history:

  ```bash
  java -cp out com.mygit.Main log
  ```

### Branching and switching

- `branch`  
  List branches:

  ```bash
  java -cp out com.mygit.Main branch
  ```

- `branch create <name>`  
  Create a new branch at `HEAD`:

  ```bash
  java -cp out com.mygit.Main branch create feature/login
  ```

- `branch -delete <name>`  
  Delete a fully-merged branch:

  ```bash
  java -cp out com.mygit.Main branch -delete feature/login
  ```

- `checkout <branch|commit>`  
  Check out a branch or commit:

  ```bash
  java -cp out com.mygit.Main checkout main
  java -cp out com.mygit.Main checkout a1b2c3d4
  ```

- `checkout -create <branch>`  
  Create and switch to a new branch:

  ```bash
  java -cp out com.mygit.Main checkout -create feature/api
  ```

- `switch <branch>`  
  Switch branches with safety checks for dirty trees:

  ```bash
  java -cp out com.mygit.Main switch main
  ```

### Merging and reverting

- `merge <branch|commit>`  
  Merge another branch or commit into the current branch:

  ```bash
  java -cp out com.mygit.Main merge feature/api
  ```

- `revert <commit>`  
  Create a new commit that reverts the given commit:

  ```bash
  java -cp out com.mygit.Main revert a1b2c3d4
  ```

### Working tree and index

- `diff`  
  Show diffs for modified files in the working tree:

  ```bash
  java -cp out com.mygit.Main diff
  ```

- `diff --staged`  
  Show diffs for staged changes:

  ```bash
  java -cp out com.mygit.Main diff --staged
  ```

- `reset --mixed`  
  Reset the index to `HEAD` but keep working tree:

  ```bash
  java -cp out com.mygit.Main reset --mixed
  ```

- `reset --hard`  
  Reset index and working tree to `HEAD`:

  ```bash
  java -cp out com.mygit.Main reset --hard
  ```

- `restore <file>`  
  Restore a file from `HEAD` to working tree:

  ```bash
  java -cp out com.mygit.Main restore src/main/java/com/mygit/Main.java
  ```

- `restore --staged <file|.>`  
  Unstage a file or all files:

  ```bash
  java -cp out com.mygit.Main restore --staged .
  ```

### Stash

- `stash`  
  Save dirty tracked changes and clean the working tree:

  ```bash
  java -cp out com.mygit.Main stash
  ```

- `stash apply`  
  Reapply the latest stash but keep it:

  ```bash
  java -cp out com.mygit.Main stash apply
  ```

- `stash pop`  
  Reapply the latest stash and drop it:

  ```bash
  java -cp out com.mygit.Main stash pop
  ```

### Maintenance

- `gc`  
  Run garbage collection over objects and pack files:

  ```bash
  java -cp out com.mygit.Main gc
  ```

---

## Running the JavaFX UI

The JavaFX UI entry point is `com.mygit.UI.MainApp`.

After compiling (as shown above), run:

```bash
java -cp out com.mygit.UI.MainApp
```

You may need to provide JavaFX modules on the module path depending on your JDK distribution, for example:

```bash
java --module-path /path/to/javafx/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp out com.mygit.UI.MainApp
```

### UI Overview

- **Repository menu**
  - Refresh repository state
  - Stash changes
  - Apply stash
- **Sidebar**
  - Home, Status, Log, Branches, Diff, Commit, Merge
- **Home**
  - Repository path, current branch, HEAD state, working tree state
  - Last commit SHA, message, author, and timestamp
  - When opened in a folder without `.mygit`, shows an empty state with "No repository created yet" and a "Create Repo" button, then leads into configuration after initialization
- **Status**
  - Lists staged, modified, and untracked files
- **Log**
  - Shows commit history using `GitLogService`
- **Branches**
  - List branches; create, switch, and delete with dialogs and error messages
- **Diff**
  - Select a file and view diff output (working tree or staged); text diffs only, binary files show a simple "Binary file, diff not supported" message

---

## Implementation Highlights

- Custom object store in `.mygit/objects` with Git-compatible blob/tree/commit encoding
- Minimal index format implementation in `com.mygit.index.Index` and `IndexEntry`
- Reference handling via `RefsUtil` (`HEAD`, `refs/heads/*`)
- Pack file reading support through `pack/*`
- Status and ignore handling via `StatusCommand` and `IgnoreMatcher`
- Shared configuration via `util.Config` so both CLI and JavaFX UI use the same `user.name` and `user.email` values for commits
- Diff command and UI handle binary files gracefully by skipping textual diff and showing a simple placeholder message instead
- JavaFX MVVM-style separation: controller, model, and service layers

The code is structured to be readable and educational rather than highly optimized, with an emphasis on mirroring Git’s behavior where reasonable.

---

## Limitations and Scope

This is intentionally a simplified implementation of Git. Some notable limitations:

- No remote operations (`clone`, `fetch`, `push`, `pull`)  
- No sophisticated conflict resolution UI  
- Limited merge strategies and conflict handling  
- Only a subset of Git commands and options are implemented  
- Stash management supports a single latest stash snapshot

Despite these limitations, the project is suitable for:

- Exploring Git internals
- Testing workflows on local repositories
- Demonstrating how a DVCS can be built from first principles

---

## Contributing

Contributions, suggestions, and pull requests are welcome.  
If you find a bug or have an idea for an improvement:

1. Open an issue describing the problem or feature.
2. If possible, include steps to reproduce or a small example.
3. Submit a pull request with focused, well-structured changes.

---

## License

Add your preferred license here (for example, MIT, Apache-2.0, or GPL-3.0).

