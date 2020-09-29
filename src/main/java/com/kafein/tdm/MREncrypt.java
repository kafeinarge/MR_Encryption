package com.kafein.tdm;

import com.kafein.tdm.connection.ConnectionTest;
import com.kafein.tdm.enumaration.DatabaseType;
import com.kafein.tdm.validator.IsPortValidator;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

import java.io.*;
import java.util.Properties;

import java.util.Scanner;

/*
 Utility class to provide encryption for the application setting file
 */
public class MREncrypt {

    private final String appPropFileName = "application.properties";

    public static void main(String[] args) {
        MREncrypt mrEncrypt = new MREncrypt();

        if (args.length < 6) {
            System.out.println("Some parameters are missing!");
            System.out.println("Usage : java - jar MREncrypt host port database username password propFilePath");
            System.exit(1);
        }
        IsPortValidator isPortValidator = new IsPortValidator();
        if (!isPortValidator.validate(args[1])) {
            System.out.println("Please enter valid port!");
            System.exit(1);
        }
        mrEncrypt.encryptPropertiesWithArguments(mrEncrypt.getDbUrl(args[0], args[1], args[2]), args[3], args[4], args[5],true);


    }


    // prepare encrypted application file by using command line arguments
    public void encryptPropertiesWithArguments(String dbUrl, String dbUsername, String dbPassword,
                                               String propFilePath, boolean isStandAlone) {

        ConnectionTest connectionTest = new ConnectionTest();
        if (!connectionTest.isConnectedToDatabase(DatabaseType.MSSQL, dbUrl, dbUsername, dbPassword)) {
            return;
        }

        StringEncryptor encryptor = getStringEncryptor();

        Properties prop = new Properties();

        try {
            File file = new File(propFilePath);
            if (!file.exists() || !file.isDirectory()) {
                System.out.println("You entered : " + propFilePath + "\n");
                System.out.println("Please enter a valid path \n");
                return;
            }

            String propertiesFile = formatPath(propFilePath);
            if (isStandAlone) {
                if (!checkOldPassword(propertiesFile)) {
                    System.out.println("You have entered wrong password 3 times!");
                    return;
                }
            }
            // to keep lang property first
            if (new File(propertiesFile).exists()) {
                InputStream in = new FileInputStream(propertiesFile);
                prop.load(in);
                in.close();
            }

            OutputStream output = new FileOutputStream(propertiesFile);

            String encryptedUN = encryptor.encrypt(dbUsername);
            String encryptedPS = encryptor.encrypt(dbPassword);

            prop.setProperty("spring.datasource.hikari.connectionTimeout", "20000");
            prop.setProperty("spring.datasource.hikari.maximumPoolSize", "5");
            prop.setProperty("spring.jpa.hibernate.ddl-auto", "update");
            prop.setProperty("server.port", "8084");
            prop.setProperty("spring.datasource.username", "ENC(" + encryptedUN + ")");
            prop.setProperty("spring.datasource.password", "ENC(" + encryptedPS + ")");
            prop.setProperty("spring.datasource.url", dbUrl);
            prop.setProperty("spring.datasource.url", dbUrl);
            prop.setProperty("jasypt.encryptor.bean", "mrEncryptor");
            prop.store(output, null);

            output.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // format path of application properties
    private String formatPath(String path) {
        if (path.lastIndexOf("/") == path.length() - 1) {
            path = path.substring(0, path.length() - 1) + "/" + appPropFileName;
        } else if (path.lastIndexOf("\\") == path.length() - 1) {
            path = path.substring(0, path.length() - 1) + "\\" + appPropFileName;
        } else {
            if (path.contains("\\")) {
                path = path + "\\" + appPropFileName;
            } else {
                path = path + "/" + appPropFileName;
            }
        }
        return path;
    }

    // returns postgres url
    public String getDbUrl(String dbHost, String dbPort, String dbDatabase) {
        return "jdbc:sqlserver://" + dbHost + ":" + dbPort + ";" + "database=" + dbDatabase + ";authenticationScheme=NTLM;";
    }

    // check if old db password is true
    public boolean checkOldPassword(String propertiesFile) {
        Properties prop = new Properties();
        boolean result = false;
        try {
            File file = new File(propertiesFile);

            if (file.exists()) {

                InputStream input = new FileInputStream(file.getAbsolutePath());

                prop.load(input);
                input.close();

                String dbPassword = prop.getProperty("spring.datasource.password");

                StringEncryptor encryptor = getStringEncryptor();
                String encryptedPass = encryptor.decrypt(getEncryptedText(dbPassword));


                String oldPass = getPassword(false);
                while (oldPass.isEmpty() || oldPass == null) {
                    oldPass = getPassword(false);
                }
                int count = 1;
                while (!oldPass.equals(encryptedPass) && count < 3) {
                    oldPass = getPassword(true);
                    count++;
                }
                if (!(count == 3 && !oldPass.equals(encryptedPass))) {
                    result = true;
                }
            } else {
                result = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    // read old db password from terminal
    private String getPassword(boolean isEnteredBefore) {
        Scanner reader = new Scanner(System.in);
        if (isEnteredBefore) {
            System.out.println("Old password doesn't match. Please try again : ");
        } else {
            System.out.println("Please enter old db password to continue : ");
        }
        return reader.nextLine();
    }

    public StringEncryptor getStringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword("KAFEIN_2020!");
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    private String getEncryptedText (String text) {
        if (text.startsWith("ENC(")) {
            return text.substring(4, text.length()-1);
        }
        return text;
    }
}
