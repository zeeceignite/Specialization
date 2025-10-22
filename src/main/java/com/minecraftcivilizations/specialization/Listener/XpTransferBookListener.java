package com.minecraftcivilizations.specialization.Listener;

import com.minecraftcivilizations.specialization.Player.CustomPlayer;
import com.minecraftcivilizations.specialization.Skill.SkillType;
import com.minecraftcivilizations.specialization.SmartEntity.SmartEntity;
import com.minecraftcivilizations.specialization.Specialization;
import com.minecraftcivilizations.specialization.util.CoreUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import static com.minecraftcivilizations.specialization.util.MathUtils.*;

import java.nio.channels.Channel;
import java.util.*;

/**
 * @author JFrogy
 */

public class XpTransferBookListener implements Listener {


    private static final float VOLUME = 1;
    private static final float PITCH = 1;
    private static final float PITCH_VARIANCE = 0.1f;

    private static final Map<UUID, Long> lastSignTime = new HashMap<>();
    private static final NamespacedKey XP_BLESSED_KEY = new NamespacedKey(Specialization.getInstance(), "xp_blessed_book");
    private static final NamespacedKey XP_AMOUNT_KEY = new NamespacedKey(Specialization.getInstance(), "xp_amount");




    private void applyBookInstructions(BookMeta book_meta) {
        book_meta.setPages(List.of("""
            §5Xp Level Transfer
            §lSign To Confirm

            Amount: """));
    }


    /**
     * The event for grabbing XP from another player
     */
    /**
     *  XpTransfer system
     * @author JFrogy
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSneakRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;


        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();


        // --- Bless writable book ---
        if (player.isSneaking() && item.getType() == Material.WRITABLE_BOOK) {
            event.setCancelled(true);

            if(item.hasItemMeta() && item.getItemMeta() instanceof BookMeta m){
                if(m.getPersistentDataContainer().has(XP_BLESSED_KEY)){
                    player.sendMessage(ChatColor.RED + "Book is already blessed");
                    return;
                }
            }

            CustomPlayer cp = CoreUtil.getPlayer(player.getUniqueId());
            if (cp.getSkillLevel(SkillType.LIBRARIAN) < 3) {
                player.sendMessage(ChatColor.RED + "You must be a Librarian level 3 to bless XP books.");
                return;
            }

            BookMeta book_meta = (BookMeta) item.getItemMeta();
            if (book_meta == null) return; // this should never happen


            //Blessing of book will be successful

            book_meta.setDisplayName(ChatColor.AQUA + "Blessed XP Transfer Book");
            book_meta.setLore(List.of(
                    ChatColor.GRAY + "Blessed by " + player.getName(),
                    ChatColor.DARK_AQUA + "Write an amount and sign to store XP."
            ));
            applyBookInstructions(book_meta);
            book_meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            book_meta.setEnchantmentGlintOverride(true);
            book_meta.getPersistentDataContainer().set(XP_BLESSED_KEY, PersistentDataType.INTEGER, 1);
            item.setItemMeta(book_meta);

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.AMBIENT, VOLUME, PITCH +random(-PITCH_VARIANCE, PITCH_VARIANCE));
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
            ItemStack is = player.getInventory().getItemInMainHand();
            is.setAmount(is.getAmount()-1);
            player.sendMessage(ChatColor.GREEN + "You absorbed " + xp + " XP from the book!");
            new SmartEntitySpiral(player);
        }
    }

    @EventHandler
    public void onPlayerSignXpBook(PlayerEditBookEvent event) {

        int max_levels = 3; // the maximum levels transferrable into a book.

        Player player = event.getPlayer();
        ItemStack book = player.getInventory().getItemInMainHand();
        if (book == null || book.getType() != Material.WRITABLE_BOOK) return;


        BookMeta old_book_meta = (BookMeta) book.getItemMeta();
        if (old_book_meta == null) return;
        Integer blessed = old_book_meta.getPersistentDataContainer().get(XP_BLESSED_KEY, PersistentDataType.INTEGER);
        if (blessed == null || blessed != 1) return;

        event.setCancelled(true); // --- Prevent default written book creation

        if (!event.isSigning()){
            applyBookInstructions(old_book_meta);
            return;
         }

        long now = System.currentTimeMillis();
        if (now - lastSignTime.getOrDefault(player.getUniqueId(), 0L) < 1000L) {
            player.sendMessage(ChatColor.RED + "Wait a moment before signing another book.");
            return;
        }
        lastSignTime.put(player.getUniqueId(), now);

        BookMeta meta = event.getNewBookMeta();
        List<String> pages = meta.getPages();
        if (pages.isEmpty()) {
            player.sendMessage(ChatColor.RED + "bruv what are you doing?");
            return;
        }


        String[] page_split = pages.getFirst().split("Amount:");
        if(page_split.length<2){
            return;
        }
        String number_string = page_split[1].strip();
        int requestedLevels;
        try{
            requestedLevels = Math.max(0, Integer.valueOf(number_string));
        }catch(NumberFormatException e){
            player.sendMessage(ChatColor.RED + "Invalid number entered: "+number_string);
            return;
        }

        if(requestedLevels > max_levels){
            requestedLevels = max_levels;
        }
        //do value checks here TODO

        int playerLevel = player.getLevel();
        if (requestedLevels > playerLevel) requestedLevels = playerLevel;

        int currentXp = getTotalXpForLevel(playerLevel);
        int targetXp = getTotalXpForLevel(playerLevel - requestedLevels);
        int totalXp = currentXp - targetXp;

        //Check if any xp is being transfered
        if(totalXp<=0){
            player.sendMessage(ChatColor.RED+"You have no xp to transfer. Go play the game.");
            return;
        }

        // this is temporary
        meta.setDisplayName(ChatColor.RESET+""+ChatColor.AQUA+"Tome of Knowledge with " + totalXp + "");
//        meta.setLore(ChatColor.RESET+""+ChatColor.AQUA+"Tome of Knowledge with " + totalXp + "");

        player.setTotalExperience(targetXp);
        player.setLevel(playerLevel - requestedLevels);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DECORATED_POT_INSERT, SoundCategory.AMBIENT, 0.6f, PITCH +random(-PITCH_VARIANCE, PITCH_VARIANCE));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 0.1f, PITCH +-0.4f +random(-PITCH_VARIANCE, PITCH_VARIANCE));
        // --- Replace writable book with enchanted XP book (one tick later) ---
        Bukkit.getScheduler().runTask(Specialization.getInstance(), () -> {
            finalizeBook(meta, totalXp, player);
        });
    }


    private static void  finalizeBook(BookMeta old_meta, int totalXp, Player player) {
        ItemStack xpBook = new ItemStack(Material.BOOK);
        ItemMeta xpMeta = xpBook.getItemMeta();

        if (player.getInventory().getItemInMainHand().getType() != Material.WRITABLE_BOOK){
            player.sendMessage(ChatColor.GREEN + "Stop trying to exploit nerd...");
            return;
        }


        xpMeta.setDisplayName(old_meta.getDisplayName());
//            xpMeta.setDisplayNae(ChatColor.LIGHT_PURPLE + "Stored XP Book");
        xpMeta.setLore(List.of(
                ChatColor.DARK_PURPLE + "Contains " + totalXp + " XP",
                ChatColor.GRAY + "Shift-right-click to absorb.",
                ChatColor.WHITE + "Signed by: " + ChatColor.GOLD+old_meta.getTitle())
        );
        // ChatColor.GOLD+event.getNewBookMeta().getTitle())
        xpMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        xpMeta.setEnchantmentGlintOverride(true);
        xpMeta.getPersistentDataContainer().set(XP_AMOUNT_KEY, PersistentDataType.INTEGER, totalXp);
        xpBook.setItemMeta(xpMeta);

        player.getInventory().setItemInMainHand(xpBook);
        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "XP sealed into book: " + totalXp + " points.");
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


    /**
     * Spiral effect
     * @author Alectriciti
     */
    class SmartEntitySpiral extends SmartEntity{

        double height = 0;

        public SmartEntitySpiral(Entity owner){
            super(owner, owner.getLocation());
        }

        @Override
        public void update() {
            Location old_location = location.clone();
            height+=0.1;
            double radius = 0.8;
            double x = Math.sin(height*8)*radius;
            double z = Math.cos(height*8)*radius;

            if(owner!=null) {
                location = owner.getLocation().add(x, height, z);
            }
            location.getWorld().spawnParticle(Particle.ENCHANT, lerpLocationFast(old_location, location,0.5f), 0, 0,0,0);
            location.getWorld().spawnParticle(Particle.ENCHANT, location, 0, 0,0,0);

            if(tick%3==0) {
                float pitch = 0.8f+((float)tick/30f);
                location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.1f, pitch);
            }
//            location.getWorld().spawnParticle
            if(tick>20){
                destroy();
            }
        }
    }

}
