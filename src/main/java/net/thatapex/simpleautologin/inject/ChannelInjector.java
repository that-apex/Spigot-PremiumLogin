package net.thatapex.simpleautologin.inject;

import java.lang.reflect.Field;
import java.util.List;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import net.minecraft.server.v1_11_R1.MinecraftServer;
import net.minecraft.server.v1_11_R1.ServerConnection;
import net.thatapex.simpleautologin.SimpleAutoLogin;
import org.bukkit.craftbukkit.v1_11_R1.CraftServer;

public class ChannelInjector
{
    private final SimpleAutoLogin plugin;
    private final MinecraftServer server;

    public ChannelInjector(final SimpleAutoLogin plugin)
    {
        this.plugin = plugin;
        this.server = ((CraftServer) plugin.getServer()).getServer();
    }

    public void inject()
    {
        try
        {
            final Field field = ServerConnection.class.getDeclaredField("g");
            field.setAccessible(true);
            @SuppressWarnings ("unchecked")
            final List<ChannelFuture> futures = (List<ChannelFuture>) field.get(this.server.getServerConnection());
            futures.stream().map(ChannelFuture::channel).forEach(this::handleChannel);
        }
        catch (final ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void handleChannel(final Channel channel)
    {
        channel.pipeline().addFirst("SimpleAutoLogin|Acceptor", new NettyAcceptor(this.plugin));
        this.plugin.getLogger().info("Injecting an Acceptor to main channel pipeline");
    }
}
