package com.morpheus

import org.slf4j.*
import groovy.util.logging.Slf4j
import com.morpheus.*
import grails.util.Holders
import static groovy.json.JsonOutput.*
import groovy.json.JsonSlurper

//Def and variables needed by the class here

//Call constructor
def domainId=21
def serverId=24
def classTool = new MyWindowsOsTool(userDisplayName,customOptions)
//Call Member
def pshell = classTool.runIt(domainId,serverId)
println(pshell)

@Slf4j
class MyWindowsOsTool {

	private String name
    private customOptions
    private grailsApplication
    private mainContext
    //declare any services you want to access
    private windowsOsService
    private provisionService


    public MyWindowsOsTool(name,customOptions,intId=0,zoneId=0,siteId=0) {
        
        this.grailsApplication = Holders.grailsApplication
        this.mainContext = Holders.grailsApplication.mainContext
    	this.name = name
        this.customOptions = customOptions
        log.info("name is ${name}")
        log.info("customOptions is ${customOptions}")
        // Load up the services
        this.windowsOsService = this.mainContext["windowsOsService"]
        this.provisionService = this.mainContext["vmwareProvisionService"]
        log.info("Constructor provisionService ${provisionService}")
        
        //may need to start a session

    }
    

    public runIt(domainId,serverId) {
        
        def opts = [networkConfig : [networkDomain : [ : ]]]
        log.info("runIt method: loading domain ${domainId}")
		JsonSlurper jsonSlurper = new JsonSlurper()
        NetworkDomain.withNewSession {session ->
           NetworkDomain domain = NetworkDomain.get(domainId)
           ComputeServer server = ComputeServer.get(serverId)

           log.info("MyWindowsOsTool - domain - ${domain.getProperties()}")
           opts.customized = true
           opts.unattendCustomized = false
           opts.desiredHostname = null
           opts.guestExec = false
           opts.networkConfig.networkDomain.name = domain.name
           opts.networkConfig.networkDomain.domainUsername = domain.domainUsername
           opts.networkConfig.networkDomain.domainPassword = domain.domainPassword
           opts.networkConfig.networkDomain.dcServer = domain.dcServer
           opts.networkConfig.networkDomain.ouPath = domain.ouPath
           // test new computerName functionality if opts are set
           if (!opts.customized && !opts.unattendCustomized) {
               computerName = opts.desiredHostname ?: server.getExternalHostname()
               log.info("MyWindowsOsTool - Joining Active Directory with a new ComputerName: ${computerName}")
           }
           
           def reboot = false
           def computerName = ""
           log.info("MyWindowsOsTool - overriding computername : ${computerName}")
           Map commandOpts = [sshUsername: server.sshUsername, sshPassword: server.sshPassword, guestExec: (opts.guestExec == true)]
           log.info("MyWindowsOsTool - opts - ${opts}")
           def cmd = windowsOsService.buildJoinDomainScript(opts.networkConfig.networkDomain,computerName,reboot)           
           log.info("MyWindowsOsTool - cmd ${cmd}")
           def commandResult = provisionService.executeComputeServerCommand(server, cmd, commandOpts)
           Map rpcData
           try {
               rpcData = jsonSlurper.parseText(commandResult.results?.data)
           } catch (ex) {
               log.error("MyWindowsOsTool - failed to parse json response ${ex.getMessage()}")
           }
           log.info("Full commandResult ${prettyPrint(toJson(commandResult))}")
           log.info("Powershell response from commandResult.results.data ${rpcData}")
           // correctly set commandResult success value of reboot signal (3010)
           commandResult.success = (rpcData.status == 3010) ? true : commandResult.success
           def goodJoin = (rpcData.status == 0 || rpcData.status== 3010)  
           log.info("Domain joined successfully ${goodJoin}")
           return cmd
        }


    }
    
}