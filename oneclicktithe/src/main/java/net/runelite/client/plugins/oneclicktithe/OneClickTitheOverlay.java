package net.runelite.client.plugins.oneclicktithe;

import com.google.common.base.Strings;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ColorUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;

public class OneClickTitheOverlay extends Overlay {

    private final Client client;

    private final OneClickTithePlugin plugin;

    private final ModelOutlineRenderer modelOutlineRenderer;
    @Inject
    private OneClickTitheOverlay(Client client, OneClickTithePlugin plugin,  ModelOutlineRenderer modelOutlineRenderer){
        this.client = client;
        this.plugin = plugin;
        this.modelOutlineRenderer = modelOutlineRenderer;
    }

    @Override
    public Dimension render(Graphics2D graphics) {

        Stroke stroke = new BasicStroke((float) 2);
        Color color = Color.CYAN;
        int i = 1;
        if(plugin.EAST_PATCHES.size() > 0) {
            for (GameObject obj : plugin.EAST_PATCHES.values()) {
            LocalPoint lp = obj.getLocalLocation();
            if(lp != null){
                drawTile(graphics, lp, color, String.valueOf(i), stroke);
            }
            i++;
        }
        }
        if(plugin.WEST_PATCHES.size() > 0){
            color = Color.GREEN;
           // for(GameObject obj : plugin.WEST_PATCHES){
             //   Shape clickbox = obj.getClickbox();
            //    if (clickbox != null)
            //    {
               //     Color clickBoxColor = ColorUtil.colorWithAlpha(color, color.getAlpha() / 12);
              //      OverlayUtil.renderPolygon(graphics, clickbox, color, clickBoxColor, stroke);
             //   }
        //    }
        }
        return null;
    }

    private void drawTile(Graphics2D graphics, LocalPoint lp, Color color, @Nullable String label, Stroke borderStroke)
    {


        if (lp == null)
        {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly != null)
        {
            OverlayUtil.renderPolygon(graphics, poly, color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 100), borderStroke);
        } else {
        }

        if (!Strings.isNullOrEmpty(label))
        {
            Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, lp, label, 0);
            if (canvasTextLocation != null)
            {
                graphics.setFont(new Font("Arial", 1, 15));
                OverlayUtil.renderTextLocation(graphics, canvasTextLocation, label, color);
            }
        }
    }
}
