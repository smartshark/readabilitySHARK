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

@SuppressWarnings({"PMD.UseUtilityClass", "PMD.CloseResource", "PMD.SystemPrintln", "PMD.ShortClassName", "PMD.CommentRequired", "PMD.NcssCount"})
public final class Main {

    public static void main(final String[] args) {
        final Params params = Params.getInstance();
        params.init(args);

        // if we only print help we are not doing anything
        if(params.isHelpOnly()) {
            return;
        }

        // we do not want verbose mongo output
        final Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.SEVERE);

        // we also reset stderr here because morphia outputs its logger config to stderr
        // (https://github.com/MorphiaOrg/morphia/issues/1214)
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream old = System.err;
        final PrintStream tmp = new PrintStream(baos);
        System.setErr(tmp);

        final Morphia morphia = new Morphia();
        morphia.mapPackage("de.ugoe.cs.smartshark.model");

        // then we put stderr back because we need it to pass errors to the serverSHARK
        System.err.flush();
        System.setErr(old);
        tmp.close();

        final MongoClientURI uri = new MongoClientURI(Utils.createMongoDBURI(params.getDbUser(), params.getDbPassword(), params.getDbHostname(), params.getDbPort(), params.getDbAuthentication(), params.useSSL()));
        final MongoClient mongoClient = new MongoClient(uri);
        final Datastore datastore = morphia.createDatastore(mongoClient, params.getDbName());

        final Project project = datastore.createQuery(Project.class).field("name").equal(params.getProjectName()).get();
        final VCSSystem vcsSystem = datastore.createQuery(VCSSystem.class).field("url").equal(params.getRepositoryUrl()).field("project_id").equal(project.getId()).get();
        final Commit commit = datastore.createQuery(Commit.class).field("vcs_system_id").equal(vcsSystem.getId()).field("revision_hash").equal(params.getRevision()).get();

        // parse all files for the current commit
        final String[] extensions = {"java"};
        final File file = new File(params.getRepositoryPath());
        final Collection<File> files = FileUtils.listFiles(file, extensions, true);

        for(final File f: files) {
            // we need to subtract the repository path so that we can match the long_names
            final StringBuffer subtract = new StringBuffer();
            subtract.append(params.getRepositoryPath());
            if(!subtract.substring(subtract.length() - 1).equals("/")) {
                subtract.append('/');
            }
            final String repoPath = f.getAbsolutePath().replace(subtract.toString(), "");

            try {
                final double result2 = ReadabilityHelper.getMeanReadabilityBuse(f);
                final double result = ReadabilityHelper.getMeanReadability(f);

                final de.ugoe.cs.smartshark.model.File mongoFile = datastore.createQuery(de.ugoe.cs.smartshark.model.File.class).field("path").equal(repoPath).field("vcs_system_id").equal(vcsSystem.getId()).get();

                // fetch code entity state by s_key
                final String sKey = CodeEntityState.calculateIdentifier(repoPath, commit.getId(), mongoFile.getId());
                CodeEntityState ces = datastore.createQuery(CodeEntityState.class).field("s_key").equal(sKey).get();

                // if we did not find one we create one
                if (ces == null) {
                    ces = new CodeEntityState();
                    ces.setsKey(sKey);
                    ces.setLongName(repoPath);
                    ces.setCeType("file");
                    ces.setCommitId(commit.getId());
                    ces.setFileId(mongoFile.getId());
                }

                Map<String, Double> metrics = ces.getMetrics();
                if (metrics == null) {
                    metrics = new HashMap<>();
                }
                metrics.put("readability_scalabrino", result);
                metrics.put("readability_buse", result2);

                ces.setMetrics(metrics);
                datastore.save(ces);

            }catch(NoFileException e) {
                System.err.println("no file exception thrown for " + repoPath);
            }catch(ReadabilityParserException e1) {
                System.err.println("Parse error for file: " + repoPath);
            }catch(IOException e2) {
                System.err.println("IO error for file: " + repoPath + ", : " + e2.getMessage());
            }catch(NoSuchAlgorithmException e) {
                System.err.println("No such algorithm " + e.getMessage());
            }finally {
                System.err.flush();
            }
        }

        mongoClient.close();
    }
}