package com.continuuity.passport.testhelper;

import org.hsqldb.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 * Test Helper for unit/integration tests
 * Uses HSQLDB instance for testing
 */
public class HyperSQL {

  private static Server server = null;

  protected static Connection connection;

  private static final String CREATE_ACCOUNT_TABLE = "CREATE TABLE account (id INTEGER IDENTITY , " +
                                                     "first_name VARCHAR(50),last_name VARCHAR(50), " +
                                                     "company VARCHAR(50),email_id VARCHAR(50), " +
                                                     "password VARCHAR(100),confirmed INTEGER, " +
                                                     "api_key VARCHAR(100),account_created_at DATETIME," +
                                                     "dev_suite_downloaded_at TIMESTAMP DEFAULT null," +
                                                     "payment_info_provided_at TIMESTAMP DEFAULT null," +
                                                     "UNIQUE (email_id)" +
                                                     ")";
  private static final String CREATE_NONCE_TABLE = "CREATE TABLE nonce (nonce_id INTEGER IDENTITY," +
                                                   "id VARCHAR(100), nonce_expires_at TIMESTAMP, UNIQUE (id)" +
                                                   ")";
  private static final String DROP_ACCOUNT_TABLE = "DROP TABLE account";
  private static final String DROP_NONCE_TABLE = "DROP TABLE nonce";


  public static void startHsqlDB() throws SQLException, ClassNotFoundException {

    System.out.println("======================================START======================================");

    server = new Server();
    server.setLogWriter(null);
    server.setPort(1234);
    server.setSilent(true);
    server.setDatabaseName(0, "xdb");
    server.setDatabasePath(0, "mem:test");
    server.start();
    Class.forName("org.hsqldb.jdbcDriver");
    connection = DriverManager.getConnection("jdbc:hsqldb:mem:test;" +
      "hsqldb.default_table_type=cached;hsqldb.sql.enforce_size=false", "sa", "");


    connection.createStatement().execute(CREATE_ACCOUNT_TABLE);
    connection.createStatement().execute(CREATE_NONCE_TABLE);


  }


  public static void stopHsqlDB() throws SQLException {

    System.out.println("======================================STOP=======================================");
    connection.createStatement().execute(DROP_ACCOUNT_TABLE);
    connection.createStatement().execute(DROP_NONCE_TABLE);

    connection.close();
    server.stop();
  }

}
