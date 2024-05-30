package com.morpheus

import org.slf4j.*
import groovy.util.logging.Slf4j
import com.morpheus.*
import grails.util.Holders
import static groovy.json.JsonOutput.*

//Def and variables needed by the class here

//Call constructor
def classTool = new MyClassTool(userDisplayName,customOptions)
//Call Member
classTool.runIt()

@Slf4j
class MyClassTool {

	private String name
    private customOptions
    private grailsApplication
    private mainContext
    //declare any services you want to access
    private ansibleService


    public MyClassTool(name,customOptions,intId=0,zoneId=0,siteId=0) {
        
        this.grailsApplication = Holders.grailsApplication
        this.mainContext = Holders.grailsApplication.mainContext
    	this.name = name
        this.customOptions = customOptions
        log.info("name is ${name}")
        log.info("customOptions is ${customOptions}")
        // Load up the services
        this.ansibleService = this.mainContext["ansibleService"]
        //may need to start a session

    }
    

    public runIt() {
        
        log.info("runIt method")

    }
    
}