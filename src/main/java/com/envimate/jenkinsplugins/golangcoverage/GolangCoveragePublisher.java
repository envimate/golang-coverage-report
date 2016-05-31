package com.envimate.jenkinsplugins.golangcoverage;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Result;
import hudson.tasks.*;
import hudson.util.ExceptionCatchingThreadFactory;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Extension of {@link Recorder}.
 * <p>
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link GolangCoveragePublisher} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields )
 * to remember the configuration.
 * <p>
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked.
 *
 * @author nisabek
 */
public class GolangCoveragePublisher extends Recorder implements SimpleBuildStep {
    private final String outFilePath;
    private final static String COMMAND_TEMPLATE = "/bin/bash -c \"go tool cover -html=%s -o coverage.html\"";

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor public GolangCoveragePublisher(String outFilePath) {
        this.outFilePath = outFilePath;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getOutFilePath() {
        return outFilePath;
    }

    @Override public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace,
        @Nonnull Launcher launcher, @Nonnull TaskListener taskListener)
        throws InterruptedException, IOException {
        PrintStream logger = taskListener.getLogger();

        // on master
        File targetBuildDirectory =
            new File(run.getRootDir(), GolangCoverageReportBuildAction.BASE_URL);
        if (!targetBuildDirectory.exists()) {
            boolean result = targetBuildDirectory.mkdirs();
            if (!result) {
                logger.println("[GolangCoveragePublisher] Could not create directory for report");
                run.setResult(Result.UNSTABLE);
                return;
            }
        }

        FilePath target = new FilePath(targetBuildDirectory);
        workspace.copyRecursiveTo(outFilePath, target);

        String cmdStr = String.format(COMMAND_TEMPLATE, outFilePath);

        launcher.launch().cmdAsSingleString(cmdStr).envs(run.getEnvironment(taskListener))
            .stderr(taskListener.getLogger()).stdout(taskListener.getLogger()).pwd(workspace).join();

        workspace.copyRecursiveTo("**/*.html", target);

        run.addAction(new GolangCoverageReportBuildAction(run));
    }

    /**
     * Descriptor for {@link GolangCoveragePublisher}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * <p>
     * <p>
     * See <tt>src/main/resources/com/envimate/jenkinsplugins/golangcoverage/GolangCoveragePublisher/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * Performs on-the-fly validation of the form field 'outFilePath'.
         * <p>
         * This parameter receives the value that the user has typed.
         *
         * @return Indicates the outcome of the validation. This is sent to the browser.
         * <p>
         * Note that returning {@link FormValidation#error(String)} does not
         * prevent the form from being saved. It just means that a message
         * will be displayed to the user.
         */
        public FormValidation doCheckName(@QueryParameter String value)
            throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please provide the out file path from the test run");
            if (value.startsWith("/"))
                return FormValidation.error("The path should be relative to the workspace");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Publish golang test report";
        }
    }

    @Override public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}

