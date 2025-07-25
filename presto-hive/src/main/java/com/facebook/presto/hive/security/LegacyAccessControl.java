/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.hive.security;

import com.facebook.presto.common.Subfield;
import com.facebook.presto.hive.HiveColumnConverterProvider;
import com.facebook.presto.hive.HiveTransactionManager;
import com.facebook.presto.hive.TransactionalMetadata;
import com.facebook.presto.hive.metastore.MetastoreContext;
import com.facebook.presto.hive.metastore.Table;
import com.facebook.presto.spi.ColumnMetadata;
import com.facebook.presto.spi.SchemaTableName;
import com.facebook.presto.spi.connector.ConnectorAccessControl;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import com.facebook.presto.spi.security.AccessControlContext;
import com.facebook.presto.spi.security.ConnectorIdentity;
import com.facebook.presto.spi.security.PrestoPrincipal;
import com.facebook.presto.spi.security.Privilege;
import com.facebook.presto.spi.security.ViewExpression;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.facebook.presto.spi.security.AccessDeniedException.denyAddColumn;
import static com.facebook.presto.spi.security.AccessDeniedException.denyAddConstraint;
import static com.facebook.presto.spi.security.AccessDeniedException.denyDropColumn;
import static com.facebook.presto.spi.security.AccessDeniedException.denyDropConstraint;
import static com.facebook.presto.spi.security.AccessDeniedException.denyDropTable;
import static com.facebook.presto.spi.security.AccessDeniedException.denyRenameColumn;
import static com.facebook.presto.spi.security.AccessDeniedException.denyRenameTable;
import static java.util.Objects.requireNonNull;

public class LegacyAccessControl
        implements ConnectorAccessControl
{
    private final HiveTransactionManager hiveTransactionManager;
    private final boolean allowDropTable;
    private final boolean allowRenameTable;
    private final boolean allowAddColumn;
    private final boolean allowDropColumn;
    private final boolean allowRenameColumn;
    private final boolean allowDropConstraint;
    private final boolean allowAddConstraint;

    @Inject
    public LegacyAccessControl(
            HiveTransactionManager hiveTransactionManager,
            LegacySecurityConfig securityConfig)
    {
        this.hiveTransactionManager = requireNonNull(hiveTransactionManager, "hiveTransactionManager is null");

        requireNonNull(securityConfig, "securityConfig is null");
        allowDropTable = securityConfig.getAllowDropTable();
        allowRenameTable = securityConfig.getAllowRenameTable();
        allowAddColumn = securityConfig.getAllowAddColumn();
        allowDropColumn = securityConfig.getAllowDropColumn();
        allowRenameColumn = securityConfig.getAllowRenameColumn();
        allowDropConstraint = securityConfig.getAllowDropConstraint();
        allowAddConstraint = securityConfig.getAllowAddConstraint();
    }

    @Override
    public void checkCanCreateSchema(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String schemaName)
    {
    }

    @Override
    public void checkCanDropSchema(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String schemaName)
    {
    }

    @Override
    public void checkCanRenameSchema(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String schemaName, String newSchemaName)
    {
    }

    @Override
    public void checkCanShowSchemas(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context)
    {
    }

    @Override
    public Set<String> filterSchemas(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, Set<String> schemaNames)
    {
        return schemaNames;
    }

    @Override
    public void checkCanShowCreateTable(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
    }

    @Override
    public void checkCanCreateTable(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
    }

    @Override
    public void checkCanSetTableProperties(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName, Map<String, Object> properties)
    {
    }

    @Override
    public void checkCanDropTable(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
        if (!allowDropTable) {
            denyDropTable(tableName.toString());
        }

        TransactionalMetadata metadata = hiveTransactionManager.get(transaction);
        // TODO: Refactor code to inject metastore headers using AccessControlContext instead of empty()
        MetastoreContext metastoreContext = new MetastoreContext(identity, context.getQueryId().getId(), context.getClientInfo(), context.getClientTags(), context.getSource(), Optional.empty(), false, HiveColumnConverterProvider.DEFAULT_COLUMN_CONVERTER_PROVIDER, context.getWarningCollector(), context.getRuntimeStats());
        Optional<Table> target = metadata.getMetastore().getTable(metastoreContext, tableName.getSchemaName(), tableName.getTableName());

        if (!target.isPresent()) {
            denyDropTable(tableName.toString(), "Table not found");
        }

        if (!identity.getUser().equals(target.get().getOwner())) {
            denyDropTable(tableName.toString(), "Owner of the table is different from session user");
        }
    }

    @Override
    public void checkCanRenameTable(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName, SchemaTableName newTableName)
    {
        if (!allowRenameTable) {
            denyRenameTable(tableName.toString(), newTableName.toString());
        }
    }

    @Override
    public void checkCanTruncateTable(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
    }

    @Override
    public void checkCanShowTablesMetadata(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String schemaName)
    {
    }

    @Override
    public Set<SchemaTableName> filterTables(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, Set<SchemaTableName> tableNames)
    {
        return tableNames;
    }

    @Override
    public void checkCanShowColumnsMetadata(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
    }

    @Override
    public List<ColumnMetadata> filterColumns(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName, List<ColumnMetadata> columns)
    {
        return columns;
    }

    @Override
    public void checkCanAddColumn(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
        if (!allowAddColumn) {
            denyAddColumn(tableName.toString());
        }
    }

    @Override
    public void checkCanDropColumn(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
        if (!allowDropColumn) {
            denyDropColumn(tableName.toString());
        }
    }

    @Override
    public void checkCanRenameColumn(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
        if (!allowRenameColumn) {
            denyRenameColumn(tableName.toString());
        }
    }

    @Override
    public void checkCanSelectFromColumns(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName, Set<Subfield> columnOrSubfieldNames)
    {
    }

    @Override
    public void checkCanInsertIntoTable(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
    }

    @Override
    public void checkCanDeleteFromTable(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
    }

    @Override
    public void checkCanUpdateTableColumns(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName, Set<String> updatedColumns)
    {
    }

    @Override
    public void checkCanCreateView(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName viewName)
    {
    }

    @Override
    public void checkCanRenameView(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName viewName, SchemaTableName newViewName)
    {
    }

    @Override
    public void checkCanDropView(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName viewName)
    {
    }

    @Override
    public void checkCanCreateViewWithSelectFromColumns(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName, Set<String> columnNames)
    {
    }

    @Override
    public void checkCanSetCatalogSessionProperty(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String propertyName)
    {
    }

    @Override
    public void checkCanGrantTablePrivilege(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, Privilege privilege, SchemaTableName tableName, PrestoPrincipal grantee, boolean withGrantOption)
    {
    }

    @Override
    public void checkCanRevokeTablePrivilege(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, Privilege privilege, SchemaTableName tableName, PrestoPrincipal revokee, boolean grantOptionFor)
    {
    }

    @Override
    public void checkCanCreateRole(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String role, Optional<PrestoPrincipal> grantor)
    {
    }

    @Override
    public void checkCanDropRole(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String role)
    {
    }

    @Override
    public void checkCanGrantRoles(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, Set<String> roles, Set<PrestoPrincipal> grantees, boolean withAdminOption, Optional<PrestoPrincipal> grantor, String catalogName)
    {
    }

    @Override
    public void checkCanRevokeRoles(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, Set<String> roles, Set<PrestoPrincipal> grantees, boolean adminOptionFor, Optional<PrestoPrincipal> grantor, String catalogName)
    {
    }

    @Override
    public void checkCanSetRole(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String role, String catalogName)
    {
    }

    @Override
    public void checkCanShowRoles(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String catalogName)
    {
    }

    @Override
    public void checkCanShowCurrentRoles(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String catalogName)
    {
    }

    @Override
    public void checkCanShowRoleGrants(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, String catalogName)
    {
    }

    @Override
    public void checkCanDropConstraint(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
        if (!allowDropConstraint) {
            denyDropConstraint(tableName.toString());
        }
    }

    @Override
    public void checkCanAddConstraint(ConnectorTransactionHandle transaction, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
        if (!allowAddConstraint) {
            denyAddConstraint(tableName.toString());
        }
    }

    @Override
    public List<ViewExpression> getRowFilters(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName)
    {
        return ImmutableList.of();
    }

    @Override
    public Map<ColumnMetadata, ViewExpression> getColumnMasks(ConnectorTransactionHandle transactionHandle, ConnectorIdentity identity, AccessControlContext context, SchemaTableName tableName, List<ColumnMetadata> columns)
    {
        return ImmutableMap.of();
    }
}
