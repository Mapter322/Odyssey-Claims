package earth.terrarium.cadmus.common.commands.claims;

public enum ClaimCommandType {
    UNCLAIM("cadmus unclaim"),
    UNCLAIM_AREA("cadmus unclaim area"),
    TOWN_CREATE("cadmus town create"),
    TOWN_ADD("cadmus town add"),
    ;

    private final String command;

    ClaimCommandType(String command) {
        this.command = command;
    }

    public String command() {
        return command;
    }
}
