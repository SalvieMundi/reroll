package draylar.reroll.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.reroll.Reroll;
import draylar.reroll.RerollClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends HandledScreen<EnchantmentScreenHandler> {

    @Unique private static final Identifier REROLL_TEXTURE = new Identifier("reroll", "textures/reroll_button.png");
    @Unique private static final Identifier REROLL_TEXTURE_IN = new Identifier("reroll", "textures/reroll_button_in.png");

    // TODO: these might change while the player is in the enchanting UI.
    @Unique private int playerLapis = 0;
    @Unique private int playerLevels = 0;

    private EnchantmentScreenMixin(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void onInit(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
        int lapis = 0;

        for(int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if(stack.getItem().equals(Items.LAPIS_LAZULI)) {
                lapis+=stack.getCount();
            }
        }

        playerLevels = inventory.player.experienceLevel;
        playerLapis = lapis;

        // Prevent delay on button first time due to packets
        RerollClient.getLapisPerReroll();
        RerollClient.getExpPerReroll();
    }

    @Inject(
            method = "drawBackground",
            at = @At("RETURN")
    )
    private void onDrawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        assert client != null;

        int x = (this.width - this.backgroundWidth) / 2 + 160;
        int y = (this.height - this.backgroundHeight) / 2 + 73;
        
        RenderSystem.setShaderTexture(0, REROLL_TEXTURE);
        DrawableHelper.drawTexture(matrices, x, y, 0, 0, 9, 9, 9, 9);
    }
    
    @Inject(
    		method = "render",
    		at = @At("RETURN")
    )
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        assert client != null;

        int x = (this.width - this.backgroundWidth) / 2 + 160;
        int y = (this.height - this.backgroundHeight) / 2 + 73;
        
        if(mouseX >= x && mouseX <= x + 9 && mouseY >= y && mouseY <= y + 9) {
            RenderSystem.setShaderTexture(0, REROLL_TEXTURE_IN);
            DrawableHelper.drawTexture(matrices, x, y, 0, 0, 9, 9, 9, 9);

            List<Text> content = new ArrayList<>();
            content.add(new TranslatableText("reroll.tooltip").formatted(Formatting.GRAY));

            if(RerollClient.getExpPerReroll() > 0) {
                MutableText expPrompt = new TranslatableText("reroll.exp_prompt").formatted(Formatting.GREEN);
                MutableText expText = new TranslatableText("reroll.exp_amount", RerollClient.getExpPerReroll());

                if(playerLevels < RerollClient.getExpPerReroll()) {
                    expText = expText.formatted(Formatting.RED);
                } else {
                    expText = expText.formatted(Formatting.GRAY);
                }

                content.add(expPrompt.append(expText));
            }

            if(RerollClient.getLapisPerReroll() > 0) {
                MutableText lapisPrompt = new TranslatableText("reroll.lapis_prompt").formatted(Formatting.BLUE);
                MutableText lapisText = new TranslatableText("reroll.lapis_amount", RerollClient.getLapisPerReroll());

                if(playerLapis < RerollClient.getLapisPerReroll()) {
                    lapisText = lapisText.formatted(Formatting.RED);
                } else {
                    lapisText = lapisText.formatted(Formatting.GRAY);
                }

                content.add(lapisPrompt.append(lapisText));
            }
            
            this.renderTooltip(matrices, content, x, y);
        }
    }

    @Inject(
            method = "mouseClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        int x = (this.width - this.backgroundWidth) / 2 + 160;
        int y = (this.height - this.backgroundHeight) / 2 + 73;

        if(mouseX >= x && mouseX <= x + 9 && mouseY >= y && mouseY <= y + 9) {
            ClientPlayNetworking.send(Reroll.REROLL_PACKET, PacketByteBufs.create());
            cir.setReturnValue(true);
        }
    }
}
