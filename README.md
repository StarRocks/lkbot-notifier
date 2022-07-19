# Jenkins Notifications Plugin for Lark Group Robot via OpenAPIs
This plugin allows sending jenkins job status notification to lark work group.

To use lark @Jenkins CI Assistant chat bot, please go to the plugin provided by bytedance at [github bytedance/jenkins-plugin-lark](https://github.com/bytedance/jenkins-plugin-lark).

This plugin is inspired by the `jenkins-plugin-lark` plugin, implements a message card style notification from a custom bot via its webhook address.

## How to Use

**1. Add a custom bot into chat group**

Refer to [lark custom bot documentation](https://open.feishu.cn/document/ukTMukTMukTM/ucTM5YjL3ETO24yNxkjN?fb=1), get the webhook address.

**2. Get plugin and install into Jenkins System**

**3. Configure the Jenkins plugin**
 - Select the Job in Jenkins and configure the plugin and Webhook address.

 ![](./static/config_plugin.png?raw=true)

 ![](./static/job_plugin.png?raw=true)

 - Bot sends the build result to the specified group.

 ![](./static/card_message.png?raw=true)

## Acknowledgement
This plugin is inspired by the **jenkins-plugin-lark** provided by bytedance at [github bytedance/jenkins-plugin-lark](https://github.com/bytedance/jenkins-plugin-lark).

## License
Apache 2.0 License
