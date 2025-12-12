package db;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Manages the DataSource for database connections.
 * Uses MySQL's built-in connection pooling via MysqlDataSource.
 */
public final class DataSourceManager {

    private static volatile DataSource dataSource;
    private static volatile DataSource dataSourceWithoutDb;

    /**
     * Get the DataSource for the main database.
     * Thread-safe singleton pattern.
     */
    public static DataSource getDataSource() throws SQLException {
        if (dataSource == null) {
            synchronized (DataSourceManager.class) {
                if (dataSource == null) {
                    dataSource = createDataSource(true);
                }
            }
        }
        return dataSource;
    }

    /**
     * Get the DataSource without a specific database (for creating databases).
     * Thread-safe singleton pattern.
     */
    public static DataSource getDataSourceWithoutDb() throws SQLException {
        if (dataSourceWithoutDb == null) {
            synchronized (DataSourceManager.class) {
                if (dataSourceWithoutDb == null) {
                    dataSourceWithoutDb = createDataSource(false);
                }
            }
        }
        return dataSourceWithoutDb;
    }

    private static DataSource createDataSource(boolean withDatabase) throws SQLException {
        MysqlDataSource mysqlDataSource = new MysqlDataSource();
        
        // Parse URL to extract host and port
        String url = withDatabase ? DatabaseConfig.getJdbcUrlWithDb() : DatabaseConfig.JDBC_URL_WITHOUT_DB;
        
        // Set connection properties
        mysqlDataSource.setServerName("127.0.0.1");
        mysqlDataSource.setPort(3306);
        mysqlDataSource.setUser(DatabaseConfig.JDBC_USER);
        mysqlDataSource.setPassword(DatabaseConfig.JDBC_PASSWORD);
        
        if (withDatabase) {
            mysqlDataSource.setDatabaseName(DatabaseConfig.DB_NAME);
        }
        
        // Connection pool settings
        mysqlDataSource.setUseSSL(false);
        mysqlDataSource.setServerTimezone("UTC");
        mysqlDataSource.setAllowPublicKeyRetrieval(true);
        
        // Connection pool configuration
        mysqlDataSource.setCachePrepStmts(true);
        mysqlDataSource.setPrepStmtCacheSize(250);
        mysqlDataSource.setPrepStmtCacheSqlLimit(2048);
        mysqlDataSource.setUseServerPrepStmts(true);
        mysqlDataSource.setRewriteBatchedStatements(true);
        
        // Connection timeout and pool size
        mysqlDataSource.setConnectTimeout(5000); // 5 seconds
        mysqlDataSource.setMaxReconnects(3);
        
        return mysqlDataSource;
    }

    /**
     * Reset the DataSource instances (useful for testing or reconfiguration).
     */
    public static void reset() {
        synchronized (DataSourceManager.class) {
            dataSource = null;
            dataSourceWithoutDb = null;
        }
    }
}

