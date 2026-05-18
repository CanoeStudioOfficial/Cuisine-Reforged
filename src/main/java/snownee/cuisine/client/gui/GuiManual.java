package snownee.cuisine.client.gui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import snownee.cuisine.Cuisine;
import snownee.cuisine.util.I18nUtil;
import snownee.kiwi.client.AdvancedFontRenderer;
import snownee.kiwi.client.FontUtil;
import snownee.kiwi.client.gui.element.DrawableResource;

public class GuiManual extends GuiScreen
{
    private static final ResourceLocation BOOK_GUI_TEXTURES = new ResourceLocation(Cuisine.MODID, "textures/gui/patchouli.png");

    private static final int PAGE_HEIGHT = 180;
    private static final int PAGE_WIDTH = 272;
    private static final int PAGE_MARGIN = 20;

    private DrawableResource pageGrid;

    private final String chatRoomURL;
    private final String mcmodWikiURL;
    private final String text = I18nUtil.translateWithEscape("gui.welcome");

    private float scale = 1.0f;
    private int scaledWidth;
    private int scaledHeight;

    public GuiManual()
    {
        pageGrid = new DrawableResource(BOOK_GUI_TEXTURES, 0, 0, PAGE_WIDTH, PAGE_HEIGHT, 0, 0, 0, 0, 512, 256);
        if (Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode().startsWith("zh"))
        {
            chatRoomURL = "https://jq.qq.com/?_wv=1027&k=5GXZnpl";
            mcmodWikiURL = "https://www.mcmod.cn/class/1291.html";
        }
        else
        {
            chatRoomURL = "https://discord.gg/KzGQW7a";
            mcmodWikiURL = "";
        }
    }

    private void calculateScale()
    {
        float scaleX = (float) (this.width - 20) / PAGE_WIDTH;
        float scaleY = (float) (this.height - 20) / PAGE_HEIGHT;
        scale = Math.min(1.0f, Math.min(scaleX, scaleY));
        scaledWidth = (int) (PAGE_WIDTH * scale);
        scaledHeight = (int) (PAGE_HEIGHT * scale);
    }

    @Override
    public void initGui()
    {
        this.fontRenderer = AdvancedFontRenderer.INSTANCE;
        calculateScale();
        this.buttonList.clear();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int btnX = centerX - 40;
        int btnY = centerY + (scaledHeight / 2) - 70;

        this.addButton(new GuiButton(0, btnX, btnY, 80, 20, I18nUtil.translate("gui.openLink")));
        this.addButton(new GuiButton(1, btnX, btnY + 25, 80, 20, I18nUtil.translate("gui.close")));

        if (mc.getLanguageManager().getCurrentLanguage().getLanguageCode().startsWith("zh"))
        {
            this.addButton(new GuiButton(2, btnX, btnY - 25, 80, 20, I18nUtil.translate("gui.openWiki")));
        }
    }

    @Override
    public void onGuiClosed()
    {
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        calculateScale();

        GlStateManager.pushMatrix();
        int i = (this.width - scaledWidth) / 2;
        int j = (this.height - scaledHeight) / 2;
        GlStateManager.translate(i, j, 0);
        GlStateManager.scale(scale, scale, 1.0f);

        mc.getTextureManager().bindTexture(BOOK_GUI_TEXTURES);
        pageGrid.draw(mc, 0, 0);

        int originX = PAGE_MARGIN;
        int originY = PAGE_MARGIN - 5;

        List<String> strs = FontUtil.drawSplitStringOverflow(fontRenderer, text, originX, originY, getClientX(), getClientY(), 0, false);
        if (!strs.isEmpty())
        {
            originX += PAGE_WIDTH / 2;
            FontUtil.drawSplitStringOverflow(fontRenderer, strs, originX, originY, getClientX(), getClientY(), 0, false);
        }

        GlStateManager.popMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int getClientX()
    {
        return PAGE_WIDTH / 2 - 2 * PAGE_MARGIN + 5;
    }

    private int getClientY()
    {
        return PAGE_HEIGHT - 2 * PAGE_MARGIN;
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.enabled)
        {
            if (button.id == 0)
            {
                GuiConfirmOpenLink guiConfirm = new GuiConfirmOpenLink(this, chatRoomURL, 3, true);
                guiConfirm.disableSecurityWarning();
                mc.displayGuiScreen(guiConfirm);
            }
            else if (button.id == 1)
            {
                mc.displayGuiScreen(null);
            }
            else if (button.id == 2)
            {
                GuiConfirmOpenLink guiConfirm = new GuiConfirmOpenLink(this, mcmodWikiURL, 4, true);
                guiConfirm.disableSecurityWarning();
                mc.displayGuiScreen(guiConfirm);
            }
        }
    }

    @Override
    public void confirmClicked(boolean result, int id)
    {
        if (result && (id == 3 || id == 4))
        {
            try
            {
                java.awt.Desktop.getDesktop().browse(new URI(id == 3 ? chatRoomURL : mcmodWikiURL));
            }
            catch (URISyntaxException wrongURI)
            {
                Cuisine.logger.error("The chat room link '{}' seems to be malformed", chatRoomURL);
                Cuisine.logger.debug("Exception caught: {}", wrongURI);
            }
            catch (Exception e)
            {
                Cuisine.logger.error("Couldn't open link", e);
            }
        }
        mc.displayGuiScreen(null);
    }
}
