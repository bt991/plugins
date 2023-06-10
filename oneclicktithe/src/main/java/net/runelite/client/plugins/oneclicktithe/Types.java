package net.runelite.client.plugins.oneclicktithe;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Item;
import net.runelite.api.ItemID;

@Getter(AccessLevel.PUBLIC)
@RequiredArgsConstructor
public class Types {
    public enum Seeds{
        GOLOVANOVA(1, net.runelite.api.ItemID.GOLOVANOVA_SEED),
        BOLOGANO(2, net.runelite.api.ItemID.BOLOGANO_SEED),
        LOGAVANO(3, net.runelite.api.ItemID.LOGAVANO_SEED);

        public final int ID;
        public final int ItemID;

        Seeds(int ID,int ItemID){
            this.ID = ID;
            this.ItemID = ItemID;
        }
    }

    public enum Sides{
        EAST,
        WEST
    }
}

