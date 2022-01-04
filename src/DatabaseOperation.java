import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DatabaseOperation {
	private static Logger logger = Logger.getLogger(UserInterface.class);

	private String directory, dbName;
	private JSONObject dbDetails;
	private JSONArray databaseList;
	private Connection con;
	private int option = 0;

	DatabaseOperation(JSONObject pathObj) {
		directory = pathObj.get("directoryPath").toString();
		databaseList = (JSONArray) pathObj.get("databases");
		dbConnection();
	}

	private int userInput() {
		// Select database
		Scanner input = new Scanner(System.in);
		System.out.println("=========================");
		System.out.println(" 1. project \n 2. project1 \n 3. project2");
		System.out.print("Select database : ");
		option = input.nextInt();
		if (databaseList.size() < option || option <= 0 ) {
			System.out.println("Incorrect option");
			System.exit(0);
		} 
		return option;
	}

	// connecting establishment..
	private void dbConnection() {
		try {
			int selectedDb = userInput();
			dbDetails = (JSONObject) databaseList.get(selectedDb - 1);
			dbName = dbDetails.get("name").toString();
			String dbUsername = dbDetails.get("username").toString();
			String dbPassword = dbDetails.get("password").toString();

			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + dbName, dbUsername, dbPassword);
		} 
		catch (ClassNotFoundException | SQLException e) {
			logger.error(e.getMessage());
		}
	}

	private int getSchemaVersion() {
		int endkey = 0;
		try {
			Statement stmt = con.createStatement();

			DatabaseMetaData dbm = con.getMetaData();
			ResultSet tables = dbm.getTables(dbName, null, "Schema_version", null);
			if (tables.next()) {
				String max_Tskey = "SELECT MAX(DB_END_TSKEY) as max_end_tskey FROM SCHEMA_VERSION;";
				ResultSet result = stmt.executeQuery(max_Tskey);
				if (result.next()) {
					endkey = result.getInt("max_end_tskey");
				}
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return endkey;
	}

	public void update() {
		try {
			Statement stmt = con.createStatement();

			// get all files from folder
			File[] listofFiles = new File(directory + "/Scripts").listFiles();

			// execute scripts files one by one
			for (int i = 0; i < listofFiles.length; i++) {
				File Filename = new File(listofFiles[i].getName().toString());
				String newSchemaversion = Filename.toString().split("_")[0];

				// TODO names
				if (getSchemaVersion() < Integer.parseInt(newSchemaversion)) {
					Scanner sc = new Scanner(new File(listofFiles[i].toString()));
					logger.info(Filename + " Execution started.");
					while (sc.hasNext()) {
						try {
							String query = sc.nextLine();
							// TODO try catch
							if (!query.isEmpty()) {
								logger.info(query);
								stmt.execute(query);
							}
						} catch (SQLException | NoSuchElementException e) {
							logger.error(e.getMessage());
						}

					}
					logger.info(Filename + " Successfully executed...");
				}
			}

			System.out.println("Execution completed..");
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		} 
		finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}
}
