package com.passivemobs;

import com.passivemobs.asm.NoPlayerTargetTransformer;
import com.passivemobs.event.PassiveMobsEventHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.*;
import net.minecraft.command.*;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

import java.util.List;

@Mod(modid = PassiveMobsMod.MODID, name = PassiveMobsMod.NAME, version = PassiveMobsMod.VERSION,
     acceptedMinecraftVersions = "[1.7.10]", acceptableRemoteVersions = "*")
public class PassiveMobsMod {

    public static final String MODID   = "passivemobs";
    public static final String NAME    = "Passive Mobs";
    public static final String VERSION = "1.4.0";

    public static boolean enabled = true;
    public static boolean allowBosses = true;
    public static String[] excludedEntities = new String[0];
    private static java.io.File configFile;

    private static void loadConfig() {
        Configuration cfg = new Configuration(configFile);
        cfg.load();
        enabled = cfg.getBoolean("enabled", "general", true, "Master switch. Set to false to disable the mod.");
        allowBosses = cfg.getBoolean("allowBosses", "general", true,
            "Leave IBossDisplayData and Galacticraft IBoss bosses untouched, including targeting and damage, to preserve encounters.");
        excludedEntities = cfg.getStringList("excludedEntities", "general", new String[0],
            "Fully qualified entity class or interface names to leave untouched (includes subclasses/implementations). Use for custom bosses or encounter triggers.");
        if (cfg.hasChanged()) cfg.save();
        NoPlayerTargetTransformer.active = enabled;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
        loadConfig();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        PassiveMobsEventHandler handler = new PassiveMobsEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBase() {
            @Override public String getCommandName() { return "passivemobs"; }
            @Override public int getRequiredPermissionLevel() { return 2; }
            @Override public String getCommandUsage(ICommandSender s) { return "/passivemobs [on|off|reload]"; }

            @Override
            public void processCommand(ICommandSender s, String[] args) {
                if (args.length == 0) {
                    s.addChatMessage(new ChatComponentText("§aPassiveMobs: §f" + (enabled ? "§aON" : "§cOFF")));
                    return;
                }
                switch (args[0]) {
                    case "on":  enabled = true;  NoPlayerTargetTransformer.active = true;  chat(s, "§aPassiveMobs enabled."); break;
                    case "off": enabled = false; NoPlayerTargetTransformer.active = false; chat(s, "§cPassiveMobs disabled! Mobs will attack!"); break;
                    case "reload":
                        loadConfig();
                        chat(s, "§aConfig reloaded. Enabled: " + enabled);
                        break;
                }
            }

            @SuppressWarnings("rawtypes")
            @Override
            public List addTabCompletionOptions(ICommandSender s, String[] args) {
                return args.length == 1 ? getListOfStringsMatchingLastWord(args, "on", "off", "reload") : null;
            }

            private void chat(ICommandSender s, String msg) {
                s.addChatMessage(new ChatComponentText(msg));
            }
        });
    }
}
