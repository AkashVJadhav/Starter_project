import java.io.File;
import java.io.FileReader;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class UserInterface {
	// TODO better classname
	private static Logger logger = Logger.getLogger(UserInterface.class);

	// TODO return type
	public static boolean deleteDirectory(File file) {

		// TODO file check directory.
		if (file.exists()) {
			if (file.listFiles() != null) {
				for (File subfile : file.listFiles()) {

					if (subfile.isDirectory()) {
						deleteDirectory(subfile);
					}
					subfile.delete();
				}
			}
			file.delete();
			return true;

		} else {
			logger.info("Folder is not exist");
			return true;
		}
	}

	public static void main(String[] args) throws Exception {

		PropertyConfigurator.configure("log4j.properties");

		JSONParser jsonparser = new JSONParser();
		FileReader reader = new FileReader("./jsonfiles/data.json");
		JSONObject pathObj = (JSONObject) jsonparser.parse(reader);

		FetchScript clone = new FetchScript(pathObj);
		clone.cloneFolder();

		Scanner input = new Scanner(System.in);

		System.out.println("---- Select Option ----");
		System.out.println("1. Update Database");
		System.out.println("2. Retrive Scripts");
		System.out.println("Choose Option :");
		int option = input.nextInt();

		switch (option) {
		case 1:
			DatabaseOperation db = new DatabaseOperation(pathObj);
			db.update();
			break;
		case 2:
			clone.retriveScript();
			break;
		default:
			System.out.println("Incorrect option");
		}
		input.close();
	}
}