package com.morpheus

import org.slf4j.*
import groovy.util.logging.Slf4j
import com.morpheus.*
import grails.util.Holders
import static groovy.json.JsonOutput.*

//Def and variables needed by the class here

//Call constructor - need to tweak these
def domainId=21
def serverId=15
def classTool = new MyWindowsOsTool(userDisplayName,customOptions)
//Call Member
classTool.runIt(domainId,serverId)

@Slf4j
class MyWindowsOsTool {

	private String name
    private customOptions
    private grailsApplication
    private mainContext
    //declare any services you want to access
    private windowsOsService


    public MyWindowsOsTool(name,customOptions,intId=0,zoneId=0,siteId=0) {
        this.grailsApplication = Holders.grailsApplication
        this.mainContext = Holders.grailsApplication.mainContext
    	this.name = name
        this.customOptions = customOptions
        log.info("name is ${name}")
        log.info("customOptions is ${customOptions}")
        // Load up the services
        this.windowsOsService = this.mainContext["windowsOsService"]
        //may need to start a session

    }
    
    public runIt(domainId,serverId) {
        def opts = [networkConfig : [networkDomain : [ : ]]]
        log.info("runIt method: loading domain ${domainId}")
        NetworkDomain.withNewSession {session ->
           NetworkDomain domain = NetworkDomain.get(domainId)
           ComputeServer server = ComputeServer.get(serverId)
           log.info("MyWindowsOsTool - domain - ${domain.getProperties()}")
           opts.customized = true
           opts.unattendCustomized = true
           opts.desiredHostName = null
           opts.networkConfig.networkDomain.name = domain.name
           opts.networkConfig.networkDomain.domainUsername = domain.domainUsername
           opts.networkConfig.networkDomain.domainPassword = domain.domainPassword
           opts.networkConfig.networkDomain.dcServer = domain.dcServer
           opts.networkConfig.networkDomain.ouPath = domain.ouPath
           log.info("MyWindowsOsTool - opts - ${opts}")
           def cmd = windowsOsService.buildJoinDomainScript(server,opts)
           log.info("MyWindowsOsTool - cmd ${cmd}")
        }
    } 
}