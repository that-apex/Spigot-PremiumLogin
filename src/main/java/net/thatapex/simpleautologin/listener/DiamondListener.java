package net.thatapex.simpleautologin.listener;

import net.thatapex.simpleautologin.PremiumStatus;
import net.thatapex.simpleautologin.SimpleAutoLogin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public final class DiamondListener implements Listener
{
    private final SimpleAutoLogin plugin;

    public DiamondListener(final SimpleAutoLogin plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();

        final PremiumStatus status = this.plugin.getPremiumStatus(player);
        if (status == PremiumStatus.PREMIUM)
        {
            player.sendMessage(ChatColor.AQUA + "Diamonds!");
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
        }
        else if (status == PremiumStatus.NONPREMIUM)
        {
            player.sendMessage("no premium == no diamonds :/");
        }
    }
}
