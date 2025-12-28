package com.mygit;

import com.mygit.command.*;
import com.mygit.util.Config;

public class Main {

    public static void main(String[] args) {

        if (args.length == 0) {
            printHelp();
            return;
        }

        String cmd = args[0];

        try {
            switch (cmd) {

                case "init":
                    new InitCommand().run();
                    break;

                case "add":
                    handleAdd(args);
                    break;

                case "commit":
                    handleCommit(args);
                    break;

                case "status":
                    new StatusCommand().run();
                    break;

                case "log":
                    new LogCommand().run();
                    break;

                case "branch":
                    handleBranch(args);
                    break;

                case "checkout":
                    handleCheckout(args);
                    break;

                case "switch":
                    handleSwitch(args);
                    break;

                case "merge":
                    handleMerge(args);
                    break;

                case "diff":
                    handleDiff(args);
                    break;

                case "reset":
                    handleReset(args);
                    break;

                case "restore":
                    handleRestore(args);
                    break;

                case "revert":
                    handleRevert(args);
                    break;

                case "config":
                    handleConfig(args);
                    break;

                case "gc":
                    new GCCommand().run();
                    break;

                case "stash":
                    handleStash(args);
                    break;

                default:
                    System.out.println("Unknown command: " + cmd);
                    printHelp();
            }

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = e.toString();
            }
            System.err.println("Error: " + msg);
        }
    }

    // =========================================================
    // Handlers
    // =========================================================

    private static void handleAdd(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: mygit add <file>");
            return;
        }
        new AddCommand().run(args[1]);
    }

    private static void handleCommit(String[] args) {
        if (args.length < 3 || !"-m".equals(args[1])) {
            System.out.println("Usage: mygit commit -m \"message\"");
            return;
        }
        String author = Config.userName() + " <" + Config.userEmail() + ">";
        new CommitCommand().run(author, args[2]);
    }

    private static void handleBranch(String[] args) {
        BranchCommand cmd = new BranchCommand();

        if (args.length == 1) {
            cmd.run(); // list branches
        } else if (args.length == 3 && "create".equals(args[1])) {
            cmd.run("create", args[2]);
        } else if (args.length == 3 && "-delete".equals(args[1])) {
            cmd.deleteBranch(args[2]);
        } else {
            System.out.println("Usage: mygit branch [create <name> | -delete <name>]");
        }
    }

    private static void handleCheckout(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: mygit checkout <branch|commit>");
            return;
        }
        if ("-create".equals(args[1])) {
            if (args.length < 3) {
                System.out.println("Usage: mygit checkout -create <branch>");
                return;
            }
            new CheckOutCommand().run(new String[] { "-create", args[2] });
        } else {
            new CheckOutCommand().run(args[1]);
        }
    }

    private static void handleSwitch(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: mygit switch <branch>");
            return;
        }
        new SwitchCommand().run(args[1]);
    }

    private static void handleMerge(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: mygit merge <branch|commit>");
            return;
        }
        String author = Config.userName() + " <" + Config.userEmail() + ">";
        new MergeCommand().run(args[1], author, null);
    }

    private static void handleDiff(String[] args) {
        DiffCommand diff = new DiffCommand();
        if (args.length == 2 && "--staged".equals(args[1])) {
            diff.runStaged();
        } else {
            diff.run();
        }
    }

    private static void handleReset(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: mygit reset --mixed | --hard");
            return;
        }
        new ResetCommand().run(new String[] { args[1] });
    }

    private static void handleRestore(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: mygit restore <file>");
            return;
        }
        new RestoreCommand().run(args[1]);
    }

    private static void handleRevert(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: mygit revert <commit>");
            return;
        }
        new RevertCommand().run(args[1]);
    }

    private static void handleConfig(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: mygit config user.name|user.email <value>");
            return;
        }
        new ConfigCommand().run(new String[] { args[1], args[2] });
    }

    private static void handleStash(String[] args) {
        new StashCommand().run(args);
    }

    // =========================================================
    // Help
    // =========================================================

    private static void printHelp() {
        System.out.println("mygit - a custom Git implementation");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  mygit init");
        System.out.println("  mygit add <file>");
        System.out.println("  mygit commit -m \"message\"");
        System.out.println("  mygit status");
        System.out.println("  mygit log");
        System.out.println("  mygit branch [create <name> | -delete <name>]");
        System.out.println("  mygit checkout <branch|commit>");
        System.out.println("  mygit switch <branch>");
        System.out.println("  mygit merge <branch|commit>");
        System.out.println("  mygit diff [--staged]");
        System.out.println("  mygit reset --mixed|--hard");
        System.out.println("  mygit restore <file>");
        System.out.println("  mygit revert <commit>");
        System.out.println("  mygit config user.name|user.email <value>");
        System.out.println("  mygit stash [apply|pop]");
        System.out.println("  mygit gc");
    }
}
