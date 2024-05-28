Function Grep-Code {
    [CmdletBinding()]
    param (
        [String]$Pattern,
        [String]$Path
    )


    $GroovyFiles = Get-ChildItem -Path $Path -Include *.groovy -Recurse
    $MatchedItems = $GroovyFiles | Select-String -Pattern $Pattern
    return $MatchedItems | Select-Object -Property Filename,LineNumber,@{N="MatchedLine";E={$_.Line.trim()}}
}
