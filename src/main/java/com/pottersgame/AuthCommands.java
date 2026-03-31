package com.pottersgame;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public class AuthCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            
            dispatcher.register(Commands.literal("register")
                .then(Commands.argument("password", StringArgumentType.word())
                .then(Commands.argument("confirmPassword", StringArgumentType.word())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String pwd = StringArgumentType.getString(context, "password");
                    String confirm = StringArgumentType.getString(context, "confirmPassword");

                    if (Combatpersistence.authManager.isAuthenticated(player)) {
                        player.sendSystemMessage(Component.literal(Combatpersistence.config.alreadyLoggedIn));
                        return 0;
                    }
                    if (Combatpersistence.authManager.isRegistered(player.getUUID())) {
                        player.sendSystemMessage(Component.literal(Combatpersistence.config.alreadyRegistered));
                        return 0;
                    }
                    if (!pwd.equals(confirm)) {
                        player.sendSystemMessage(Component.literal(Combatpersistence.config.passwordMismatch));
                        return 0;
                    }

                    CompletableFuture.supplyAsync(() -> Combatpersistence.authManager.hashPassword(pwd), Combatpersistence.IO_EXECUTOR)
                        .thenAccept(hash -> {
                            context.getSource().getServer().execute(() -> {
                                // Verify player is still connected and valid after async hashing
                                if (player.connection != null && !player.isRemoved()) {
                                    Combatpersistence.authManager.applyRegistration(player, hash);
                                    Combatpersistence.authManager.onAuthenticated(player);
                                    player.sendSystemMessage(Component.literal(Combatpersistence.config.registerSuccess));
                                    SkinManager.applySkin(player, player.getName().getString());
                                }
                            });
                        });
                    
                    return 1;
                }))));

            dispatcher.register(Commands.literal("login")
                .then(Commands.argument("password", StringArgumentType.word())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String pwd = StringArgumentType.getString(context, "password");

                    if (Combatpersistence.authManager.isAuthenticated(player)) {
                        player.sendSystemMessage(Component.literal(Combatpersistence.config.alreadyLoggedIn));
                        return 0;
                    }

                    CompletableFuture.supplyAsync(() -> Combatpersistence.authManager.checkPassword(player, pwd), Combatpersistence.IO_EXECUTOR)
                        .thenAccept(success -> {
                            context.getSource().getServer().execute(() -> {
                                if (success) {
                                    // Verify player is still connected and valid after async hashing
                                    if (player.connection != null && !player.isRemoved()) {
                                        Combatpersistence.authManager.finishLogin(player);
                                        Combatpersistence.authManager.onAuthenticated(player);
                                        player.sendSystemMessage(Component.literal(Combatpersistence.config.loginSuccess));
                                        String skin = Combatpersistence.authManager.getCustomSkin(player.getUUID());
                                        SkinManager.applySkin(player, skin != null ? skin : player.getName().getString());
                                    }
                                } else {
                                    if (player.connection != null && !player.isRemoved()) {
                                        player.sendSystemMessage(Component.literal(Combatpersistence.config.incorrectPassword));
                                    }
                                }
                            });
                        });
                    
                    return 1;
                })));

            dispatcher.register(Commands.literal("skin")
                .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!Combatpersistence.authManager.isAuthenticated(player)) {
                        player.sendSystemMessage(Component.literal(Combatpersistence.config.authRequiredForSkin));
                        return 0;
                    }
                    String skinName = StringArgumentType.getString(context, "name");
                    Combatpersistence.authManager.setCustomSkin(player.getUUID(), skinName);
                    SkinManager.applySkin(player, skinName);
                    player.sendSystemMessage(Component.literal(String.format(Combatpersistence.config.skinAppliedMessage, skinName)));
                    return 1;
                })));

            dispatcher.register(Commands.literal("premium")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!Combatpersistence.authManager.isAuthenticated(player)) {
                        player.sendSystemMessage(Component.literal("§cYou must log in first!"));
                        return 0;
                    }

                    player.sendSystemMessage(Component.literal("§6Verifying premium status..."));
                    
                    SkinManager.fetchSkin(player.getName().getString()).thenAccept(prop -> {
                        context.getSource().getServer().execute(() -> {
                            if (prop != null) {
                                Combatpersistence.authManager.setPremium(player.getUUID(), true);
                                player.sendSystemMessage(Component.literal("§a§lSUCCESS! §fPremium status verified. Your autologin session is now 30 days."));
                            } else {
                                player.sendSystemMessage(Component.literal("§cVerification failed! Could not find a premium account with your name."));
                            }
                        });
                    });
                    
                    return 1;
                }));
        });
    }
}
