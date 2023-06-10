package net.runelite.client.plugins.oneclickredwoods;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(value="oneclickredwoods")
public interface OneClickRedwoodsConfig
        extends Config {
    @ConfigItem(position=0, keyName="debug", name="Debug", description="Enable this for bug reports if getting stuck. prints on click.")
    default public boolean debug() {
        return true;
    }

    @ConfigItem(position=1, keyName="useSpec", name="Use Special Attack", description="Uses special attack if its 100% before chopping.")
    default public boolean useSpec() {
        return true;
    }

    @ConfigItem(position=2, keyName="useBank", name="Use Deposit Box", description="Uses deposit box to bank the logs. Dropping doesn't work rn.")
    default public boolean useBank() {
        return true;
    }

    @ConfigItem(position=3, keyName="collectNests", name="Collect Nests", description="Collects all bird nests.")
    default public boolean collectNests() {
        return false;
    }
}
