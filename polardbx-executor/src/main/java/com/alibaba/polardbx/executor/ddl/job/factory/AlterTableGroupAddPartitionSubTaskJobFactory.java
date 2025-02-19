/*
 * Copyright [2013-2021], Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.polardbx.executor.ddl.job.factory;

import com.alibaba.polardbx.common.utils.Pair;
import com.alibaba.polardbx.gms.tablegroup.PartitionGroupRecord;
import com.alibaba.polardbx.optimizer.OptimizerContext;
import com.alibaba.polardbx.optimizer.config.table.ComplexTaskMetaManager;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.rel.PhyDdlTableOperation;
import com.alibaba.polardbx.optimizer.core.rel.ddl.data.AlterTableGroupAddPartitionPreparedData;
import com.alibaba.polardbx.optimizer.core.rel.ddl.data.AlterTableGroupBasePreparedData;
import com.alibaba.polardbx.optimizer.core.rel.ddl.data.AlterTableGroupItemPreparedData;
import com.alibaba.polardbx.optimizer.partition.PartitionInfo;
import com.alibaba.polardbx.optimizer.partition.PartitionInfoUtil;
import com.alibaba.polardbx.optimizer.tablegroup.AlterTableGroupSnapShotUtils;
import org.apache.calcite.rel.core.DDL;
import org.apache.calcite.sql.SqlAlterTableAddPartition;
import org.apache.calcite.sql.SqlAlterTableGroup;
import org.apache.calcite.sql.SqlNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlterTableGroupAddPartitionSubTaskJobFactory extends AlterTableGroupSubTaskJobFactory {

    final AlterTableGroupAddPartitionPreparedData parentPrepareData;

    public AlterTableGroupAddPartitionSubTaskJobFactory(DDL ddl,
                                                        AlterTableGroupAddPartitionPreparedData parentPrepareData,
                                                        AlterTableGroupItemPreparedData preparedData,
                                                        List<PhyDdlTableOperation> phyDdlTableOperations,
                                                        Map<String, List<List<String>>> tableTopology,
                                                        Map<String, Set<String>> targetTableTopology,
                                                        Map<String, Set<String>> sourceTableTopology,
                                                        Map<String, Pair<String, String>> orderedTargetTableLocations,
                                                        String targetPartition,
                                                        boolean skipBackfill,
                                                        ComplexTaskMetaManager.ComplexTaskType taskType,
                                                        ExecutionContext executionContext) {
        super(ddl, parentPrepareData, preparedData, phyDdlTableOperations, tableTopology, targetTableTopology,
            sourceTableTopology, orderedTargetTableLocations, targetPartition, skipBackfill, taskType,
            executionContext);
        this.parentPrepareData = parentPrepareData;
    }

    @Override
    protected PartitionInfo generateNewPartitionInfo() {
        PartitionInfo curPartitionInfo =
            OptimizerContext.getContext(preparedData.getSchemaName()).getPartitionInfoManager()
                .getPartitionInfo(preparedData.getTableName());

        SqlNode sqlNode = ((SqlAlterTableGroup) ddl.getSqlNode()).getAlters().get(0);

        PartitionInfo newPartInfo = AlterTableGroupSnapShotUtils
            .getNewPartitionInfo(
                parentPrepareData,
                curPartitionInfo,
                true,
                sqlNode,
                preparedData.getOldPartitionNames(),
                preparedData.getNewPartitionNames(),
                parentPrepareData.getTableGroupName(),
                null,
                preparedData.getInvisiblePartitionGroups(),
                orderedTargetTableLocations,
                executionContext);
        return newPartInfo;
    }

    public AlterTableGroupBasePreparedData getParentPrepareData() {
        return parentPrepareData;
    }
}
