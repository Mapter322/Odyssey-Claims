package earth.terrarium.cadmus.common.commands.claims;

public enum ClaimCommandType {
    CLAIM("cadmus claim"),
    CLAIM_AREA("cadmus claim area"),
    UNCLAIM("cadmus unclaim"),
    UNCLAIM_AREA("cadmus unclaim area"),
    UNCLAIM_ALL("cadmus unclaim all"),
    ;

    private final String command;

    ClaimCommandType(String command) {
        this.command = command;
    }

    public String command() {
        return command;
    }
}
