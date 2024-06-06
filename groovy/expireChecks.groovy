package com.morpheus

import org.slf4j.*
import groovy.util.logging.Slf4j
import com.morpheus.*
import grails.util.Holders
import static groovy.json.JsonOutput.*
import grails.gorm.transactions.Transactional
import grails.converters.*
import com.morpheus.monitoring.MonitorCheck
import groovy.time.TimeCategory
import com.bertram.rabbitmq.conf.RabbitConsumer
import org.hibernate.FetchMode
import org.hibernate.criterion.CriteriaSpecification

//Def and variables needed by the class here

//Call constructor
def classTool = new MyMonitorCheck(userDisplayName,customOptions)
//Call Member
classTool.runIt()

@Slf4j
class MyMonitorCheck {

	private String name
    private customOptions
    private grailsApplication
    private mainContext
    //declare any services you want to access
    private monitorCheckService
    private uptime


    public MyMonitorCheck(name,customOptions) {
        
        this.grailsApplication = Holders.grailsApplication
        this.mainContext = Holders.grailsApplication.mainContext
    	this.name = name
        this.customOptions = customOptions
        log.info("name is ${name}")
        log.info("customOptions is ${customOptions}")
        // Load up the services
        this.monitorCheckService = this.mainContext["monitorCheckService"]
        //may need to start a session

    }
    

    public runIt() {
        
        log.info("runIt method")
        MonitorCheck.withNewSession {session ->
           for(int i = 0;i<20;i++) {
               log.info("runIt loop ${i} - checking expireChecks")
               expireChecks()
               sleep(5000)
           }

        }

    }

    public expireChecks() {
        try {
			JobStatus jobStatus = JobStatus.findByName('lastCheckExpireStatus')
			def lastCheckExpireStatus = jobStatus?.lastRunDate?.time ?: null

			def now = new Date()
			Integer max=100
			Integer offset=0
			//jobStatus.save(flush:true)
            log.info("MyMonitorClass lastCheckExpireStatus ${lastCheckExpireStatus}")
			def checksPastDue
			use( TimeCategory ) {
                checksPastDue = MonitorCheck.where{ active == true && deleted == false && health != 0 && nextRunDate < (now + 2.minutes) && (checkType.pushOnly == true || container != null)}.join("checkType").list()
                log.info("checksQuery : ${checksPastDue}")

                checksPastDue?.each { check ->
                    log.info("MyMonitorClass check ${check.dump()}")
                    def interval = (check.checkInterval ?: (check.checkType.defaultInterval ?: defaultInterval))
                    if(check.nextRunDate.time + (60000*2) < now.time && (lastCheckExpireStatus == null || lastCheckExpireStatus > check.nextRunDate.time)) {
                        def result = [checkId:check.id, success:false, startDate:now, endDate:new Date(), message:'unheard from beyond check interval limit.',
                            refId:check.id, jobType:'monitorUpdate']
                        log.info("MyMonitorCheck - mock update nextRunDate ${new Date(now.time + interval)}")    
                        //MonitorCheck.where{ id == check.id}.updateAll(nextRunDate: new Date(now.time + interval))
                        log.info("MyMonitorCheck - mock sendRabbitMessage applianceMonitorQueue : ${result}")
                        //sendRabbitMessage('main', '', ApplianceJobService.applianceMonitorQueue, result)
                    }
                }

                log.info("Out of while loop checksPastDue ${checksPastDue.dump()}")
			}
		}catch(e) {
			log.error("Error handling Expired Push Checks", e)
			throw(e)
		}
	}
}




