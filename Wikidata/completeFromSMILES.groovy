// Copyright (C) 2016-2019  Egon Willighagen
// License: MIT

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

smiFile = "/Wikidata/cas2.smi"; 

qsFile = "/Wikidata/output.quickstatements"

// a helper function
def upgradeChemFormula(formula) {
  formula = formula.replace("0","₀");
  formula = formula.replace("1","₁");
  formula = formula.replace("2","₂");
  formula = formula.replace("3","₃");
  formula = formula.replace("4","₄");
  formula = formula.replace("5","₅");
  formula = formula.replace("6","₆");
  formula = formula.replace("7","₇");
  formula = formula.replace("8","₈");
  formula = formula.replace("9","₉");
}
def renewFile(file) {
  if (ui.fileExists(file)) ui.remove(file)
  ui.newFile(file)
  return file
}

// reset the output (SD file + QuickStatements)
renewFile(qsFile)
new File(bioclipse.fullPath(smiFile)).eachLine { line ->
  if (line.startsWith("#")) return
  if (line.startsWith("?")) return
  sleep(250) // keep PubChem happy
  smiles = ""; ismiles = null; id = ""; inchikey = ""; extid = ""; name = ""
  existingQcode = ""; classInfo = ""
  if (line.contains("\t")) {
    fields = line.split("\t")
    if (fields.length == 3) (smiles, ismiles, compoundQ) = fields
  }

  if (ismiles != null && ismiles != "") smiles = ismiles

  if (smiles.contains(".")) {
    println "Skipping ${compoundQ} bc it is a salt: ${smiles}"
    return
  }
  if (smiles.contains("*") || smiles.contains("[R")) {
    println "Skipping ${compoundQ} bc it has a R group: ${smiles}"
    return
  }

  try {
    mol = cdk.fromSMILES(smiles)
  } catch (Exception exception) {
    println exception.message
    return
  }
  atomsWithUndefinedStereo = cdk.getAtomsWithUndefinedStereo(mol)
  if (atomsWithUndefinedStereo.size() > 0) {
    println "Molecule has ${atomsWithUndefinedStereo.size()} undefined stereo atoms: ${smiles}"
    return
  } else {
    println "Molecule has no undefined stereo atoms"
  }
  
  smilesProp = "P233"
  if (smiles.contains("@") ||
      smiles.contains("/") ||
      smiles.contains("\\")) smilesProp = "P2017"
  
  inchiObj = inchi.generate(mol)
  inchiShort = inchiObj.value.substring(6)
  key = inchiObj.key

  formula = upgradeChemFormula(cdk.molecularFormula(mol))
  
  // Create the Wikidata QuickStatement, see https://tools.wmflabs.org/wikidata-todo/quick_statements.php
  
  item = compoundQ
  existingQcode = compoundQ
  
  pubchemLine = ""
  if (bioclipse.isOnline()) {
    pcResults = pubchem.search(key)
    sleep(250) // keep PubChem happy
    if (pcResults.size == 1) {
      cid = pcResults[0]
    }
  }

  println "===================="
  println "$formula is already in Wikidata as " + existingQcode
  
  item = compoundQ
  pubchemLine = pubchemLine.replace("LAST", compoundQ)

  statement = """
    $classInfo"""

  // check for missing properties
  sparql = """
    PREFIX wdt: <http://www.wikidata.org/prop/direct/>
    SELECT ?compound ?formula ?key ?inchi ?smiles ?pubchem WHERE {
      VALUES ?compound { <http://www.wikidata.org/entity/${existingQcode}> }
      OPTIONAL { ?compound wdt:$smilesProp ?smiles }
      OPTIONAL { ?compound wdt:P274 ?formula }
      OPTIONAL { ?compound wdt:P235 ?key }
      OPTIONAL { ?compound wdt:P234 ?inchi }
      OPTIONAL { ?compound wdt:P662 ?pubchem }
    }
  """
  if (bioclipse.isOnline()) {
    results = rdf.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    if (results.rowCount > 0) {
      if (results.get(1,"smiles") == null || results.get(1,"smiles").trim().length() == 0) {
         if (smiles.length() <= 400) statement += "      $item\t$smilesProp\t\"$smiles\"\n"
      }
      if (results.get(1,"formula") == null || results.get(1,"formula").trim().length() == 0) statement += "      $item\tP274\t\"$formula\"\n"
      if (results.get(1,"key") == null || results.get(1,"key").trim().length() == 0)         statement += "      $item\tP235\t\"$key\"\n"
      if (results.get(1,"inchi") == null || results.get(1,"inchi").trim().length() == 0) {
         if (inchiShort.length() <= 400) statement += "      $item\tP234\t\"$inchiShort\"\n"
      }
      if (results.get(1,"pubchem") == null || results.get(1,"pubchem").trim().length() == 0)     statement += "      $pubchemLine\n"
    }
  }
  
  ui.append(qsFile, statement + "\n")
    
  println "===================="

  return
}
