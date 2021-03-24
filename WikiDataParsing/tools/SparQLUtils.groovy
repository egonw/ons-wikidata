package tools

@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.12')

/**@
* Query SparQL end point with query, include pause to avoid overuse
*
* @param workspaceRoot String: A string for the root area of the workspace
* @param endPoint String: The desired endpoint for queries, defaults to WikiData
* @param queryString String: The query to send to the end point
* @return Query results
*/
def getWikiData(String workspaceRoot, String endPoint, String queryString){
    try {
        // Add pause to prevent too many calls to SparQL endpoint
        println 'Querying SPARQL end point...'
        sleep(150)
        rdf = new net.bioclipse.managers.RDFManager(workspaceRoot)
        results = rdf.sparqlRemote(endPoint, queryString)
        println 'Returning query results..'
        return results
    }
    catch (Exception e) {
        println '# exception: ' + e.message
    }
    return []
}
