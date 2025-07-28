package com.example;

import org.apache.ignite.Ignition;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.WALMode;

import java.util.Iterator;
import javax.cache.Cache;

public class IgniteCacheInspector {

    public static void main(String[] args) {
        // Configure Ignite the same way as the writer
        IgniteConfiguration cfg = new IgniteConfiguration();

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        storageCfg.setWalMode(WALMode.FSYNC);
        storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(true);
        storageCfg.setStoragePath("/home/adi-gnome/Personal/Systems-Study/ignite/ignite-java-client/db/storage");
        cfg.setDataStorageConfiguration(storageCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            ignite.cluster().active(true);

            System.out.println("\n=== Inspecting 'acidTestCache' Contents ===");
            IgniteCache<Integer, String> cache = ignite.cache("acidTestCache");

            if (cache == null) {
                System.out.println("Cache 'acidTestCache' does not exist or failed to load.");
                return;
            }

            Iterator<Cache.Entry<Integer, String>> iter = cache.iterator();

            if (!iter.hasNext()) {
                System.out.println("Cache is empty.");
            }

            while (iter.hasNext()) {
                Cache.Entry<Integer, String> entry = iter.next();
                System.out.printf("Key %d => %s%n", entry.getKey(), entry.getValue());
            }

            System.out.println("=== Inspection Complete ===");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to inspect cache: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
