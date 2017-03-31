package com.envimate.jenkinsplugins.golangcoverage;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Run;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

public class GolangCoverageReportBuildAction implements Action {
    public static final String BASE_URL = "golang-coverage-report";
    public static final String DEFAULT_URL = "coverage.html";
    public static final String GOLANG_COVERAGE_ICON = "/plugin/golang-coverage-report/golang.png";
    private final Run<?, ?> run;

    public GolangCoverageReportBuildAction(Run<?, ?> run) {
        this.run = run;
    }

    @Override public String getIconFileName() {
        return GOLANG_COVERAGE_ICON;
    }

    //Auto-generated
    @Override public String getDisplayName() {
        return Messages.SidePanel_DisplayName();
    }

    @Override public String getUrlName() {
        return BASE_URL;
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException,
        ServletException {
        // since Jenkins blocks JavaScript as described at
        // https://wiki.jenkins-ci.org/display/JENKINS/Configuring+Content+Security+Policy and fact that plugin uses JS
        // to display charts, following must be applied
        System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "");

        DirectoryBrowserSupport
            dbs = new DirectoryBrowserSupport(this, new FilePath(dir()), getTitle(), getUrlName(),
            false);

        dbs.setIndexFileName(DEFAULT_URL);
        dbs.generateResponse(req, rsp, this);
    }

    protected String getTitle(){
        return this.run.getDisplayName();
    }

    protected File dir() {
        return new File(run.getRootDir(), BASE_URL);
    }

}
