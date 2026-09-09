package earth.terrarium.cadmus.client.compat.xaero;

import earth.terrarium.cadmus.api.claims.ClaimApi;
import earth.terrarium.cadmus.client.CadmusClient;
import earth.terrarium.cadmus.client.CadmusModals;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommandType;
import earth.terrarium.cadmus.common.constants.ConstantComponents;
import earth.terrarium.cadmus.common.towns.TownManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ChunkPos;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.List;

public class CadmusRightClickOptions {

    public static void addRightClickOptions(GuiMap screen, List<RightClickOption> options, MapTileSelection selection) {
        if (selection == null) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        int left = selection.getLeft();
        int top = selection.getTop();
        int right = selection.getRight();
        int bottom = selection.getBottom();

        ChunkPos startPos = new ChunkPos(left, top);
        ChunkPos endPos = new ChunkPos(right, bottom);

        if (startPos.equals(endPos)) {
            if (ClaimApi.API.isClaimed(player.level(), startPos)) {
                addUnclaimOptions(screen, options, startPos);
            } else {
                addClaimOptions(screen, options, startPos);
            }
        } else {
            addUnclaimAreaOptions(screen, options, startPos, endPos);
            addClaimAreaOptions(screen, options, startPos, endPos);
        }
    }

    private static void addUnclaimOptions(GuiMap screen, List<RightClickOption> options, ChunkPos pos) {
        options.add(new BetterRightClickOption("gui.xaero_pac_unclaim_chunks", options.size(), screen, () ->
            CadmusClient.sendTeamlessClaimCommand(ClaimCommandType.UNCLAIM, "%s %s".formatted(pos.getMaxBlockX(), pos.getMaxBlockZ()))));
    }

    private static void addClaimOptions(GuiMap screen, List<RightClickOption> options, ChunkPos pos) {
        options.add(new BetterRightClickOption("gui.cadmus.claim_map.create_town", options.size(), screen, () ->
            openCreateTownModal(pos, pos)));
    }

    private static void addUnclaimAreaOptions(GuiMap screen, List<RightClickOption> options, ChunkPos startPos, ChunkPos endPos) {
        options.add(new BetterRightClickOption("gui.xaero_pac_unclaim_chunks", options.size(), screen, () ->
            CadmusClient.sendTeamlessClaimCommand(ClaimCommandType.UNCLAIM_AREA, "%s %s %s %s".formatted(startPos.getMaxBlockX(), startPos.getMaxBlockZ(), endPos.getMaxBlockX(), endPos.getMaxBlockZ()))));
    }

    private static void addClaimAreaOptions(GuiMap screen, List<RightClickOption> options, ChunkPos startPos, ChunkPos endPos) {
        options.add(new BetterRightClickOption("gui.cadmus.claim_map.create_town", options.size(), screen, () ->
            openCreateTownModal(startPos, endPos)));
    }

    private static void openCreateTownModal(ChunkPos startPos, ChunkPos endPos) {
        CadmusModals.input(
            ConstantComponents.CREATE_TOWN_MODAL_TITLE,
            ConstantComponents.CREATE_TOWN_MODAL_DESCRIPTION,
            ConstantComponents.CREATE_TOWN_MODAL_PLACEHOLDER,
            TownManager.MAX_TOWN_NAME_LENGTH,
            ConstantComponents.CREATE_TOWN_MODAL_CONFIRM,
            TownManager::isValidTownName,
            name -> CadmusClient.sendTownCreate(name, startPos, endPos)
        );
    }

    private static class BetterRightClickOption extends RightClickOption {

        private final Runnable action;

        public BetterRightClickOption(String key, int index, GuiMap screen, Runnable action) {
            super(key, index, screen);
            this.action = action;
        }

        @Override
        public void onAction(Screen screen) {
            action.run();
        }
    }
}
