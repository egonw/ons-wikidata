// Copyright (C) 2019  Egon Willighagen
// License: MIT

@Grab(group='io.github.egonw.bacting', module='bioclipse-core', version='2.8.0.2')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.9-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.8')
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.8')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);

output = "/PubChem/wikidata.csv"
ui.renewFile(output)

// the following query gets all chemical with a canonical or isomeric SMILES
sparql = """
SELECT (STR(?chemicalRes) as ?chemical) ?chemicalResLabel ?canonical ?isomeric WITH {
  SELECT DISTINCT ?chemicalRes WHERE {
    ?chemicalRes wdt:P233 | wdt:P2017 [] .
  } LIMIT 1000
} AS %CHEMICALS { 
  INCLUDE %CHEMICALS
  OPTIONAL { ?chemicalRes wdt:P233 ?canonical }
  OPTIONAL { ?chemicalRes wdt:P2017 ?isomeric }
  SERVICE wikibase:label { bd:serviceParam wikibase:language "[AUTO_LANGUAGE],en". }
}
  """

regIDs = new HashSet<String>();

ui.append(output,
  "PUBCHEM_EXT_DATASOURCE_REGID," +
  "PUBCHEM_SUBSTANCE_SYNONYM," +
  "PUBCHEM_EXT_SUBSTANCE_URL," +
  "PUBCHEM_EXT_DATASOURCE_SMILES\n"
)
if (bioclipse.isOnline()) {
  try {
    rawResults = bioclipse.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    results = rdf.processSPARQLXML(rawResults, sparql)
    1.upto(results.rowCount) { chemCounter ->
      wdid   = results.get(chemCounter, "chemical")
      label  = results.get(chemCounter, "chemicalResLabel")
      canSmi = results.get(chemCounter, "canonical")
      isoSmi = results.get(chemCounter, "isomeric")
      outSmi = (isoSmi ? isoSmi : canSmi)
      qid = wdid.substring(31)
      if (!regIDs.contains(qid)) {
        regIDs.add(qid)
        try {
          mol = cdk.fromSMILES(outSmi)
          outCmp = "${qid},"
          label = label.replaceAll("\"", "''").replaceAll('\n',"").replaceAll('\r',"")
          if (label.contains(",")) {
            outCmp += "\"" + label + "\","
          } else {
            outCmp += label + ","
          }
          outCmp += "https://tools.wmflabs.org/scholia/${qid},"
          outCmp += "$outSmi\n"
          ui.append(output, outCmp)
        } catch (Exception exception) {
          println("${qid}: ${exception.message}")
        }
      }
    }
  } catch (Exception exception) {
    println exception.message
  }
}
