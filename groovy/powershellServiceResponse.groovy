package com.morpheus

import org.slf4j.*
import groovy.util.logging.Slf4j
import com.morpheus.*
import grails.util.Holders
import static groovy.json.JsonOutput.*

//Def and variables needed by the class here

// Set a the ID of a compute server
serverId = 24

//Call constructor
def classTool = new MyServiceResponseTool(userDisplayName,customOptions)
//Call Member
classTool.runIt(serverId)

@Slf4j
class MyServiceResponseTool {

	private String name
    private customOptions
    private grailsApplication
    private mainContext
    //declare any services you want to access
    private provisionService
    private windowsOsService


    public MyServiceResponseTool(name,customOptions) {
        
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
    

    public runIt(serverId) {
        
        log.info("runIt method - getting ComputeServer with id ${serverId}")
        ComputeServer.withNewSession {session ->
            ComputeServer server = ComputeServer.get(serverId)
            log.info("Agent Mode for server is ${provisionService.getAgentMode(server)}")
            Map commandOpts = [sshUsername: server.sshUsername, sshPassword: server.sshPassword, guestExec: false]
            def commandResult
            def cmd = buildTestScript()
            log.info("Test Script ${cmd}")
            commandResult = provisionService.executeComputeServerCommand(server,cmd,commandOpts)
            log.info("commandResult : ${commandResult}")
        }

    }
    
    public buildTestScript() {
        String cmdTemplate = """
            # Powershell Service Response test
            $rtn = [PSCustomObject]@{status=0;cmdOut=$Null;errOut=$Null}
            try {
                $winId = [System.Security.Principal.WindowsIdentity]::GetCurrent()
                $principal = [System.Security.Principal.WindowsPrincipal]$winId
                $tokenGroups = $winId.Groups | Foreach-Object {$_.Translate([System.Security.Principal.NTAccount]).toString()}
                $isAdmin=$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
                $rtn.cmdOut = [PSCustomObject]@{
                    userId=$winId.Name;
                    computerName=[Environment]::MachineName;
                    authenticationType=$winId.AuthenticationType;
                    impersonation = $winId.ImpersonationLevel.ToString();
                    isAdmin=$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
                    localProfile=[Environment]::GetEnvironmentVariable("LOCALAPPDATA");
                    tokenGroups=$tokenGroups;
                    isSystem=$winId.isSystem;
                    isService=$tokenGroups -contains "NT AUTHORITY\SERVICE";
                    isNetwork=$tokenGroups -contains "NT AUTHORITY\NETWORK";
                    isBatch=$tokenGroups -contains "NT AUTHORITY\BATCH";
                    isInteractive=$tokenGroups -contains "NT AUTHORITY\INTERACTIVE";
                    isNtlmToken=$tokenGroups -contains "NT AUTHORITY\NTLM Authentication"
                }
            }
            catch {
                $rtn.status=1
                $rtn.errOut = [PSCustomObject]@{message="Error while querying session details. Exception: {0}" -F $_.Exception.Message}
            }
            if ($AsJson) {
                return $rtn | ConvertTo-Json -Depth 3
            } else {
                return $rtn
            }
        """
        String runCmd = cmdTemplate.stripIndent()
        return runCmd
    }

    public ServiceResponse handleWindowsRpc(commandResult) {

    }
    
}