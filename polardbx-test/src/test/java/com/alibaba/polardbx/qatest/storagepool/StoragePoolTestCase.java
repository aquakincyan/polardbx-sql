package com.alibaba.polardbx.qatest.storagepool;

import com.alibaba.polardbx.qatest.ddl.auto.locality.LocalityTestBase;
import com.alibaba.polardbx.qatest.ddl.auto.locality.LocalityTestCaseUtils.LocalityTestCaseTask;
import com.alibaba.polardbx.qatest.storagepool.LegacyStoragePoolTestCase.StoragePoolTestCaseTask;
import net.jcip.annotations.NotThreadSafe;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.FileNotFoundException;

@NotThreadSafe
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StoragePoolTestCase extends LocalityTestBase {
    public void runTestCase(String resourceFile) throws FileNotFoundException, InterruptedException {
        String resourceDir = "partition/env/StoragePoolTest/" + resourceFile;
        String fileDir = getClass().getClassLoader().getResource(resourceDir).getPath();
        StoragePoolTestCaseTask storagePoolTestCaseTask = new StoragePoolTestCaseTask(fileDir);
        storagePoolTestCaseTask.execute(tddlConnection);
    }
    /*
     * for create partition table.
     * (hash_partition, range_partition, list_partition)
     * (full_part_spec, non_full_part_spec)
     * (int_partition_key, string_partition_key)
     * (with_gsi, without_gsi)
     * (table_level_locality, partition_level_localiy, table_and_partition_level_locality, no_locality)
     *
     * for create other table
     * (broadcast_table, single_table)
     * (with_gsi, without_gsi)
     *
     * for repartition
     * (broad->single, broad->partition, partition->single, partiton->broadcast, single->broad, single->partition)
     *
     * for modify partition
     * (move, add, split, merge, split_by_hot_value, extract)
     *
     * for set locality
     *
     * for rebalance
     */

    @Test
    public void testCase01StoragePoolDemo() throws FileNotFoundException, InterruptedException {
        runTestCase("storage_pool_demo.test.yml");
    }

    @Test
    public void testCase02ListPartitionTableOperation() throws FileNotFoundException, InterruptedException {
        runTestCase("list_partition_table_storage_pool.test.yml");
    }

    @Test
    public void testCase03HashPartitionTableOperation() throws FileNotFoundException, InterruptedException {
        runTestCase("hash_partition_table_storage_pool.test.yml");
    }

    @Test
    public void testCase04SingleTableOperation() throws FileNotFoundException, InterruptedException {
        runTestCase("single_table_storage_pool.test.yml");
    }

}
