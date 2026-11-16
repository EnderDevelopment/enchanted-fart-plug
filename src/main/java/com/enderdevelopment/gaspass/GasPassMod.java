package com.enderdevelopment.gaspass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class GasPassMod implements ModInitializer {
    public static final String MOD_ID = "gas_pass";
    public static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> GAS_PASS_KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MOD_ID, "gas_pass"));
    private static final long COOLDOWN_TICKS = 200;
    private final Map<UUID, Long> lastFartTicks = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
            lastFartTicks.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        });
    }

    private void tickPlayer(ServerPlayer player) {
        var enchantment = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(GAS_PASS_KEY);
        boolean enchanted = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HAND) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0) {
                enchanted = true;
                break;
            }
        }
        if (!enchanted) return;
        long now = player.level().getGameTime();
        long previous = lastFartTicks.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - previous < COOLDOWN_TICKS) return;
        lastFartTicks.put(player.getUUID(), now);
        var level = (net.minecraft.server.level.ServerLevel) player.level();
        level.playSeededSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.9F, 0.45F, now);
        level.sendParticles(ParticleTypes.NOXIOUS_GAS,
                player.getX(), player.getY() + 0.15, player.getZ(), 8, 0.25, 0.12, 0.25, 0.01);
    }
}
