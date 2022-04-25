import hudson.model.*;
 
 
pipeline{
 
   agent any
   stages{
      stage("Hello Pipeline") {
         steps {
             script {
                println "Hello Pipeline!"
                println env.JOB_NAME
                println env.BUILD_NUMBER
            }
         }
      }
 
      stage("Init paramters in json") {
         steps {
             script {
 
                println "read josn input file"
                json_file = INPUT_JSON? INPUT_JSON.trim() : ""
                prop = readJSON file : json_file
                name = prop.NAME? prop.NAME.trim() : ""
                println "Name:" + name
            }
         }
      }
   }
 
}