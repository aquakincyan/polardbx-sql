/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.rel.logical;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelInput;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.List;

/**
 * A <code>LogicalTableScan</code> reads all the rows from a
 * {@link RelOptTable}.
 *
 * <p>If the table is a <code>net.sf.saffron.ext.JdbcTable</code>, then this is
 * literally possible. But for other kinds of tables, there may be many ways to
 * read the data from the table. For some kinds of table, it may not even be
 * possible to read all of the rows unless some narrowing constraint is applied.
 *
 * <p>In the example of the <code>net.sf.saffron.ext.ReflectSchema</code>
 * schema,</p>
 *
 * <blockquote>
 * <pre>select from fields</pre>
 * </blockquote>
 *
 * <p>cannot be implemented, but</p>
 *
 * <blockquote>
 * <pre>select from fields as f
 * where f.getClass().getName().equals("java.lang.String")</pre>
 * </blockquote>
 *
 * <p>can. It is the optimizer's responsibility to find these ways, by applying
 * transformation rules.</p>
 */
public final class LogicalTableScan extends TableScan {
  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a LogicalTableScan.
   *
   * <p>Use {@link #create} unless you know what you're doing.
   */
  public LogicalTableScan(RelOptCluster cluster, RelTraitSet traitSet,
      RelOptTable table) {
    super(cluster, traitSet, table);
  }

  public LogicalTableScan(RelOptCluster cluster, RelTraitSet traitSet,
                          RelOptTable table, SqlNodeList hints) {
    super(cluster, traitSet, table, hints);
  }

  public LogicalTableScan(RelOptCluster cluster, RelTraitSet traitSet,
                          RelOptTable table, SqlNodeList hints, SqlNode indexNode, RexNode flashback,
                          SqlNode partitions) {
    super(cluster, traitSet, table, hints, indexNode, flashback, partitions);
  }

  @Deprecated // to be removed before 2.0
  public LogicalTableScan(RelOptCluster cluster, RelOptTable table) {
    this(cluster, cluster.traitSetOf(Convention.NONE), table);
  }

  /**
   * Creates a LogicalTableScan by parsing serialized output.
   */
  public LogicalTableScan(RelInput input) {
    super(input);
  }

  @Override public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    assert traitSet.containsIfApplicable(Convention.NONE);
    assert inputs.isEmpty();
    return this;
  }

  /** Creates a LogicalTableScan.
   *
   * @param cluster Cluster
   * @param relOptTable Table
   */
  public static LogicalTableScan create(RelOptCluster cluster,
                                        final RelOptTable relOptTable) {
      return create(cluster, relOptTable, new SqlNodeList(SqlParserPos.ZERO), null, null, null);
  }

  public static LogicalTableScan create(RelOptCluster cluster,
                                        final RelOptTable relOptTable, SqlNodeList hints, SqlNode indexNode,
                                        RexNode flashback, SqlNode partitions) {

    final Table table = relOptTable.unwrap(Table.class);
    final RelTraitSet traitSet =
        cluster.traitSetOf(Convention.NONE)
            .replaceIfs(RelCollationTraitDef.INSTANCE,
                (Supplier<List<RelCollation>>) () -> {
                  if (table != null) {
                    return table.getStatistic().getCollations();
                  }
                  return ImmutableList.of();
                }).simplify();
    return new LogicalTableScan(cluster, traitSet, relOptTable, hints, indexNode, flashback, partitions);
  }


}

// End LogicalTableScan.java
