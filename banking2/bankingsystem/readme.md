# Secured Banking System

Système bancaire sécurisé avec implémentation **from scratch** de **SHA-256** et **RSA-2048** en Java.

## 🎯 Objectif du Projet

Démontrer la compréhension des algorithmes cryptographiques fondamentaux en implémentant :
- **SHA-256** (hachage) pour les mots de passe et PINs
- **RSA-2048** (chiffrement asymétrique) pour les transactions bancaires

**IMPORTANT** : Ce projet est à but **pédagogique**. Pour une application en production, utilisez les bibliothèques cryptographiques standards (javax.crypto, Bouncy Castle, etc.).

---

## 📊 Architecture

```
SecuredBankingSystem/
├── src/
│   ├── Main.java                     # Interface JavaFX
│   ├── dao/                          # Data Access Objects
│   │   ├── AccountDao.java
│   │   ├── TransactionDao.java
│   │   └── UserDao.java
│   ├── db/                           # Configuration Base de Données
│   │   ├── DatabaseConfig.java
│   │   ├── DatabaseInitializer.java
│   │   └── DataSourceManager.java
│   ├── model/                        # Modèles de Données
│   │   ├── Account.java
│   │   ├── BankTransaction.java
│   │   └── User.java
│   ├── security/
│   │   ├── auth/
│   │   │   └── SecurityUtils.java    # Utilitaires d'authentification
│   │   ├── encryption/
│   │   │   └── RSAEncryption.java    #  Implémentation RSA from scratch
│   │   └── hashing/
│   │       └── SHA256Hashing.java    #  Implémentation SHA-256 from scratch
│   └── service/
│       ├── AuthService.java          # Service d'authentification
│       ├── BankingService.java       # Opérations bancaires
│       └── RSAKeyService.java        # Gestion des clés RSA
└── pom.xml                           # Configuration Maven
```

---

## 🔄 Système de Migration de Base de Données

### Vue d'ensemble

Le projet inclut un **système de migration automatique** qui permet de mettre à jour le schéma de base de données sans perte de données. Ce système détecte et applique automatiquement les changements de schéma au démarrage de l'application.

### Fonctionnement

**Fichier** : `src/db/DatabaseInitializer.java`

Le système de migration s'exécute automatiquement à chaque démarrage dans la méthode `initialize()` :

```java
public static void initialize() throws SQLException {
    createDatabaseIfNotExists();    // 1. Créer la DB si nécessaire
    createTablesIfNotExist();       // 2. Créer les tables si nécessaire
    migrateSchema();                // 3. ⭐ Appliquer les migrations
}
```

### Migrations Implémentées

#### Migration 1 : Colonne `user_id`
- **Objectif** : Permettre plusieurs comptes bancaires par utilisateur
- **Action** : Ajoute la colonne `user_id` à la table `users` si elle n'existe pas
- **Sécurité** : Vérifie l'existence avant d'ajouter (idempotent)

```sql
ALTER TABLE users ADD COLUMN user_id VARCHAR(64);
CREATE INDEX idx_user_id ON users(user_id);
```

#### Migration 2 : Chiffrement des Transactions
- **Objectif** : Migrer de transactions en clair vers transactions chiffrées RSA
- **Actions** : Ajoute 4 nouvelles colonnes chiffrées
    - `from_account_id_encrypted` (TEXT)
    - `to_account_id_encrypted` (TEXT)
    - `amount_encrypted` (TEXT)
    - `description_encrypted` (TEXT)

```sql
ALTER TABLE transactions ADD COLUMN from_account_id_encrypted TEXT;
ALTER TABLE transactions ADD COLUMN to_account_id_encrypted TEXT;
ALTER TABLE transactions ADD COLUMN amount_encrypted TEXT;
ALTER TABLE transactions ADD COLUMN description_encrypted TEXT;
```

### Compatibilité Ascendante

Le système maintient la **compatibilité avec les anciennes données** :

```java
// TransactionDao.java - Déchiffrement avec fallback
try {
    String encryptedFromId = rs.getString("from_account_id_encrypted");
    if (encryptedFromId != null && !encryptedFromId.isEmpty()) {
        // Nouvelle version : données chiffrées
        String decryptedFromId = rsaKeyService.decrypt(encryptedFromId);
        t.setFromAccountId(Integer.parseInt(decryptedFromId));
    } else {
        // Ancienne version : fallback vers colonnes non chiffrées
        try {
            Integer fromId = rs.getInt("from_account_id");
            if (!rs.wasNull()) {
                t.setFromAccountId(fromId);
            }
        } catch (SQLException e) {
            // Colonne n'existe pas, laisser null
        }
    }
} catch (Exception e) {
    // En cas d'erreur de déchiffrement, essayer les colonnes legacy
}
```

### Détection Automatique des Colonnes

La méthode `columnExists()` vérifie de manière thread-safe si une colonne existe :

```java
private static boolean columnExists(Connection connection, 
                                   String tableName, 
                                   String columnName) throws SQLException {
    String query = """
        SELECT COUNT(*) as count 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = ? 
        AND COLUMN_NAME = ?
        """;
    // ... exécution de la requête
}
```

### Avantages du Système

✅ **Idempotent** : Peut être exécuté plusieurs fois sans effets secondaires  
✅ **Non-destructif** : Ne supprime jamais de données existantes  
✅ **Automatique** : Aucune intervention manuelle nécessaire  
✅ **Thread-safe** : Utilise des requêtes préparées et synchronized blocks  
✅ **Informatif** : Affiche des messages de confirmation dans la console

### Exemple de Sortie Console

```
✓ Database 'secured_banking' verified/created successfully.
✓ Table 'users' verified/created
✓ Table 'accounts' verified/created
✓ Table 'transactions' verified/created
✓ Table 'rsa_keys' verified/created
✓ Migration: Added 'user_id' column to users table
✓ Migration: Added 'from_account_id_encrypted' column
✓ Migration: Added 'to_account_id_encrypted' column
✓ Migration: Added 'amount_encrypted' column
✓ Migration: Added 'description_encrypted' column
✓ Schema migrations completed
```

### Scénarios d'Utilisation

#### Scénario 1 : Première Installation
1. Application détecte qu'aucune base de données n'existe
2. Crée `secured_banking`
3. Crée toutes les tables avec colonnes finales
4. Migrations ne font rien (colonnes déjà présentes)

#### Scénario 2 : Mise à Jour d'une Installation Existante
1. Application détecte une ancienne version de la base
2. Tables `users` et `transactions` existent sans colonnes chiffrées
3. Migrations ajoutent les colonnes manquantes
4. Données existantes restent intactes
5. Nouvelles transactions utilisent le chiffrement

#### Scénario 3 : Installation Déjà à Jour
1. Application détecte que toutes les colonnes existent
2. Migrations vérifient et ne font rien
3. Message "✓ Schema migrations completed"

### Ajouter une Nouvelle Migration

Pour ajouter une migration, modifiez `DatabaseInitializer.migrateSchema()` :

```java
private static void migrateSchema() throws SQLException {
    DataSource dataSource = DataSourceManager.getDataSource();
    try (Connection connection = dataSource.getConnection()) {
        
        // Migration existante...
        
        // Nouvelle migration
        if (!columnExists(connection, "accounts", "ma_nouvelle_colonne")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                    "ALTER TABLE accounts ADD COLUMN ma_nouvelle_colonne VARCHAR(255)"
                );
                System.out.println("✓ Migration: Added 'ma_nouvelle_colonne' column");
            }
        }
        
        System.out.println("✓ Schema migrations completed");
    }
}
```

### Limitations

⚠️ **Pas de rollback** : Les migrations ne peuvent pas être annulées automatiquement  
⚠️ **Migrations additives uniquement** : Conçu pour ajouter des colonnes, pas pour supprimer  
⚠️ **Pas de versioning** : Pas de suivi de numéro de version de schéma

### Recommandations pour Production

Pour un système de production, considérez :

1. **Flyway** ou **Liquibase** : Outils professionnels de migration
   ```xml
   <!-- pom.xml -->
   <dependency>
       <groupId>org.flywaydb</groupId>
       <artifactId>flyway-core</artifactId>
       <version>9.22.0</version>
   </dependency>
   ```

2. **Migrations versionnées** : Fichiers SQL numérotés
   ```
   V1__Initial_schema.sql
   V2__Add_user_id.sql
   V3__Add_encryption_columns.sql
   ```

3. **Rollback automatique** : Capacité à revenir en arrière
4. **Tests de migration** : Vérifier les migrations sur des données de test
5. **Backup automatique** : Sauvegarder avant chaque migration

---

## 💡 Pourquoi ce Système est Important

Dans un projet réel, vous ne pouvez pas simplement **supprimer et recréer** la base de données à chaque modification du schéma car vous perdriez toutes les données des utilisateurs.

Le système de migration permet de :
- ✅ Déployer de nouvelles versions sans perte de données
- ✅ Faire évoluer le schéma progressivement
- ✅ Maintenir plusieurs versions de l'application simultanément
- ✅ Faciliter le développement et le débogage

**Ce système transforme un projet académique en un projet avec des pratiques professionnelles.**

## 🔐 Algorithmes Implémentés

### 1. SHA-256 (Secure Hash Algorithm 256-bit)

**Fichier** : `src/security/hashing/SHA256Hashing.java`

**Implémentation** :
-  Padding du message (ajout de bits pour obtenir un multiple de 512 bits)
-  Initialisation avec les constantes H0-H7 (racines carrées des 8 premiers nombres premiers)
-  64 constantes de tour K (racines cubiques des 64 premiers nombres premiers)
-  Traitement par blocs de 512 bits
-  64 tours de compression par bloc
-  Fonctions de rotation et opérations bitwise (ROTR, Σ, σ)

**Utilisation dans le projet** :
```java
// Hachage des mots de passe
String passwordHash = SHA256Hashing.hash(salt + ":" + password);

// Hachage des PINs
String pinHash = SHA256Hashing.hash("PIN:" + salt + ":" + pin);
```

**Caractéristiques** :
- Sortie : 256 bits (64 caractères hexadécimaux)
- Résistance aux collisions : ~2^128 opérations
- Effet avalanche : Changer 1 bit → ~50% du hash change

### 2. RSA-2048 (Rivest-Shamir-Adleman)

**Fichier** : `src/security/encryption/RSAEncryption.java`

**Implémentation** :
-  Génération de deux nombres premiers p et q (1024 bits chacun)
-  Calcul du module n = p × q (2048 bits)
-  Calcul de l'indicatrice d'Euler φ(n) = (p-1)(q-1)
-  Choix de l'exposant public e = 65537
-  Calcul de l'exposant privé d = e^(-1) mod φ(n)
-  Chiffrement : C = M^e mod n
-  Déchiffrement : M = C^d mod n

**Utilisation dans le projet** :
```java
// Génération de clés
RSAEncryption.KeyPair keyPair = RSAEncryption.generateKeyPair(2048);

// Chiffrement
BigInteger encrypted = RSAEncryption.encrypt(plaintext, keyPair.publicExponent, keyPair.modulus);

// Déchiffrement
BigInteger decrypted = RSAEncryption.decrypt(encrypted, keyPair.privateExponent, keyPair.modulus);
```

**Application dans le projet** :
- Chiffrement des IDs de compte dans les transactions
- Chiffrement des montants de transaction
- Chiffrement des descriptions de transaction

**Limitations** :
- RSA-2048 peut chiffrer maximum ~245 bytes (avec PKCS#1 padding)
- Descriptions limitées à 200 caractères
- Génération des clés : 2-5 secondes au premier démarrage

---

## 🗄️ Base de Données

### Schéma

```sql
-- Table des utilisateurs
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash CHAR(64) NOT NULL,      -- SHA-256
    password_salt CHAR(32) NOT NULL,
    pin_hash CHAR(64) NOT NULL,           -- SHA-256
    pin_salt CHAR(32) NOT NULL,
    user_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des comptes bancaires
CREATE TABLE accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    account_number VARCHAR(32) NOT NULL UNIQUE,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Table des transactions (TOUTES LES DONNÉES CHIFFRÉES)
CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    from_account_id_encrypted TEXT,        -- RSA-2048
    to_account_id_encrypted TEXT,          -- RSA-2048
    amount_encrypted TEXT NOT NULL,        -- RSA-2048
    description_encrypted TEXT,            -- RSA-2048
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des clés RSA
CREATE TABLE rsa_keys (
    id INT AUTO_INCREMENT PRIMARY KEY,
    key_name VARCHAR(64) NOT NULL UNIQUE,
    public_key_modulus TEXT NOT NULL,
    public_key_exponent TEXT NOT NULL,
    private_key_exponent TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🚀 Installation et Démarrage

### Prérequis

- **Java 21** ou supérieur
- **MySQL 8.0+**
- **JavaFX 21** (inclus dans pom.xml)

### Configuration de la Base de Données

1. Démarrez MySQL
2. Modifiez `src/db/DatabaseConfig.java` :

```java
private static final String DEFAULT_USER = "root";
private static final String DEFAULT_PASSWORD = "votre_mot_de_passe";
```

**OU** définissez les variables d'environnement :
```bash
export DB_USER=root
export DB_PASS=votre_mot_de_passe
```

### Compilation et Exécution

```bash
# Compiler le projet
javac Main (ou mvn compile si on utilise Maven)

# Lancer l'application
java Main (ou mvn javax:run si on utilise Maven)
```

La base de données et les tables seront créées automatiquement au premier lancement.

---

## 🧪 Fonctionnalités

### 1. Authentification
-  Création de compte avec mot de passe et PIN
-  Connexion sécurisée
-  Hachage SHA-256 avec salt pour les mots de passe
-  Hachage SHA-256 avec salt pour les PINs

### 2. Gestion des Comptes
-  Création de comptes bancaires
-  Consultation du solde
-  Support de multiples comptes par utilisateur

### 3. Opérations Bancaires
-  **Dépôt** : Ajouter de l'argent sur un compte
-  **Retrait** : Retirer de l'argent d'un compte
-  **Transfert** : Transférer entre deux comptes
-  Validation du PIN pour toutes les opérations

### 4. Sécurité des Transactions
-  Toutes les transactions sont chiffrées avec RSA-2048
-  Les IDs de compte sont chiffrés
-  Les montants sont chiffrés
-  Les descriptions sont chiffrées
-  Transactions atomiques avec JDBC (commit/rollback)
-  Verrouillage pessimiste (FOR UPDATE) pour éviter les race conditions

### 5. Historique
-  Consultation de l'historique des transactions
-  Déchiffrement automatique pour l'affichage
-  Filtrage par compte

---

## 📈 Validations et Limites

| Élément | Limite | Raison |
|---------|--------|--------|
| **Montant minimum** | 0.01 | Transactions significatives |
| **Montant maximum** | 1,000,000.00 | Protection contre erreurs |
| **Description** | 200 caractères | Limite RSA-2048 |
| **PIN** | 4-8 chiffres | Sécurité vs usabilité |
| **Mot de passe** | 8+ caractères | Sécurité minimale |

---

## 🔬 Tests Recommandés

### Test 1 : SHA-256
```java
String hash1 = SHA256Hashing.hash("test");
String hash2 = SHA256Hashing.hash("test");
// hash1 == hash2 (déterministe)

String hash3 = SHA256Hashing.hash("Test");
// hash3 != hash1 (sensible à la casse)
```

### Test 2 : RSA
```java
KeyPair kp = RSAEncryption.generateKeyPair(2048);
BigInteger msg = new BigInteger("12345");
BigInteger encrypted = RSAEncryption.encrypt(msg, kp.publicExponent, kp.modulus);
BigInteger decrypted = RSAEncryption.decrypt(encrypted, kp.privateExponent, kp.modulus);
// msg == decrypted
```

### Test 3 : Scénario Complet
1. Créer un utilisateur
2. Se connecter
3. Créer un compte
4. Déposer 1000.00
5. Retirer 200.00
6. Créer un deuxième compte
7. Transférer 300.00 du premier au deuxième compte
8. Vérifier l'historique des transactions

---

## ⚠️ Limitations Connues (Projet Académique)

### Sécurité
-  **Pas de padding OAEP pour RSA** : Vulnérable aux attaques (OK pour projet éducatif)
-  **Une seule itération de SHA-256** : Production nécessite PBKDF2 avec 10,000+ itérations
-  **Clé privée RSA stockée en clair** : Production nécessite HSM ou chiffrement avec clé maître
-  **Pas de 2FA** : Authentification à facteur unique

### Performance
-  **Génération RSA lente** : 2-5 secondes au premier démarrage
-  **Déchiffrement lent** : Chaque transaction nécessite 3 déchiffrements RSA
-  **Pas de cache** : Les transactions sont déchiffrées à chaque affichage

### Recommandations pour Production
1. Utiliser `javax.crypto.Cipher` avec padding OAEP pour RSA
2. Utiliser PBKDF2, bcrypt ou Argon2 pour les mots de passe
3. Utiliser chiffrement hybride (RSA pour clé AES, AES pour données)
4. Stocker les clés privées dans un HSM ou coffre-fort sécurisé
5. Implémenter un système de logging et d'audit
6. Ajouter des tests unitaires complets
7. Implémenter rate limiting pour prévenir brute force

---

## 📚 Références Académiques

### SHA-256
- **FIPS 180-4** : Secure Hash Standard (SHS)
- **RFC 6234** : US Secure Hash Algorithms

### RSA
- **RFC 8017** : PKCS #1: RSA Cryptography Specifications Version 2.2
- **Rivest, Shamir, Adleman (1977)** : "A Method for Obtaining Digital Signatures and Public-Key Cryptosystems"

### Ressources
- NIST Cryptographic Standards : https://csrc.nist.gov/
- Applied Cryptography (Bruce Schneier)
- Cryptography Engineering (Ferguson, Schneier, Kohno)

---

## 👥 Auteur

**Projet Académique** - Implémentation de SHA-256 et RSA from scratch

---

## 📄 Licence

Ce projet est à but **éducatif uniquement**. Ne pas utiliser en production.

---

## 🎓 Démonstrations Pédagogiques

Pour voir les algorithmes en action de manière isolée :

```bash
# Démonstration SHA-256
java -cp target/classes demo.SHA256Demo

# Démonstration RSA
java -cp target/classes demo.RSADemo

# Tests unitaires
java -cp target/classes test.SecurityTest
```

*(Créer ces fichiers si nécessaire)*

---

## Support

Pour toute question sur l'implémentation des algorithmes, consultez :
- Les commentaires dans `SHA256Hashing.java`
- Les commentaires dans `RSAEncryption.java`
- La documentation académique citée ci-dessus
