package net.thatapex.simpleautologin;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.thatapex.simpleautologin.inject.ChannelInjector;
import net.thatapex.simpleautologin.listener.DiamondListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleAutoLogin extends JavaPlugin
{
    private Map<UUID, PremiumStatus> premiumStatuses = new WeakHashMap<>();

    @Override
    public void onEnable()
    {
        new ChannelInjector(this).inject();

        this.getServer().getPluginManager().registerEvents(new DiamondListener(this), this);
    }

    public PremiumStatus getPremiumStatus(final Player player)
    {
        return this.getPremiumStatus(player.getUniqueId());
    }

    public PremiumStatus getPremiumStatus(final UUID uuid)
    {
        return this.premiumStatuses.getOrDefault(uuid, PremiumStatus.UNKNOWN);
    }

    public void setPremiumStatus(final UUID uuid, final PremiumStatus status)
    {
        this.premiumStatuses.put(uuid, status);
    }
}
