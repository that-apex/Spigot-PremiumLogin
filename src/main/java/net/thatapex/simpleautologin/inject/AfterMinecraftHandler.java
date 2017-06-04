package net.thatapex.simpleautologin.inject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import net.minecraft.server.v1_11_R1.NetworkManager;
import net.thatapex.simpleautologin.SimpleAutoLogin;
import net.thatapex.simpleautologin.auth.CustomHandshakeListener;

final class AfterMinecraftHandler extends ChannelInitializer<Channel>
{
    private final SimpleAutoLogin plugin;

    AfterMinecraftHandler(final SimpleAutoLogin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    protected void initChannel(final Channel channel) throws Exception
    {
        final NetworkManager networkManager = (NetworkManager) channel.pipeline().get("packet_handler");
        networkManager.setPacketListener(new CustomHandshakeListener(this.plugin, networkManager));
    }
}
