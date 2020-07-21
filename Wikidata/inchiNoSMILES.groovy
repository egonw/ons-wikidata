// Copyright (C) 2019  Egon Willighagen
// License: MIT

//   Takes a list of InChIs and fetches SMILES for them from PubChem.

// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-pubchem', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.0.12')
workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
pubchem = new net.bioclipse.managers.PubChemManager(workspaceRoot);

idIndex = null
inchikeyIndex = 0

sparql = """
SELECT ?cmp ?key WITH {
  SELECT ?cmp ?key WHERE {
    ?cmp wdt:P235 ?key .
  } LIMIT 5000
} AS %CMPDS WHERE {
  INCLUDE %CMPDS
  MINUS { ?cmp wdt:P233 | wdt:P2017 [] }
}
"""

if (bioclipse.isOnline()) {
  rawResults = bioclipse.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
  )
  results = rdf.processSPARQLXML(rawResults, sparql)
}
// make a map
map = new HashMap<String,String>()
for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  map.put(rowVals[1], rowVals[0])
}

for (inchikey in map.keySet()) {
  sleep(150) // keep PubChem happy
  try {
    smiles = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${inchikey}/property/IsomericSMILES/TXT")
    String[] smiLines = smiles.split("\r\n|\r|\n");
    if (smiLines.length > 1) {
      println "# Multiple SMILES returned for $inchikey"
      continue
    }
    smiles = smiles.trim()

    if (smiles.contains(".")) {
      println "# Skipping SMILES of disconnected structure for $inchikey"
      continue
    }

    mol = cdk.fromSMILES(smiles)

    atomsWithUndefinedStereo = cdk.getAtomsWithUndefinedStereo(mol)
    if (atomsWithUndefinedStereo.size() > 0) {
      println "# Molecule has ${atomsWithUndefinedStereo.size()} undefined stereo atoms: ${smiles}"
      continue
    } else {
      // println "# Molecule has no undefined stereo atoms"
    }

    inchiObj = inchi.generate(mol)
    inchiShort = inchiObj.value.substring(6)
    key = inchiObj.key
    if (key != inchikey) {
      println "# Key mismatch: started with $inchikey but SMILES translates to $key"
      continue
    }
    output = smiles.trim()
    println output
  } catch (Exception e) { println "# ${inchikey} exception: " + e.message }
}
