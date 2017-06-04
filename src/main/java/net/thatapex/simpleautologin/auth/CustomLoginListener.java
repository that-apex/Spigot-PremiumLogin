package net.thatapex.simpleautologin.auth;

import javax.crypto.SecretKey;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.LoginListener;
import net.minecraft.server.v1_11_R1.MinecraftServer;
import net.minecraft.server.v1_11_R1.NetworkManager;
import net.minecraft.server.v1_11_R1.PacketLoginInEncryptionBegin;
import net.minecraft.server.v1_11_R1.PacketLoginInStart;
import net.minecraft.server.v1_11_R1.PacketLoginOutEncryptionBegin;
import net.minecraft.server.v1_11_R1.PacketLoginOutSetCompression;
import net.minecraft.server.v1_11_R1.PacketLoginOutSuccess;
import net.thatapex.simpleautologin.SimpleAutoLogin;
import net.thatapex.simpleautologin.utils.MojangAPI;
import org.apache.commons.lang3.Validate;
import org.bukkit.craftbukkit.v1_11_R1.CraftServer;
import org.bukkit.craftbukkit.v1_11_R1.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;

final class CustomLoginListener extends LoginListener
{
    private static final int           MAX_TICKS_LOGIN       = 600;
    private static final AtomicInteger executorThreadCounter = new AtomicInteger();
    private static final Executor      executor              = Executors.newCachedThreadPool(runnable -> new Thread(runnable, "SimpleAutoLogin Auth Task #" + executorThreadCounter.getAndIncrement()));
    private static final Random        nonceRandom           = new Random();

    private final SimpleAutoLogin plugin;
    private final NetworkManager  networkManager;
    private final MinecraftServer minecraftServer;
    private       GameProfile     gameProfile;
    private       State           currentState;
    private       byte[]          nonce;
    private       EntityPlayer    player;
    private       int             ticks;
    private       SecretKey       loginKey;

    CustomLoginListener(final SimpleAutoLogin plugin, final NetworkManager networkManager, final String hostname)
    {
        super(((CraftServer) plugin.getServer()).getServer(), networkManager);
        this.plugin = plugin;
        this.networkManager = networkManager;
        this.minecraftServer = ((CraftServer) plugin.getServer()).getServer();
        this.currentState = State.WAITING_FOR_START;

        this.nonce = new byte[4];
        nonceRandom.nextBytes(this.nonce);

        super.hostname = hostname;
    }

    @Override
    public void a(final PacketLoginInStart packet)
    {
        Validate.validState(this.currentState == State.WAITING_FOR_START, "Invalid state");
        this.gameProfile = packet.a();

        // check if nickname is premium
        MojangAPI.checkNicknamePremiumStatusAsync(this.gameProfile.getName(), status -> {
            this.plugin.getLogger().info("Premium status for nickname  " + this.gameProfile.getName() + " is " + status + ", handling start packet...");

            if (status)
            {
                // handle premium
                this.currentState = State.WATITING_FOR_ENCRYPTION;
                this.networkManager.sendPacket(new PacketLoginOutEncryptionBegin("", this.minecraftServer.O().getPublic(), this.nonce));
            }
            else
            {
                // handle non premium
                executor.execute(new NonPremiumUserAuthenticatorTask(this));
            }
        });
    }

    /*  only premium clients will send this packet */
    @Override
    public void a(final PacketLoginInEncryptionBegin packet)
    {
        Validate.validState(this.currentState == State.WATITING_FOR_ENCRYPTION, "Invalid state");

        final PrivateKey privateKey = this.minecraftServer.O().getPrivate();
        Validate.validState(Arrays.equals(this.nonce, packet.b(privateKey)), "Invalid nonce");

        this.currentState = State.PREMIUM_AUTH;
        this.loginKey = packet.a(privateKey);
        this.networkManager.a(this.loginKey);

        executor.execute(new PremiumUserAuthenticatorTask(this));
    }

    /* tick() */
    @Override
    public void F_()
    {
        if (this.currentState == State.READY_TO_ACCEPT)
        {
            this.joinPlayer();
        }
        else if (this.currentState == State.DELAY_ACCEPT)
        {
            final EntityPlayer entityPlayer = this.minecraftServer.getPlayerList().a(this.gameProfile.getId());
            if (entityPlayer == null)
            {
                this.currentState = State.READY_TO_ACCEPT;
                this.minecraftServer.getPlayerList().a(this.networkManager, this.player);
                this.player = null;
            }
        }

        if (this.ticks++ == MAX_TICKS_LOGIN)
        {
            this.disconnect("Took too long to log in");
        }
    }

    private void joinPlayer()
    {
        final EntityPlayer loginAttempt = this.minecraftServer.getPlayerList().attemptLogin(this, this.gameProfile, this.hostname);

        if (loginAttempt != null)
        {
            this.currentState = State.ACCEPTED;

            // set compression
            if ((this.minecraftServer.aG() >= 0) && !this.networkManager.isLocal())
            {
                //noinspection unchecked
                this.networkManager.sendPacket(new PacketLoginOutSetCompression(this.minecraftServer.aG()), future -> this.networkManager.setCompressionLevel(this.minecraftServer.aG()));
            }

            this.networkManager.sendPacket(new PacketLoginOutSuccess(this.gameProfile));

            final EntityPlayer player = this.minecraftServer.getPlayerList().a(this.gameProfile.getId());
            if (player != null)
            {
                this.currentState = State.DELAY_ACCEPT;
                this.player = this.minecraftServer.getPlayerList().processLogin(this.gameProfile, loginAttempt);
            }
            else
            {
                this.minecraftServer.getPlayerList().a(this.networkManager, this.minecraftServer.getPlayerList().processLogin(this.gameProfile, loginAttempt));
            }
        }
    }

    @Override
    public void initUUID()
    {
        final UUID realUuid;
        if (this.networkManager.spoofedUUID != null)
        {
            realUuid = this.networkManager.spoofedUUID;
        }
        else
        {
            // offline player uuid
            realUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.gameProfile.getName()).getBytes(Charsets.UTF_8));
        }

        // recreate game profile with real uuid
        this.gameProfile = new GameProfile(realUuid, this.gameProfile.getName());

        // copy properties
        if (this.networkManager.spoofedProfile != null)
        {
            final PropertyMap properties = this.gameProfile.getProperties();

            for (final Property property : this.networkManager.spoofedProfile)
            {
                properties.put(property.getName(), property);
            }
        }
    }

    public void fireEvents() throws Exception
    {
        // get data
        final String playerName = this.gameProfile.getName();
        final InetAddress playerAddress = ((InetSocketAddress) this.networkManager.getSocketAddress()).getAddress();
        final UUID playerUuid = this.gameProfile.getId();
        final CraftServer craftServer = this.minecraftServer.server;

        // call async pre login event
        final AsyncPlayerPreLoginEvent asyncPlayerPreLoginEvent = new AsyncPlayerPreLoginEvent(playerName, playerAddress, playerUuid);
        craftServer.getPluginManager().callEvent(asyncPlayerPreLoginEvent);

        // call pre login event
        if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0)
        {
            final PlayerPreLoginEvent event = new PlayerPreLoginEvent(playerName, playerAddress, playerUuid);
            if (asyncPlayerPreLoginEvent.getResult() != Result.ALLOWED)
            {
                event.disallow(asyncPlayerPreLoginEvent.getResult(), asyncPlayerPreLoginEvent.getKickMessage());
            }

            final Waitable<Result> waitable = new Waitable<Result>()
            {
                @Override
                protected Result evaluate()
                {
                    craftServer.getPluginManager().callEvent(event);
                    return event.getResult();
                }
            };

            this.minecraftServer.processQueue.add(waitable);

            if (waitable.get() != Result.ALLOWED)
            {
                this.disconnect(event.getKickMessage());
                return;
            }
        }
        else if (asyncPlayerPreLoginEvent.getLoginResult() != org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.ALLOWED)
        {
            this.disconnect(asyncPlayerPreLoginEvent.getKickMessage());
            return;
        }

        this.minecraftServer.server.getLogger().info("UUID of player " + playerUuid + " is " + playerName);
        this.currentState = State.READY_TO_ACCEPT;
    }

    public MinecraftServer getMinecraftServer()
    {
        return this.minecraftServer;
    }

    public NetworkManager getNetworkManager()
    {
        return this.networkManager;
    }

    public SimpleAutoLogin getPlugin()
    {
        return this.plugin;
    }

    public GameProfile getGameProfile()
    {
        return this.gameProfile;
    }

    void setGameProfile(final GameProfile gameProfile)
    {
        this.gameProfile = gameProfile;
    }

    SecretKey getLoginKey()
    {
        return this.loginKey;
    }

    public enum State
    {
        WAITING_FOR_START,
        WATITING_FOR_ENCRYPTION,
        PREMIUM_AUTH,
        READY_TO_ACCEPT,
        DELAY_ACCEPT,
        ACCEPTED
    }
}
