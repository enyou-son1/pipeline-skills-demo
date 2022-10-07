@Library('wfs')
import net.gree.wfs.Configuration
import net.gree.wfs.GithubAPI
import groovy.json.*
import jenkins.*
import jenkins.model.*
def jenkins = null
def configuration = null
def tempest = null
def namespace = null
def jobName = null
def commandArg = null
def objectName = null
def environmentChoices = [
    'dev1',
    'dev1-gl',
    'qa',
    'qa-gl',
    'payment',
    'payment-gl',
    'review',
    'review-gl',
    'review2',
    'review2-gl',
    'staging-jp',
    'staging-gl',
    'production-jp',
    'production-ap',
    'production-us',
    'production-eu'
]
pipeline {
    agent {
        kubernetes {
            yaml """\
                apiVersion: v1
                kind: Pod
                spec:
                    nodeSelector:
                        cloud.google.com/gke-nodepool: pool-jenkins
                    tolerations:
                    - key: "app"
                      operator: "Equal"
                      value: "build"
                      effect: "NoExecute"
                    containers:
                    - name: git
                      image: alpine/git:v2.30.0
                      command:
                      - cat
                      tty: true
            """.stripIndent()
        }
    }
    parameters {
        choice(
            name: 'environment',
            choices: environmentChoices,
            description: 'デプロイ環境'
        )
        string(name: 'count', defaultValue: '100', description: '削除件数. -1で削除できるもの全てを削除する')
        string(name: 'partition', defaultValue: '1000', description: '1ループで削除する件数.')
        string(name: 'beforeDay', defaultValue: '30', description: '何日より前のデータを削除するかどうか')
        booleanParam(name: 'dryRun', description: 'テストモードで実行 テストチャンネルに通知されます')
        string(name: 'sleep', defaultValue: '1', description: '分割削除間のスリープ時間(秒). staleTime以上を設定')
        string(name: 'stale', defaultValue: '1', description: 'spannerのstale readの時間(秒)')
        string(name: 'priority', defaultValue: '2', description: 'spannerのpriority. low -> , medium -> 2, high -> 3')
        string(name: 'channel', defaultValue: 'tempest-bot-dev', description: '通知先Slackチャンネル名')
    }
    triggers {
        parameterizedCron('''
            00 00 */1 * * %environment=production-jp; count=100000; partition=10000; beforeDay=7; dryRun=false; sleep=1; stale=1; priority=2; channel=tempest-bot-production;
            30 00 */1 * * %environment=production-ap; count=100000; partition=10000; beforeDay=7; dryRun=false; sleep=1; stale=1; priority=2; channel=tempest-bot-production;
            00 01 */1 * * %environment=production-us; count=100000; partition=10000; beforeDay=7; dryRun=false; sleep=1; stale=1; priority=2; channel=tempest-bot-production;
            30 01 */1 * * %environment=production-eu; count=100000; partition=10000; beforeDay=7; dryRun=false; sleep=1; stale=1; priority=2; channel=tempest-bot-production;
        ''')
    }
    stages {
        stage('Setup') {
            stages {
                stage('Get Context') {
                    steps {
                        container('git') {
                            script {
                                jenkins = "${WORKSPACE}/jenkins"
                                configuration = new Configuration(
                                    readFile(file : "${jenkins}/configuration.json")
                                )
                            }
                        }
                    }
                }
            }
        }
        stage('NotifyStart') {
            steps {
                container('git') {
                    script {
                        server = configuration.resolve("server", params.environment)
                        def dryRunMessage = ""
                        if (params.dryRun.toBoolean()) {
                            dryRunMessage = "[dryRun]"
                        }
                        def attachments = [
                            [
                                pretext: dryRunMessage + "古いフレンド申請書の削除を開始します",
                                title: "#${BUILD_NUMBER} ${JOB_NAME}",
                                title_link: BUILD_URL,
                                text: "デプロイ先: ${server.name}"
                            ]
                        ]
                        slackSend(channel: params.channel, attachments: attachments)
                        print(params.toString())
                    }
                }
            }
        }
        stage('Delete Friend Application') {
            steps {
                container('git') {
                    script {
                        println "フレンド申請書削除開始"
                        // 本番はsidecarを立ち上げる
                        def useSidecar = false
                        if (
                            params.environment == "production-jp" ||
                            params.environment == "production-ap" ||
                            params.environment == "production-us" ||
                            params.environment == "production-eu"
                        ) {
                            useSidecar = true
                        }
                        def dryRunOption = ""
                        if (params.dryRun.toBoolean()) {
                            dryRunOption = "--dry-run"
                            println "dryRunモードで実行します"
                        }
                        def maxCountOption = ""
                        if (params.count != "") {
                            maxCountOption = "-c ${params.count}"
                        }
                        def partitionOption = ""
                        if (params.partition != "") {
                            partitionOption = "-p ${params.partition}"
                        }
                        def beforeDayOption = ""
                        if (params.beforeDay != "") {
                            beforeDayOption = "-d ${params.beforeDay}"
                        }
                        def sleepOption = ""
                        if (params.sleep != "") {
                            sleepOption = "-s ${params.sleep}"
                        }
                        def staleOption = ""
                        if (params.stale != "") {
                            staleOption = "-r ${params.stale}"
                        }
                        def priorityOption = ""
                        if (params.priority != "") {
                            priorityOption = "--priority ${params.priority}"
                        }
                        def command = """
                            sleep 100;
                            CLI_MODE=1 /usr/local/bin/php cli/delete_friend_application.php \\
                                ${dryRunOption} ${maxCountOption} ${beforeDayOption} ${sleepOption} ${staleOption} ${priorityOption} ${partitionOption};
                            php cli/finish_unison_log.php
                        """
                        println "command: ${command}"
                        build(
                            job: 'Server_Execute_Cli_Fix_Tmp',
                            parameters: [
                                [$class: 'StringParameterValue', name: 'command', value: command],
                                [$class: 'StringParameterValue', name: 'environment', value: environment],
                                [$class: 'StringParameterValue', name: 'channel', value: channel],
                                [$class: 'BooleanParameterValue', name: 'useSidecar', value: useSidecar]
                            ]
                        )
                    }
                }
            }
        }
        
        
    }
    post {
        success {
            script {
                def dryRunMessage = ""
                if (params.dryRun.toBoolean()) {
                    dryRunMessage = "[dryRun]"
                }
                def message =
"""
environment: %s
"""
                def attachments = [
                    [
                        color: "good",
                        pretext: dryRunMessage + "古いフレンド申請書の削除が完了しました",
                        title: "#${BUILD_NUMBER} ${JOB_NAME} @${NODE_NAME}",
                        title_link: BUILD_URL,
                        text: String.format(
                            message,
                            params.environment,
                        )
                    ]
                ]
                slackSend(channel:params.channel, attachments: attachments)
            }
        }
        failure {
            script {
                def message =
"""
environment: %s
"""
                def attachments = [
                    [
                        color: "danger",
                        pretext: "コマンド実行失敗",
                        title: "#${BUILD_NUMBER} ${JOB_NAME} @${NODE_NAME}",
                        title_link: BUILD_URL,
                        text: String.format(
                            message,
                            params.environment,
                        )
                    ]
                ]
                slackSend(channel:params.channel, attachments: attachments)
            }
        }
    }
}