package io.github.avacadowizard120.omni.legacyfabric;

import io.github.avacadowizard120.omni.OmniConfig;
import java.util.Locale;

public final class LegacyOmniCommand {
    private LegacyOmniCommand() {
    }

    public static String handle(String rawCommand) {
        if (rawCommand == null) {
            return null;
        }

        String command = rawCommand.trim();
        if (!command.startsWith("/")) {
            return null;
        }
        command = command.substring(1).trim();
        if (!command.equals("omni") && !command.startsWith("omni ")) {
            return null;
        }

        String[] parts = command.split("\\s+");
        String action = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "status";

        if ("enable".equals(action) || "enabled".equals(action) || "on".equals(action) || "true".equals(action)) {
            OmniConfig.setEnabled(true);
            return "Enabled.";
        }
        if ("disable".equals(action) || "disabled".equals(action) || "off".equals(action) || "false".equals(action)) {
            OmniConfig.setEnabled(false);
            return "Disabled.";
        }
        if ("reload".equals(action)) {
            OmniConfig.reload();
            return "Config reloaded. Status: " + OmniConfig.statusText() + ".";
        }
        if ("status".equals(action)) {
            return "Status: " + OmniConfig.statusText() + ".";
        }

        return "Usage: /omni [enable|disable|status|reload]";
    }
}
