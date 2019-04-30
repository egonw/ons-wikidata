// Copyright (C) 2019  Egon Willighagen
// License: MIT

//   Takes a list of InChIs and fetches SMILES for them from PubChem.

// Bacting config
@Grab(group='net.bioclipse.bacting', module='managers-cdk', version='0.0.4-SNAPSHOT')
@Grab(group='net.bioclipse.bacting', module='managers-rdf', version='0.0.3')
@Grab(group='net.bioclipse.bacting', module='managers-ui', version='0.0.3')
@Grab(group='net.bioclipse.bacting', module='managers-pubchem', version='0.0.3')
@Grab(group='net.bioclipse.bacting', module='managers-inchi', version='0.0.3')
workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
pubchem = new net.bioclipse.managers.PubChemManager(workspaceRoot);

inchiFile = "/ExtIdentifiers/Accession_to_InChi-Key.txt_uniqMissing.txt"
idIndex = 1
inchikeyIndex = 0

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT DISTINCT ?key WHERE {
  ?compound wdt:P235 ?key .
}
"""

if (bioclipse.isOnline()) {
  results = rdf.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
  )
}
// make a map
map = new HashSet()
for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  map.add(rowVals[0])  
}

new File(bioclipse.fullPath(inchiFile)).eachLine { line ->
  inchikey = null; extid = null; name = ""
  if (line.contains("\t")) {
    fields = line.trim().split("\t")
    inchikey = fields[inchikeyIndex]
    extid = fields[idIndex]
    if (fields.length == 3) name = fields[2]
  } else {
    inchikey = line.trim()
  }
  
  if (map.contains(inchikey)) {
    println "# $inchikey is already in Wikidata"
    return
  }

  sleep(150) // keep PubChem happy
  try {
    smiles = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${inchikey}/property/IsomericSMILES/TXT")
    String[] smiLines = smiles.split("\r\n|\r|\n");
    if (smiLines.length > 1) {
      println "# Multiple SMILES returned for $inchikey"
      return
    }
    smiles = smiles.trim()
    mol = cdk.fromSMILES(smiles)

    atomsWithUndefinedStereo = cdk.getAtomsWithUndefinedStereo(mol)
    if (atomsWithUndefinedStereo.size() > 0) {
      println "# Molecule has ${atomsWithUndefinedStereo.size()} undefined stereo atoms: ${smiles}"
      return
    } else {
      // println "# Molecule has no undefined stereo atoms"
    }

    inchiObj = inchi.generate(mol)
    inchiShort = inchiObj.value.substring(6)
    key = inchiObj.key
    if (key != inchikey) { println "# Key mismatch: started with $inchikey but SMILES translates to $key"; return }
    output = smiles.trim()
    if (extid != null) {
      output += "\t" + extid
    }
    if (name != null) {
      output += "\t" + name
    }
    println output
  } catch (Exception e) { println "# ${inchikey} exception: " + e.message }
}


