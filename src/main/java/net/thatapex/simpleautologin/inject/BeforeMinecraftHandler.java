package net.thatapex.simpleautologin.inject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import net.thatapex.simpleautologin.SimpleAutoLogin;

final class BeforeMinecraftHandler extends ChannelInitializer<Channel>
{
    private final SimpleAutoLogin plugin;

    BeforeMinecraftHandler(final SimpleAutoLogin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    protected void initChannel(final Channel channel) throws Exception
    {
        channel.pipeline().addLast("SimpleAutoLogin|AfterMinecraftHandler", new AfterMinecraftHandler(this.plugin));
    }
}
