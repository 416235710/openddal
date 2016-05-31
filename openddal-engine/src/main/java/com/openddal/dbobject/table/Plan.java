/*
 * Copyright 2014-2016 the original author or authors
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
package com.openddal.dbobject.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.openddal.command.expression.Expression;
import com.openddal.command.expression.ExpressionVisitor;
import com.openddal.dbobject.table.TableFilter.TableFilterVisitor;
import com.openddal.engine.Session;
import com.openddal.message.Trace;
import com.openddal.util.New;

/**
 * A possible query execution plan. The time required to execute a query depends
 * on the order the tables are accessed.
 */
public class Plan {

    private final TableFilter[] filters;
    private final HashMap<TableFilter, PlanItem> planItems = New.hashMap();
    private final Expression[] allConditions;
    private final TableFilter[] allFilters;

    /**
     * Create a query plan with the given order.
     *
     * @param filters the tables of the query
     * @param count the number of table items
     * @param condition the condition in the WHERE clause
     */
    public Plan(TableFilter[] filters, int count, Expression condition) {
        this.filters = new TableFilter[count];
        System.arraycopy(filters, 0, this.filters, 0, count);
        final ArrayList<Expression> allCond = New.arrayList();
        final ArrayList<TableFilter> all = New.arrayList();
        if (condition != null) {
            allCond.add(condition);
        }
        for (int i = 0; i < count; i++) {
            TableFilter f = filters[i];
            f.visit(new TableFilterVisitor() {
                @Override
                public void accept(TableFilter f) {
                    all.add(f);
                    if (f.getJoinCondition() != null) {
                        allCond.add(f.getJoinCondition());
                    }
                }
            });
        }
        allConditions = new Expression[allCond.size()];
        allCond.toArray(allConditions);
        allFilters = new TableFilter[all.size()];
        all.toArray(allFilters);
    }

    /**
     * Get the plan item for the given table.
     *
     * @param filter the table
     * @return the plan item
     */
    public PlanItem getItem(TableFilter filter) {
        return planItems.get(filter);
    }

    /**
     * The the list of tables.
     *
     * @return the list of tables
     */
    public TableFilter[] getFilters() {
        return filters;
    }

    /**
     * Remove all index conditions that can not be used.
     */
    public void removeUnusableIndexConditions() {
        for (int i = 0; i < allFilters.length; i++) {
            TableFilter f = allFilters[i];
            setEvaluatable(f, true);
            if (i < allFilters.length - 1 ||
                    f.getSession().getDatabase().getSettings().earlyFilter) {
                // the last table doesn't need the optimization,
                // otherwise the expression is calculated twice unnecessarily
                // (not that bad but not optimal)
                f.optimizeFullCondition(false);
            }
            f.removeUnusableIndexConditions();
        }
        for (TableFilter f : allFilters) {
            setEvaluatable(f, false);
        }
    }

    /**
     * Calculate the cost of this query plan.
     *
     * @param session the session
     * @return the cost
     */
    public double calculateCost(Session session) {
        Trace t = session.getTrace();
        if (t.isDebugEnabled()) {
            t.debug("Plan       : calculate cost for plan {0}", Arrays.toString(allFilters));
        }
        double cost = 1;
        boolean invalidPlan = false;
        for (int i = 0; i < allFilters.length; i++) {
            TableFilter tableFilter = allFilters[i];
            if (t.isDebugEnabled()) {
                t.debug("Plan       :   for table filter {0}", tableFilter);
            }
            PlanItem item = tableFilter.getBestPlanItem(session, allFilters, i);
            planItems.put(tableFilter, item);
            if (t.isDebugEnabled()) {
                t.debug("Plan       :   plan item cost {0} scanning strategy is {1}",
                        item.cost, item.getScanningStrategy());
            }
            cost += cost * item.cost;
            setEvaluatable(tableFilter, true);
            Expression on = tableFilter.getJoinCondition();
            if (on != null) {
                if (!on.isEverything(ExpressionVisitor.EVALUATABLE_VISITOR)) {
                    invalidPlan = true;
                    break;
                }
            }
        }
        if (invalidPlan) {
            cost = Double.POSITIVE_INFINITY;
        }
        if (t.isDebugEnabled()) {
            session.getTrace().debug("Plan       : plan cost {0}", cost);
        }
        for (TableFilter f : allFilters) {
            setEvaluatable(f, false);
        }
        return cost;
    }

    private void setEvaluatable(TableFilter filter, boolean b) {
        filter.setEvaluatable(filter, b);
        for (Expression e : allConditions) {
            e.setEvaluatable(filter, b);
        }
    }
}
