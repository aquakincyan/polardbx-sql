/*
 * Copyright 1999-2017 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.polardbx.druid.sql.ast.statement;

import com.alibaba.polardbx.druid.sql.ast.SQLName;
import com.alibaba.polardbx.druid.sql.ast.SQLObjectImpl;
import com.alibaba.polardbx.druid.sql.visitor.SQLASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class SQLAlterTableSetOption extends SQLObjectImpl implements SQLAlterTableItem {
    private List<SQLAssignItem> options = new ArrayList<SQLAssignItem>();
    private SQLName on;

    boolean isAlterTableGroup = false;
    boolean force = false;

    public SQLAlterTableSetOption() {

    }

    public List<SQLAssignItem> getOptions() {
        return options;
    }

    public void addOption(SQLAssignItem item) {
        item.setParent(this);
        this.options.add(item);
    }

    public boolean isAlterTableGroup() {
        return isAlterTableGroup;
    }

    public void setAlterTableGroup(boolean alterTableGroup) {
        isAlterTableGroup = alterTableGroup;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    @Override
    protected void accept0(SQLASTVisitor visitor) {
        if (visitor.visit(this)) {
            acceptChild(visitor, options);
            acceptChild(visitor, on);
        }
        visitor.endVisit(this);
    }

    public SQLName getOn() {
        return on;
    }

    public void setOn(SQLName x) {
        if (x != null) {
            x.setParent(this);
        }
        this.on = x;
    }
}
