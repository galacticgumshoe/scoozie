# Hocon API

Scoozie provides a Hocon parser that allows the definition of Oozie artefacts via hocon files.

## Usage

```scala
import org.antipathy.scoozie.Scoozie

val artefacts = Scoozie.fromConfig(Paths.get("/path/to/some.conf"))
artefacts.save(Paths.get("/some/output/directory/"))
```

Depending on the content of the configuration, this will output the following files:

* workflow.xml
* coordinator.xml
* job.properties (if a coordinator is specified, this will contain properties for both of the above files)

Properties in the workflow and coordinator will be substituted for generated variable names and values contained in the `job.properties`. 

## Hocon format

Scoozie expects the hocon file to be in the format below to generate artefacts for both the workflow and coordinatior.
The `workflow` key is the only element that is required. The `validate` key is used to validate transitions on the workflow.
```hocon
workflow {}
coordinator: {} //optional
validate {} //optional
```

### Workflow

See below for an example workflow defined in Hocon.

```hocon
workflow {
  name: "someworkflow"
  path: "/path/to/workflow.xml" //in HDFS
  credentials {
    name: "someCredentials"
    type: "credentialsType"
    configuration: {
      credentials1:"value1", //optional additional properties to include with credentials
      credentials2:"value2",
      credentials3:"value3",
      credentials4:"value4"
    }
  }
  job-xml: "/path/to/job.xml"
  transitions: [ //array of action objects that comprise the workflow transitions
    {
      type:"start"  //type is required for all actions
      ok-to:"decisionNode"
    },
    {
      name:"decisionNode"
      type:"decision"
      default: "sparkAction"
      switches: {
        sparkAction: "someVar eq 1"
        hiveAction: "someOtherVar eq someVar"
      }
    }
    {
      name:"sparkAction"
      type:"spark"
      job-xml: "someSettings"
      spark-master-url: "masterurl"
      spark-mode: "mode"
      spark-job-name: "Jobname"
      main-class: "somemainclass"
      spark-jar: "spark.jar"
      spark-options: "spark-options"
      command-line-arguments: []
      prepare: {
        delete: "deletePath"
        mkdir: "makePath"
      }
      files: []
      configuration: {}
      ok-to: "shellAction"
      error-to: "errorEmail"
    },
    {
      name:"hiveAction"
      type: "hive"
      job-xml: "settings"
      script-name: "script.hql"
      script-location: "/some/location"
      parameters: []
      files: []
      configuration: {}
      ok-to: "shellAction"
      error-to: "errorEmail"
    },
    {
      name:"shellAction"
      type:"shell"
      script-name: "script.sh"
      script-location: "/some/location"
      command-line-arguments: []
      environment-variables: []
      files: []
      configuration: {}
      ok-to: "end"
      error-to: "errorEmail"
    },
    {
      name:"errorEmail"
      type:"email"
      to: ["a@a.com"]
      cc: []
      subject: "hello"
      body: "yep"
      ok-to: "kill"
      error-to: "kill"
    },
    {
      type: "kill"
      message: "workflow is kill"
    },
    {
      type:"end"
    }
  ]
  configuration: { //optional workflow additional properties
    workflow1:"value1",
    workflow2:"value2",
    workflow3:"value3",
    workflow4:"value4"
  }
  yarn-config {
    name-node: "someNameNode"
    job-tracker: "someJobTracker"
  }
  
  sla: {
     nominal-time: "nominalTime" //optional
     should-start: "10 * MINUTES" //optional
     should-end: "30 * MINUTES" //optional
     max-duration: "30 * MINUTES" //optional
     alert-events: ["start_miss","end_miss","duration_miss"] //optional
     alert-contacts: ["a@a.com", "b@b.com"] //optional
     notification-message: ${workflow.name} "is breaching SLA" //optional
     upstream-applications: ["app1", "app2"] //optional
   }
}
```

### Coordinator

Example co-ordinator:
```hocon
coordinator: {
  name: "someCoordinator"
  path: "/path/to/coordinator.xml" //in HDFS
  frequency: "someFreq"
  start: "someStart"
  end: "someEnd"
  timezone: "someTimezone"
  configuration: {
    prop1: value1
    prop2: value2
    prop3: value3
    prop4: value4
  }
  sla: {
   nominal-time: "nominalTime"
   should-start: "10 * MINUTES" //optional
   should-end: "30 * MINUTES" //optional
   max-duration: "30 * MINUTES" //optional
   alert-events: ["start_miss","end_miss","duration_miss"] //optional
   alert-contacts: ["a@a.com", "b@b.com"] //optional
   notification-message: ${coordinator.name} "is breaching SLA" //optional
   upstream-applications: ["app1", "app2"] //optional
 }
}
```

### Validation
Scoozie provides validation of the generated artefacts against the Oozie XSDs for each of the actions listed above as well as the workflow and coordinators.  
In addition basic loop checking and testing of workflow transitions is done before writing disk.

In addition, when creating the configuration class, you may create a validation object as shown below:
```hocon
validate {
  transitions = "start -> action1 -> action2 -> end"
}
```

The transitions are represented as a string with the ` -> ` symbol representing a transition between two nodes.  
No distinction is made between a successful transition or an error transition for this representation.  Forks are represented as 

```$xslt

Start -> someFork -> (action1, action2) -> someJoin -> End
```

For decisions, the default node is used.

Further examples can be seen in the `WorkflowTestRunnerSpec` class.

For example, the workflow specified above would have the following validation string:

```hocon
validate {
  transitions = "start -> decisionNode -> sparkAction -> shellAction -> end"
}
```

An example Hocon file can be seen [here](./src/test/resources/conf/slas.conf) or [here](./src/test/resources/conf/workflowAndCoordinator.conf)

### Available actions

The actions listed below are currently available via the Hocon API.  Unless otherwise specified all properties are required.

Most actions allow specifying a `prepare` and a `configuration` property, these are optional, but should be specified in a key value format, e.g:


```hocon
configuration: {
    prop1: value1
    prop2: value2
    prop3: value3
    prop4: value4
}
```

The `job-xml` key is also optional for any actions that specify it.
The `prepare` option should be specified in the following format:

```hocon
prepare: {
  delete: "deletePath"
  mkdir: "makePath"
}
```

#### Control nodes

Start:

```hocon
{
  type:"start"
  ok-to:"someNode" 
}
```
End:
```hocon
{
  type:"end"
}
```
Kill:
```hocon
{
  type: "kill"
  message: "error message"
}
```
Fork:
```hocon
{
    name:"someFork"
    type:"fork"
    paths: ["action1", "action2"] //names of the actions
}
```
Join:
```hocon
{
    name:""
    type:"join"
    ok-to: ""
}
```

decision:
```hocon
{
    name:""
    type:"decision"
    default: ""
    switches: {
      action1: ""  //any variables referenced in here may need to be
      action2: "" // manually added to the job properties
    }
 }
```
#### Action nodes

Action nodes also take an optional `sla` key, the format for these keys is:

```hocon
sla: {
  nominal-time: ""
  should-start: ""
  should-end: ""
  max-duration: ""
  alert-events: []  // allowed values are "start_miss", "end_miss","duration_miss"
  alert-contacts: []
  notification-message: ""
  upstream-applications: []
}
```

available actions are:

DistCP:
```hocon
{
  name: ""
  type: "distcp"
  arguments: []
  java-options: ""
  configuration: {}
  prepare: {}
  ok-to: ""
  error-to: ""
}
```
Email:
```hocon
{
  name: ""
  type: "email"
  to: []
  cc: []
  subject: ""
  body: ""
  content-type: "" //optional
  ok-to: ""
  error-to: ""
}
```
FileSystem:
```hocon
{
  name: ""
  type: "filesystem"
  job-xml: ""
  configuration: {}
  steps: [
    //any of
    {delete: ""},
    {mkdir: ""},
    {touchz: ""},
    {chmod: {path:"", permissions:"", dir-files:""}}, //note: Strings 
    {move: {source:"",  target:"" }}
  ]
  ok-to: ""
  error-to: ""
}
```
Hive:
```hocon
{
  name: ""
  type: "hive"
  job-xml: ""
  script-name: ""
  script-location: ""
  files: []
  configuration: {}
  prepare: {}
  ok-to: ""
  error-to: ""
}
```
Java:
```hocon
{
  name: ""
  type: "java"
  job-xml: ""
  main-class: ""
  java-jar: ""
  java-options: ""
  command-line-arguments: []
  files: []
  capture-output: true //optional
  configuration: {}
  prepare: {}
  ok-to: ""
  error-to: ""
}
```
pig:
```hocon
{
  name: ""
  type: "pig"
  script: ""
  params: []
  arguments: []
  files: []
  job-xml: ""
  configuration: {}
  prepare: {}
  ok-to: ""
  error-to: ""
}
```
Shell:
```hocon
{
  name: ""
  type: "shell"
  script-name: ""
  script-location: ""
  command-line-arguments: []
  environment-variables: []
  files: []
  capture-output: true //optional
  job-xml: "" 
  configuration: {}
  prepare: {}
  ok-to: ""
  error-to: ""
}
```
Spark:
```hocon
{
  name: ""
  type: "spark"
  spark-master-url: ""
  spark-mode: ""
  spark-job-name: ""
  main-class: ""
  spark-jar: ""
  spark-options: ""
  command-line-arguments: []
  job-xml: "" 
  configuration: {}
  prepare: {}
  ok-to: ""
  error-to: ""
}
```
Sqoop:
```hocon
{
  name: ""
  type: "sqoop"
  command: "" //specify this or command-line-arguments
  command-line-arguments: [] //ignored if command is specified
  files: []
  job-xml: ""
  configuration: {}
  prepare: {}
  ok-to: ""
  error-to: ""
}
```
Ssh:
```hocon
{
  name: ""
  type: "ssh"
  host: ""
  command: ""
  capture-output: true //optional
  command-line-arguments: []
  ok-to: ""
  error-to: ""
}
```
Sub-Workflow:
```hocon
{
  name: ""
  type: "subworflow"
  application-path: ""
  propagate-configuration: true //optional
  configuration: {}
  ok-to: ""
  error-to: ""
}
```