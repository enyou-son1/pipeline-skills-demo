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
					println "read json input file"
					json_file = INPUT_JSON? INPUT_JSON.trim() : ""
					prop = readJSON file : json_file
					name = prop.NAME? prop.NAME.trim() : ""
					println "Name:" + name
					age = prop.AGE? prop.AGE.trim() : ""
					println "Age:" + age
					phone = prop.PHONE_NUMBER? prop.PHONE_NUMBER.trim() : ""
					println "Phone:" + phone
					address = prop.ADDRESS? prop.ADDRESS.trim() : ""
					println "Address:" + address
					email = prop.EMAIL? prop.EMAIL.trim() : ""
					println "Email:" + email
					gender = prop.GENDER? prop.GENDER.trim() : ""
					println "Gender:" + gender
					is_marry = prop.IS_MARRY? prop.IS_MARRY.trim() : false
					println "is_marry:" + is_marry
				}
			}
		}
	}
}