/*
 * Copyright 2014-2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openddal.excutor.ddl;

import com.openddal.command.ddl.TruncateTable;
import com.openddal.dbobject.Right;
import com.openddal.dbobject.table.TableMate;
import com.openddal.route.rule.TableNode;

/**
 * @author <a href="mailto:jorgie.mail@gmail.com">jorgie li</a>
 */
public class TruncateTableExecutor extends DefineCommandExecutor<TruncateTable> {

    /**
     * @param prepared
     */
    public TruncateTableExecutor(TruncateTable prepared) {
        super(prepared);
    }


    @Override
    public int executeUpdate() {
        TableMate table = castTableMate(prepared.getTable());
        session.commit(true);
        session.getUser().checkRight(table, Right.DELETE);
        TableNode[] nodes = table.getPartitionNode();
        execute(nodes);
        return 0;

    }


    @Override
    protected String doTranslate(TableNode node) {
        String forTable = node.getCompositeObjectName();
        StringBuilder sql = new StringBuilder();
        sql.append("TRUNCATE TABLE ");
        sql.append(identifier(forTable));
        return sql.toString();
    }

}