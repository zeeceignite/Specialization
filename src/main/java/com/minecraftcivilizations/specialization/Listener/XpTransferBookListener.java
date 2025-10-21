package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XpTransferBookListener implements Listener {

    private static final Map<UUID, Long> lastSignTime = new HashMap<>();
    private static final NamespacedKey XP_BLESSED_KEY = new NamespacedKey(Specialization.getInstance(), "xp_blessed_book");
    private static final NamespacedKey XP_AMOUNT_KEY = new NamespacedKey(Specialization.getInstance(), "xp_amount");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSneakRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;

        // --- Bless writable book ---
        if (player.isSneaking() && item.getType() == Material.WRITABLE_BOOK) {
            event.setCancelled(true);

            CustomPlayer cp = CoreUtil.getPlayer(player.getUniqueId());
            if (cp.getSkillLevel(SkillType.LIBRARIAN) < 3) {
                player.sendMessage(ChatColor.RED + "You must be a Librarian level 3 to bless XP books.");
                return;
            }

            BookMeta meta = (BookMeta) item.getItemMeta();
            if (meta == null) return;

            meta.setDisplayName(ChatColor.AQUA + "Blessed XP Transfer Book");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Blessed by a Librarian.",
                    ChatColor.DARK_PURPLE + "Write an amount and sign to store XP."
            ));
            meta.setPages(List.of(ChatColor.DARK_PURPLE + "Xp Level Transfer Amount: "));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(XP_BLESSED_KEY, PersistentDataType.INTEGER, 1);
            item.setItemMeta(meta);

            player.sendMessage(ChatColor.GREEN + "Book blessed successfully!");
            Bukkit.getScheduler().runTask(Specialization.getInstance(), new Runnable() {
                @Override
                public void run() {
                    player.closeInventory();
                }
            });
            return;
        }

        // --- Redeem stored XP ---
        if (player.isSneaking() && item.getType() == Material.BOOK) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            Integer xp = meta.getPersistentDataContainer().get(XP_AMOUNT_KEY, PersistentDataType.INTEGER);
            if (xp == null || xp <= 0) return;

            event.setCancelled(true);
            player.giveExp(xp);
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(ChatColor.GREEN + "You absorbed " + xp + " XP from the book!");
        }
    }
    @EventHandler
    public void onPlayerSignXpBook(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        ItemStack book = player.getInventory().getItemInMainHand();
        if (book == null || book.getType() != Material.WRITABLE_BOOK) return;

        BookMeta oldMeta = (BookMeta) book.getItemMeta();
        if (oldMeta == null) return;
        Integer blessed = oldMeta.getPersistentDataContainer().get(XP_BLESSED_KEY, PersistentDataType.INTEGER);
        if (blessed == null || blessed != 1) return;

        // --- Prevent default written book creation ---
        event.setCancelled(true);

        long now = System.currentTimeMillis();
        if (now - lastSignTime.getOrDefault(player.getUniqueId(), 0L) < 1000L) {
            player.sendMessage(ChatColor.RED + "Wait a moment before signing another book.");
            return;
        }
        lastSignTime.put(player.getUniqueId(), now);

        BookMeta meta = event.getNewBookMeta();
        List<String> pages = meta.getPages();
        if (pages.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Book has no pages.");
            return;
        }

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(pages.get(0));
        if (!matcher.find()) {
            player.sendMessage(ChatColor.RED + "Invalid number entered.");
            return;
        }
        

        int requestedLevels = Integer.parseInt(matcher.group());

        int playerLevel = player.getLevel();
        if (requestedLevels > playerLevel) requestedLevels = playerLevel;

        int currentXp = getTotalXpForLevel(playerLevel);
        int targetXp = getTotalXpForLevel(playerLevel - requestedLevels);
        int totalXp = currentXp - targetXp;

        player.setTotalExperience(targetXp);
        player.setLevel(playerLevel - requestedLevels);

        // --- Replace writable book with enchanted XP book (one tick later) ---
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            ItemStack xpBook = new ItemStack(Material.BOOK);
            ItemMeta xpMeta = xpBook.getItemMeta();
            xpMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Stored XP Book");
            xpMeta.setLore(List.of(
                    ChatColor.DARK_PURPLE + "Contains " + totalXp + " XP",
                    ChatColor.GRAY + "Shift-right-click to absorb."
            ));
            xpMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            xpMeta.setEnchantmentGlintOverride(true);
            xpMeta.getPersistentDataContainer().set(XP_AMOUNT_KEY, PersistentDataType.INTEGER, totalXp);
            xpBook.setItemMeta(xpMeta);

            player.getInventory().setItemInMainHand(xpBook);
            player.updateInventory();
            player.sendMessage(ChatColor.GREEN + "XP sealed into book: " + totalXp + " points.");
        });
    }


    private static int getTotalXpForLevel(int level) {
        if (level <= 16) return (int) (Math.pow(level, 2) + 6 * level);
        if (level <= 31) return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastSignTime.remove(event.getPlayer().getUniqueId());
    }
}
