// Copyright (C) 2019  Egon Willighagen
// License: MIT

// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.8')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.8')
workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

sparql = """
PREFIX wdt:      <http://www.wikidata.org/prop/direct/>
PREFIX wd:       <http://www.wikidata.org/entity/>
PREFIX wikibase: <http://wikiba.se/ontology#>
PREFIX bd:       <http://www.bigdata.com/rdf#>

SELECT ?guideline ?guidelineLabel WHERE {
  ?guideline wdt:P1433 ?series .
  ?series wdt:P361 wd:Q7072447 .
  SERVICE wikibase:label { bd:serviceParam wikibase:language "[AUTO_LANGUAGE],en". }
} ORDER BY ?guideline
"""

if (bioclipse.isOnline()) {
  results = rdf.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
  )
  for (i in 1..results.rowCount) {
    String iri = results.get(i, "guideline").replace("wd:","http://www.wikidata.org/entity/")
    String title = results.get(i, "guidelineLabel")
    println "    <!-- ${iri} -->\n"
    println "    <owl:Class rdf:about=\"${iri}\">"
    println "      <rdfs:subClassOf rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C17564\"/>"
    println "      <rdfs:label xml:lang=\"en\">${title}</rdfs:label>"
    println "    </owl:Class>\n"
  }
}
