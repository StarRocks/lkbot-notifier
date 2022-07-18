package com.starrocks.botv2;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

public class BotNotifier extends Notifier {
    private boolean onStart;
    private boolean onSuccess;
    private boolean onFailed;
    private boolean onAbort;
    private String sendUrlList;

    public String getSendUrlList() {
        return sendUrlList;
    }

    public boolean isOnStart() {
        return onStart;
    }

    public boolean isOnSuccess() {
        return onSuccess;
    }

    public boolean isOnFailed() {
        return onFailed;
    }

    public boolean isOnAbort() {
        return onAbort;
    }

    @DataBoundConstructor
    public BotNotifier(
        boolean onStart, boolean onSuccess, boolean onFailed, boolean onAbort, String sendUrlList) {
        super();

        this.onStart = onStart;
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
        this.onAbort = onAbort;
        this.sendUrlList = sendUrlList;
    }

    public BotService newBotService(AbstractBuild build, TaskListener listener) {
        return new BotServiceImpl(
            sendUrlList, onStart, onSuccess, onFailed, onAbort, listener, build);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        return true;
    }

    @Override
    public BotNotifierDescriptor getDescriptor() {
        return (BotNotifierDescriptor) super.getDescriptor();
    }

    @Extension
    public static class BotNotifierDescriptor extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Lark Bot(v2) Notification";
        }
    }
}
