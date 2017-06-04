package net.thatapex.simpleautologin.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.Validate;

public final class MojangAPI
{
    private static final AtomicInteger          executorThreadCounter = new AtomicInteger();
    private static final Executor               executor              = Executors.newCachedThreadPool(runnable -> new Thread(runnable, "Mojang API Query #" + executorThreadCounter.getAndIncrement()));
    private static final Cache<String, Boolean> cache                 = CacheBuilder.<String, Boolean>newBuilder().expireAfterWrite(300L, TimeUnit.SECONDS).build();

    private MojangAPI()
    {
    }

    public static void checkNicknamePremiumStatusAsync(final String nickname, final Consumer<Boolean> callback)
    {
        Validate.isTrue(nickname.matches("[a-zA-Z0-9_]{1,16}"), "Invalid nickname");

        synchronized (cache)
        {
            final Boolean cached = cache.getIfPresent(nickname);
            if (cached != null)
            {
                callback.accept(cached);
                return;
            }
        }

        executor.execute(() -> {
            try
            {
                final HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + nickname).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                final boolean status = connection.getResponseCode() == 200;

                synchronized (cache)
                {
                    cache.put(nickname, status);
                }

                callback.accept(status);
            }
            catch (final java.io.IOException e)
            {
                callback.accept(false);
                throw new RuntimeException("Validation exception", e);
            }
        });
    }
}
