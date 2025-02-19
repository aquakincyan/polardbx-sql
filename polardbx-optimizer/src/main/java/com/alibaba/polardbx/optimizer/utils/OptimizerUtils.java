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

package com.alibaba.polardbx.optimizer.utils;

import com.alibaba.polardbx.common.exception.NotSupportException;
import com.alibaba.polardbx.common.jdbc.ParameterContext;
import com.alibaba.polardbx.common.jdbc.ParameterMethod;
import com.alibaba.polardbx.common.jdbc.Parameters;
import com.alibaba.polardbx.common.jdbc.RawString;
import com.alibaba.polardbx.common.utils.ExecutorMode;
import com.alibaba.polardbx.config.ConfigDataMode;
import com.alibaba.polardbx.druid.sql.ast.SqlType;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.planner.Planner;
import com.alibaba.polardbx.optimizer.core.rel.LogicalView;
import com.alibaba.polardbx.optimizer.parse.bean.PreparedParamRef;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelVisitor;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.logical.LogicalSemiJoin;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.util.Util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.alibaba.polardbx.common.jdbc.ITransactionPolicy.TransactionClass.EXPLICIT_TRANSACTION;
import static com.alibaba.polardbx.common.jdbc.ITransactionPolicy.TransactionClass.SUPPORT_SHARE_READVIEW_TRANSACTION;

/**
 * @since 5.0.0
 */
public class OptimizerUtils {
    public static final String EMPTY_KEY = "NO_IN_EXPR";

    public static Date parseDate(String str, String[] parsePatterns) throws ParseException {
        try {
            return parseDate(str, parsePatterns, Locale.ENGLISH);
        } catch (ParseException e) {
            return parseDate(str, parsePatterns, Locale.getDefault());
        }
    }

    public static String buildInExprKey(Map<Integer, ParameterContext> currentParameter) {
        StringBuilder key = new StringBuilder();
        for (int i = 1; i <= currentParameter.size(); i++) {
            ParameterContext pc = currentParameter.get(i);
            if (pc != null && pc.getValue() instanceof RawString) {
                RawString rawString = (RawString) pc.getValue();
                key.append(rawString.size()).append(":");
            }
        }
        if (key.length() > 1) {
            key.setLength(key.length() - 1);
            return key.toString();
        } else {
            return EMPTY_KEY;
        }
    }

    public static String buildInExprKey(ExecutionContext ec) {
        if (ec.getParams() == null) {
            return EMPTY_KEY;
        }
        return buildInExprKey(ec.getParams().getCurrentParameter());
    }

    public static Date parseDate(String str, String[] parsePatterns, Locale locale) throws ParseException {
        if ((str == null) || (parsePatterns == null)) {
            throw new IllegalArgumentException("Date and Patterns must not be null");
        }

        SimpleDateFormat parser = null;
        ParsePosition pos = new ParsePosition(0);

        for (int i = 0; i < parsePatterns.length; i++) {
            if (i == 0) {
                parser = new SimpleDateFormat(parsePatterns[0], locale);
            } else {
                parser.applyPattern(parsePatterns[i]);
            }
            pos.setIndex(0);
            Date date = parser.parse(str, pos);
            if ((date != null) && (pos.getIndex() == str.length())) {
                return date;
            }
        }

        throw new NotSupportException("Unable to parse the date: " + str);
    }

    public static Map<Integer, ParameterContext> buildParam(List<?> params) {
        return buildParam(params, null);
    }

    public static Map<Integer, ParameterContext> buildParam(List<?> params, ExecutionContext executionContext) {
        Int2ObjectOpenHashMap<ParameterContext> newParam = new Int2ObjectOpenHashMap<>();
        if (params == null) {
            return newParam;
        }
        for (int i = 0, j = 1; i < params.size(); i++, j++) {
            Object o = params.get(i);
            if (executionContext != null && executionContext.isExecutingPreparedStmt()) {
                if (o instanceof PreparedParamRef) {
                    o = ((PreparedParamRef) o).getValue();
                } else {
                    o = Planner.processSingleParam(i, o, executionContext);
                }
            }
            ParameterContext pc = new ParameterContext(getParameterMethod(o), new Object[] {j, o});
            newParam.put(j, pc);
        }
        return newParam;
    }

    /**
     * Batch prepare 参数构建
     */
    public static List<Map<Integer, ParameterContext>> buildBatchParam(List<Object> params,
                                                                       ExecutionContext executionContext) {
        List<Map<Integer, ParameterContext>> batchParameters = new ArrayList<>();
        for (Map<Integer, ParameterContext> oldMap : executionContext.getParams().getBatchParameters()) {
            Map<Integer, ParameterContext> newMap = new HashMap<>();
            for (int i = 0, j = 1; i < params.size(); i++, j++) {
                if (params.get(i) instanceof PreparedParamRef) {
                    PreparedParamRef preparedParamRef = (PreparedParamRef) params.get(i);
                    int index = preparedParamRef.getIndex() + 1;
                    ParameterContext oldCtx = oldMap.get(index);
                    ParameterContext newCtx =
                        new ParameterContext(oldCtx.getParameterMethod(), new Object[] {j, oldCtx.getValue()});
                    newMap.put(j, newCtx);
                } else {
                    Object o = params.get(i);
                    o = Planner.processSingleParam(i, o, executionContext);
                    ParameterContext pc = new ParameterContext(getParameterMethod(o), new Object[] {j, o});
                    newMap.put(j, pc);
                }
            }
            batchParameters.add(newMap);
        }
        return batchParameters;
    }

    public static ParameterMethod getParameterMethod(Object v) {
        if (v instanceof String) {
            return ParameterMethod.setString;
        } else if (v instanceof Boolean) {
            return ParameterMethod.setBoolean;
        } else {
            return ParameterMethod.setObject1;
        }
    }

    public static boolean supportedSqlKind(SqlNode ast) {
        switch (ast.getKind()) {
        case SELECT:
        case UNION:
        case INTERSECT:
        case EXCEPT:
        case ORDER_BY:
        case INSERT:
        case REPLACE:
        case UPDATE:
        case DELETE:
        case CREATE_MATERIALIZED_VIEW:
        case DROP_MATERIALIZED_VIEW:
        case REFRESH_MATERIALIZED_VIEW:
        case CREATE_VIEW:
        case DROP_VIEW:
        case CREATE_TABLE:
        case DROP_TABLE:
        case CREATE_INDEX:
        case DROP_INDEX:
        case RENAME_TABLE:
        case ALTER_INDEX:
        case ALTER_TABLE:
        case TRUNCATE_TABLE:
        case ALTER_SEQUENCE:
        case CREATE_SEQUENCE:
        case DROP_SEQUENCE:
        case FLASHBACK_TABLE:
        case PURGE:
        case RENAME_SEQUENCE:
        case EXPLAIN:
        case ALTER_RULE:
        case CREATE_DATABASE:
        case DROP_DATABASE:
        case CREATE_JAVA_FUNCTION:
        case DROP_JAVA_FUNCTION:
        case CHANGE_CONSENSUS_ROLE:
        case ALTER_SYSTEM_SET_CONFIG:
        case LOCK_TABLE:
        case CREATE_TRIGGER:
        case DROP_TRIGGER:
        case PUSH_DOWN_UDF:
        case CREATE_FUNCTION:
        case DROP_FUNCTION:
        case ALTER_FUNCTION:
        case ALTER_PROCEDURE:
        case CREATE_PROCEDURE:
        case DROP_PROCEDURE:
        case WITH:
        case WITH_ITEM:
        case ALTER_TABLEGROUP:
        case CREATE_TABLEGROUP:
        case DROP_TABLEGROUP:
        case UNARCHIVE:
        case ALTER_TABLE_SET_TABLEGROUP:
        case REFRESH_TOPOLOGY:
        case CREATE_SCHEDULE:
        case DROP_SCHEDULE:
        case ALTER_FILESTORAGE:
        case DROP_FILESTORAGE:
        case CREATE_FILESTORAGE:
        case PAUSE_SCHEDULE:
        case CONTINUE_SCHEDULE:
        case FIRE_SCHEDULE:
        case CREATE_JOINGROUP:
        case DROP_JOINGROUP:
        case ALTER_JOINGROUP:
        case MERGE_TABLEGROUP:
        case ALTER_DATABASE:
        case CREATE_STORAGE_POOL:
        case DROP_STORAGE_POOL:
        case ALTER_STORAGE_POOL:
        case INSPECT_INDEX:
            return true;
        default:
            if (ast.isA(SqlKind.DAL)) {
                return true;
            }
            return false;
        }
    }

    public static boolean findRexSubquery(RelNode rootRel) {
        class RexSubqueryParamFinder extends RelVisitor {
            @Override
            public void visit(RelNode node, int ordinal, RelNode parent) {
                if (node instanceof LogicalFilter) {
                    if (RexUtil.hasSubQuery(((LogicalFilter) node).getCondition())) {
                        throw Util.FoundOne.NULL;
                    }
                } else if (node instanceof LogicalProject) {
                    if (((LogicalProject) node).getProjects().stream().anyMatch(rex -> RexUtil.hasSubQuery(rex))) {
                        throw Util.FoundOne.NULL;
                    }
                } else if (node instanceof LogicalSemiJoin) {
                    throw Util.FoundOne.NULL;
                }
                super.visit(node, ordinal, parent);
            }

            boolean run(RelNode node) {
                try {
                    go(node);
                    return false;
                } catch (Util.FoundOne e) {
                    return true;
                }
            }
        }

        return new RexSubqueryParamFinder().run(rootRel);
    }

    public static List<RexDynamicParam> findSubquery(RelNode rootRel) {
        class RelDynamicParamFinder extends RelVisitor {
            private List<RexDynamicParam> scalar = Lists.newArrayList();

            @Override
            public void visit(RelNode node, int ordinal, RelNode parent) {
                if (node instanceof LogicalFilter) {
                    DynamicDeepFinder dynamicDeepFinder = new DynamicDeepFinder(scalar);
                    ((LogicalFilter) node).getCondition().accept(dynamicDeepFinder);
                } else if (node instanceof LogicalProject) {
                    for (RexNode r : ((LogicalProject) node).getProjects()) {
                        DynamicDeepFinder dynamicDeepFinder = new DynamicDeepFinder(scalar);
                        r.accept(dynamicDeepFinder);
                    }
                }
                super.visit(node, ordinal, parent);
            }

            List<RexDynamicParam> run(RelNode node) {
                go(node);
                return scalar;
            }
        }

        return new RelDynamicParamFinder().run(rootRel);
    }

    public static boolean findSemiJoin(RelNode rootRel) {
        class RelSemiJoinFinder extends RelVisitor {
            private boolean semiJoin = false;

            @Override
            public void visit(RelNode node, int ordinal, RelNode parent) {
                if (node instanceof LogicalSemiJoin) {
                    semiJoin = true;
                }
                if (!semiJoin) {
                    super.visit(node, ordinal, parent);
                }
            }

            boolean run(RelNode node) {
                go(node);
                return semiJoin;
            }
        }

        return new RelSemiJoinFinder().run(rootRel);
    }

    static public class DynamicDeepFinder extends RexVisitorImpl<Void> {
        private List<RexDynamicParam> scalar;

        public DynamicDeepFinder(List<RexDynamicParam> scalar) {
            super(true);
            this.scalar = scalar;
        }

        @Override
        public Void visitDynamicParam(RexDynamicParam dynamicParam) {
            if (dynamicParam.getIndex() == -2 || dynamicParam.getIndex() == -3) {
                scalar.add(dynamicParam);
            }
            return null;
        }

        public List<RexDynamicParam> getScalar() {
            return scalar;
        }
    }

    public static boolean hasSubquery(RelNode rootRel) {
        class SubqueryFinder extends RelVisitor {

            private List<RexDynamicParam> scalar = Lists.newArrayList();

            @Override
            public void visit(RelNode node, int ordinal, RelNode parent) {

                if (node == null) {
                    return;
                }

                if (node instanceof LogicalFilter) {
                    DynamicDeepFinder dynamicDeepFinder = new DynamicDeepFinder(scalar);
                    ((LogicalFilter) node).getCondition().accept(dynamicDeepFinder);
                } else if (node instanceof LogicalProject) {
                    for (RexNode r : ((LogicalProject) node).getProjects()) {
                        DynamicDeepFinder dynamicDeepFinder = new DynamicDeepFinder(scalar);
                        r.accept(dynamicDeepFinder);
                    }
                } else if (node instanceof LogicalView) {
                    if (((LogicalView) node).getCorrelateVariableScalar().size() > 0) {
                        throw Util.FoundOne.NULL;
                    }
                }
                if (scalar.size() > 0) {
                    throw Util.FoundOne.NULL;
                }

                super.visit(node, ordinal, parent);
            }

            boolean run(RelNode node) {
                try {
                    go(node);
                    return false;
                } catch (Util.FoundOne e) {
                    return true;
                }
            }
        }

        return new SubqueryFinder().run(rootRel);
    }

    public static boolean hasApply(RelNode rootRel) {
        class SubqueryFinder extends RelVisitor {

            private List<RexDynamicParam> scalar = Lists.newArrayList();

            @Override
            public void visit(RelNode node, int ordinal, RelNode parent) {

                if (node == null) {
                    return;
                }

                if (node instanceof LogicalFilter) {
                    DynamicDeepFinder dynamicDeepFinder = new DynamicDeepFinder(scalar);
                    ((LogicalFilter) node).getCondition().accept(dynamicDeepFinder);
                } else if (node instanceof LogicalProject) {
                    for (RexNode r : ((LogicalProject) node).getProjects()) {
                        DynamicDeepFinder dynamicDeepFinder = new DynamicDeepFinder(scalar);
                        r.accept(dynamicDeepFinder);
                    }
                } else if (node instanceof LogicalView) {
                    if (((LogicalView) node).getScalarList().size() > 0) {
                        throw Util.FoundOne.NULL;
                    }
                }
                if (scalar.size() > 0) {
                    throw Util.FoundOne.NULL;
                }

                super.visit(node, ordinal, parent);
            }

            boolean run(RelNode node) {
                try {
                    go(node);
                    return false;
                } catch (Util.FoundOne e) {
                    return true;
                }
            }
        }

        return new SubqueryFinder().run(rootRel);
    }

    public static boolean allowMultipleReadConns(ExecutionContext context, LogicalView logicalView) {
        boolean ret = useExplicitTransaction(context);
        if (ret) {
            boolean shareReadView = context.isShareReadView() && context.getTransaction().
                getTransactionClass().isA(SUPPORT_SHARE_READVIEW_TRANSACTION);
            if (!shareReadView && !context.isAutoCommit()) {
                return false;
            } else {
                if (!isSelectQuery(context)) {
                    return false;
                }
                if (logicalView != null) {
                    return ((IDistributedTransaction) context.getTransaction()).allowMultipleReadConns()
                        && logicalView.getLockMode() == SqlSelect.LockMode.UNDEF;
                } else {
                    return ((IDistributedTransaction) context.getTransaction()).allowMultipleReadConns();
                }
            }
        } else {
            return true;
        }
    }

    public static boolean isSelectQuery(ExecutionContext context) {
        if (context.getFinalPlan() == null || context.getFinalPlan().getAst() == null) {
            return context.getSqlType() == SqlType.SELECT;
        } else {
            return context.getFinalPlan().getAst().getKind().belongsTo(SqlKind.QUERY);
        }
    }

    public static boolean useExplicitTransaction(ExecutionContext context) {
        //Autocommit is true, but the GSI must be in transaction.
        boolean ret = context.getTransaction().getTransactionClass().isA(EXPLICIT_TRANSACTION);
        return ret && ConfigDataMode.isMasterMode() && !isMppMode(context);
    }

    private static boolean isMppMode(ExecutionContext context) {
        return context.getExecuteMode() == ExecutorMode.MPP;
    }
}
