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

package com.alibaba.polardbx.repo.mysql.handler;

import com.alibaba.polardbx.common.constants.SequenceAttribute.Type;
import com.alibaba.polardbx.common.constants.TransactionAttribute;
import com.alibaba.polardbx.common.jdbc.BatchInsertPolicy;
import com.alibaba.polardbx.common.properties.ConfigParam;
import com.alibaba.polardbx.common.properties.ConnectionParams;
import com.alibaba.polardbx.common.properties.ConnectionProperties;
import com.alibaba.polardbx.common.properties.LongConfigParam;
import com.alibaba.polardbx.common.properties.ParamManager;
import com.alibaba.polardbx.common.properties.SystemPropertiesHelper;
import com.alibaba.polardbx.common.utils.logger.Logger;
import com.alibaba.polardbx.common.utils.logger.LoggerFactory;
import com.alibaba.polardbx.config.ConfigDataMode;
import com.alibaba.polardbx.config.InstanceRoleManager;
import com.alibaba.polardbx.executor.cursor.Cursor;
import com.alibaba.polardbx.executor.cursor.ExecutorCursor;
import com.alibaba.polardbx.executor.cursor.impl.ArrayResultCursor;
import com.alibaba.polardbx.executor.handler.HandlerCommon;
import com.alibaba.polardbx.executor.operator.FilterExec;
import com.alibaba.polardbx.executor.operator.ResultSetCursorExec;
import com.alibaba.polardbx.executor.spi.IRepository;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.TddlRelDataTypeSystemImpl;
import com.alibaba.polardbx.optimizer.core.TddlTypeFactoryImpl;
import com.alibaba.polardbx.optimizer.core.datatype.DataTypes;
import com.alibaba.polardbx.optimizer.core.expression.calc.IExpression;
import com.alibaba.polardbx.optimizer.core.rel.dal.LogicalShow;
import com.alibaba.polardbx.optimizer.core.row.Row;
import com.alibaba.polardbx.optimizer.sequence.SequenceManagerProxy;
import com.alibaba.polardbx.optimizer.utils.RelUtils;
import com.alibaba.polardbx.optimizer.utils.RexUtils;
import com.alibaba.polardbx.ssl.SSLVariables;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlShowVariables;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author chenmo.cm
 */
public class LogicalShowVariablesMyHandler extends HandlerCommon {

    private static final Logger logger = LoggerFactory.getLogger(LogicalShowVariablesMyHandler.class);

    public LogicalShowVariablesMyHandler(IRepository repo) {
        super(repo);
    }

    public void collectDnVariables(
        LogicalShow show, TreeMap<String, Object> variables, ExecutionContext executionContext) {
        //extract dn variables from cn
        if (executionContext.getServerVariables() != null) {
            for (Map.Entry<String, Object> entry : executionContext.getServerVariables().entrySet()) {
                variables.put(entry.getKey(), entry.getValue());
            }
        }
        if (executionContext.getExtraServerVariables() != null) {
            for (Map.Entry<String, Object> entry : executionContext.getExtraServerVariables().entrySet()) {
                variables.put(entry.getKey(), entry.getValue());
            }
        }

        //extract dn variables from mysql

        Cursor cursor = null;
        try {
            cursor = repo.getCursorFactory().repoCursor(executionContext, show);
            extractVariableFromCursor(variables, cursor);
        } finally {
            if (cursor != null) {
                cursor.close(new ArrayList<>());
            }
        }
    }

    public void collectCnVariables(TreeMap<String, Object> variables, ExecutionContext executionContext) {
        boolean showAllParams = executionContext.getParamManager().getBoolean(
            ConnectionParams.SHOW_ALL_PARAMS);
        if (showAllParams) {
            //show all cn params
            for (Map.Entry<String, ConfigParam> entry : ConnectionParams.SUPPORTED_PARAMS.entrySet()) {
                variables.put(entry.getKey().toLowerCase(Locale.ROOT),
                    executionContext.getParamManager().get(entry.getKey()));
            }
        }
        //show the cn params which is set in current session.
        if (executionContext.getConnection() != null) {
            Map<String, Object> objectMap = executionContext.getConnection().getConnectionVariables();
            if (objectMap != null) {
                for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                    variables.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
                }
            }
        }

        //show the cn params which must be show.
        variables.put(
            ConnectionProperties.GROUP_CONCAT_MAX_LEN.toLowerCase(Locale.ROOT),
            executionContext.getParamManager().getInt(ConnectionParams.GROUP_CONCAT_MAX_LEN));

        variables.put(
            ConnectionProperties.SQL_SELECT_LIMIT.toLowerCase(Locale.ROOT),
            executionContext.getParamManager().getLong(ConnectionParams.SQL_SELECT_LIMIT));

        // DRDS_TRANSACTION_POLICY
        variables.put(
            TransactionAttribute.DRDS_TRANSACTION_POLICY.toLowerCase(Locale.ROOT),
            executionContext.getConnection().getTrxPolicy().toString());

        // BATCH_INSERT_POLICY
        variables.put(
            BatchInsertPolicy.getVariableName().toLowerCase(Locale.ROOT),
            executionContext.getConnection().getBatchInsertPolicy(executionContext.getExtraCmds()).getName());

        // DRDS_INSTANCE_ROLE
        variables.put(
            InstanceRoleManager.INSTANCE_ROLE_VARIABLE.toLowerCase(Locale.ROOT),
            InstanceRoleManager.INSTANCE.getInstanceRole());

        // SHARE_READ_VIEW
        variables.put(
            TransactionAttribute.SHARE_READ_VIEW.toLowerCase(Locale.ROOT),
            executionContext.isShareReadView());
    }

    public void updateReturnVariables(TreeMap<String, Object> variables, ExecutionContext executionContext) {
        // For ssl configurations
        SSLVariables.fill(variables);

        if (variables.containsKey("max_allowed_packet")) {
            String maxAllowedPacket = System.getProperty("maxAllowedPacket", String.valueOf(1024 * 1024));
            String maxAllowedPacketCustom =
                String.valueOf(executionContext.getParamManager().getLong(ConnectionParams.MAX_ALLOWED_PACKET));
            if (StringUtils.isNotEmpty(maxAllowedPacketCustom)) {
                maxAllowedPacket = maxAllowedPacketCustom;
            }

            variables.put("max_allowed_packet", maxAllowedPacket);
        }

        if (variables.containsKey("max_user_connections")) {
            variables.put("max_user_connections", System.getProperty("maxConnection", "20000"));
        }

        if (variables.containsKey("max_connections")) {
            variables.put("max_connections", System.getProperty("maxConnection", "20000"));
        }

        if (variables.containsKey("autocommit")) {
            if (executionContext.isAutoCommit()) {
                variables.put("autocommit", "ON");
            } else {
                variables.put("autocommit", "OFF");
            }
        }

        if (variables.containsKey("read_only")) {
            if (ConfigDataMode.isMasterMode()) {
                variables.put("read_only", "OFF");
            } else if (ConfigDataMode.isSlaveMode()) {
                variables.put("read_only", "ON");
            }
        }

        boolean allSequencesGroupOrTime = SequenceManagerProxy.getInstance()
            .areAllSequencesSameType(executionContext.getSchemaName(), new Type[] {Type.GROUP, Type.TIME});

        if (variables.containsKey("auto_increment_increment") && allSequencesGroupOrTime) {
            // Since the steps of Group and Time-based Sequence are fixed to 1,
            // so we have to override auto_increment_increment set on RDS for
            // correct behavior of generated keys.
            variables.put("auto_increment_increment", 1);
        }

        // fill session variable
        if (variables.containsKey(ConnectionProperties.SQL_SELECT_LIMIT)) {
            variables.put(ConnectionProperties.SQL_SELECT_LIMIT.toLowerCase(Locale.ROOT),
                executionContext.getParamManager().getLong(ConnectionParams.SQL_SELECT_LIMIT));
        }

        // server_id , use values from session or inst_config, to override values from dn
        if (variables.containsKey("server_id")) {
            String key = ConnectionProperties.SERVER_ID.toLowerCase(Locale.ROOT);
            variables.put(key, executionContext.getParamManager().getLong(ConnectionParams.SERVER_ID));
            if (executionContext.getExtraServerVariables().containsKey(key)) {
                variables.put(key, executionContext.getExtraServerVariables().get(key));
            }
        }
    }

    @Override
    public Cursor handle(RelNode logicalPlan, ExecutionContext executionContext) {

        final LogicalShow show = (LogicalShow) logicalPlan;
        final TreeMap<String, Object> variables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        //extract dn variables
        collectDnVariables(show, variables, executionContext);
        //extract cn variables
        collectCnVariables(variables, executionContext);
        //modify the variables
        updateReturnVariables(variables, executionContext);

        ArrayResultCursor result = new ArrayResultCursor("Show Variables");
        result.addColumn("Variable_name", DataTypes.StringType);
        result.addColumn("Value", DataTypes.StringType);
        result.initMeta();

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result.addRow(new Object[] {entry.getKey(), entry.getValue()});
        }
        final SqlShowVariables showVariables = (SqlShowVariables) show.getNativeSqlNode();
        if (showVariables.like != null) {
            final String pattern = RelUtils.stringValue(showVariables.like);
            RexBuilder rexBuilder = new RexBuilder(new TddlTypeFactoryImpl(TddlRelDataTypeSystemImpl.getInstance()));
            RexNode likeCondition = rexBuilder.makeCall(
                SqlStdOperatorTable.LIKE,
                Arrays
                    .asList(rexBuilder.makeInputRef(rexBuilder.getTypeFactory().createSqlType(SqlTypeName.VARCHAR), 0),
                        rexBuilder.makeLiteral(pattern)));
            IExpression expression = RexUtils.buildRexNode(likeCondition, executionContext);

            FilterExec filterExec =
                new FilterExec(new ResultSetCursorExec(
                    result, executionContext, Long.MAX_VALUE), expression, null, executionContext);
            return new ExecutorCursor(filterExec, result.getMeta());
        }
        return result;
    }

    private void extractVariableFromCursor(TreeMap<String, Object> variables, Cursor cursor) {
        Row row;
        while ((row = cursor.next()) != null) {
            String variableName = row.getString(0);
            String variableValue = row.getString(1);
            variables.put(variableName, variableValue);
        }
    }
}
