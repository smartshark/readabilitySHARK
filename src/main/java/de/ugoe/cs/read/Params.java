package de.ugoe.cs.read;

import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;

/**
 * Singleton Parameter holder for CLI Params and Options.
 */
public class Params {

    private static Params instance;

    private String repositoryUrl;
    private String repositoryPath;
    private String revision;
    private String projectName;

    private String logLevel;

    private String dbUser;
    private String dbPassword;
    private String dbName;
    private String dbAuthenticationDatabase;
    private String dbHostname;
    private String dbPort;
    private Boolean dbSsl;

    private Boolean help = false;

    private String outputFile;

    private Params () {}

    public static synchronized Params getInstance() {
        if (Params.instance == null) {
            Params.instance = new Params();
        }
        return Params.instance;
    }

    public void init(String[] args) {
        Options options = new Options();

        options.addOption("help", false, "print this message" );
        options.addOption("project_name", true, "Name of the project.");
        options.addOption("u", "repository_url", true, "URL of the repository.");
        options.addOption("i", "input", true, "Path of the cloned repository.");
        options.addOption("r", "rev", true, "Revision of the commit.");
        options.addOption("ll", "log_level", true, "Log level for stdout (DEBUG, INFO)");

        options.addOption("U", "db_user", true, "User of the MongoDB.");
        options.addOption("P", "db_password", true, "Password of the MongoDB user.");
        options.addOption("DB", "db_database", true, "Database to write to in the MongoDB.");

        options.addOption("a", "db_authentication", true, "Authentication database of the MongoDB user.");

        options.addOption("H", "db_hostname", true, "Hostname of the MongoDB.");
        options.addOption("p", "db_port", true, "Database port of the MongoDB.");
        options.addOption("ssl", false, "Enable SSL for MongoDB connection.");
        options.addOption("of", "output_file", true, "Output file");

        CommandLineParser parser = new GnuParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            projectName = cmd.getOptionValue("project_name");
            repositoryUrl = cmd.getOptionValue("u");
            repositoryPath = cmd.getOptionValue("i");
            revision = cmd.getOptionValue("r");

            logLevel = cmd.getOptionValue("ll");

            dbUser = cmd.getOptionValue("U");
            dbPassword = cmd.getOptionValue("P");
            dbName = cmd.getOptionValue("DB");
            dbAuthenticationDatabase = cmd.getOptionValue("a");
            dbHostname = cmd.getOptionValue("H");
            dbPort = cmd.getOptionValue("p");
            dbSsl = cmd.hasOption("ssl");
            outputFile = cmd.getOptionValue("of");

            if(cmd.hasOption("help")) {
                help = true;
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("readabilitySHARK", options);
            }
        }
        catch(ParseException exp) {
            System.err.println("Command Line Options Parsing failed.  Reason: " + exp.getMessage());
        }
    }

    public String getProjectName() {
        return projectName;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbHostname() {
        return dbHostname;
    }

    public String getDbPort() {
        return dbPort;
    }

    public String getDbAuthenticationDatabase() {
        return dbAuthenticationDatabase;
    }

    public boolean getDbSsl() {
        return dbSsl;
    }
    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getRevision() {
        return revision;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public boolean getHelp() {
        return help;
    }

    public String outputFile() {
        return outputFile;
    }
}
