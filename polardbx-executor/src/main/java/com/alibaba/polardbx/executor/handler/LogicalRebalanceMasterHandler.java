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

package com.alibaba.polardbx.executor.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.polardbx.common.cdc.CdcConstants;
import com.alibaba.polardbx.common.cdc.ResultCode;
import com.alibaba.polardbx.common.exception.TddlRuntimeException;
import com.alibaba.polardbx.common.exception.code.ErrorCode;
import com.alibaba.polardbx.common.utils.HttpClientHelper;
import com.alibaba.polardbx.druid.util.StringUtils;
import com.alibaba.polardbx.executor.cursor.Cursor;
import com.alibaba.polardbx.executor.cursor.impl.AffectRowCursor;
import com.alibaba.polardbx.executor.spi.IRepository;
import com.alibaba.polardbx.net.util.CdcTargetUtil;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.rel.dal.LogicalDal;
import com.alibaba.polardbx.optimizer.utils.RelUtils;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlRebalanceMaster;

/**
 * @author yudong
 * @since 2023/3/1 17:14
 **/
public class LogicalRebalanceMasterHandler extends HandlerCommon {

    public LogicalRebalanceMasterHandler(IRepository repo) {
        super(repo);
    }

    @Override
    public Cursor handle(RelNode logicalPlan, ExecutionContext executionContext) {
        SqlRebalanceMaster sqlRebalanceMaster = (SqlRebalanceMaster) ((LogicalDal) logicalPlan).getNativeSqlNode();
        SqlNode with = sqlRebalanceMaster.getWith();
        String groupName = with == null ? "" : RelUtils.lastStringValue(with);
        String daemonEndpoint;
        if (StringUtils.isEmpty(groupName)) {
            daemonEndpoint = CdcTargetUtil.getDaemonMasterTarget();
        } else {
            daemonEndpoint = CdcTargetUtil.getDaemonMasterTarget(groupName);
        }

        String res;
        try {
            res = HttpClientHelper.doGet("http://" + daemonEndpoint + "/system/rebalance");
        } catch (Exception e) {
            throw new RuntimeException("rebalance master failed", e);
        }

        ResultCode resultCode = JSON.parseObject(res, ResultCode.class);
        if (resultCode.getCode() == CdcConstants.SUCCESS_CODE) {
            return new AffectRowCursor(0);
        } else {
            throw new TddlRuntimeException(ErrorCode.ERR_CDC_GENERIC, resultCode.getMsg());
        }
    }
}
