package com.modulardreams.equipment;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

import com.modulardreams.material.MaterialTier;
import com.modulardreams.material.ModMaterials;
import com.modulardreams.material.ModularMaterial;
import com.modulardreams.modifier.Modifier;
import com.modulardreams.part.PartType;
import com.modulardreams.stats.ModTraits;

/**
 * The in-game guide book ("Materials & You").
 *
 * Right-clicking builds the book content dynamically from the material,
 * part and modifier registries — so the guide can never go stale — and opens
 * the vanilla written-book UI via {@code Player#openItemGui}. No custom client
 * code required.
 */
public class GuideBookItem extends Item {

        public GuideBookItem(Properties properties) {
                super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
                if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
                        book.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT, buildContent());
                        serverPlayer.openItemGui(book, hand);
                }
                return InteractionResult.SUCCESS;
        }

        private WrittenBookContent buildContent() {
                List<Filterable<Component>> pages = new ArrayList<>();
                GuidePages.intro(pages);
                GuidePages.gettingStarted(pages);
                GuidePages.toolPages(pages);
                GuidePages.materialPages(pages);
                GuidePages.armorPages(pages);
                GuidePages.modifierPages(pages);
                GuidePages.repairPages(pages);
                GuidePages.tipPages(pages);
                return new WrittenBookContent(Filterable.passThrough("Materials & You"), "Modular Dreams", 0, pages, true);
        }
}
