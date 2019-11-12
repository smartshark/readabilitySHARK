package de.ugoe.cs.read;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;

import de.ugoe.cs.read.exceptions.NoFileException;
import de.ugoe.cs.read.exceptions.ReadabilityParserException;
import org.apache.commons.io.FileUtils;

import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.Datastore;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import de.ugoe.cs.smartshark.Utils;
import de.ugoe.cs.smartshark.model.Project;
import de.ugoe.cs.smartshark.model.VCSSystem;
import de.ugoe.cs.smartshark.model.Commit;
import de.ugoe.cs.smartshark.model.CodeEntityState;

public class Main {

    public static void main(String[] args) {
        Params p = Params.getInstance();
        p.init(args);

        // if we only print help we are not doing anything
        if(p.getHelp()) {
            return;
        }

        // we do not want verbose mongo output
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.SEVERE);

        // we also reset stderr here because morphia outputs its logger config to stderr (https://github.com/MorphiaOrg/morphia/issues/1214)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.err;
        PrintStream tmp = new PrintStream(baos);
        System.setErr(tmp);

        final Morphia morphia = new Morphia();
        morphia.mapPackage("de.ugoe.cs.smartshark.model");

        // then we put stderr back because we need it to pass errors to the serverSHARK
        System.err.flush();
        System.setErr(old);

        MongoClientURI uri = new MongoClientURI(Utils.createMongoDBURI(p.getDbUser(), p.getDbPassword(), p.getDbHostname(), p.getDbPort(), p.getDbAuthenticationDatabase(), p.getDbSsl()));
        MongoClient mongoClient = new MongoClient(uri);
        Datastore datastore = morphia.createDatastore(mongoClient, p.getDbName());

        Project project = datastore.createQuery(Project.class).field("name").equal(p.getProjectName()).get();
        VCSSystem vcsSystem = datastore.createQuery(VCSSystem.class).field("url").equal(p.getRepositoryUrl()).field("project_id").equal(project.getId()).get();
        Commit commit = datastore.createQuery(Commit.class).field("vcs_system_id").equal(vcsSystem.getId()).field("revision_hash").equal(p.getRevision()).get();

        // parse all files for the current commit
        String[] extensions = {"java"};
        File file = new File(p.getRepositoryPath());
        Collection<File> files = FileUtils.listFiles(file, extensions, true);

        for(File f: files) {
            // we need to subtract the repository path so that we can match the long_names
            String subtract = p.getRepositoryPath();
            if(!subtract.substring(subtract.length() - 1).equals("/")) {
                subtract += "/";
            }
            String repoPath = f.getAbsolutePath().replace(subtract, "");


            //System.out.println("file " + repoPath);
            try {
                //System.out.println("calculate results buse");
                double result2 = Readability.getMeanReadabilityBuse(f);

                //System.out.println("calculate results scalabrino");
                double result = Readability.getMeanReadability(f);

                //System.out.println("results: " + result2 + ", " + result);
                de.ugoe.cs.smartshark.model.File mongoFile = datastore.createQuery(de.ugoe.cs.smartshark.model.File.class).field("path").equal(repoPath).field("vcs_system_id").equal(vcsSystem.getId()).get();

                // fetch code entity state by s_key
                String sKey = CodeEntityState.calculateIdentifier(repoPath, commit.getId(), mongoFile.getId());
                CodeEntityState ces = datastore.createQuery(CodeEntityState.class).field("s_key").equal(sKey).get();

                // if we did not find one we create one
                if (ces == null) {
                    ces = new CodeEntityState();
                    ces.setsKey(sKey);
                    ces.setLongName(repoPath);
                    ces.setCeType("file");
                    ces.setCommitId(commit.getId());
                    ces.setFileId((mongoFile.getId()));
                    // System.err.println("No CodeEntityState found for: " + repoPath);
                } else {
                    // fr.write(p.getRevision() + ";" + ces.getLongName() + ";" + result + "\n");
                }

                Map<String, Double> metrics = ces.getMetrics();
                if (metrics == null) {
                    metrics = new HashMap<String, Double>();
                }
                metrics.put("readability_scalabrino", result);
                metrics.put("readability_buse", result2);

                ces.setMetrics(metrics);
                datastore.save(ces);

            }catch(NoFileException e) {
                System.out.println("no file exception thrown for " + repoPath);
                System.err.println("no file exception thrown for " + repoPath);
            }catch(ReadabilityParserException e1) {
                System.out.println("no file exception thrown for " + repoPath);
                System.err.println("Parse error for file: " + repoPath);
            }catch(IOException e2) {
                System.out.println("no file exception thrown for " + repoPath);
                System.err.println("IO error for file: " + repoPath + ", : " + e2.getMessage());
            }catch(NoSuchAlgorithmException e) {
                System.out.println("no file exception thrown for " + repoPath);
                System.err.println("No such algorithm " + e.getMessage());
            }finally {
                System.err.flush();
            }
        }
    }
}