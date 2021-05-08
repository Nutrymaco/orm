package com.nutrymaco.orm.migration;

import com.datastax.oss.driver.api.core.CqlSession;
import com.nutrymaco.orm.config.ConfigurationOwner;
import com.nutrymaco.orm.query.Database;
import com.nutrymaco.orm.schema.db.Table;
import com.nutrymaco.orm.schema.lang.Field;

import java.util.logging.Logger;

import static com.nutrymaco.orm.util.DBUtil.isTableExists;

public class MigrationTableManager {

    private static final String KEYSPACE = ConfigurationOwner.getConfiguration().keyspace();

    private static final Database database = ConfigurationOwner.getConfiguration().database();
    private static final CqlSession session = ConfigurationOwner.getConfiguration().session();
    private static final Logger logger = Logger.getLogger(MigrationTableManager.class.getSimpleName());

    public static MigrationTableManager getInstance() {
        return new MigrationTableManager();
    }

    // убирает из таблицы айдишек для миграции
    public void syncId(Table table, Object id) {
        var idTableName = getIdTableName(table);

        if (!isTableExists(idTableName)) {
            logger.info(() -> "id table for table : %s does not exist, creating".formatted(table.name()));
            createIdTable(table);
        }

        logger.info(() -> "delete id : %s from id table : %s".formatted(id, idTableName));
        database.execute("""
                DELETE FROM %s.%s WHERE id = %s
                """.formatted(KEYSPACE, idTableName, id));
    }

    public long getCountOfIds(Table table) {
        var idTableName = getIdTableName(table);

        if (!isTableExists(idTableName)) {
            logger.info(() -> "id table for table : %s does not exist, creating".formatted(table.name()));
            createIdTable(table);
        }

        logger.info("select count from id table : %s".formatted(idTableName));
        long countOfIds = database.execute("""
                    SELECT count(*) FROM %s.%s
                    """.formatted(KEYSPACE, idTableName)).get(0).getLong(0);
        logger.info(() -> "for table : %s count of ids to migrate : %d".formatted(table.name(), countOfIds));
        return countOfIds;
    }

    public boolean isIdTableExists(Table table) {
        return isTableExists(getIdTableName(table));
    }

    private void createIdTable(Table table) {
        var idTableName = getIdTableName(table);
        logger.info("creating id table : %s".formatted(idTableName));
        database.execute("""
                CREATE TABLE %s.%s (
                    id text,
                    primary key ((id))
                    )
                """.formatted(KEYSPACE, idTableName));

        var originalTableName = table.entity().getName();
        var idFieldName = table.entity().getFields().stream().filter(Field::isUnique).findFirst().orElseThrow().getName();
        var tokenRanges = session.getMetadata().getTokenMap().orElseThrow().getTokenRanges();
        logger.info("filling id table : %s with values from table : %s".formatted(idTableName, originalTableName));
        tokenRanges.forEach(tokenRange -> {
            var rows = database.execute("""
                    SELECT %s FROM %s.%s WHERE token(%s) > %s and token(%s) < %s
                    """.formatted(idFieldName, KEYSPACE, originalTableName,
                    idFieldName, tokenRange.getStart(),
                    idFieldName, tokenRange.getEnd()));
            rows.stream()
                    .map(row -> row.getObject(0))
                    .map(id -> "INSERT INTO %s.%s (id) VALUES (%s)"
                            .formatted(KEYSPACE, idTableName, id))
                    .forEach(database::execute);
        });
    }

    private static String getIdTableName(Table table) {
        int indexOfBy = table.name().startsWith("By")
                ? table.name().indexOf("By", 2)
                : table.name().indexOf("By");

        var idTableName = indexOfBy == -1
                ? (table.name() + "Id")
                : (table.name().substring(0, indexOfBy) + "Id" + table.name().substring(indexOfBy));
        return idTableName;
    }
}
