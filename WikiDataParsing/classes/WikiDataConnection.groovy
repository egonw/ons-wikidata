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

    /**@
     * Check whether the compound currently exists in WikiData
     * Check whether the properties of that compound exist
     * Check whether the QID in pubchem matchers WikiData
     * Two options:
     * Query each compound and property
     * Query all compounds and properties
     */
    def checkCompoundProperties() {
        if (bioclipse.isOnline()) {
            results = rdf.sparqlRemote(
            'https://query.wikidata.org/sparql', sparql
          )
            // results check
            missing = results.rowCount == 0

            // Get the compound element from the first row of the results
            pcExistingQcode = results.get(1, 'compound')

            // Set the paper reference
            if (paperQ != null) paperProv = "\tS248\t$paperQ"

            item = existingQcode.substring(32)
            pubchemLine = pubchemLine.replace('LAST', 'Q' + existingQcode.substring(32))
        }
    }

}
