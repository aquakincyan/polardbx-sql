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
package org.apache.calcite.sql.type;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rules that determine whether a type is assignable from another type.
 */
public class SqlTypeAssignmentRule implements SqlTypeMappingRule {
  //~ Static fields/initializers ---------------------------------------------

  private static final SqlTypeAssignmentRule INSTANCE;

  //~ Instance fields --------------------------------------------------------

  private final Map<SqlTypeName, ImmutableSet<SqlTypeName>> map;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a {@code SqlTypeAssignmentRules} with specified type mappings {@code map}.
   * <p>Make this constructor private intentionally, use {@link #instance()}.
   * @param map The type mapping, for each map entry, the values types can be assigned to
   *            the key type
   */
  private SqlTypeAssignmentRule(
      Map<SqlTypeName, ImmutableSet<SqlTypeName>> map) {
    this.map = ImmutableMap.copyOf(map);
  }

  static {
    final SqlTypeMappingRules.Builder rules = SqlTypeMappingRules.builder();

    final Set<SqlTypeName> rule = new HashSet<>();

    // IntervalYearMonth is assignable from...
    for (SqlTypeName interval : SqlTypeName.YEAR_INTERVAL_TYPES) {
      rules.add(interval, SqlTypeName.YEAR_INTERVAL_TYPES);
    }
    for (SqlTypeName interval : SqlTypeName.DAY_INTERVAL_TYPES) {
      rules.add(interval, SqlTypeName.DAY_INTERVAL_TYPES);
    }
    for (SqlTypeName interval : SqlTypeName.DAY_INTERVAL_TYPES) {
      final Set<SqlTypeName> dayIntervalTypes = SqlTypeName.DAY_INTERVAL_TYPES;
      rules.add(interval, dayIntervalTypes);
    }

    // YEAR is assignable from INT_TYPES
    rule.clear();
    rule.addAll(SqlTypeName.INT_TYPES);
    rule.add(SqlTypeName.YEAR);
    rules.add(SqlTypeName.YEAR, rule);

    // BIG_BIT is assignable from ...
    rule.clear();
    rules.add(SqlTypeName.BIG_BIT, EnumSet.of(SqlTypeName.BIG_BIT, SqlTypeName.BIGINT));

    // BIT is assignable from ...
    rule.clear();
    rule.add(SqlTypeName.BIT);
    rule.add(SqlTypeName.INTEGER);
    rule.add(SqlTypeName.BIGINT);
    rules.add(SqlTypeName.BIT, rule);

    // MULTISET is assignable from...
    rules.add(SqlTypeName.MULTISET, EnumSet.of(SqlTypeName.MULTISET));

    // TINYINT is assignable from...
    rules.add(SqlTypeName.TINYINT, EnumSet.of(SqlTypeName.TINYINT, SqlTypeName.BIGINT));

    // TINYINT_UNSIGNED is assignable from...
    rule.clear();
    rule.add(SqlTypeName.TINYINT);
    rule.add(SqlTypeName.TINYINT_UNSIGNED);
    rule.add(SqlTypeName.BIGINT);
    rules.add(SqlTypeName.TINYINT_UNSIGNED, rule);

    // SMALLINT is assignable from...
    rule.add(SqlTypeName.SMALLINT);
    rules.add(SqlTypeName.SMALLINT, rule);
    rule.add(SqlTypeName.SMALLINT_UNSIGNED);
    rule.add(SqlTypeName.BIGINT);
    rules.add(SqlTypeName.SMALLINT_UNSIGNED, rule);

    // MEDIUMINT is assignable from...
    rule.add(SqlTypeName.MEDIUMINT);
    rules.add(SqlTypeName.MEDIUMINT, rule);
    rule.add(SqlTypeName.MEDIUMINT_UNSIGNED);
    rule.add(SqlTypeName.BIGINT);
    rules.add(SqlTypeName.MEDIUMINT_UNSIGNED, rule);

    // INTEGER is assignable from...
    rule.add(SqlTypeName.INTEGER);
    rule.add(SqlTypeName.YEAR);
    rule.add(SqlTypeName.BIGINT);
    rules.add(SqlTypeName.INTEGER, rule);
    rule.add(SqlTypeName.INTEGER_UNSIGNED);
    rules.add(SqlTypeName.INTEGER_UNSIGNED, rule);

    // BIGINT is assignable from...
    rule.add(SqlTypeName.BIGINT);
    rules.add(SqlTypeName.BIGINT, rule);
    rule.add(SqlTypeName.BIGINT_UNSIGNED);
    rules.add(SqlTypeName.BIGINT_UNSIGNED, rule);

    // SIGNED is assignable from...
    rule.add(SqlTypeName.SIGNED);
    rule.add(SqlTypeName.UNSIGNED);
    rules.add(SqlTypeName.SIGNED, rule);
    rules.add(SqlTypeName.UNSIGNED, rule);

    // FLOAT (up to 64 bit floating point) is assignable from...
    rule.clear();
    rule.add(SqlTypeName.TINYINT);
    rule.add(SqlTypeName.SMALLINT);
    rule.add(SqlTypeName.INTEGER);
    rule.add(SqlTypeName.BIGINT);
    rule.add(SqlTypeName.DECIMAL);
    rule.add(SqlTypeName.FLOAT);
    rules.add(SqlTypeName.FLOAT, rule);

    // REAL (32 bit floating point) is assignable from...
    rule.clear();
    rule.add(SqlTypeName.TINYINT);
    rule.add(SqlTypeName.SMALLINT);
    rule.add(SqlTypeName.INTEGER);
    rule.add(SqlTypeName.BIGINT);
    rule.add(SqlTypeName.DECIMAL);
    rule.add(SqlTypeName.FLOAT);
    rule.add(SqlTypeName.REAL);
    rules.add(SqlTypeName.REAL, rule);

    // DOUBLE is assignable from...
    rule.clear();
    rule.add(SqlTypeName.TINYINT);
    rule.add(SqlTypeName.SMALLINT);
    rule.add(SqlTypeName.INTEGER);
    rule.add(SqlTypeName.BIGINT);
    rule.add(SqlTypeName.DECIMAL);
    rule.add(SqlTypeName.FLOAT);
    rule.add(SqlTypeName.REAL);
    rule.add(SqlTypeName.DOUBLE);
    rules.add(SqlTypeName.DOUBLE, rule);

    // DECIMAL is assignable from...
    rule.clear();
    rule.add(SqlTypeName.TINYINT);
    rule.add(SqlTypeName.SMALLINT);
    rule.add(SqlTypeName.INTEGER);
    rule.add(SqlTypeName.BIGINT);
    rule.add(SqlTypeName.REAL);
    rule.add(SqlTypeName.DOUBLE);
    rule.add(SqlTypeName.DECIMAL);
    rules.add(SqlTypeName.DECIMAL, rule);

    // VARBINARY is assignable from...
    rule.clear();
    rule.add(SqlTypeName.VARBINARY);
    rule.add(SqlTypeName.BINARY);
    rule.add(SqlTypeName.BLOB);
    rules.add(SqlTypeName.VARBINARY, rule);

    // CHAR is assignable from...
    rules.add(SqlTypeName.CHAR, EnumSet.of(SqlTypeName.CHAR));

    // VARCHAR is assignable from...
    rule.clear();
    rule.add(SqlTypeName.CHAR);
    rule.add(SqlTypeName.VARCHAR);
    rules.add(SqlTypeName.VARCHAR, rule);

    // BOOLEAN is assignable from...
    rules.add(SqlTypeName.BOOLEAN, EnumSet.of(SqlTypeName.BOOLEAN));

    // BINARY is assignable from...
    rule.clear();
    rule.add(SqlTypeName.BINARY);
    rule.add(SqlTypeName.VARBINARY);
    rule.add(SqlTypeName.BLOB);
    rules.add(SqlTypeName.BINARY, rule);

    // Blob is assignable from...
    rule.clear();
    rule.add(SqlTypeName.BINARY);
    rule.add(SqlTypeName.VARBINARY);
    rule.add(SqlTypeName.BLOB);
    rules.add(SqlTypeName.BLOB, rule);

    // DATE is assignable from...
    rule.clear();
    rule.add(SqlTypeName.DATE);
    rule.add(SqlTypeName.TIMESTAMP);
    rules.add(SqlTypeName.DATE, rule);

    // TIME is assignable from...
    rule.clear();
    rule.add(SqlTypeName.TIME);
    rule.add(SqlTypeName.TIMESTAMP);
    rules.add(SqlTypeName.TIME, rule);

    // TIME WITH LOCAL TIME ZONE is assignable from...
    rules.add(SqlTypeName.TIME_WITH_LOCAL_TIME_ZONE,
        EnumSet.of(SqlTypeName.TIME_WITH_LOCAL_TIME_ZONE));

    // TIMESTAMP is assignable from ...
    rules.add(SqlTypeName.TIMESTAMP, EnumSet.of(SqlTypeName.TIMESTAMP));

    // TIMESTAMP WITH LOCAL TIME ZONE is assignable from...
    rules.add(SqlTypeName.TIMESTAMP_WITH_LOCAL_TIME_ZONE,
        EnumSet.of(SqlTypeName.TIMESTAMP_WITH_LOCAL_TIME_ZONE));

    // GEOMETRY is assignable from ...
    rules.add(SqlTypeName.GEOMETRY, EnumSet.of(SqlTypeName.GEOMETRY));

    // ARRAY is assignable from ...
    rules.add(SqlTypeName.ARRAY, EnumSet.of(SqlTypeName.ARRAY));

    // ANY is assignable from ...
    rule.clear();
    rule.add(SqlTypeName.TINYINT);
    rule.add(SqlTypeName.TINYINT_UNSIGNED);
    rule.add(SqlTypeName.SMALLINT);
    rule.add(SqlTypeName.SMALLINT_UNSIGNED);
    rule.add(SqlTypeName.MEDIUMINT);
    rule.add(SqlTypeName.MEDIUMINT_UNSIGNED);
    rule.add(SqlTypeName.INTEGER);
    rule.add(SqlTypeName.INTEGER_UNSIGNED);
    rule.add(SqlTypeName.SIGNED);
    rule.add(SqlTypeName.UNSIGNED);
    rule.add(SqlTypeName.YEAR);
    rule.add(SqlTypeName.BIGINT);
    rule.add(SqlTypeName.BIGINT_UNSIGNED);
    rule.add(SqlTypeName.DECIMAL);
    rule.add(SqlTypeName.FLOAT);
    rule.add(SqlTypeName.REAL);
    rule.add(SqlTypeName.TIME);
    rule.add(SqlTypeName.DATE);
    rule.add(SqlTypeName.TIMESTAMP);
    rules.add(SqlTypeName.ANY, rule);

    INSTANCE = new SqlTypeAssignmentRule(rules.map);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns an instance.
   */
  public static SqlTypeAssignmentRule instance() {
    return INSTANCE;
  }

  @Override
  public Map<SqlTypeName, ImmutableSet<SqlTypeName>> getTypeMapping() {
    return this.map;
  }
}

// End SqlTypeAssignmentRule.java
