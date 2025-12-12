import db.DatabaseInitializer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.Account;
import model.BankTransaction;
import model.User;
import service.AuthService;
import service.BankingService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Main extends Application {

    private final AuthService authService = new AuthService();
    private final BankingService bankingService = new BankingService();

    private Stage primaryStage;
    private User currentUser;
    private Account selectedAccount;
    
    // UI component references for refreshing
    private TabPane mainTabs;
    private ListView<String> accountsListView;
    private ListView<String> allAccountsListView;
    private ComboBox<String> depositAccountCombo;
    private ComboBox<String> withdrawAccountCombo;
    private ComboBox<String> transferFromCombo;
    private ComboBox<String> transferToCombo;
    private ComboBox<String> transactionsAccountCombo;
    private ListView<String> transactionsListView;

    public static void main(String[] args) {
        // Initialize DB schema before launching UI
        try {
            DatabaseInitializer.initialize();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Secured Banking System");
        primaryStage.setScene(createAuthScene());
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    private Scene createAuthScene() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: white;");

        Tab loginTab = new Tab("Login");
        loginTab.setContent(createLoginPane());

        Tab registerTab = new Tab("Register");
        registerTab.setContent(createRegisterPane());

        tabPane.getTabs().addAll(loginTab, registerTab);

        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: #ffffff;");

        // Use the current window size if available, otherwise use default
        double sceneWidth = (primaryStage.getWidth() > 0 && !Double.isNaN(primaryStage.getWidth())) 
            ? primaryStage.getWidth() : 1000;
        double sceneHeight = (primaryStage.getHeight() > 0 && !Double.isNaN(primaryStage.getHeight())) 
            ? primaryStage.getHeight() : 700;
        
        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        // Make root resizable
        root.setPrefSize(sceneWidth, sceneHeight);
        // Apply formal styling
        String css = """
            .tab-pane .tab-header-area .tab-header-background {
                -fx-background-color: #4169E1;
            }
            .tab-pane .tab {
                -fx-background-color: #4169E1;
            }
            .tab-pane .tab:selected {
                -fx-background-color: #E6F0FF;
                -fx-border-color: #4169E1;
            }
            .tab-pane .tab:selected .tab-label {
                -fx-text-fill: #4169E1;
            }
            .tab-pane .tab .tab-label {
                -fx-text-fill: white;
            }
            """;
        scene.getStylesheets().add("data:text/css;charset=utf-8," + css);
        return scene;
    }

    private VBox createLoginPane() {
        VBox vbox = new VBox(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(40));

        Label titleLabel = new Label("Welcome Back");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.rgb(65, 105, 225)); // Royal Blue

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(30));

        Label userLabel = new Label("Username:");
        userLabel.setFont(Font.font(14));
        userLabel.setTextFill(Color.BLACK);
        TextField userField = new TextField();
        userField.setPrefWidth(250);
        userField.setStyle("-fx-font-size: 14px; -fx-border-color: #4169E1; -fx-border-width: 1px;");

        Label pwdLabel = new Label("Password:");
        pwdLabel.setFont(Font.font(14));
        pwdLabel.setTextFill(Color.BLACK);
        PasswordField pwdField = new PasswordField();
        pwdField.setPrefWidth(250);
        pwdField.setStyle("-fx-font-size: 14px; -fx-border-color: #4169E1; -fx-border-width: 1px;");

        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(250);
        loginButton.setPrefHeight(35);
        loginButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        loginButton.setOnMouseEntered(e -> loginButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"));
        loginButton.setOnMouseExited(e -> loginButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"));

        Label messageLabel = new Label();
        messageLabel.setFont(Font.font(12));

        grid.add(userLabel, 0, 0);
        grid.add(userField, 1, 0);
        grid.add(pwdLabel, 0, 1);
        grid.add(pwdField, 1, 1);
        grid.add(loginButton, 1, 2);
        grid.add(messageLabel, 1, 3);

        loginButton.setOnAction(e -> {
            String username = userField.getText();
            String password = pwdField.getText();
            try {
                User user = authService.login(username, password);
                if (user == null) {
                    messageLabel.setText("Invalid credentials");
                    messageLabel.setTextFill(Color.RED);
                } else {
                    this.currentUser = user;
                    // Preserve window size and position
                    double width = primaryStage.getWidth();
                    double height = primaryStage.getHeight();
                    double x = primaryStage.getX();
                    double y = primaryStage.getY();
                    boolean maximized = primaryStage.isMaximized();
                    boolean fullScreen = primaryStage.isFullScreen();
                    
                    showDashboard();
                    
                    // Restore window size and position
                    if (fullScreen) {
                        primaryStage.setFullScreen(true);
                    } else if (maximized) {
                        primaryStage.setMaximized(true);
                    } else {
                        primaryStage.setWidth(width);
                        primaryStage.setHeight(height);
                        primaryStage.setX(x);
                        primaryStage.setY(y);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                messageLabel.setText("Error: " + ex.getMessage());
                messageLabel.setTextFill(Color.RED);
            }
        });

        vbox.getChildren().addAll(titleLabel, grid);
        return vbox;
    }

    private VBox createRegisterPane() {
        VBox vbox = new VBox(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(40));

        Label titleLabel = new Label("Create Account");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.rgb(65, 105, 225)); // Royal Blue

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(30));

        Label userLabel = new Label("Username:");
        userLabel.setFont(Font.font(14));
        userLabel.setTextFill(Color.BLACK);
        TextField userField = new TextField();
        userField.setPrefWidth(250);
        userField.setStyle("-fx-font-size: 14px; -fx-border-color: #4169E1; -fx-border-width: 1px;");

        Label pwdLabel = new Label("Password:");
        pwdLabel.setFont(Font.font(14));
        pwdLabel.setTextFill(Color.BLACK);
        PasswordField pwdField = new PasswordField();
        pwdField.setPrefWidth(250);
        pwdField.setStyle("-fx-font-size: 14px; -fx-border-color: #4169E1; -fx-border-width: 1px;");

        Label pinLabel = new Label("PIN (4+ digits):");
        pinLabel.setFont(Font.font(14));
        pinLabel.setTextFill(Color.BLACK);
        PasswordField pinField = new PasswordField();
        pinField.setPrefWidth(250);
        pinField.setStyle("-fx-font-size: 14px; -fx-border-color: #4169E1; -fx-border-width: 1px;");

        Label auxIdLabel = new Label("Auxiliary User ID:");
        auxIdLabel.setFont(Font.font(14));
        auxIdLabel.setTextFill(Color.BLACK);
        TextField auxIdField = new TextField();
        auxIdField.setPrefWidth(250);
        auxIdField.setStyle("-fx-font-size: 14px; -fx-border-color: #4169E1; -fx-border-width: 1px;");
        auxIdField.setPromptText("Optional: Link to existing user");

        Label auxIdHint = new Label("(Leave empty to create new user)");
        auxIdHint.setFont(Font.font(11));
        auxIdHint.setTextFill(Color.GRAY);

        Button registerButton = new Button("Register");
        registerButton.setPrefWidth(250);
        registerButton.setPrefHeight(35);
        registerButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        registerButton.setOnMouseEntered(e -> registerButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"));
        registerButton.setOnMouseExited(e -> registerButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"));

        Label messageLabel = new Label();
        messageLabel.setFont(Font.font(12));

        grid.add(userLabel, 0, 0);
        grid.add(userField, 1, 0);
        grid.add(pwdLabel, 0, 1);
        grid.add(pwdField, 1, 1);
        grid.add(pinLabel, 0, 2);
        grid.add(pinField, 1, 2);
        grid.add(auxIdLabel, 0, 3);
        grid.add(auxIdField, 1, 3);
        grid.add(auxIdHint, 1, 4);
        grid.add(registerButton, 1, 5);
        grid.add(messageLabel, 1, 6);

        registerButton.setOnAction(e -> {
            String username = userField.getText();
            String password = pwdField.getText();
            String pin = pinField.getText();
            String auxId = auxIdField.getText().trim();
            try {
                User user;
                if (auxId.isEmpty()) {
                    // Create new user with new account
                    user = authService.register(username, password, pin);
                    // Also create a default account for the new user
                    bankingService.createDefaultAccountForUser(user.getId());
                    messageLabel.setText("Registration successful! Your User ID: " + user.getUserId() + ". You can now log in.");
                    messageLabel.setTextFill(Color.GREEN);
                } else {
                    // Link to existing user - verify password and PIN before creating account
                    try {
                        // Verify password and PIN before creating account
                        Account newAccount = bankingService.createAccountForUserIdWithAuth(auxId, password, pin);
                        messageLabel.setText("Account created successfully! Account: " + newAccount.getAccountNumber() + 
                            ". Please log in with your existing username.");
                        messageLabel.setTextFill(Color.GREEN);
                    } catch (IllegalArgumentException iae) {
                        // User ID not found or authentication failed
                        if (iae.getMessage().contains("User ID not found")) {
                            // User ID not found, create new user with this ID
                            user = authService.register(username, password, pin, auxId);
                            bankingService.createDefaultAccountForUser(user.getId());
                            messageLabel.setText("Registration successful! Your User ID: " + user.getUserId() + ". You can now log in.");
                            messageLabel.setTextFill(Color.GREEN);
                        } else {
                            // Authentication failed (invalid password or PIN)
                            messageLabel.setText("Error: " + iae.getMessage());
                            messageLabel.setTextFill(Color.RED);
                        }
                    }
                }
                userField.clear();
                pwdField.clear();
                pinField.clear();
                auxIdField.clear();
            } catch (Exception ex) {
                ex.printStackTrace();
                messageLabel.setText("Error: " + ex.getMessage());
                messageLabel.setTextFill(Color.RED);
            }
        });

        vbox.getChildren().addAll(titleLabel, grid);
        return vbox;
    }

    private void showDashboard() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: #ffffff;");

        // Top bar
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: #4169E1;");

        Label welcomeLabel = new Label("Welcome, " + currentUser.getUsername());
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        welcomeLabel.setTextFill(Color.WHITE);

        Label userIdLabel = new Label("User ID: " + (currentUser.getUserId() != null ? currentUser.getUserId() : "N/A"));
        userIdLabel.setFont(Font.font(12));
        userIdLabel.setTextFill(Color.rgb(240, 248, 255));

        Button logoutButton = new Button("Logout");
        logoutButton.setStyle("-fx-background-color: #000000; -fx-text-fill: white; -fx-font-weight: bold;");
        logoutButton.setOnMouseEntered(e -> logoutButton.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-font-weight: bold;"));
        logoutButton.setOnMouseExited(e -> logoutButton.setStyle("-fx-background-color: #000000; -fx-text-fill: white; -fx-font-weight: bold;"));

        HBox.setHgrow(welcomeLabel, Priority.ALWAYS);
        topBar.getChildren().addAll(welcomeLabel, userIdLabel, logoutButton);

        logoutButton.setOnAction(e -> {
            currentUser = null;
            selectedAccount = null;
            
            // Preserve window size and position
            double width = primaryStage.getWidth();
            double height = primaryStage.getHeight();
            double x = primaryStage.getX();
            double y = primaryStage.getY();
            boolean maximized = primaryStage.isMaximized();
            boolean fullScreen = primaryStage.isFullScreen();
            
            primaryStage.setScene(createAuthScene());
            
            // Restore window size and position
            if (fullScreen) {
                primaryStage.setFullScreen(true);
            } else if (maximized) {
                primaryStage.setMaximized(true);
            } else {
                primaryStage.setWidth(width);
                primaryStage.setHeight(height);
                primaryStage.setX(x);
                primaryStage.setY(y);
            }
        });

        // Main content area with tabs
        mainTabs = new TabPane();
        mainTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab accountsTab = new Tab("My Accounts");
        accountsTab.setContent(createAccountsPane());

        Tab depositTab = new Tab("Deposit");
        depositTab.setContent(createDepositPane());

        Tab withdrawTab = new Tab("Withdraw");
        withdrawTab.setContent(createWithdrawPane());

        Tab transferTab = new Tab("Transfer");
        transferTab.setContent(createTransferPane());

        Tab transactionsTab = new Tab("Transactions");
        transactionsTab.setContent(createTransactionsPane());

        mainTabs.getTabs().addAll(accountsTab, depositTab, withdrawTab, transferTab, transactionsTab);
        
        // Add listener to refresh data when switching tabs
        mainTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                refreshAllUI();
            }
        });

        root.setTop(topBar);
        root.setCenter(mainTabs);

        // Use the current window size if available, otherwise use default
        double sceneWidth = (primaryStage.getWidth() > 0 && !Double.isNaN(primaryStage.getWidth())) 
            ? primaryStage.getWidth() : 1000;
        double sceneHeight = (primaryStage.getHeight() > 0 && !Double.isNaN(primaryStage.getHeight())) 
            ? primaryStage.getHeight() : 700;
        
        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        // Make root resizable
        root.setPrefSize(sceneWidth, sceneHeight);
        // Apply formal styling
        String css = """
            .tab-pane .tab-header-area .tab-header-background {
                -fx-background-color: #4169E1;
            }
            .tab-pane .tab {
                -fx-background-color: #4169E1;
            }
            .tab-pane .tab:selected {
                -fx-background-color: #E6F0FF;
                -fx-border-color: #4169E1;
            }
            .tab-pane .tab:selected .tab-label {
                -fx-text-fill: #4169E1;
            }
            .tab-pane .tab .tab-label {
                -fx-text-fill: white;
            }
            """;
        scene.getStylesheets().add("data:text/css;charset=utf-8," + css);
        primaryStage.setScene(scene);
    }

    private VBox createAccountsPane() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

        Label titleLabel = new Label("My Accounts");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.rgb(65, 105, 225)); // Royal Blue

        accountsListView = new ListView<>();
        accountsListView.setPrefHeight(300);
        accountsListView.setStyle("-fx-font-size: 14px; -fx-border-color: #4169E1; -fx-border-width: 1px;");

        Label allAccountsLabel = new Label("All Account Numbers in System:");
        allAccountsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        allAccountsLabel.setTextFill(Color.BLACK);

        allAccountsListView = new ListView<>();
        allAccountsListView.setPrefHeight(200);
        allAccountsListView.setStyle("-fx-font-size: 12px; -fx-border-color: #4169E1; -fx-border-width: 1px;");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        Button createAccountButton = new Button("Create New Account");
        createAccountButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-weight: bold;");
        createAccountButton.setOnMouseEntered(e -> createAccountButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-weight: bold;"));
        createAccountButton.setOnMouseExited(e -> createAccountButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-weight: bold;"));
        
        Label messageLabel = new Label();
        messageLabel.setFont(Font.font(12));
        
        createAccountButton.setOnAction(e -> {
            try {
                if (currentUser.getUserId() == null || currentUser.getUserId().isEmpty()) {
                    messageLabel.setText("Error: No User ID found. Please register first.");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }
                Account newAccount = bankingService.createAccountForUserId(currentUser.getUserId());
                messageLabel.setText("New account created: " + newAccount.getAccountNumber());
                messageLabel.setTextFill(Color.GREEN);
                refreshAllUI();
            } catch (Exception ex) {
                ex.printStackTrace();
                messageLabel.setText("Error: " + ex.getMessage());
                messageLabel.setTextFill(Color.RED);
            }
        });

        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshButton.setOnMouseEntered(e -> refreshButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-weight: bold;"));
        refreshButton.setOnMouseExited(e -> refreshButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-weight: bold;"));

        refreshButton.setOnAction(e -> {
            refreshAllUI();
        });

        buttonBox.getChildren().addAll(createAccountButton, messageLabel);
        vbox.getChildren().addAll(titleLabel, buttonBox, accountsListView, allAccountsLabel, allAccountsListView, refreshButton);

        reloadAccounts(accountsListView);
        reloadAllAccounts(allAccountsListView);

        return vbox;
    }

    private VBox createDepositPane() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

        Label titleLabel = new Label("Deposit Money");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.rgb(65, 105, 225)); // Royal Blue

        depositAccountCombo = new ComboBox<>();
        depositAccountCombo.setPrefWidth(300);
        depositAccountCombo.setPromptText("Select Account");
        depositAccountCombo.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        TextField amountField = new TextField();
        amountField.setPrefWidth(300);
        amountField.setPromptText("Amount");
        amountField.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        TextField descriptionField = new TextField();
        descriptionField.setPrefWidth(300);
        descriptionField.setPromptText("Description (optional)");
        descriptionField.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        PasswordField pinField = new PasswordField();
        pinField.setPrefWidth(300);
        pinField.setPromptText("Enter PIN");
        pinField.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        Button depositButton = new Button("Deposit");
        depositButton.setPrefWidth(300);
        depositButton.setPrefHeight(40);
        depositButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        depositButton.setOnMouseEntered(e -> depositButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"));
        depositButton.setOnMouseExited(e -> depositButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"));

        Label messageLabel = new Label();
        messageLabel.setFont(Font.font(12));

        // Load accounts into combo box
        loadAccountsIntoCombo(depositAccountCombo);

        depositButton.setOnAction(e -> {
            try {
                String selected = depositAccountCombo.getValue();
                if (selected == null) {
                    messageLabel.setText("Please select an account");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }

                String accountNumber = selected.split(" ")[0];
                Account account = bankingService.getAccountByNumber(accountNumber);
                if (account == null) {
                    messageLabel.setText("Account not found");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }

                if (!authService.verifyPin(currentUser, pinField.getText())) {
                    messageLabel.setText("Invalid PIN");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }

                BigDecimal amount = new BigDecimal(amountField.getText());
                String description = descriptionField.getText().isEmpty() ? "Deposit" : descriptionField.getText();

                bankingService.deposit(account.getId(), amount, description);
                messageLabel.setText("Deposit successful! New balance: " + 
                    bankingService.getAccountById(account.getId()).getBalance());
                messageLabel.setTextFill(Color.GREEN);

                amountField.clear();
                descriptionField.clear();
                pinField.clear();
                refreshAllUI();
            } catch (Exception ex) {
                ex.printStackTrace();
                messageLabel.setText("Error: " + ex.getMessage());
                messageLabel.setTextFill(Color.RED);
            }
        });

        vbox.getChildren().addAll(titleLabel, depositAccountCombo, amountField, descriptionField, pinField, depositButton, messageLabel);
        return vbox;
    }

    private VBox createWithdrawPane() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

        Label titleLabel = new Label("Withdraw Money");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.rgb(65, 105, 225)); // Royal Blue

        withdrawAccountCombo = new ComboBox<>();
        withdrawAccountCombo.setPrefWidth(300);
        withdrawAccountCombo.setPromptText("Select Account");
        withdrawAccountCombo.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        TextField amountField = new TextField();
        amountField.setPrefWidth(300);
        amountField.setPromptText("Amount");
        amountField.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        TextField descriptionField = new TextField();
        descriptionField.setPrefWidth(300);
        descriptionField.setPromptText("Description (optional)");
        descriptionField.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        PasswordField pinField = new PasswordField();
        pinField.setPrefWidth(300);
        pinField.setPromptText("Enter PIN");
        pinField.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        Button withdrawButton = new Button("Withdraw");
        withdrawButton.setPrefWidth(300);
        withdrawButton.setPrefHeight(40);
        withdrawButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        withdrawButton.setOnMouseEntered(e -> withdrawButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"));
        withdrawButton.setOnMouseExited(e -> withdrawButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"));

        Label messageLabel = new Label();
        messageLabel.setFont(Font.font(12));

        // Load accounts into combo box
        loadAccountsIntoCombo(withdrawAccountCombo);

        withdrawButton.setOnAction(e -> {
            try {
                String selected = withdrawAccountCombo.getValue();
                if (selected == null) {
                    messageLabel.setText("Please select an account");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }

                String accountNumber = selected.split(" ")[0];
                Account account = bankingService.getAccountByNumber(accountNumber);
                if (account == null) {
                    messageLabel.setText("Account not found");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }

                if (!authService.verifyPin(currentUser, pinField.getText())) {
                    messageLabel.setText("Invalid PIN");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }

                BigDecimal amount = new BigDecimal(amountField.getText());
                String description = descriptionField.getText().isEmpty() ? "Withdrawal" : descriptionField.getText();

                bankingService.withdraw(account.getId(), amount, description);
                messageLabel.setText("Withdrawal successful! New balance: " + 
                    bankingService.getAccountById(account.getId()).getBalance());
                messageLabel.setTextFill(Color.GREEN);

                amountField.clear();
                descriptionField.clear();
                pinField.clear();
                refreshAllUI();
            } catch (Exception ex) {
                ex.printStackTrace();
                messageLabel.setText("Error: " + ex.getMessage());
                messageLabel.setTextFill(Color.RED);
            }
        });

        vbox.getChildren().addAll(titleLabel, withdrawAccountCombo, amountField, descriptionField, pinField, withdrawButton, messageLabel);
        return vbox;
    }

    private VBox createTransferPane() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

        Label titleLabel = new Label("Transfer Funds");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.rgb(65, 105, 225)); // Royal Blue

        transferFromCombo = new ComboBox<>();
        transferFromCombo.setPrefWidth(300);
        transferFromCombo.setPromptText("From Account");
        transferFromCombo.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        transferToCombo = new ComboBox<>();
        transferToCombo.setPrefWidth(300);
        transferToCombo.setPromptText("To Account Number");
        transferToCombo.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        TextField amountField = new TextField();
        amountField.setPrefWidth(300);
        amountField.setPromptText("Amount");
        amountField.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        TextField descriptionField = new TextField();
        descriptionField.setPrefWidth(300);
        descriptionField.setPromptText("Description (optional)");
        descriptionField.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        PasswordField pinField = new PasswordField();
        pinField.setPrefWidth(300);
        pinField.setPromptText("Enter PIN");
        pinField.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        Button transferButton = new Button("Transfer");
        transferButton.setPrefWidth(300);
        transferButton.setPrefHeight(40);
        transferButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        transferButton.setOnMouseEntered(e -> transferButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"));
        transferButton.setOnMouseExited(e -> transferButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"));

        Label messageLabel = new Label();
        messageLabel.setFont(Font.font(12));

        // Load accounts
        loadAccountsIntoCombo(transferFromCombo);
        loadAllAccountsIntoCombo(transferToCombo);

        transferButton.setOnAction(e -> {
            try {
                String fromSelected = transferFromCombo.getValue();
                String toAccountNumber = transferToCombo.getValue();
                
                if (fromSelected == null || toAccountNumber == null) {
                    messageLabel.setText("Please select both accounts");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }

                String fromAccountNumber = fromSelected.split(" ")[0];
                Account from = bankingService.getAccountByNumber(fromAccountNumber);
                Account to = bankingService.getAccountByNumber(toAccountNumber);

                if (from == null || to == null) {
                    messageLabel.setText("Account not found");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }

                if (!authService.verifyPin(currentUser, pinField.getText())) {
                    messageLabel.setText("Invalid PIN");
                    messageLabel.setTextFill(Color.RED);
                    return;
                }

                BigDecimal amount = new BigDecimal(amountField.getText());
                String description = descriptionField.getText().isEmpty() ? "Transfer" : descriptionField.getText();

                bankingService.transfer(from.getId(), to.getId(), amount, description);
                messageLabel.setText("Transfer successful!");
                messageLabel.setTextFill(Color.GREEN);

                amountField.clear();
                descriptionField.clear();
                pinField.clear();
                refreshAllUI();
            } catch (Exception ex) {
                ex.printStackTrace();
                messageLabel.setText("Error: " + ex.getMessage());
                messageLabel.setTextFill(Color.RED);
            }
        });

        vbox.getChildren().addAll(titleLabel, transferFromCombo, transferToCombo, amountField, descriptionField, pinField, transferButton, messageLabel);
        return vbox;
    }

    private VBox createTransactionsPane() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

        Label titleLabel = new Label("Transaction History");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.rgb(65, 105, 225)); // Royal Blue

        transactionsAccountCombo = new ComboBox<>();
        transactionsAccountCombo.setPrefWidth(300);
        transactionsAccountCombo.setPromptText("Select Account");
        transactionsAccountCombo.setStyle("-fx-border-color: #4169E1; -fx-border-width: 1px;");

        transactionsListView = new ListView<>();
        transactionsListView.setPrefHeight(400);
        transactionsListView.setStyle("-fx-font-size: 12px; -fx-border-color: #4169E1; -fx-border-width: 1px;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshButton.setOnMouseEntered(e -> refreshButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-weight: bold;"));
        refreshButton.setOnMouseExited(e -> refreshButton.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-weight: bold;"));

        // Load accounts
        loadAccountsIntoCombo(transactionsAccountCombo);

        transactionsAccountCombo.setOnAction(e -> {
            String selected = transactionsAccountCombo.getValue();
            if (selected != null) {
                String accountNumber = selected.split(" ")[0];
                try {
                    Account account = bankingService.getAccountByNumber(accountNumber);
                    if (account != null) {
                        reloadTransactions(account.getId(), transactionsListView);
                    }
                } catch (SQLException ex) {
                    transactionsListView.getItems().clear();
                    transactionsListView.getItems().add("Error: " + ex.getMessage());
                }
            }
        });

        refreshButton.setOnAction(e -> {
            refreshAllUI();
        });

        vbox.getChildren().addAll(titleLabel, transactionsAccountCombo, transactionsListView, refreshButton);
        return vbox;
    }

    private void reloadAccounts(ListView<String> listView) {
        listView.getItems().clear();
        if (currentUser == null) {
            return;
        }
        try {
            List<Account> accounts = bankingService.getAccountsForUser(currentUser.getId());
            for (Account a : accounts) {
                listView.getItems().add(a.getAccountNumber() + " - Balance: $" + a.getBalance());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            listView.getItems().add("Error loading accounts: " + e.getMessage());
        }
    }

    private void reloadAllAccounts(ListView<String> listView) {
        listView.getItems().clear();
        try {
            List<String> accountNumbers = bankingService.getAllAccountNumbers();
            for (String accNum : accountNumbers) {
                listView.getItems().add(accNum);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            listView.getItems().add("Error loading accounts: " + e.getMessage());
        }
    }
    
    /**
     * Load accounts into a combo box with balance information
     */
    private void loadAccountsIntoCombo(ComboBox<String> combo) {
        combo.getItems().clear();
        try {
            List<Account> accounts = bankingService.getAccountsForUser(currentUser.getId());
            for (Account acc : accounts) {
                combo.getItems().add(acc.getAccountNumber() + " (Balance: " + acc.getBalance() + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Load all account numbers into a combo box
     */
    private void loadAllAccountsIntoCombo(ComboBox<String> combo) {
        combo.getItems().clear();
        try {
            List<String> allAccounts = bankingService.getAllAccountNumbers();
            for (String accNum : allAccounts) {
                combo.getItems().add(accNum);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Refresh all UI components after operations
     */
    private void refreshAllUI() {
        if (currentUser == null) return;
        
        // Refresh accounts list
        if (accountsListView != null) {
            reloadAccounts(accountsListView);
        }
        
        // Refresh all accounts list
        if (allAccountsListView != null) {
            reloadAllAccounts(allAccountsListView);
        }
        
        // Refresh combo boxes
        if (depositAccountCombo != null) {
            loadAccountsIntoCombo(depositAccountCombo);
        }
        
        if (withdrawAccountCombo != null) {
            loadAccountsIntoCombo(withdrawAccountCombo);
        }
        
        if (transferFromCombo != null) {
            loadAccountsIntoCombo(transferFromCombo);
        }
        
        if (transferToCombo != null) {
            loadAllAccountsIntoCombo(transferToCombo);
        }
        
        if (transactionsAccountCombo != null) {
            loadAccountsIntoCombo(transactionsAccountCombo);
            
            // Refresh transactions if an account is selected
            String selected = transactionsAccountCombo.getValue();
            if (selected != null && transactionsListView != null) {
                String accountNumber = selected.split(" ")[0];
                try {
                    Account account = bankingService.getAccountByNumber(accountNumber);
                    if (account != null) {
                        reloadTransactions(account.getId(), transactionsListView);
                    }
                } catch (SQLException ex) {
                    transactionsListView.getItems().clear();
                    transactionsListView.getItems().add("Error: " + ex.getMessage());
                }
            }
        }
    }

    private void reloadTransactions(int accountId, ListView<String> listView) {
        listView.getItems().clear();
        try {
            List<BankTransaction> transactions = bankingService.getTransactionHistory(accountId);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            if (transactions.isEmpty()) {
                listView.getItems().add("No transactions found");
            } else {
                for (BankTransaction t : transactions) {
                    String type;
                    String fromAccount = null;
                    String toAccount = null;
                    
                    if (t.getFromAccountId() == null) {
                        type = "DEPOSIT";
                        // For deposit, get the receiving account
                        try {
                            Account toAcc = bankingService.getAccountById(t.getToAccountId());
                            toAccount = toAcc != null ? toAcc.getAccountNumber() : "N/A";
                        } catch (Exception e) {
                            toAccount = "N/A";
                        }
                    } else if (t.getToAccountId() == null) {
                        type = "WITHDRAWAL";
                        // For withdrawal, get the source account
                        try {
                            Account fromAcc = bankingService.getAccountById(t.getFromAccountId());
                            fromAccount = fromAcc != null ? fromAcc.getAccountNumber() : "N/A";
                        } catch (Exception e) {
                            fromAccount = "N/A";
                        }
                    } else {
                        type = "TRANSFER";
                        // For transfer, get both accounts
                        try {
                            Account fromAcc = bankingService.getAccountById(t.getFromAccountId());
                            fromAccount = fromAcc != null ? fromAcc.getAccountNumber() : "N/A";
                            Account toAcc = bankingService.getAccountById(t.getToAccountId());
                            toAccount = toAcc != null ? toAcc.getAccountNumber() : "N/A";
                        } catch (Exception e) {
                            fromAccount = "N/A";
                            toAccount = "N/A";
                        }
                    }
                    
                    String line;
                    if (type.equals("DEPOSIT")) {
                        line = String.format("[%s] %s - To: %s - Amount: $%s - %s",
                            t.getCreatedAt() != null ? t.getCreatedAt().format(formatter) : "N/A",
                            type,
                            toAccount,
                            t.getAmount(),
                            t.getDescription() != null ? t.getDescription() : "");
                    } else if (type.equals("WITHDRAWAL")) {
                        line = String.format("[%s] %s - From: %s - Amount: $%s - %s",
                            t.getCreatedAt() != null ? t.getCreatedAt().format(formatter) : "N/A",
                            type,
                            fromAccount,
                            t.getAmount(),
                            t.getDescription() != null ? t.getDescription() : "");
                    } else {
                        line = String.format("[%s] %s - From: %s - To: %s - Amount: $%s - %s",
                            t.getCreatedAt() != null ? t.getCreatedAt().format(formatter) : "N/A",
                            type,
                            fromAccount,
                            toAccount,
                            t.getAmount(),
                            t.getDescription() != null ? t.getDescription() : "");
                    }
                    listView.getItems().add(line);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            listView.getItems().add("Error loading transactions: " + e.getMessage());
        }
    }
}
