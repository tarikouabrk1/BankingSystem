# MySQL JDBC Driver Setup

## Problem
The error "No suitable driver found for jdbc:mysql://..." means the MySQL JDBC driver is not on your classpath.

## Solution: Add MySQL Connector/J

### Step 1: Download MySQL Connector/J
1. Go to: https://dev.mysql.com/downloads/connector/j/
2. Select "Platform Independent" 
3. Download the ZIP file (e.g., `mysql-connector-j-8.3.0.zip`)
4. Extract the ZIP file
5. Find the JAR file inside (e.g., `mysql-connector-j-8.3.0.jar`)

### Step 2: Add to IntelliJ IDEA Project

**Option A: Using Project Structure (Recommended)**
1. In IntelliJ IDEA, go to **File → Project Structure** (Ctrl+Alt+Shift+S)
2. Select **Libraries** on the left
3. Click the **+** button → **Java**
4. Navigate to and select the `mysql-connector-j-8.3.0.jar` file
5. Click **OK**
6. Make sure it's added to your module
7. Click **Apply** → **OK**

**Option B: Using lib folder**
1. Create a `lib` folder in your project root (if it doesn't exist)
2. Copy the `mysql-connector-j-8.3.0.jar` file into the `lib` folder
3. In IntelliJ IDEA, go to **File → Project Structure** (Ctrl+Alt+Shift+S)
4. Select **Modules** → your module → **Dependencies** tab
5. Click **+** → **JARs or directories**
6. Select the `lib` folder or the JAR file
7. Click **Apply** → **OK**

### Step 3: Verify
After adding the driver, rebuild your project:
- **Build → Rebuild Project**

Then run the application again. The MySQL driver should now be found.

## Alternative: Using Maven (if you want to switch to Maven later)

If you convert to Maven, add this to `pom.xml`:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.3.0</version>
</dependency>
```

