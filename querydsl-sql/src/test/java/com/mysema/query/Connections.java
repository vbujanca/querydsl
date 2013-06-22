/*
 * Copyright 2011, Mysema Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysema.query;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;

import org.hsqldb.types.Types;

import com.mysema.query.ddl.CreateTableClause;
import com.mysema.query.ddl.DropTableClause;
import com.mysema.query.sql.DerbyTemplates;
import com.mysema.query.sql.H2Templates;
import com.mysema.query.sql.HSQLDBTemplates;
import com.mysema.query.sql.OracleTemplates;
import com.mysema.query.sql.PostgresTemplates;
import com.mysema.query.sql.SQLServerTemplates;
import com.mysema.query.sql.SQLTemplates;

/**
 * @author tiwe
 *
 */
public final class Connections {

    public static final int TEST_ROW_COUNT = 100;

    private static ThreadLocal<Connection> connHolder = new ThreadLocal<Connection>();

    private static ThreadLocal<Target> targetHolder = new ThreadLocal<Target>();

    private static ThreadLocal<SQLTemplates> templatesHolder = new ThreadLocal<SQLTemplates>();

    // datetest
    private static final String CREATE_TABLE_DATETEST = "create table DATE_TEST(DATE_TEST date)";

    // survey
    private static final String CREATE_TABLE_SURVEY =
            "create table SURVEY(ID int auto_increment, NAME varchar(30), NAME2 varchar(30))";

    // test
    private static final String CREATE_TABLE_TEST = "create table TEST(NAME varchar(255))";

    // timetest
    private static final String CREATE_TABLE_TIMETEST = "create table TIME_TEST(TIME_TEST time)";

    // employee
    private static final String INSERT_INTO_EMPLOYEE = "insert into EMPLOYEE " +
        "(ID, FIRSTNAME, LASTNAME, SALARY, DATEFIELD, TIMEFIELD, SUPERIOR_ID) " +
        "values (?,?,?,?,?,?,?)";

    private static final String INSERT_INTO_TEST_VALUES = "insert into TEST values(?)";

    private static ThreadLocal<Statement> stmtHolder = new ThreadLocal<Statement>();

    private static boolean derbyInited, sqlServerInited, h2Inited, hsqlInited, mysqlInited, cubridInited, oracleInited, postgresInited, sqliteInited;

    public static void close() throws SQLException{
        if (stmtHolder.get() != null) {
            stmtHolder.get().close();
        }
        if (connHolder.get() != null) {
            connHolder.get().close();
        }
    }

    public static Connection getConnection() {
        return connHolder.get();
    }

    public static Target getTarget() {
        return targetHolder.get();
    }

    public static SQLTemplates getTemplates() {
        return templatesHolder.get();
    }

    public static void setTemplates(SQLTemplates templates) {
        templatesHolder.set(templates);
    }

    private static Connection getDerby() throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        String url = "jdbc:derby:target/demoDB;create=true";
        return DriverManager.getConnection(url, "", "");
    }

    private static Connection getHSQL() throws SQLException, ClassNotFoundException {
        Class.forName("org.hsqldb.jdbcDriver");
        String url = "jdbc:hsqldb:target/tutorial";
        return DriverManager.getConnection(url, "sa", "");
    }

    private static Connection getH2() throws SQLException, ClassNotFoundException{
        Class.forName("org.h2.Driver");
        String url = "jdbc:h2:target/h2;LOCK_MODE=0";
        return DriverManager.getConnection(url, "sa", "");
    }

    private static Connection getMySQL() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/querydsl";
        return DriverManager.getConnection(url, "querydsl", "querydsl");
    }

    private static Connection getOracle() throws SQLException, ClassNotFoundException{
        Class.forName("oracle.jdbc.driver.OracleDriver");
        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        return DriverManager.getConnection(url, "querydsl", "querydsl");
    }

    private static Connection getPostgres() throws ClassNotFoundException, SQLException{
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/querydsl";
        return DriverManager.getConnection(url, "querydsl","querydsl");
    }

    private static Connection getSQLServer() throws ClassNotFoundException, SQLException{
        Class.forName("net.sourceforge.jtds.jdbc.Driver");
        String url = "jdbc:jtds:sqlserver://localhost:1433/querydsl";
        return DriverManager.getConnection(url, "querydsl","querydsl");
    }

    private static Connection getCubrid() throws ClassNotFoundException, SQLException {
        Class.forName("cubrid.jdbc.driver.CUBRIDDriver");
        String url = "jdbc:cubrid:localhost:30000:demodb:public::";
        return DriverManager.getConnection(url);
    }

    private static Connection getSQLite() throws SQLException, ClassNotFoundException {
        //System.setProperty("sqlite.purejava", "true");
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:target/sample.db");
    }

    private static CreateTableClause createTable(SQLTemplates templates, String table) {
        return new CreateTableClause(connHolder.get(), templates, table);
    }

    public static void dropTable(SQLTemplates templates, String table) throws SQLException{
        new DropTableClause(connHolder.get(), templates, table).execute();
    }

    public static void dropType(Statement stmt, String type) throws SQLException {
        try {
            stmt.execute("drop type " + type);
        } catch (SQLException e) {
            if (!e.getMessage().contains("does not exist")) {
                throw e;
            }
        }
    }

    public static Statement getStatement() {
        return stmtHolder.get();
    }

    private static void createEmployeeTable(SQLTemplates templates) {
        createTable(templates, "EMPLOYEE")
        .column("ID", Integer.class)
        .column("FIRSTNAME", String.class).size(50)
        .column("LASTNAME", String.class).size(50)
        .column("SALARY",Double.class)
        .column("DATEFIELD",Date.class)
        .column("TIMEFIELD",Time.class)
        .column("SUPERIOR_ID",Integer.class)
        .primaryKey("PK_EMPLOYEE", "ID")
        .foreignKey("FK_SUPERIOR","SUPERIOR_ID").references("EMPLOYEE","ID")
        .execute();
    }

    public static void initDerby() throws SQLException, ClassNotFoundException {
        targetHolder.set(Target.DERBY);
        SQLTemplates templates = new DerbyTemplates();
        Connection c = getDerby();
        connHolder.set(c);
        Statement stmt = c.createStatement();
        stmtHolder.set(stmt);

        if (derbyInited) {
            return;
        }

        // types
        dropType(stmt, "price restrict");
        stmt.execute("create type price external name 'com.example.Price' language java");

        // survey
        dropTable(templates, "SURVEY");
        stmt.execute("create table SURVEY(" +
        	"ID int generated by default as identity(start with 1, increment by 1), " +
        	"NAME varchar(30)," +
        	"NAME2 varchar(30))");
        stmt.execute("insert into SURVEY values (1,'Hello World','Hello')");

        // test
        dropTable(templates, "TEST");
        stmt.execute(CREATE_TABLE_TEST);
        stmt.execute("create index test_name on test(name)");
        PreparedStatement pstmt = c.prepareStatement(INSERT_INTO_TEST_VALUES);
        try{
            for (int i = 0; i < TEST_ROW_COUNT; i++) {
                pstmt.setString(1, "name" + i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }finally{
            pstmt.close();
        }

        // employee
        dropTable(templates, "EMPLOYEE");

        createEmployeeTable(templates);

        addEmployees(INSERT_INTO_EMPLOYEE);

        // date_test and time_test
        dropTable(templates, "TIME_TEST");
        stmt.execute(CREATE_TABLE_TIMETEST);

        dropTable(templates, "DATE_TEST");
        stmt.execute(CREATE_TABLE_DATETEST);
        derbyInited = true;
    }



    public static void initSQLServer() throws SQLException, ClassNotFoundException {
        targetHolder.set(Target.SQLSERVER);
        SQLTemplates templates = new SQLServerTemplates();
        Connection c = getSQLServer();
        connHolder.set(c);
        Statement stmt = c.createStatement();
        stmtHolder.set(stmt);

        if (sqlServerInited) {
            return;
        }

        // survey
        dropTable(templates, "SURVEY");
        stmt.execute("create table SURVEY(ID int, NAME varchar(30), NAME2 varchar(30))");
        stmt.execute("insert into SURVEY values (1, 'Hello World', 'Hello')");

        // test
        dropTable(templates, "TEST");
        stmt.execute(CREATE_TABLE_TEST);
        PreparedStatement pstmt = c.prepareStatement(INSERT_INTO_TEST_VALUES);
        try{
            for (int i = 0; i < TEST_ROW_COUNT; i++) {
                pstmt.setString(1, "name" + i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }finally{
            pstmt.close();
        }

        // employee
        dropTable(templates, "EMPLOYEE");
        createEmployeeTable(templates);
        addEmployees(INSERT_INTO_EMPLOYEE);

        // date_test and time_test
        dropTable(templates, "TIME_TEST");
        dropTable(templates, "DATE_TEST");
        stmt.execute(CREATE_TABLE_TIMETEST);
        stmt.execute(CREATE_TABLE_DATETEST);
        sqlServerInited = true;
    }

    public static void initH2() throws SQLException, ClassNotFoundException{
        targetHolder.set(Target.H2);
        SQLTemplates templates = new H2Templates();
        Connection c = getH2();
        connHolder.set(c);
        Statement stmt = c.createStatement();
        stmtHolder.set(stmt);

        if (h2Inited) {
            return;
        }

        // qtest
        stmt.execute("drop table QTEST if exists");
        stmt.execute("create table QTEST (ID int IDENTITY(1,1) NOT NULL,  C1 int NULL)");

        // survey
        stmt.execute("drop table SURVEY if exists");
        stmt.execute(CREATE_TABLE_SURVEY);
        stmt.execute("insert into SURVEY values (1, 'Hello World', 'Hello');");
        stmt.execute("alter table SURVEY alter column id int auto_increment");

        // test
        stmt.execute("drop table TEST if exists");
        stmt.execute(CREATE_TABLE_TEST);
        PreparedStatement pstmt = c.prepareStatement(INSERT_INTO_TEST_VALUES);
        try{
            for (int i = 0; i < TEST_ROW_COUNT; i++) {
                pstmt.setString(1, "name" + i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }finally{
            pstmt.close();
        }

        // employee
        stmt.execute("drop table EMPLOYEE if exists");
        createEmployeeTable(templates);
        stmt.execute("alter table EMPLOYEE alter column id int auto_increment");
        addEmployees(INSERT_INTO_EMPLOYEE);


        // date_test and time_test
        stmt.execute("drop table TIME_TEST if exists");
        stmt.execute("drop table DATE_TEST if exists");
        stmt.execute(CREATE_TABLE_TIMETEST);
        stmt.execute(CREATE_TABLE_DATETEST);
        h2Inited = true;
    }

    public static void initSQLite() throws SQLException, ClassNotFoundException{
        targetHolder.set(Target.SQLITE);
//        SQLTemplates templates = new SQLiteTemplates();
        Connection c = getSQLite();
        connHolder.set(c);
        Statement stmt = c.createStatement();
        stmtHolder.set(stmt);

        if (sqliteInited) {
            return;
        }

        // qtest
        stmt.execute("drop table if exists QTEST");
        stmt.execute("create table QTEST (ID int IDENTITY(1,1) NOT NULL,  C1 int NULL)");

        // survey
        stmt.execute("drop table if exists SURVEY");
        stmt.execute("create table SURVEY(ID int auto_increment, " +
                "NAME varchar(30)," +
                "NAME2 varchar(30)," +
                "constraint suryey_pk primary key(ID))");
        stmt.execute("insert into SURVEY values (1,'Hello World','Hello');");

        // test
        stmt.execute("drop table if exists TEST");
        stmt.execute(CREATE_TABLE_TEST);
        PreparedStatement pstmt = c.prepareStatement(INSERT_INTO_TEST_VALUES);
        try{
            for (int i = 0; i < TEST_ROW_COUNT; i++) {
                pstmt.setString(1, "name" + i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }finally{
            pstmt.close();
        }

        // employee
        stmt.execute("drop table if exists EMPLOYEE");
        stmt.execute("create table EMPLOYEE ( " +
                "ID INT AUTO_INCREMENT, " +
                "FIRSTNAME VARCHAR(50), " +
                "LASTNAME VARCHAR(50), " +
                "SALARY DECIMAL, "  +
                "DATEFIELD DATE, " +
                "TIMEFIELD TIME, " +
                "SUPERIOR_ID INT, " +
                "CONSTRAINT PK_EMPLOYEE PRIMARY KEY(ID),"+
                "CONSTRAINT FK_SUPERIOR FOREIGN KEY(SUPERIOR_ID) REFERENCES EMPLOYEE(ID) " +
              ")");
        addEmployees(INSERT_INTO_EMPLOYEE);


        // date_test and time_test
        stmt.execute("drop table if exists TIME_TEST");
        stmt.execute("drop table if exists DATE_TEST");
        stmt.execute(CREATE_TABLE_TIMETEST);
        stmt.execute(CREATE_TABLE_DATETEST);
        sqliteInited = true;
    }

    public static void initHSQL() throws SQLException, ClassNotFoundException{
        targetHolder.set(Target.HSQLDB);
        SQLTemplates templates = new HSQLDBTemplates();
        Connection c = getHSQL();
        connHolder.set(c);
        Statement stmt = c.createStatement();
        stmtHolder.set(stmt);

        if (hsqlInited) {
            return;
        }

        // dual
        stmt.execute("drop table DUAL if exists");
        stmt.execute("create table DUAL ( DUMMY varchar(1) )");
        stmt.execute("insert into DUAL (DUMMY) values ('X')");

        // survey
        stmt.execute("drop table SURVEY if exists");
        //stmt.execute(CREATE_TABLE_SURVEY);
        stmt.execute("create table SURVEY(" +
                "ID int generated by default as identity, " +
                "NAME varchar(30)," +
                "NAME2 varchar(30))");
        stmt.execute("insert into SURVEY values (1, 'Hello World', 'Hello')");

        // test
        stmt.execute("drop table TEST if exists");
        stmt.execute(CREATE_TABLE_TEST);
        PreparedStatement pstmt = c.prepareStatement(INSERT_INTO_TEST_VALUES);
        try{
            for (int i = 0; i < TEST_ROW_COUNT; i++) {
                pstmt.setString(1, "name" + i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }finally{
            pstmt.close();
        }

        // employee
        stmt.execute("drop table EMPLOYEE if exists");
        createEmployeeTable(templates);
        stmt.execute("alter table EMPLOYEE alter column id int generated by default as identity");
        addEmployees(INSERT_INTO_EMPLOYEE);

        // date_test and time_test
        stmt.execute("drop table TIME_TEST if exists");
        stmt.execute("drop table DATE_TEST if exists");
        stmt.execute(CREATE_TABLE_TIMETEST);
        stmt.execute(CREATE_TABLE_DATETEST);
        hsqlInited = true;
    }

    public static void initCubrid() throws SQLException, ClassNotFoundException{
        targetHolder.set(Target.CUBRID);
        //SQLTemplates templates = new MySQLTemplates();
        Connection c = getCubrid();
        connHolder.set(c);
        Statement stmt = c.createStatement();
        stmtHolder.set(stmt);

        if (cubridInited) {
            return;
        }

        // survey
        stmt.execute("drop table if exists SURVEY");
        stmt.execute("create table SURVEY(ID int auto_increment(16693,2), " +
                        "NAME varchar(30)," +
                        "NAME2 varchar(30)," +
                        "constraint suryey_pk primary key(ID))");
        stmt.execute("insert into SURVEY values (1,'Hello World','Hello');");

        // test
        stmt.execute("drop table if exists \"TEST\"");
        stmt.execute("create table \"TEST\"(NAME varchar(255))");
        PreparedStatement pstmt = c.prepareStatement("insert into \"TEST\" values(?)");
        try{
            for (int i = 0; i < TEST_ROW_COUNT; i++) {
                pstmt.setString(1, "name" + i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }finally{
            pstmt.close();
        }

        // employee
        stmt.execute("drop table if exists EMPLOYEE");
        //createEmployeeTable(templates);
        stmt.execute("create table EMPLOYEE ( " +
          "ID INT PRIMARY KEY AUTO_INCREMENT, " +
          "FIRSTNAME VARCHAR(50), " +
          "LASTNAME VARCHAR(50), " +
          "SALARY DECIMAL, "  +
          "DATEFIELD DATE, " +
          "TIMEFIELD TIME, " +
          "SUPERIOR_ID INT, " +
          "CONSTRAINT FK_SUPERIOR FOREIGN KEY(SUPERIOR_ID) REFERENCES EMPLOYEE(ID) " +
        ")");

        addEmployees(INSERT_INTO_EMPLOYEE);

        // date_test and time_test
        stmt.execute("drop table if exists TIME_TEST");
        stmt.execute("drop table if exists DATE_TEST");
        stmt.execute(CREATE_TABLE_TIMETEST);
        stmt.execute(CREATE_TABLE_DATETEST);
        cubridInited = true;
    }

    public static void initMySQL() throws SQLException, ClassNotFoundException{
        targetHolder.set(Target.MYSQL);
        //SQLTemplates templates = new MySQLTemplates();
        Connection c = getMySQL();
        connHolder.set(c);
        Statement stmt = c.createStatement();
        stmtHolder.set(stmt);

        if (mysqlInited) {
            return;
        }

        // survey
        stmt.execute("drop table if exists SURVEY");
        stmt.execute("create table SURVEY(ID int primary key auto_increment, " +
        		"NAME varchar(30)," +
        		"NAME2 varchar(30))");
        stmt.execute("insert into SURVEY values (1,'Hello World','Hello');");

        // test
        stmt.execute("drop table if exists TEST");
        stmt.execute(CREATE_TABLE_TEST);
        PreparedStatement pstmt = c.prepareStatement(INSERT_INTO_TEST_VALUES);
        try{
            for (int i = 0; i < TEST_ROW_COUNT; i++) {
                pstmt.setString(1, "name" + i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }finally{
            pstmt.close();
        }

        // employee
        stmt.execute("drop table if exists EMPLOYEE");
        //createEmployeeTable(templates);
        stmt.execute("create table EMPLOYEE ( " +
          "ID INT PRIMARY KEY AUTO_INCREMENT, " +
          "FIRSTNAME VARCHAR(50), " +
          "LASTNAME VARCHAR(50), " +
          "SALARY DECIMAL, "  +
          "DATEFIELD DATE, " +
          "TIMEFIELD TIME, " +
          "SUPERIOR_ID INT, " +
          "CONSTRAINT FK_SUPERIOR FOREIGN KEY(SUPERIOR_ID) REFERENCES EMPLOYEE(ID) " +
        ")");

        addEmployees(INSERT_INTO_EMPLOYEE);

        // date_test and time_test
        stmt.execute("drop table if exists TIME_TEST");
        stmt.execute("drop table if exists DATE_TEST");
        stmt.execute(CREATE_TABLE_TIMETEST);
        stmt.execute(CREATE_TABLE_DATETEST);
        mysqlInited = true;
    }

    public static void initOracle() throws SQLException, ClassNotFoundException{
        targetHolder.set(Target.ORACLE);
        SQLTemplates templates = new OracleTemplates();
        Connection c = getOracle();
        connHolder.set(c);
        Statement stmt = c.createStatement();
        stmtHolder.set(stmt);

        if (oracleInited) {
            return;
        }

        // types
        stmt.execute("create or replace type ssn_t as object (ssn_type char(11))");

        // survey
        dropTable(templates, "SURVEY");
        stmt.execute("create table SURVEY (ID number(10,0), " +
        		"NAME varchar(30 char)," +
        		"NAME2 varchar(30 char))");

        try {
            stmt.execute("drop sequence survey_seq");
        } catch(SQLException e) {
            if (!e.getMessage().contains("sequence does not exist")) {
                throw e;
            }
        }

        stmt.execute("create sequence survey_seq");
        stmt.execute("create or replace trigger survey_trigger\n"+
          "before insert on survey\n"+
          "for each row\n" +
          "when (new.id is null)\n"+
          "begin\n"+
          "  select survey_seq.nextval into :new.id from dual;\n"+
          "end;\n");

        stmt.execute("insert into SURVEY values (1,'Hello World','Hello')");

        // test
        dropTable(templates, "TEST");
        stmt.execute("create table TEST(name varchar(255))");
        String sql  = "insert into TEST values(?)";
        PreparedStatement pstmt = c.prepareStatement(sql);
        for (int i = 0; i < TEST_ROW_COUNT; i++) {
            pstmt.setString(1, "name" + i);
            pstmt.addBatch();
        }
        pstmt.executeBatch();

        // employee
        dropTable(templates, "EMPLOYEE");
        stmt.execute("create table EMPLOYEE ( " +
            "ID NUMBER(10,0), " +
            "FIRSTNAME VARCHAR2(50 CHAR), " +
            "LASTNAME VARCHAR2(50 CHAR), " +
            "SALARY DOUBLE PRECISION, " +
            "DATEFIELD DATE, " +
            "TIMEFIELD TIMESTAMP, " +
            "SUPERIOR_ID NUMBER(10,0), " +
            "CONSTRAINT PK_EMPLOYEE PRIMARY KEY(ID), " +
            "CONSTRAINT FK_SUPERIOR FOREIGN KEY(SUPERIOR_ID) REFERENCES EMPLOYEE(ID)" +
         ")");

        addEmployees(INSERT_INTO_EMPLOYEE);

        // date_test and time_test
        dropTable(templates, "DATE_TEST");
        stmt.execute("create table date_test(date_test date)");
        oracleInited = true;
    }

    public static void initPostgres() throws SQLException, ClassNotFoundException{
        targetHolder.set(Target.POSTGRES);
        SQLTemplates templates = new PostgresTemplates(true);
        // NOTE : unquoted identifiers are converted to lower case in Postgres
        Connection c = getPostgres();
        connHolder.set(c);
        Statement stmt = c.createStatement();
        stmtHolder.set(stmt);

        if (postgresInited) {
            return;
        }

        // types
        dropType(stmt, "u_country");
        stmt.execute("create type u_country as enum ('Brazil', 'England', 'Germany')");

        dropType(stmt, "u_street_type");
        stmt.execute("create type u_street_type as (street VARCHAR(100), number VARCHAR(30))");

        // survey
        dropTable(templates, "SURVEY");
        try {
            stmt.execute("drop sequence SURVEY_SEQ");
        } catch(SQLException e) {
            if (!e.getMessage().contains("does not exist")) {
                throw e;
            }
        }
        stmt.execute("create sequence SURVEY_SEQ");
        stmt.execute("create table \"SURVEY\"(" +
        		"\"ID\" int DEFAULT NEXTVAL('SURVEY_SEQ'), " +
        		"\"NAME\" varchar(30), \"NAME2\" varchar(30))");
        stmt.execute("insert into \"SURVEY\" values (1, 'Hello World')");

        // test
        dropTable(templates, "TEST");
        stmt.execute(quote(CREATE_TABLE_TEST,"TEST","NAME"));
        String sql = quote(INSERT_INTO_TEST_VALUES,"TEST");
        PreparedStatement pstmt = c.prepareStatement(sql);
        try{
            for (int i = 0; i < TEST_ROW_COUNT; i++) {
                pstmt.setString(1, "name" + i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }finally{
            pstmt.close();
        }

        // employee
        // stmt.execute("drop table employee if exists");
        dropTable(templates, "EMPLOYEE");
        createEmployeeTable(templates);
        addEmployees("insert into \"EMPLOYEE\" " +
            "(\"ID\", \"FIRSTNAME\", \"LASTNAME\", \"SALARY\", \"DATEFIELD\", \"TIMEFIELD\", \"SUPERIOR_ID\") " +
            "values (?,?,?,?,?,?,?)");

        // date_test and time_test
        dropTable(templates, "TIME_TEST");
        dropTable(templates, "DATE_TEST");
        stmt.execute(quote(CREATE_TABLE_TIMETEST, "TIME_TEST"));
        stmt.execute(quote(CREATE_TABLE_DATETEST, "DATE_TEST"));
        postgresInited = true;
    }

    static void addEmployee(String sql, int id, String firstName, String lastName,
            double salary, int superiorId) throws SQLException {
        PreparedStatement stmt = connHolder.get().prepareStatement(sql);
        stmt.setInt(1, id);
        stmt.setString(2, firstName);
        stmt.setString(3,lastName);
        stmt.setDouble(4, salary);
        stmt.setDate(5, Constants.date);
        stmt.setTime(6, Constants.time);
        if (superiorId <= 0) {
            stmt.setNull(7, Types.INTEGER);
        } else {
            stmt.setInt(7, superiorId);
        }
        stmt.execute();
        stmt.close();
    }

    private static void addEmployees(String sql) throws SQLException {
        addEmployee(sql, 1, "Mike", "Smith", 160000, -1);
        addEmployee(sql, 2, "Mary", "Smith", 140000, -1);

        // Employee under Mike
        addEmployee(sql, 10, "Joe", "Divis", 50000, 1);
        addEmployee(sql, 11, "Peter", "Mason", 45000, 1);
        addEmployee(sql, 12, "Steve", "Johnson", 40000, 1);
        addEmployee(sql, 13, "Jim", "Hood", 35000, 1);

        // Employee under Mike
        addEmployee(sql, 20, "Jennifer", "Divis", 60000, 2);
        addEmployee(sql, 21, "Helen", "Mason", 50000, 2);
        addEmployee(sql, 22, "Daisy", "Johnson", 40000, 2);
        addEmployee(sql, 23, "Barbara", "Hood", 30000, 2);
    }

    private static String quote(String sql, String... identifiers) {
        String rv = sql;
        for (String id : identifiers) {
            rv = rv.replace(id,  "\"" + id +  "\"");
        }
        return rv;
    }

    private Connections() {}
}
