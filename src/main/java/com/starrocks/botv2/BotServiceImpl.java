package com.starrocks.botv2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class BotServiceImpl implements BotService {
    private Logger logger = LoggerFactory.getLogger(BotService.class);

    private boolean onStart;
    private boolean onSuccess;
    private boolean onFailed;
    private boolean onAbort;
    private TaskListener listener;
    private AbstractBuild build;
    private String[] urlList;

    private static final String START = "start";
    private static final String SUCCESS = "success";
    private static final String FAILED = "failed";
    private static final String ABORT = "abort";

    public BotServiceImpl(String sendUrlList, boolean onStart, boolean onSuccess, boolean onFailed,
        boolean onAbort, TaskListener listener, AbstractBuild build) {
        this.onStart = onStart;
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
        this.onAbort = onAbort;
        this.listener = listener;
        this.build = build;
        urlList = parseSendList(sendUrlList);
    }

    private String[] parseSendList(String sendList) {
        String[] urlLis = null;
        try {
            urlLis = sendList.split(";");
        } catch (Exception e) {
            logger.error("parse list error", e);
        }
        return urlLis;
    }

    private String getBuildUrl() {
        String getRootUrl = getDefaultURL();
        if (getRootUrl.endsWith("/")) {
            return getRootUrl + build.getUrl();
        } else {
            return getRootUrl + "/" + build.getUrl();
        }
    }

    private String getJobUrl() {
        String getRootUrl = getDefaultURL();
        if (getRootUrl.endsWith("/")) {
            return getRootUrl + build.getProject().getUrl();
        } else {
            return getRootUrl + "/" + build.getProject().getUrl();
        }
    }

    private String getDefaultURL() {
        Jenkins instance = Jenkins.getInstance();
        return instance.getRootUrl() != null ? instance.getRootUrl() : "";
    }

    @Override
    public void start(String userDescription) {
        if (onStart) {
            logger.info("send link msg from " + listener.toString());
            sendMsg(START, userDescription);
        }
    }

    @Override
    public void success(String userDescription) {
        logger.info("description:" + userDescription);
        if (onSuccess) {
            logger.info("send link msg from " + listener.toString());
            sendMsg(SUCCESS, userDescription);
        }
    }

    @Override
    public void failed() {
        if (onFailed) {
            logger.info("send link msg from " + listener.toString());
            sendMsg(FAILED);
        }
    }

    @Override
    public void abort() {
        if (onAbort) {
            logger.info("send link msg from " + listener.toString());
            sendMsg(ABORT);
        }
    }

    private void sendMsg(String type) {
        sendMsg(type, null);
    }

    private JSONObject buildMessageCard(
        String jobName, String causedby, String status, String duration, String jobUrl) {
        // refer to https://open.feishu.cn/tool/cardbuilder?lang=en-US on how to build messageCard json string
        final String card_tmpl = "{'config':{'wide_screen_mode':true},'elements':[{'tag':'hr'},"
            + "{'tag':'markdown','content':'**📌 Job Name:**\n%s\n**🎯 CausedBy:** \n%s'},{'"
            + "fields':[{'is_short':true,'text':{'content':'**🗳 Job Status: **\n%s','tag':'lark_md'}},"
            + "{'is_short':true,'text':{'content':'**⏰ Duration: **\n%s','tag':'lark_md'}}],'tag':'div'},{"
            + "'actions':[{'tag':'button','text':{'content':'Build Details','tag':'plain_text'},"
            + "'type':'primary','url':'%s'}],'tag':'action'}],"
            + "'header':{'template':'%s','title':{'content':'%s Jenkins Build Notification','tag':'plain_text'}}}";
        // save a few types from escaping quotes
        String fmt = card_tmpl.replace("'", "\"");
        String color = "";
        String title_emo = "";
        if (status == START || status == ABORT) {
            color = "yellow";
            title_emo = "☁️";
        } else if (status == SUCCESS) {
            color = "green";
            title_emo = "☀️";
        } else if (status == FAILED) {
            color = "red";
            title_emo = "🌧";
        }
        JSONObject object = JSON.parseObject(
            String.format(fmt, jobName, causedby, status, duration, jobUrl, color, title_emo));
        return object;
    }

    private void sendMsg(String type, @Nullable String shortDescription) {
        if (urlList == null) {
            return;
        }
        HttpClient client = getHttpClient();
        System.out.println(urlList.toString());

        String causedby = "";
        if ((StringUtils.equals(type, START) || StringUtils.equals(type, SUCCESS))
            && shortDescription != null) {
            causedby = shortDescription;
        }
        String jobName =
            String.format("%s %s", build.getProject().getDisplayName(), build.getDisplayName());
        String duration = String.format("%d ms", build.getDuration());
        String jobUrl = getBuildUrl();
        JSONObject msgCard = buildMessageCard(jobName, causedby, type, duration, jobUrl);

        JSONObject body = new JSONObject();
        body.put("msg_type", "interactive");
        body.put("card", msgCard);

        String bodyJsonString = body.toJSONString();
        logger.info(bodyJsonString);

        StringRequestEntity requestEntity = null;
        try {
            requestEntity = new StringRequestEntity(bodyJsonString, "application/json", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error("requestEntity encode error", e);
        }

        for (String url : urlList) {
            if (!TextUtils.isEmpty(url)) {
                PostMethod post = new PostMethod(url);
                post.setRequestEntity(requestEntity);
                try {
                    client.executeMethod(post);
                    logger.info(post.getResponseBodyAsString());
                } catch (IOException e) {
                    logger.error("send msg error", e);
                } finally {
                    post.releaseConnection();
                }
            }
        }
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && jenkins.proxy != null) {
            ProxyConfiguration proxy = jenkins.proxy;
            if (proxy != null && client.getHostConfiguration() != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                String username = proxy.getUserName();
                String password = proxy.getPassword();
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    client.getState().setProxyCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return client;
    }
}
