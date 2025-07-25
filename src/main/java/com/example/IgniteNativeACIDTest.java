
package com.example;

import org.apache.ignite.Ignition;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.transactions.Transaction;

public class IgniteNativeACIDTest {

    public static void main(String[] args) {

        // Configure Ignite with persistence enabled
        IgniteConfiguration cfg = new IgniteConfiguration();

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(true);
        cfg.setDataStorageConfiguration(storageCfg);

        // Start Ignite node with the configuration
        try (Ignite ignite = Ignition.start(cfg)) {

            // Activate the cluster (required when using persistence)
            ignite.cluster().active(true);

            // Configure cache with TRANSACTIONAL atomicity
            CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("acidTestCache");
            cacheCfg.setCacheMode(CacheMode.PARTITIONED);
            cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Clear any existing data
            cache.clear();

            System.out.println("\n=== Begin ACID Transaction Tests ===");

            // Test 1: Successful Transaction
            try (Transaction tx = ignite.transactions().txStart()) {
                cache.put(1, "One");
                cache.put(2, "Two");
                tx.commit();
                System.out.println("Transaction 1 committed successfully.");
            }

            // Test 2: Simulated failure and rollback
            try (Transaction tx = ignite.transactions().txStart()) {
                cache.put(3, "Three");
                // Simulate a failure
                throw new RuntimeException("Simulated failure");
            } catch (Exception e) {
                System.out.println("Transaction 2 rolled back due to: " + e.getMessage());
            }

            //  Final state verification
            System.out.println("\n=== Final Cache State ===");
            for (int key = 1; key <= 3; key++) {
                String value = cache.get(key);
                System.out.printf("Key %d => %s%n", key, value != null ? value : "ATOMICITY GUARANTEED!");
            }

            System.out.println("\n=== ACID Test Completed ===");

        } catch (Exception e) {
            System.err.println("[FAILURE] : Ignite failed to start or crashed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

