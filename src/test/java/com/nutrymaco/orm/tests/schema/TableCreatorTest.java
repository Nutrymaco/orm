package com.nutrymaco.orm.tests.schema;

import com.nutrymaco.orm.query.select.SelectQueryContext;
import com.nutrymaco.orm.query.select.SelectQueryExecutor;
import com.nutrymaco.orm.schema.TableCreator;
import com.nutrymaco.orm.schema.db.Table;
import com.nutrymaco.tester.annotations.Test;
import com.nutrymaco.tester.asserting.AssertEquals;
import com.nutrymaco.tester.executing.TestExecutor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static com.nutrymaco.orm.configuration.Constants.MOVIE;
import static com.nutrymaco.orm.configuration.Constants.MOVIE_ENTITY;

/**
 * test for - {@link TableCreator}
 */
public class TableCreatorTest {

    public TableCreatorTest() throws InstantiationException, IllegalAccessException {
    }

    public static void main(String[] args) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        TestExecutor.of().execute(new TableCreatorTest());
    }

    @Test
    public void testCreateTableMovieByActorName() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var queryContext = new SelectQueryContext();
        queryContext.setEntity(MOVIE_ENTITY);
        queryContext.setConditions(List.of(MOVIE.ACTOR.NAME.eq("")));
        var table = createTable(queryContext);

        AssertEquals
                .actual(table.primaryKey().partitionColumns().stream()
                            .anyMatch(column -> column.name().equals("actor_name")))
                .expect(true);
    }

    private Table createTable(SelectQueryContext selectQueryContext) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var constructor = TableCreator.class.getDeclaredConstructor(SelectQueryContext.class);
        constructor.setAccessible(true);
        var creator = constructor.newInstance(selectQueryContext);
        var createTable = TableCreator.class.getDeclaredMethod("createTable");
        createTable.setAccessible(true);
        return (Table) createTable.invoke(creator);
    }
}