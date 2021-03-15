@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.12')
class SparQL{
    /*
    * Common helper methods for SPARQL
    */
    String workspaceRoot
    String endpoint = "https://query.wikidata.org/sparql"
    String query_string
    def rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
    /*
    * Initialize
    */
    def SparQL(workspaceRoot='..', query_string=""){
        this.workspaceRoot = workspaceRoot
        this.query_string = query_string
    }

    /*
    * Query the SPARQL endpoint with the query string
    * and the endpoint defined in the class 
    */
    def queryEndPoint(){
        if (this.endpoint == ""){
            println "No SPARQL endpoint defined, add an endpoint"
            return
        }
        if (this.query_string == ""){
            println "No query defined, please defined a SPARQL query"
            return 
        }
        return this.rdf.sparqlRemote(this.endpoint, this.query_string)
    }
}