package com.adam;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AuthCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            
            // /register <password> <confirmPassword>
            dispatcher.register(Commands.literal("register")
                .then(Commands.argument("password", StringArgumentType.word())
                .then(Commands.argument("confirmPassword", StringArgumentType.word())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String pwd = StringArgumentType.getString(context, "password");
                    String confirm = StringArgumentType.getString(context, "confirmPassword");

                    if (Combatpersistence.authManager.isAuthenticated(player)) {
                        player.sendSystemMessage(Component.literal("§cYou are already logged in!"));
                        return 0;
                    }
                    if (Combatpersistence.authManager.isRegistered(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("§cYou are already registered! Use /login <password>"));
                        return 0;
                    }
                    if (!pwd.equals(confirm)) {
                        player.sendSystemMessage(Component.literal("§cPasswords do not match!"));
                        return 0;
                    }

                    Combatpersistence.authManager.register(player, pwd);
                    player.sendSystemMessage(Component.literal("§aSuccessfully registered and logged in!"));
                    
                    // Apply default skin
                    SkinManager.applySkin(player, player.getName().getString());
                    
                    return 1;
                }))));

            // /login <password>
            dispatcher.register(Commands.literal("login")
                .then(Commands.argument("password", StringArgumentType.word())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String pwd = StringArgumentType.getString(context, "password");

                    if (Combatpersistence.authManager.isAuthenticated(player)) {
                        player.sendSystemMessage(Component.literal("§cYou are already logged in!"));
                        return 0;
                    }
                    if (!Combatpersistence.authManager.isRegistered(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("§cYou are not registered! Use /register <password> <password>"));
                        return 0;
                    }

                    if (Combatpersistence.authManager.login(player, pwd)) {
                        player.sendSystemMessage(Component.literal("§aSuccessfully logged in!"));
                        
                        // Apply their skin
                        String skin = Combatpersistence.authManager.getCustomSkin(player.getUUID());
                        SkinManager.applySkin(player, skin != null ? skin : player.getName().getString());
                    } else {
                        player.sendSystemMessage(Component.literal("§cIncorrect password!"));
                    }
                    return 1;
                })));

            // /skin <name>
            dispatcher.register(Commands.literal("skin")
                .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!Combatpersistence.authManager.isAuthenticated(player)) {
                        player.sendSystemMessage(Component.literal("§cYou must be logged in to change your skin!"));
                        return 0;
                    }
                    String skinName = StringArgumentType.getString(context, "name");
                    Combatpersistence.authManager.setCustomSkin(player.getUUID(), skinName);
                    SkinManager.applySkin(player, skinName);
                    player.sendSystemMessage(Component.literal("§aApplied skin: " + skinName));
                    return 1;
                })));
        });
    }
}