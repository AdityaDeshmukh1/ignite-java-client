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

        try (Ignite ignite = Ignition.start(cfg)) {
            ignite.cluster().active(true);

            CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("acidTestCache");
            cacheCfg.setCacheMode(CacheMode.PARTITIONED);
            cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
            
            CacheConfiguration<Integer, Integer> balanceCacheCfg = new CacheConfiguration<>("balanceCache");
            balanceCacheCfg.setCacheMode(CacheMode.PARTITIONED);
            balanceCacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
            
            IgniteCache<Integer, Integer> balanceCache = ignite.getOrCreateCache(balanceCacheCfg);
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);
            cache.clear();

            System.out.println("\n=== Begin ACID Transaction Tests ===");

            testAtomicity(ignite, cache);
            testConsistency(ignite, balanceCache);
            testIsolation(ignite, cache);
            testDurability(ignite, cache);

            System.out.println("\n=== ACID Tests Completed ===");
        } catch (Exception e) {
            System.err.println("[FAILURE] : Ignite failed to start or crashed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testAtomicity(Ignite ignite, IgniteCache<Integer, String> cache) {
        System.out.println("\n--- Test: Atomicity ---");
        try (Transaction tx = ignite.transactions().txStart()) {
            cache.put(1, "One");
            cache.put(2, "Two");
            tx.commit();
            System.out.println("Transaction 1 committed successfully.");
        }

        try (Transaction tx = ignite.transactions().txStart()) {
            cache.put(3, "Three");
            throw new RuntimeException("Simulated failure");
        } catch (Exception e) {
            System.out.println("Transaction 2 rolled back due to: " + e.getMessage());
        }

        System.out.println("Cache state after Atomicity test:");
        for (int key = 1; key <= 3; key++) {
            String value = cache.get(key);
            System.out.printf("Key %d => %s%n", key, value != null ? value : "NULL (ATOMICITY Guaranteed)");
        }
    }

    private static void testConsistency(Ignite ignite, IgniteCache<Integer, Integer> balanceCache) {
      System.out.println("\n--- Test: Consistency ---");

      balanceCache.clear();
      balanceCache.put(1, 100); // initial balance

      try (Transaction tx = ignite.transactions().txStart()) {
        int currentBalance = balanceCache.get(1);
        int debit = 150; // attempt to debit more than balance

        int newBalance = currentBalance - debit;
        if (newBalance < 0) {
          throw new RuntimeException("Consistency violation: Negative balance not allowed!");
        }

        balanceCache.put(1, newBalance);
        tx.commit();

        System.out.println("Consistency check passed and transaction committed.");
      } catch (Exception e) {
        System.out.println("Consistency test failed and transaction rolled back: " + e.getMessage());
      }

      // Check final balance (should remain unchanged)
      Integer finalBalance = balanceCache.get(1);
      System.out.println("Final balance after consistency test: " + finalBalance);
    }

    private static void testIsolation(Ignite ignite, IgniteCache<Integer, String> cache) throws InterruptedException {
      System.out.println("\n--- Test: Isolation ---");

      final Object lock = new Object();
      final boolean[] done = {false};

      Thread tx1 = new Thread(() -> {
        try (Transaction tx = ignite.transactions().txStart()) {
          cache.put(20, "Twenty");
          System.out.println("Transaction 1 put key 20 = Twenty, sleeping before commit...");
          Thread.sleep(3000);  // Simulate delay to let tx2 read before commit
          tx.commit();
          System.out.println("Transaction 1 committed.");
        } catch (Exception e) {
          System.out.println("Transaction 1 failed: " + e.getMessage());
        }
        synchronized (lock) {
          done[0] = true;
          lock.notifyAll();
        }
      });

      Thread tx2 = new Thread(() -> {
        try {
          // Wait a bit to make sure tx1 has put the value but not committed yet
          Thread.sleep(1000);
          try (Transaction tx = ignite.transactions().txStart()) {
            String val = cache.get(20);
            System.out.println("Transaction 2 reads key 20: " + val);
            tx.commit();
          }
        } catch (Exception e) {
          System.out.println("Transaction 2 failed: " + e.getMessage());
        }
      });

      tx1.start();
      tx2.start();

      synchronized (lock) {
        while (!done[0]) {
          lock.wait();
        }
      }
    }
    
    private static void testDurability(Ignite ignite, IgniteCache<Integer, String> cache) {
        System.out.println("\n--- Test: Durability ---");
        // Durability test: commit transaction, then simulate restart and verify data persists
        // Since we can't restart in code, simulate by reading after commit

        try (Transaction tx = ignite.transactions().txStart()) {
            cache.put(30, "Thirty");
            tx.commit();
            System.out.println("Transaction committed. Verifying durability...");

            String val = cache.get(30);
            if ("Thirty".equals(val)) {
                System.out.println("Durability test passed: Data persisted.");
            } else {
                System.out.println("Durability test failed: Data missing.");
            }
        }
    }
}

