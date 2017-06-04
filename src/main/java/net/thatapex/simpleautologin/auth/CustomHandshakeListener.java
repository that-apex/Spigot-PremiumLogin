package net.thatapex.simpleautologin.auth;

import net.minecraft.server.v1_11_R1.ChatComponentText;
import net.minecraft.server.v1_11_R1.EnumProtocol;
import net.minecraft.server.v1_11_R1.HandshakeListener;
import net.minecraft.server.v1_11_R1.NetworkManager;
import net.minecraft.server.v1_11_R1.PacketHandshakingInSetProtocol;
import net.minecraft.server.v1_11_R1.PacketLoginOutDisconnect;
import net.thatapex.simpleautologin.SimpleAutoLogin;
import org.bukkit.craftbukkit.v1_11_R1.CraftServer;

public class CustomHandshakeListener extends HandshakeListener
{
    private static final int CURRENT_PROTOCOL_VERSION = 316;
    private final SimpleAutoLogin plugin;
    private final NetworkManager  networkManager;

    public CustomHandshakeListener(final SimpleAutoLogin plugin, final NetworkManager networkManager)
    {
        super(((CraftServer) plugin.getServer()).getServer(), networkManager);
        this.plugin = plugin;
        this.networkManager = networkManager;
    }

    @Override
    public void a(final PacketHandshakingInSetProtocol packet)
    {
        if (packet.a() != EnumProtocol.LOGIN)
        {
            super.a(packet);
            return;
        }

        this.networkManager.setProtocol(EnumProtocol.LOGIN);

        // idc about throttling

        if (packet.b() != CURRENT_PROTOCOL_VERSION)
        {
            final ChatComponentText component = new ChatComponentText("Lol, no");
            this.networkManager.sendPacket(new PacketLoginOutDisconnect(component));
            this.networkManager.close(component);
        }
        else
        {
            this.networkManager.setPacketListener(new CustomLoginListener(this.plugin, this.networkManager, packet.hostname + ":" + packet.port));
        }
    }
}
