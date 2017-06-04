package net.thatapex.simpleautologin.auth;

import java.util.logging.Level;

import net.thatapex.simpleautologin.PremiumStatus;

final class NonPremiumUserAuthenticatorTask implements Runnable
{
    private final CustomLoginListener customLoginListener;

    NonPremiumUserAuthenticatorTask(final CustomLoginListener customLoginListener)
    {
        this.customLoginListener = customLoginListener;
    }

    @Override
    public void run()
    {
        try
        {
            this.customLoginListener.initUUID();
            this.customLoginListener.getPlugin().setPremiumStatus(this.customLoginListener.getGameProfile().getId(), PremiumStatus.NONPREMIUM);
            this.customLoginListener.fireEvents();
        }
        catch (final Exception e)
        {
            this.customLoginListener.disconnect("Failed to verify username!");
            this.customLoginListener.getPlugin().getServer().getLogger().log(Level.WARNING, "Exception verifying " + this.customLoginListener.getGameProfile().getName(), e);
        }
    }
}
