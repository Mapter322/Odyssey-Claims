package earth.terrarium.cadmus.mixins.common.flags;

import com.mojang.authlib.GameProfile;
import earth.terrarium.cadmus.common.flags.Flags;
import earth.terrarium.cadmus.common.settings.SettingDefinitions;
import earth.terrarium.cadmus.common.settings.Settings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    @Shadow
    public abstract boolean hurt(DamageSource source, float amount);

    public ServerPlayerMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        if (this.level().getGameTime() % 20 == 0) {
            float healRate = Settings.getAt(this.level(), this.chunkPosition(), SettingDefinitions.HEAL_RATE);
            if (healRate > 0) {
                this.heal(healRate);
            } else if (healRate < 0) {
                this.hurt(this.damageSources().generic(), -healRate);
            }

            float feedRate = Settings.getAt(this.level(), this.chunkPosition(), SettingDefinitions.FEED_RATE);
            if (feedRate > 0) {
                if (feedRate > this.random.nextFloat()) {
                    this.getFoodData().eat((int) Math.ceil(feedRate), feedRate);
                }
            }
        }
    }

    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void cadmus$teleportTo(double x, double y, double z, CallbackInfo ci) {
        if (!Settings.isEnabledAt(this.level(), new ChunkPos(BlockPos.containing(x, y, z)), SettingDefinitions.ALLOW_ENTRY)) {
            String message = Flags.ENTRY_DENY_MESSAGE.get(level(), chunkPosition());
            if (!message.isBlank()) {
                displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
            }
            ci.cancel();
        }

        if (!Settings.isEnabledAt(this.level(), chunkPosition(), SettingDefinitions.ALLOW_EXIT)) {
            ci.cancel();
        }
    }

    @Inject(method = "restoreFrom", at = @At(value = "HEAD", target = "Lnet/minecraft/server/level/ServerPlayer;onUpdateAbilities()V"))
    private void cadmus$restoreFrom(ServerPlayer that, boolean keepEverything, CallbackInfo ci) {
        if (!keepEverything && Settings.isEnabledAt(that.serverLevel(), that.chunkPosition(), SettingDefinitions.KEEP_INVENTORY)) {
            this.getInventory().replaceWith(that.getInventory());
            this.experienceLevel = that.experienceLevel;
            this.totalExperience = that.totalExperience;
            this.experienceProgress = that.experienceProgress;
            this.setScore(that.getScore());
        }
    }
}
