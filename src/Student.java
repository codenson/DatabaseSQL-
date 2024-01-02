
/**
 * Project to populate database and to extract requested info using SQL. 
 * 
 */
import java.io.*;
import java.sql.*;
import java.util.*;
import oracle.jdbc.driver.*;
import org.apache.ibatis.jdbc.ScriptRunner;

public class Student {

    /**
     * database connection.
     */
    static Connection con;
    /**
     * Statment to use for database execution.
     */
    static Statement stmt;
    /**
     * database.
     */
    static DatabaseMetaData dbmd;
    /**
     * Scanner to be used to prompt the user for data.
     */
    static Scanner scan = new Scanner(System.in);

    /**
     * main method.
     *
     * @param argv not used.
     */
    public static void main(String[] argv) {

        Connection con = getConn();

        try {
            runScript(con);
        } catch (FileNotFoundException ex) {
        
        } catch (SQLException ex) {
            System.err.println("SQL Error.");
        } catch (IOException ex) {
           System.err.println("IO Error.");
        }
        String choice = "";

        while (true) {
            scan = new Scanner(System.in);
            menuPrompt();
            choice = scan.nextLine();

            if (choice.equals("1")) {
                tablesChoiceMenu();

            } else if (choice.equals("2")) {

                searchByPUBLICATIONID("", 0);
            } else if (choice.equals("3")) {

                updateURL();
            } else if (choice.equals("4")) {
                break;

            }

        }

        scan.close();
        closeProgram();

    }

    /**
     * Method to update the URL section of Publications table. It prompts the
     * user for a file and id number.
     */
    public static void updateURL() {

        int id = -1;
        System.out.println("Please enter a filename:");
        String filename = scan.nextLine();
        while (true) {

            System.out.println("Please enter an integer:");
            String input = scan.nextLine();

            try {
                id = Integer.parseInt(input);
                break;
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer. Please try again.");
            }
        }

        String URL = findLinkByNumber(filename, id);

        if (URL == null) {
            System.out.println("ID does not exit. URL " + URL);
            return;
        }

        String sql = "UPDATE PUBLICATIONS SET URL = ? WHERE PUBLICATIONID = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, URL);
            pstmt.setInt(2, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Update successful");
                searchByPUBLICATIONID(id + "", 100);
            } else {
                System.err.println("Update failed, ID does not exist in the database. Try again...");
            }
        } catch (SQLException ex) {
            System.err.println("Updating the Database Failed.");

        }

    }

    /**
     * Helper method to parse the file to retieve the new URL if it exists based
     * on the id number the user is intended to update.
     *
     * @param filePath Excel file that contains publication id and updated URL.
     * @param targetNumber
     * @return
     */
    public static String findLinkByNumber(String filePath, int targetNumber) {
        // filePath = "C:\\Users\\guero\\Downloads\\url.csv"; 

        scan = null;

        try {
            scan = new Scanner(new File(filePath));
            while (scan.hasNextLine()) {
                String line = scan.nextLine();

                String[] parts = line.split(",");
                if (parts.length > 0) {

                    try {

                        int val = Integer.parseInt(parts[0]);

                        if (val == targetNumber) {
                            return parts[1].trim(); // URL is in the second part
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Number format exception for input: " + parts[0]);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filePath);
        } finally {
            if (scan != null) {
                scan.close();
            }
        }

        return null;
    }

    /**
     * Method to prompt the user to seach the table by ID and retirves and
     * displays the tuple if it exists.
     */
    public static void tablesChoiceMenu() {
        String answer = "";

        System.out.println("Which table would you like to see:");

        System.out.println("PUBLICATIONS (Yes/No):");
        answer = scan.nextLine().toLowerCase(); // Read the user's input and convert to lowercase 

        if (!answer.equals("no") && !answer.equals("yes")) {
            System.err.println("Invalid Entry. Only.\"Yes\" or \"No\" are accepted. Try again ..." + answer);
            return;
        }

        if (answer.equals("yes")) {
            // Display the PUBLICATIONS table
            String sqlCommand = "SELECT * FROM PUBLICATIONS";
            executeSqlCommand(sqlCommand);

        }

        answer = "";

        System.out.println("AUTHORS (Yes/No):");
        answer = scan.nextLine().toLowerCase(); // Read the user's input and convert to lowercase 
        if (!answer.equals("no") && !answer.equals("yes")) {
            System.err.println("Invalid Entry. Only.\"Yes\" or \"No\" are accepted. Try again ...");
            return;
        }

        if (answer.equals("yes")) {
            // Display the AUTHORS table
            String sqlCommand = "SELECT * FROM AUTHORS";
            executeSqlCommand(sqlCommand);
        }

    }

    /**
     * Method to display the menu.
     */
    public static void menuPrompt() {
        System.out.println("----------------------------------------------");
        System.out.println("    View table contents                   : 1 ");
        System.out.println("    Search by PUBLICATIONID               : 2 ");
        System.out.println("    Update URL by PUBLICATIONID           : 3 ");
        System.out.println("    Exit                                  : 4 ");
        System.out.println("----------------------------------------------");
    }

    /**
     * Method to search fot publication by ID.
     *
     * @param id publication Id
     * @param flag flag higher than zero prints too few dommains.
     */
    public static void searchByPUBLICATIONID(String id, int flag) {
        //Scanner scan = new Scanner(System.in);
        int check = 0;
        String publicationIdToSearch = "";
        if (id == "") {
            System.out.print("Enter PUBLICATIONID: ");
            while (true) {
                System.out.println("Please enter an integer:");
                String input = scan.nextLine();

                try {
                    int number = Integer.parseInt(input);
                    publicationIdToSearch = input;
                    break; // Exit the loop if input is a valid integer
                } catch (NumberFormatException e) {
                    System.err.println("Invalid Integer Entry. Please try again.");
                }
            }
        } else {

            publicationIdToSearch = id;
        }

        String sqlQuery = "SELECT P.PUBLICATIONID, P.YEAR, P.TYPE, P.TITLE, P.URL, COUNT(A.AUTHOR) AS TOTAL_AUTHORS "
                + "FROM PUBLICATIONS P "
                + "LEFT JOIN AUTHORS A ON P.PUBLICATIONID = A.PUBLICATIONID "
                + "WHERE P.PUBLICATIONID = ? "
                + "GROUP BY P.PUBLICATIONID, P.YEAR, P.TYPE, P.TITLE, P.URL";

        System.out.println("-------------------------------------------------------------------------------------------");

        try (PreparedStatement preparedStatement = con.prepareStatement(sqlQuery)) {
            preparedStatement.setString(1, publicationIdToSearch); // Set the parameter value correctly to index 1

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    // Retrieve and display the result data
                    check++;

                    String publicationId = resultSet.getString("PUBLICATIONID");
                    String year = resultSet.getString("YEAR");
                    String type = resultSet.getString("TYPE");
                    String title = resultSet.getString("TITLE");
                    String url = resultSet.getString("URL");
                    int totalAuthors = resultSet.getInt("TOTAL_AUTHORS");

                    if (flag == 0) {
                        System.out.println("publicationId  |year  | type       |   title                 |    url                  | totalAuthors  ");
                        System.out.println("------------------------------------------------------------------------------------------------------");
                        System.out.println("   " + publicationId + "        " + year + "    " + type + "  " + title + "  " + url + "      " + totalAuthors);

                    } else {
                        System.out.println("publicationId  |year  | type       |   title                 |    url                  |");
                        System.out.println("------------------------------------------------------------------------------------------------------");
                        System.out.println("   " + publicationId + "        " + year + "    " + type + "  " + title + "  " + url + "      ");

                    }

                }
                if (check == 0) {
                    System.err.println("No Records were found for the id provided. ");
                }
            }
        } catch (SQLException e) {
    
        }

        System.out.println("-------------------------------------------------------------------------------------------");
    }

    /**
     * Connects to oracle server.Prompts the user for username and password.
     *
     * @return the connection.
     */
    public static Connection getConn() {

        String driverPrefixURL = "jdbc:oracle:thin:@";
        String jdbc_url = "artemis.vsnet.gmu.edu:1521/vse18c.vsnet.gmu.edu";

        int i = 0;

        while (i++ < 3) {
            System.out.println("Enter ORACLE Server user name: ");
            String username = scan.nextLine();
            System.out.println("Enter ORACLE Server PASSWORD: ");
            String password = scan.nextLine();

            try {
                //Register Oracle driver
                DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            } catch (Exception e) {
                System.out.println("Failed to load JDBC/ODBC driver.");
                return null;
            }

            try {
                System.out.println(driverPrefixURL + jdbc_url);
                con = DriverManager.getConnection(driverPrefixURL + jdbc_url, username, password);
                dbmd = con.getMetaData();
                dbmd = con.getMetaData();
                stmt = con.createStatement();
                System.out.println("Connected.");
                break;
            } catch (SQLException e) {
                System.out.println("Wrong username or password. System terminating now..");
            }

        }

        return con;

    }

    /**
     * helper Method to retrieve data of the server.
     *
     * @param command sql command to be executed.
     */
    public static void executeSqlCommand(String command) {
        try {
         
            ResultSet resultSet = stmt.executeQuery(command);

            while (resultSet.next()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

               
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    System.out.print(columnName + "\t");
                }
                System.out.println(); // Move to the next line for data

                // Print data rows
                for (int i = 1; i <= columnCount; i++) {
                    String columnValue = resultSet.getString(i);
                    System.out.print(columnValue + "\t");
                }
                System.out.println();
            }

        } catch (SQLException ex) {

        }

    }

    /**
     * Method to run populate the data into the database.
     *
     * @param con the connection it establishes.
     * @throws SQLException
     * @throws IOException
     */
    public static void runScript(Connection con) throws SQLException, IOException {
        while (true) {
            System.out.println("Enter the name of the file to be populated:");
            String file = scan.nextLine();

            if (!file.toLowerCase().endsWith(".sql")) {
                System.err.println("The file does not appear to be an SQL script. Please enter a valid SQL file.");
                continue; 
            }

            try (Reader reader = new BufferedReader(new FileReader(file))) {
                ScriptRunner sr = new ScriptRunner(con);
                sr.runScript(reader);
                break; // Break out of the loop if script runs successfully
            } catch (FileNotFoundException ex) {
                System.err.println("No File Exists: " + file + ". Please try again.");

            }

        }
    }

    /**
     * Displays database closing info.
     */
    public static void closeProgram() {
        if (dbmd == null) {
            System.out.println("No database meta data");
        } else {
            try {
                System.out.println("Database Product Name: " + dbmd.getDatabaseProductName());
                System.out.println("Database Product Version: " + dbmd.getDatabaseProductVersion());
                System.out.println("Database Driver Name: " + dbmd.getDriverName());
                System.out.println("Database Driver Version: " + dbmd.getDriverVersion());
            } catch (SQLException ex) {
            }
        }

    }

}
