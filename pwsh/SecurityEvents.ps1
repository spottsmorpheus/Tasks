Function Get-WinSecurityEvents {
<#
    .SYNOPSIS
    Search the Security Event log for a list of Events. Can optionally return json output
#>
    param (
        [int32[]]
        #Specify a list of Event Ids to return
        $eventList,
        [int32]
        #Go back this number of days 0 (default) is all entries
        $days=0,
        [int32]
        #Specify maximum entried to return 0 is no maximum
        $maxEvents,
        [switch]
        #Return a json representation of the output
        $asJson
    )

    $filter = @{ID=$eventList;LogName="Security"} 

    if ($days) {
        $since = (Get-Date).AddDays(-1*$days)
        $filter.Add("StartTime",$since)
    }

    if ($MaxEvents) {
        $events=Get-WinEvent -FilterHashtable $filter -MaxEvents $maxEvents
    } else {
        $events=Get-WinEvent -FilterHashtable $filter
    }
    
    if ($asJson) {
        $now = get-date
        $j = [PSCustomObject]@{
            localTime=$now.toString("s");
            utcTime=$now.toUniversalTime().toString("s");
            eventlist=$eventList;
            days=$days;
            maxEvents=$maxEvents;
            matchedEvents=0;
            events=@()
        }
        if ($events) {
            # Some events to process
            $j.events = $events | Select-object -property @{n="LocalTime";e={$_.TimeCreated.ToString("s")}},@{n="UtcTime";e={$_.TimeCreated.ToUniversalTime().ToString("s")}},Id,MachineName,message
            $j.matchedEvents=$events.count
        }
        return $j | convertto-json
    } else {
        return $events
    }
}

$userId = "<%= morpheus.user.id%>"
if ($userId -match "morpheus.user.id") {
  #Not running inside Morpheus workflow or task
  Write-Host "Function Get-WinSecurityEvents Loaded"
} else {
  # Get the eventlist from the optionType
  $eventList = @(<%= customOptions.eventList %>)
  $days = <%= customOptions.eventDays %>

  $jsonResults = Get-WinSecurityEvents -EventList $eventList -Days $days -MaxEvents 50 -AsJson

  $jsonResults
}
