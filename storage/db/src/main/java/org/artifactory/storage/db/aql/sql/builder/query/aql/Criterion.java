/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.storage.db.aql.sql.builder.query.aql;

import org.artifactory.aql.AqlException;
import org.artifactory.aql.model.*;
import org.artifactory.storage.db.aql.sql.builder.query.sql.AqlToSqlQueryBuilderException;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;
import org.artifactory.storage.db.aql.sql.model.AqlFieldExtensionEnum;

import java.util.List;

import static org.artifactory.storage.db.aql.sql.model.SqlTableEnum.build_props;
import static org.artifactory.storage.db.aql.sql.model.SqlTableEnum.node_props;

/**
 * Abstract class that represent single criteria (field comparator and value).
 *
 * @author Gidi Shabat
 */
public abstract class Criterion implements AqlQueryElement {

    private static final String ESCAPING_CHAR = "^";

    private List<AqlDomainEnum> subDomains;
    private AqlVariable variable1;
    private String comparatorName;
    private AqlVariable variable2;
    private SqlTable table1;
    private SqlTable table2;
    private boolean mspOperator;

    public Criterion(List<AqlDomainEnum> subDomains, AqlVariable variable1, SqlTable table1, String comparatorName,
            AqlVariable variable2, SqlTable table2, boolean mspOperator) {
        this.subDomains = subDomains;
        this.variable1 = variable1;
        this.table1 = table1;
        this.comparatorName = comparatorName;
        this.variable2 = variable2;
        this.table2 = table2;
        this.mspOperator = mspOperator;
    }

    public AqlVariable getVariable1() {
        return variable1;
    }

    public String getComparatorName() {
        return comparatorName;
    }

    public AqlVariable getVariable2() {
        return variable2;
    }

    public SqlTable getTable1() {
        return table1;
    }

    public SqlTable getTable2() {
        return table2;
    }

    public boolean isMspOperator() {
        return mspOperator;
    }

    public List<AqlDomainEnum> getSubDomains() {
        return subDomains;
    }

    public abstract String toSql(List<Object> params) throws AqlException;

    @Override
    public boolean isOperator() {
        return false;
    }

    protected String isNotNullSql(String index, AqlField variable) {
        AqlFieldEnum fieldEnum = variable.getFieldEnum();
        AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor((AqlPhysicalFieldEnum) fieldEnum);
        return (index != null ? index : "") + extension.tableField.name() + " is not null";
    }

    protected String isNullSql(AqlField variable) {
        AqlFieldEnum fieldEnum = variable.getFieldEnum();
        AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor((AqlPhysicalFieldEnum) fieldEnum);
        return extension.tableField.name() + " is null";
    }

    protected String isNullSql(String index, AqlField variable) {
        AqlFieldEnum fieldEnum = variable.getFieldEnum();
        AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor((AqlPhysicalFieldEnum) fieldEnum);
        return (index != null ? index : "") + extension.tableField.name() + " is null";
    }

    protected String isNullSql(String index, String fieldName) {
        return (index != null ? index : "") + fieldName + " is null";
    }

    protected String fieldToSql(String index, AqlVariable variable) {
        AqlFieldEnum fieldEnum = ((AqlField) variable).getFieldEnum();
        AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor((AqlPhysicalFieldEnum) fieldEnum);
        return (index != null ? index : "") + extension.tableField.name();
    }

    protected String fieldToSql(String index, String fieldName) {
        return (index != null ? index : "") + fieldName;
    }

    protected String paramToSql(AqlVariable variable) {
        return "?";
    }

    protected Object resolveParam(AqlVariable variable) throws AqlException {
        AqlValue value = (AqlValue) variable;
        return value.toObject();
    }

    public String createSqlCriteria(AqlComparatorEnum comparatorEnum, AqlVariable variable1, SqlTable table1,
            AqlVariable variable2, List<Object> params) {
        String index1 = table1 != null && variable1 instanceof AqlField ? table1.getAlias() : "";
        switch (comparatorEnum) {
            case equals: {
                return generateEqualsQuery(variable1, variable2, params, index1);
            }
            case matches: {
                return generateMatchQuery(variable1, variable2, params, index1);
            }
            case notMatches: {
                return generateNotMatchQuery(variable1, variable2, params, index1);
            }
            case less: {
                return generateLessThanQuery(variable1, variable2, params, index1);
            }
            case greater: {
                return generateGreaterThanQuery(variable1, variable2, params, index1);
            }
            case greaterEquals: {
                return generateGreaterEqualQuery(variable1, variable2, params, index1);
            }
            case lessEquals: {
                return generateLessEqualsQuery(variable1, variable2, params, index1);
            }
            case notEquals: {
                return generateNotEqualsQuery(variable1, variable2, params, index1);
            }
            default:
                throw new IllegalStateException("Should not reach to the point of code");
        }
    }


    protected String generateNotEqualsQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        Object param = resolveParam(variable2);
        if (param != null) {
            params.add(param);
            return "(" + fieldToSql(index1, variable1) + " != " + paramToSql(variable2) + " or " +
                    isNullSql(index1, (AqlField) variable1) + ")";
        } else {
            return " " + isNotNullSql(index1, (AqlField) variable1);
        }
    }

    protected String generateLessEqualsQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        Object param = resolveParam(variable2);
        validateNotNullParam(param);
        params.add(param);
        return " " + fieldToSql(index1, variable1) + " <= " + paramToSql(variable2);
    }

    protected String generateGreaterEqualQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        Object param = resolveParam(variable2);
        validateNotNullParam(param);
        params.add(param);
        return " " + fieldToSql(index1, variable1) + " >= " + paramToSql(variable2);
    }

    protected String generateGreaterThanQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        Object param = resolveParam(variable2);
        validateNotNullParam(param);
        params.add(param);
        return " " + fieldToSql(index1, variable1) + " > " + paramToSql(variable2);
    }

    protected String generateLessThanQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        Object param = resolveParam(variable2);
        validateNotNullParam(param);
        params.add(param);
        return " " + fieldToSql(index1, variable1) + " < " + paramToSql(variable2);
    }

    protected String generateNotMatchQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        String param = resolveParamForSqlLikeQuery(variable2);
        params.add(param);
        if (variable2 instanceof AqlField) {
            throw new AqlToSqlQueryBuilderException(
                    "Illegal syntax the 'not match' operator is allowed only with 'value' in right side of the criteria.");
        }
        return "(" + fieldToSql(index1, variable1) + " not like " + paramToSql(variable2)
                + generateEscapingCriteria(param) + " or " + fieldToSql(index1, variable1) + " is null)";
    }

    protected String generateMatchQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        String param = resolveParamForSqlLikeQuery(variable2);
        params.add(param);
        if (variable2 instanceof AqlField) {
            throw new AqlToSqlQueryBuilderException(
                    "Illegal syntax the 'match' operator is allowed only with 'value' in right side of the criteria.");
        }

        return " " + fieldToSql(index1, variable1) + " like " + paramToSql(variable2) + generateEscapingCriteria(param);
    }

    protected String generateEscapingCriteria(String param) {
        String escapeSql = "";
        if (param.contains(ESCAPING_CHAR + "_")) {
            escapeSql = " escape '" + ESCAPING_CHAR + "'";
        }
        return escapeSql;
    }

    protected String resolveParamForSqlLikeQuery(AqlVariable variable) {
        Object param = resolveParam(variable);
        validateNotNullParam(param);
        String modifiedValue = (String) param;
        modifiedValue = modifiedValue.replace("_", ESCAPING_CHAR + "_");
        modifiedValue = modifiedValue.replace("%", ESCAPING_CHAR + "%");
        modifiedValue = modifiedValue.replace('*', '%');
        modifiedValue = modifiedValue.replace('?', '_');
        return modifiedValue;
    }

    protected String generateEqualsQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        Object param = resolveParam(variable2);
        if (param != null) {
            params.add(param);
            return " " + fieldToSql(index1, variable1) + " = " + paramToSql(variable2);
        } else {
            return " " + isNullSql(index1, (AqlField) variable1);
        }
    }

    protected String generatePropertyNotMatchQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        String param = resolveParamForSqlLikeQuery(variable2);
        params.add(param);
        if (variable2 instanceof AqlField) {
            throw new AqlToSqlQueryBuilderException(
                    "Illegal syntax the 'not match' operator is allowed only with 'value' in right side of the criteria.");
        }
        String tableName = table1.getTable().name();
        String fieldName = node_props == table1.getTable() ? "node_id" :
                build_props == table1.getTable() ? "build_id" : "module_id";
        return "(" + isNullSql(index1, fieldName) + " or not exists (select 1 from " + tableName
                + " where " + fieldToSql(index1, "node_id") + " = " + fieldName + " and " +
                fieldToSql(null, variable1) + " like " + paramToSql(variable2) + generateEscapingCriteria(param) + "))";
    }

    protected String generatePropertyNotEqualsQuery(AqlVariable variable1, AqlVariable variable2, List<Object> params,
            String index1) {
        String tableName = table1.getTable().name();
        String fieldName = node_props == table1.getTable() ? "node_id" :
                build_props == table1.getTable() ? "build_id" : "module_id";
        Object param = resolveParam(variable2);
        if (param != null) {
            params.add(param);
            return "(" + isNullSql(index1, fieldName) + " or not exists (select 1 from " + tableName + " where " +
                    fieldToSql(index1, fieldName) + " = " + fieldName + " and " + fieldToSql(null, variable1) +
                    " = " + paramToSql(variable2) + "))";
        } else {
            return "(" + isNullSql(index1, "node_id") + " or not exists ( select 1 from node_props where " +
                    fieldToSql(index1, fieldName) + " = " + fieldName + " and " + isNullSql((AqlField) variable1) +
                    "))";
        }
    }

    protected void validateNotNullParam(Object param) {
        if (param == null) {
            throw new AqlToSqlQueryBuilderException(
                    "Illegal syntax the 'null' values are allowed to use only with equals and not equals operators.\n");
        }
    }
}
