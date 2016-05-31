package com.openddal.excutor.cursor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.openddal.command.dml.Select;
import com.openddal.config.TableRule;
import com.openddal.dbobject.index.IndexCondition;
import com.openddal.dbobject.table.Column;
import com.openddal.dbobject.table.Table;
import com.openddal.dbobject.table.TableFilter;
import com.openddal.dbobject.table.TableMate;
import com.openddal.engine.Constants;
import com.openddal.engine.Session;
import com.openddal.message.DbException;
import com.openddal.result.Row;
import com.openddal.result.SearchRow;
import com.openddal.util.New;
import com.openddal.util.StringUtils;

public class DirectLookupCursor implements Cursor {

    private Session session;
    private final Select select;

    private Cursor cursor;

    public DirectLookupCursor(Session session, Select select) {
        this.session = session;
        this.select = select;
    }

    @Override
    public Row get() {
        if (cursor == null) {
            return null;
        }
        return cursor.get();
    }

    @Override
    public SearchRow getSearchRow() {
        return cursor.getSearchRow();
    }

    @Override
    public boolean next() {
        while (true) {
            if (cursor == null) {
                nextCursor();
                if (cursor == null) {
                    return false;
                }
            }
            if (cursor.next()) {
                return true;
            }
            cursor = null;
        }
    }

    private void nextCursor() {

    }

    @Override
    public boolean previous() {
        throw DbException.throwInternalError();
    }

    public static boolean isDirectLookupQuery(Select select) {
        ArrayList<TableFilter> topFilters = select.getTopFilters();
        ArrayList<TableFilter> filters = New.arrayList(topFilters.size());
        for (TableFilter tf : topFilters) {
            if (!StringUtils.startsWith(tf.getTableAlias(), Constants.PREFIX_JOIN)) {
                filters.add(tf);
            }
        }
        for (TableFilter tf : filters) {
            if (!tf.isFromTableMate()) {
                return false;
            }
        }
        if (filters.size() == 1) {
            return true;
        } else {
            for (TableFilter tr : filters) {
                if (!isGropTableFilter(tr, filters)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isGropTableFilter(TableFilter filter, ArrayList<TableFilter> filters) {
        TableMate table1 = (TableMate) filter.getTable();
        for (TableFilter item : filters) {
            if (item == filter) {
                continue;
            }
            TableMate table2 = (TableMate) item.getTable();
            TableRule rule1 = table1.getTableRule();
            TableRule rule2 = table2.getTableRule();
            if (!rule1.isNodeComparable(rule2)) {
                return false;
            }
        }
        return true;
    }

    private static ArrayList<TableFilter> getShardingTableJoinChain(TableFilter[] filters, TableFilter filter) {
        ArrayList<TableFilter> result = New.arrayList();
        TableMate table1 = (TableMate) filter.getTable();
        Column[] columns1 = table1.getRuleColumns();
        if (columns1 == null) {
            throw new IllegalArgumentException("not sharding TableFilter");
        }
        ArrayList<IndexCondition> conditions = filter.getIndexConditions();
        int length = table1.getColumns().length;
        IndexCondition[] masks = new IndexCondition[length];
        for (IndexCondition condition : conditions) {
            int id = condition.getColumn().getColumnId();
            if (id >= 0) {
                masks[id] = condition;
            }
        }

        if (masks != null) {
            List<Column> joinCols = New.arrayList();
            for (int i = 0, len = columns1.length; i < len; i++) {
                Column column = columns1[i];
                int index = column.getColumnId();
                IndexCondition mask = masks[index];
                if ((mask.getMask(conditions) & IndexCondition.EQUALITY) == IndexCondition.EQUALITY) {
                    Column compareColumn = mask.getCompareColumn();
                    if (compareColumn != null) {
                        joinCols.add(compareColumn);
                    }
                    if (i == columns1.length - 1) {
                        Set<Table> tables = New.hashSet();
                        for (Column column2 : joinCols) {
                            tables.add(column2.getTable());
                        }
                        for (Table table : tables) {
                            if (!(table instanceof TableMate)) {
                                continue;
                            }
                            TableMate table2 = (TableMate) table;
                            Column[] columns2 = table2.getRuleColumns();
                            if (columns2 == null) {
                                continue;
                            }
                            boolean contains = true;
                            for (Column column2 : columns2) {
                                if(!joinCols.contains(column2)) {
                                    contains = false;
                                    break;
                                }
                            }
                            if(contains) {
                                result.add(filter);
                            }
                            for (TableFilter tf : filters) {
                                
                            }
                        }
                    }
                }
            }
        }

        return result;
    }
}
