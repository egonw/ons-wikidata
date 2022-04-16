// Copyright (C) 2019-2022  Egon Willighagen
// License: MIT

@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.34')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.34')
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.34')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);

// do it in a few runs, so that errors from PubChem are easier to handle
regIDs = new HashSet<String>();

batches   = 1
batchSize = 1500000

1.upto(batches) { batchCounter ->
  offset = (batchCounter - 1) * batchSize
  // the following query gets all chemical with a canonical or isomeric SMILES
  sparql = """
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?chemical ?smiles WHERE {
    SERVICE <https://query.wikidata.org/sparql> {
      SELECT DISTINCT ?chemical ?smiles WHERE {
        ?chemical wdt:P233 | wdt:P2017 [] .
        OPTIONAL { ?chemical wdt:P233 ?canonical }
        OPTIONAL { ?chemical wdt:P2017 ?isomeric }
        BIND (COALESCE(?isomeric, ?canonical) AS ?smiles)
      }
    }
  } LIMIT ${batchSize} OFFSET ${offset}
  """

  output = "/PubChem/wikidata_${batchCounter}.csv"
  ui.renewFile(output)

  ui.append(output,
    "PUBCHEM_EXT_DATASOURCE_REGID," +
    "PUBCHEM_EXT_SUBSTANCE_URL," +
    "PUBCHEM_EXT_DATASOURCE_SMILES\n"
  )
  if (bioclipse.isOnline()) {
    try {
      rawResults = bioclipse.sparqlRemote(
        // "https://query.wikidata.org/sparql", sparql
        "https://beta.sparql.swisslipids.org/sparql?format=xml", sparql
      )
      results = rdf.processSPARQLXML(rawResults, sparql)
      1.upto(results.rowCount) { chemCounter ->
        wdid   = results.get(chemCounter, "chemical")
        smiles = results.get(chemCounter, "smiles")
        qid = wdid.substring(31)
        if (!regIDs.contains(qid) &&
            !smiles.contains(" ") && // ignore entries with spaces in the SMILES
            !smiles.contains("[R1]")) // ignore entries with R1 in the SMILES
        {
          regIDs.add(qid)
          try {
            smiles = smiles.replace("\\\\","\\")
            mol = cdk.fromSMILES(smiles)
            outCmp = "${qid},"
            outCmp += "https://scholia.toolforge.org/${qid},"
            outCmp += "${smiles}\n"
            ui.append(output, outCmp)
          } catch (Exception exception) {
            println("${qid}: ${exception.message}")
          }
        }
      }
    } catch (Exception exception) {
      println exception.message
    }
  } // end is online
} // and batch loop
