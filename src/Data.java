import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Scanner;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jgit.api.Git;

public class Data {

	static Logger logger = Logger.getLogger(Data.class);

	public static void deleteDirectory(File file) {
		if (file.exists()) {
			for (File subfile : file.listFiles()) {
				if (subfile.isDirectory()) {
					deleteDirectory(subfile);
				}
				subfile.delete();
			}
			file.delete();
		} else {
			logger.info("Folder is not exist");
		}

	}

	public static void main(String[] args) throws Exception {

		PropertyConfigurator.configure("log4j.properties");

		JSONParser jsonparser = new JSONParser();
		FileReader reader = new FileReader(".\\jsonfiles\\data.json");
		JSONObject pathObj = (JSONObject) jsonparser.parse(reader);

		final String directory = pathObj.get("directoryPath").toString();

		try {

			deleteDirectory(new File(directory));

			// User input
			String username, password;
			Scanner input = new Scanner(System.in);
			System.out.println("Enter username :");
			username = input.nextLine();
			System.out.println("Enter password :");
			password = input.nextLine();

			final String repositoryLink = pathObj.get("repositoryLink").toString();
			String branch = pathObj.get("branchName").toString();
			String branchDirectory = pathObj.get("branchDirectory").toString();

			Git repo = Git.cloneRepository()
						.setURI(repositoryLink)
						.setDirectory(new File(directory))
						.setBranchesToClone(Arrays.asList(branch))
						.setBranch(branch).setCloneAllBranches(false)
						.setCloneSubmodules(true).setNoCheckout(true)
						.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();

			repo.checkout().setStartPoint("origin/new").addPath(branchDirectory).call();

			logger.info("Successfully downloaded the repository!");

		} catch (GitAPIException | JGitInternalException e) {
			logger.error(e.getMessage());
			System.exit(0);
		}

		try {

			// Select database
			String dbUsername, dbPassword, dbName;
			JSONObject dbDetails;
			JSONArray databaseList = (JSONArray) pathObj.get("databases");

			Scanner input = new Scanner(System.in);
			System.out.println("=========================");
			System.out.println(" 1. project \n 2. project1 \n 3. project2");
			System.out.print("Select database : ");
			int databaseId = input.nextInt();
			
			if (databaseList.size() < databaseId) {
				System.out.println("Incorrect option");
			}
			

			dbDetails = (JSONObject) databaseList.get(databaseId - 1);
			dbName = dbDetails.get("name").toString();
			dbUsername = dbDetails.get("username").toString();
			dbPassword = dbDetails.get("password").toString();

			// connecting establishment..
			Class.forName("com.mysql.jdbc.Driver");
			Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + dbName, dbUsername,
					dbPassword);

			Statement stmt = con.createStatement();

			int previousSchemaversion = 0;

			// Check table exist or not
			DatabaseMetaData dbm = con.getMetaData();
			ResultSet tables = dbm.getTables(dbName, null, "Schema_version", null);

			if (tables.next()) {
				String max_Tskey = "SELECT MAX(DB_END_TSKEY) as max_end_tskey FROM SCHEMA_VERSION;";
				ResultSet result = stmt.executeQuery(max_Tskey);
				if (result.next()) {
					previousSchemaversion = result.getInt("max_end_tskey");
				}
			}

			// get all files from folder
			File[] listofFiles = new File(directory + "/Scripts").listFiles();

			// execute scripts files one by one
			for (int i = 0; i < listofFiles.length; i++) {
				File Filename = new File(listofFiles[i].getName().toString());
				String newSchemaversion = Filename.toString().split("_")[0];

				if (previousSchemaversion < Integer.parseInt(newSchemaversion)) {
					Scanner sc = new Scanner(new File(listofFiles[i].toString()));
					logger.info(Filename +" Execution started.");
					while (sc.hasNext()) {
						String query = sc.nextLine();
						if (!query.isEmpty()) {
							logger.info(query);
							stmt.execute(query);
						}
					}
					logger.info(Filename + " Successfully executed...");
				}
			}

			System.out.println("Execution completed..");
			con.close();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
}
