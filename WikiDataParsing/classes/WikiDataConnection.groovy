package classes

/**@
 * Class to manage the interactions between WikiData
 * Focus on extracting missing details required for the compound class
 */
class WikiDataConnection {

    String workspaceRoot
    String endPoint

    GroovyShell shell = new GroovyShell()
    def tools = shell.parse(new File('tools/SparQLUtils.groovy'))

    /**@
     * Create WikiData Connection object based on workspace Root (required for bacting) and endpoint
     *
     * @param workspaceRoot String: A string for the root area of the workspace
     * @param endPoint String: The desired endpoint for queries, defaults to WikiData
     * @return WikiDataConnection Object
     */
    WikiDataConnection(String workspaceRoot='..', String endPoint='https://query.wikidata.org/sparql') {
        this.workspaceRoot = workspaceRoot
        this.endPoint = endPoint
    }

    /**@
     * Query the selected end point with a string query
     *
     * @param query String: To send to the end point
     * @return: results from the query
     */
    def queryEndPoint(String queryString) {
        if (queryString == '') {
            println 'No query entered'
            return [:]
        }
        println "Executing query: \n${queryString}"
        return tools.getWikiData( this.workspaceRoot, this.endPoint,  queryString)
    }

}
