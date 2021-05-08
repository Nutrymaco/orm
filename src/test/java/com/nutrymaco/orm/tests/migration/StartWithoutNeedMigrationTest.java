package com.nutrymaco.orm.tests.migration;

import com.nutrymaco.orm.config.ConfigurationOwner;
import com.nutrymaco.orm.query.Database;
import com.nutrymaco.orm.query.Query;
import com.nutrymaco.orm.query.insert.InsertResultChooser;
import com.nutrymaco.orm.records.MovieRecord;
import com.nutrymaco.orm.tests.util.DBUtil;
import com.nutrymaco.tester.annotations.AfterAll;
import com.nutrymaco.tester.annotations.BeforeAll;
import com.nutrymaco.tester.annotations.Test;
import com.nutrymaco.tester.asserting.AssertEquals;
import com.nutrymaco.tester.asserting.AssertException;
import com.nutrymaco.tester.executing.TestExecutor;

import static com.nutrymaco.orm.configuration.Constants.MOVIE;
import static com.nutrymaco.orm.configuration.Constants.MOVIE_ENTITY;
import static com.nutrymaco.orm.configuration.MovieObjects.movies;

public class StartWithoutNeedMigrationTest {

    private static final String KEYSPACE = ConfigurationOwner.getConfiguration().keyspace();
    private static final Database database = ConfigurationOwner.getConfiguration().database();

    public static void main(String[] args) {
        TestExecutor.of().execute(new StartWithoutNeedMigrationTest());
    }

    @BeforeAll
    public void prepareDB() {
        DBUtil.dropAllTables();
        DBUtil.deleteTypes();

        Query.select(MOVIE_ENTITY)
                .where(MOVIE.YEAR.eq(2020))
                .fetchInto(MovieRecord.class);

        Query.select(MOVIE_ENTITY)
                .where(MOVIE.ACTOR.NAME.eq("Di Caprio"))
                .fetchInto(MovieRecord.class);
    }

    @Test(order = 10)
    public void insert() {
        movies.stream().map(Query::insert).forEach(InsertResultChooser::execute);
    }

    @Test(order = 20)
    public void testBaseTableCreated() {
        AssertEquals
                .actual(DBUtil.isTableExists("movie"))
                .expect(true);
    }

    @Test(order = 30)
    public void testBaseTableContainsAllMovies() {
        AssertEquals
                .actual(database.execute("SELECT * FROM %s.movie".formatted(KEYSPACE)).size())
                .expect(movies.size());
    }

    @AfterAll
    public void clearDB() throws InterruptedException {
        DBUtil.dropAllTables();
        DBUtil.deleteTypes();
//        System.exit(1);
    }
}
