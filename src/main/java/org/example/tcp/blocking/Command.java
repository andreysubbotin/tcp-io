package org.example.tcp.blocking;

public class Command {
    private final String raw;
    private final String commandName;
    private final String args;

    public Command(String raw, String commandName, String args) {
        this.raw = raw;
        this.commandName = commandName;
        this.args = args;
    }

    public String getRaw() {
        return raw;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getArgs() {
        return args;
    }
}
