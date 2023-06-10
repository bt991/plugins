package net.runelite.client.plugins.oneclicktithe;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("oneclicktithe")
public interface OneClickTitheConfig extends Config
{
    @ConfigItem(
            position = 0,
            keyName = "debug",
            name = "Debug",
            description = "Enable this for bug reports if getting stuck. prints on click."
    )
    default boolean debug(){return false;}

    @ConfigItem(
            position = 1,
            keyName = "Seed type",
            name = "Seed type",
            description = "Select type of seeds to use"
    )
    default Types.Seeds seeds() { return Types.Seeds.GOLOVANOVA; }


    @ConfigItem(
            position = 2,
            keyName = "Patch area",
            name = "Patch area",
            description = "Area of patches to use"
    )
    default Types.Sides sides() { return Types.Sides.EAST; }

    @Range(
           min = 8,
           max = 23
    )
    @ConfigItem(
            position = 3,
            keyName = "amountOfCans",
            name = "Amount of cans",
            description = "Input the amount of cans you're entering with, must be at least 8."
    )
    default int amountOfCans() { return 8; }
}