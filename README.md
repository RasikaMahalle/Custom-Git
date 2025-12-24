# MyGit – A Custom Git Implementation in Java

MyGit is a **from-scratch implementation of core Git internals** written in Java.  
The goal of this project is to deeply understand **how Git actually works internally**, not just how to use Git commands.

This project replicates Git’s core behavior including **object storage, staging area, commits, branching, merging, diffing, and safety checks**, along with a **JavaFX-based UI** for visualization.

---

## 🚀 Key Highlights

- Implements **Git internals** (blob, tree, commit objects) with SHA-1 content-addressable storage  
- Supports **core Git commands** with Git-faithful behavior  
- Includes **safety checks** to prevent overwriting modified or untracked files  
- Handles **branching, merging, and conflict resolution**  
- Provides both **CLI and GUI (JavaFX)** interfaces  
- Designed for **learning Git internals & interview-level depth**

---

## 🛠 Tech Stack

- **Language:** Java 21  
- **UI:** JavaFX  
- **Concepts:** File I/O, SHA-1 hashing, DAGs, Trees, Index/Staging Area  
- **Architecture:** CLI + GUI layered on top of a custom Git engine  

---

## 📂 Project Structure

```text
mygit/
├── .mygit/                     # Git metadata (objects, refs, index)
│   ├── objects/                # Blob, tree, commit objects (SHA-1 based)
│   ├── refs/
│   │   └── heads/              # Branch references
│   ├── index                   # Staging area
│   └── config                  # User configuration
│
├── src/
│   └── main/
│       └── java/
│           ├── com/mygit/command/   # CLI commands
│           ├── com/mygit/storage/   # Object storage
│           ├── com/mygit/index/     # Staging area
│           ├── com/mygit/util/      # Utilities
│           ├── com/mygit/pack/      # Packfiles & GC
│           ├── com/mygit/ui/        # JavaFX UI
│           ├── RefsUtil.java        # Ref & HEAD management
│           └── Main.java            # CLI entry point
│
└── README.md
```

## Git folder project strucutre (folder created using the init command in my project)
```text
.mygit/                     # Git metadata (objects, refs, index)
├── objects/                # Blob, tree, commit objects (SHA-1 based)
├── refs/
└── heads/              # Branch references
├── index                   # Staging area
└── config                  # User configuration

(Cannot add this on GitHub.)
```

## ✅ Supported Commands

### Repository & Configuration
- `mygit init`
- `mygit config user.name <name>`
- `mygit config user.email <email>`

### Staging & Commits
- `mygit add <file>`
- `mygit commit -m "message"`
- `mygit status`
- `mygit log`

### Branching & Navigation
- `mygit branch`
- `mygit branch create <name>`
- `mygit branch -delete <name>`
- `mygit checkout <branch|commit>`
- `mygit switch <branch>`

### Diff & Restore
- `mygit diff`
- `mygit diff --staged`
- `mygit restore <file>`

### Merge & History
- `mygit merge <branch|commit>`
- `mygit revert <commit>`
- `mygit reset --mixed`
- `mygit reset --hard`

### Maintenance
- `mygit gc`

---

## 🧠 Internal Design (How It Works)

### Object Model (Git-like)
- **Blob** → File contents  
- **Tree** → Directory structure (mode, name, SHA)  
- **Commit** → Tree + parent commit(s) + author + message  

All objects are:
- Stored using **SHA-1 hashing**
- Written with proper headers (`blob <size>`, `tree <size>`, `commit <size>`)
- Compressed using zlib (like real Git)

---

### Index (Staging Area)
- Tracks file metadata, SHA, permissions, and paths
- Enables accurate comparison between:
  - Working Tree
  - Index
  - HEAD commit

---

### Branching & Merging
- Branches are lightweight references under `.mygit/refs/heads`
- Merge uses:
  - Lowest Common Ancestor (LCA)
  - 3-way merge logic
  - Conflict markers for textual conflicts
- Safe branch deletion via **reachability analysis**

---

### Safety Guarantees
- Prevents checkout/switch if:
  - Modified tracked files would be overwritten
  - Untracked files would be overwritten
- Matches real Git behavior closely

---

## 🖥 JavaFX GUI

The GUI helps visualize Git internals:
- Repository overview (branch, HEAD, dirty state)
- Commit log
- File diffs (working vs staged)
- Branch listing
- Merge & conflict visibility

This makes Git internals **easy to understand visually**, especially for learning and demos.

---

## ▶️ How to Compile & Run

### Compile (CLI + UI) for windows.
```bash
javac `
--module-path D:\Libraries\javafx-sdk-21.0.9\lib `
--add-modules javafx.controls,javafx.fxml `
-d out `
(Get-ChildItem -Recurse src/main/java/*.java).FullName
```
### Run CLI
```bash
java -cp out com.mygit.Main <command>
```

### Run the JavaFX UI
```bash
java --module-path /path/to/javafx/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp out com.mygit.ui.MainApp
```

## 🎯 Why This Project?

Built to master Git internals, not just Git usage
Demonstrates strong understanding of Core Java:
  Data structures
  File systems
  Multi-threading
  Stream API
  Collections

## 📜 License
This project is for educational and learning purposes.
