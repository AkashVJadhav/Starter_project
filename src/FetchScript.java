import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.simple.JSONObject;

public class FetchScript {

	private static Logger logger = Logger.getLogger(UserInterface.class);

	private String directory, branchDirectory;
	private JSONObject pathObj;

	// TODO constructors default
	FetchScript(JSONObject pathObj) {
		this.pathObj = pathObj;
		directory = pathObj.get("directoryPath").toString();
		branchDirectory = pathObj.get("branchDirectory").toString();
	}

	public void cloneFolder() {
		try {

			if (UserInterface.deleteDirectory(new File(directory))) {
				// User input
				Scanner input = new Scanner(System.in);
				System.out.println("Enter username :");
				String username = input.nextLine();
				System.out.println("Enter password :");
				String password = input.nextLine();

				final String repositoryLink = pathObj.get("repositoryLink").toString();
				String branch = pathObj.get("branchName").toString();

				Git repo = Git.cloneRepository()
						.setURI(repositoryLink)
						.setDirectory(new File(directory))
						.setBranchesToClone(Arrays.asList(branch))
						.setBranch(branch)
						.setCloneAllBranches(false)
						.setCloneSubmodules(true)
						.setNoCheckout(true)
						.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();

				repo.checkout().setStartPoint("origin/new").addPath(branchDirectory).call();

				logger.info("Successfully downloaded the repository!");
			} else {
				System.out.println("Please modify your directory path in system.");
				System.exit(0);
			}

		} catch (GitAPIException | JGitInternalException e) {
			logger.error(e.getMessage());
			System.exit(0);
		}
	}

	// TODO spell check
	public void retriveScript() {

		try {
						
			File destination = new File(pathObj.get("storagePath").toString());
			
			if (destination.exists()) {
				UserInterface.deleteDirectory(destination);
			}

			File[] listofFiles = new File(directory +"/"+ branchDirectory).listFiles();
			if (listofFiles != null ) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
				Scanner sc = new Scanner(System.in);

				System.out.println("Enter start date (MM/dd/yyyy)");
				int startDate = Integer.parseInt(formatter.format(new Date(sc.nextLine())));

				System.out.println("Enter end date (MM/dd/yyyy)");
				int endDate = Integer.parseInt(formatter.format(new Date(sc.nextLine())));

				String content = "";
				for (int file = 0; file < listofFiles.length; file++) {
					File Filename = new File(listofFiles[file].getName().toString());
					int Schemaversion = Integer.parseInt(Filename.toString().split("_")[0]);

					try {
						if (startDate <= Schemaversion && endDate >= Schemaversion) {
							Scanner scan = new Scanner(listofFiles[file]);
							FileWriter writer = new FileWriter(destination);
							while (scan.hasNextLine()) {
								content = content.concat(scan.nextLine() + "\n");
							}
							writer.write(content);
							writer.close();
							System.out.println(Filename + " retrive succesfully at " + destination);
							scan.close();
						}
					}
					catch (IOException e) {
						logger.error(e.getMessage());
					}
				}
			}else
			{
				logger.warn(directory +"/"+ branchDirectory + " folder not present");
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

}
