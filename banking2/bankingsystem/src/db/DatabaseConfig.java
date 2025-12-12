package db;

public final class DatabaseConfig {

    // TODO: adjust these values to match your local MySQL installation
    public static final String JDBC_URL_WITHOUT_DB = "jdbc:mysql://127.0.0.1:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    public static final String DB_NAME = "secured_banking";

    // Try to get from environment variables, fallback to default values if not set
    // You can either:
    // 1. Set environment variables: DB_USER and DB_PASS
    // 2. Or modify the fallback values below to match your MySQL credentials
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = ""; // Change this to your MySQL root password

    public static final String JDBC_USER = getEnvOrDefault("DB_USER", DEFAULT_USER);
    public static final String JDBC_PASSWORD = getEnvOrDefault("DB_PASS", DEFAULT_PASSWORD);

    private static String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        if (value == null || value.isEmpty()) {
            System.out.println("Warning: Environment variable " + envVar + " not set. Using default value.");
            return defaultValue;
        }
        return value;
    }

    public static String getJdbcUrlWithDb() {
        return "jdbc:mysql://127.0.0.1:3306/" + DB_NAME + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }
}


