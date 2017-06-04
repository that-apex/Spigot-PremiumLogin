package net.thatapex.simpleautologin.inject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.thatapex.simpleautologin.SimpleAutoLogin;

final class NettyAcceptor extends ChannelInboundHandlerAdapter
{
    private final SimpleAutoLogin plugin;

    public NettyAcceptor(final SimpleAutoLogin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object o) throws Exception
    {
        final Channel channel = (Channel) o;
        channel.pipeline().addFirst("SimpleAutoLogin|BeforeMinecraftHandler", new BeforeMinecraftHandler(this.plugin));
        ctx.fireChannelRead(o);
    }
}
