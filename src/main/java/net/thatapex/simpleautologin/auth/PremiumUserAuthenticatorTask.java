package net.thatapex.simpleautologin.auth;

import javax.crypto.SecretKey;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.logging.Level;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import net.minecraft.server.v1_11_R1.MinecraftEncryption;
import net.thatapex.simpleautologin.PremiumStatus;

final class PremiumUserAuthenticatorTask implements Runnable
{
    private final CustomLoginListener customLoginListener;

    PremiumUserAuthenticatorTask(final CustomLoginListener customLoginListener)
    {
        this.customLoginListener = customLoginListener;
    }

    @Override
    public void run()
    {
        final GameProfile gameProfile = this.customLoginListener.getGameProfile();

        try
        {
            final PublicKey publicKey = this.customLoginListener.getMinecraftServer().O().getPublic();
            final SecretKey loginKey = this.customLoginListener.getLoginKey();
            final String serverKey = new BigInteger(MinecraftEncryption.a("", publicKey, loginKey)).toString(16);

            final GameProfile authentication = this.customLoginListener.getMinecraftServer().az().hasJoinedServer(new GameProfile(null, gameProfile.getName()), serverKey, this.getActualAddress());
            if(authentication != null)
            {
                if(!this.customLoginListener.getNetworkManager().isConnected())
                {
                    return;
                }

                this.customLoginListener.setGameProfile(authentication);
                this.customLoginListener.getPlugin().setPremiumStatus(authentication.getId(), PremiumStatus.PREMIUM);
                this.customLoginListener.fireEvents();
            }
            else
            {
                this.customLoginListener.disconnect("Failed to verify username");
                this.customLoginListener.getPlugin().getLogger().warning("User " + gameProfile.getName() + " tried to join with an invalid session");
            }
        }
        catch (final AuthenticationUnavailableException e)
        {
            this.reportException("Authentication servers are down", e);
        }
        catch (final Exception e)
        {
            this.reportException("Failed to verify username", e);
        }

    }

    private void reportException(final String message, final Exception e)
    {
        this.customLoginListener.disconnect(message);
        this.customLoginListener.getPlugin().getLogger().log(Level.SEVERE, message, e);
    }

    private InetAddress getActualAddress()
    {
        final SocketAddress socketaddress = this.customLoginListener.getNetworkManager().getSocketAddress();
        if (this.customLoginListener.getMinecraftServer().ac() && (socketaddress instanceof InetSocketAddress))
        {
            return ((InetSocketAddress) socketaddress).getAddress();
        }
        else
        {
            return null;
        }
    }
}
