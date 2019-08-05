// Copyright (C) 2016-2019  Egon Willighagen
// License: MIT

// Usage:
//
//   change the values of the following compoundClassQ and idProperty variables. The first is
//   is an (optional; null by default) for the superclass, and the second is the property of
//   the identifiers of the chemical to check.
//
//   The input file is a TAB separated file with two columns, a isomeric SMILES in the first,
//   and an optional identifiers matching the given property in the second.
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     https://tools.wmflabs.org/quickstatements/
//
// Changelog:
//
// 2018-12-02 Added a changelog
// 2018-12-01 Added a feature to set a superclass

// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.7')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.7')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.7')
@Grab(group='io.github.egonw.bacting', module='managers-pubchem', version='0.0.7')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.0.7')
workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
pubchem = new net.bioclipse.managers.PubChemManager(workspaceRoot);

smiFile = "/Wikidata/cas.smi"; 

compoundClassQ = null
// compoundClassQ = "Q419287" // gangliosides
// compoundClassQ = "Q60224961" // phytocassane
// compoundClassQ = "Q59944374" // 3‐butyl‐5‐(1‐oxopropyl)indolizidine, an ant creation
// compoundClassQ = "Q2981798" // ubiquinones

idProperty = null
// idProperty = "P3636" // PDB ligand ID
// idProperty = "P662" // PubChem CID
// idProperty = "P661" // ChemSpider
// idProperty = "P2064" // KNAPSaCK
idProperty = "P2057" // HMDB
// idProperty = "P665" // KEGG ID
// idProperty = "P683" // ChEBI
// idProperty = "P592" // ChEMBL
// idProperty = "P231" // CAS
idProperty = "P2063" // LIPID MAPS
// idProperty = "P3117" // CompTox

qsFile = "/Wikidata/output.quickstatements"

// if all SMILES come from the same paper, enter the Wikidata item code
// on the next line, e.g. paperQ = "Q22570477". It will be used as reference
// to some of the information 
paperQ = null

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
mols = cdk.createMoleculeList()

new File(bioclipse.fullPath(smiFile)).eachLine { line ->
  if (line.startsWith("#")) return
  sleep(250) // keep PubChem happy
  smiles = ""; id = ""; inchikey = ""; extid = ""; name = ""
  existingQcode = ""; classInfo = ""
  if (line.contains("\t")) {
    fields = line.split("\t")
    if (fields.length == 2) (smiles, extid) = fields 
    // if (fields.length == 3) (inchikey, extid, smiles) = fields
    if (fields.length == 3) (smiles, extid, name) = fields
  } else {
    smiles = line
    id = "LAST"
  }
  compoundQ = null
  
  mol = cdk.fromSMILES(smiles)
  
  smilesProp = "P233"
  if (smiles.contains("@") ||
      smiles.contains("/") ||
      smiles.contains("\\")) smilesProp = "P2017"
  
  inchiObj = inchi.generate(mol)
  inchiShort = inchiObj.value.substring(6)
  key = inchiObj.key

  // check for duplicate based on the InChIKey
  sparql = """
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound WHERE {
    ?compound wdt:P235 "$key" .
  }
  """
  if (bioclipse.isOnline()) {
    results = rdf.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    missing = results.rowCount == 0
    if (!missing) {
      existingQcode = results.get(1,"compound")
      println "InChIKey match: $existingQcode"
      missing = false
    }
  } else {
    println "no online access"
    missing = true
  }

  // check for duplicate based on the given identifier (with $idProperty)
  extidFound = false
  sparql = """
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound WHERE {
    ?compound wdt:$idProperty "$extid" .
  }
  """
  if (bioclipse.isOnline()) {
    results = rdf.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    idMissing = results.rowCount == 0
    if (!idMissing) {
      existingQcode = results.get(1,"compound")
      println "$idProperty match: $existingQcode"
      extidFound = true
    }
  } else {
    println "no online access"
    extidFound = false
  }
  missing = missing && (!extidFound)

  formula = upgradeChemFormula(cdk.molecularFormula(mol))
  
  // Create the Wikidata QuickStatement, see https://tools.wmflabs.org/wikidata-todo/quick_statements.php
  
  item = "LAST" // set to Qxxxx if you need to append info, e.g. item = "Q22579236"
  
  pubchemLine = ""
  if (bioclipse.isOnline()) {
    pcResults = pubchem.search(key)
    sleep(250) // keep PubChem happy
    if (pcResults.size == 1) {
      cid = pcResults[0]
      pubchemLine = "$item\tP662\t\"$cid\""
	  sparql = """
	  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  	  SELECT ?compound WHERE {
        ?compound wdt:P662 "$cid" .
	  }
	  """
  	
      if (bioclipse.isOnline()) {
	    results = rdf.sparqlRemote(
          "https://query.wikidata.org/sparql", sparql
	    )
	    missing = results.rowCount == 0
	    if (!missing) {
  	      pcExistingQcode = results.get(1,"compound")
  	      println "PubChem CID match: $pcExistingQcode"
  	      if (existingQcode != "") {
  	        if (existingQcode != pcExistingQcode) {
  	          println "Conflicting Qcodes: $existingQcode and $pcExistingQcode"
  	        } // else: OK, the same
  	      } else {
            existingQcode = pcExistingQcode
  	      }
  	    } else {
  	     if (existingQcode != "") {
  	       missing = false // we already found one using the InChIKey
  	     }
  	    }
	  } else {
        println "no online access"
	    missing = true
	  }
    }
  }

  paperProv = ""
  if (paperQ != null) paperProv = "\tS248\t$paperQ"

  if (!missing) {
    println "===================="
    println "$formula is already in Wikidata as " + existingQcode
  
    item = existingQcode.substring(32)
    pubchemLine = pubchemLine.replace("LAST", "Q" + existingQcode.substring(32))

    if (compoundClassQ != null) classInfo = "Q$item\tP31\t$compoundClassQ"

    statement = """
      $classInfo
      Q$item\tP31\tQ11173$paperProv\n"""

    // check for missing properties
    sparql = """
      PREFIX wdt: <http://www.wikidata.org/prop/direct/>
      SELECT ?compound ?formula ?key ?inchi ?smiles ?pubchem WHERE {
        VALUES ?compound { <${existingQcode}> }
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
      missing = results.rowCount == 0
      if (!missing) {
        if (results.get(1,"smiles") == null || results.get(1,"smiles").trim().length() == 0) {
           if (smiles.length() <= 400) statement += "      Q$item\t$smilesProp\t\"$smiles\"\n"
        }
        if (results.get(1,"formula") == null || results.get(1,"formula").trim().length() == 0) statement += "      Q$item\tP274\t\"$formula\"\n"
        if (results.get(1,"key") == null || results.get(1,"key").trim().length() == 0)         statement += "      Q$item\tP235\t\"$key\"\n"
        if (results.get(1,"inchi") == null || results.get(1,"inchi").trim().length() == 0) {
           if (inchiShort.length() <= 400) statement += "      Q$item\tP234\t\"$inchiShort\"\n"
        }
        if (results.get(1,"pubchem") == null || results.get(1,"pubchem").trim().length() == 0)     statement += "      $pubchemLine\n"
      }
    }
  
    if (idProperty != null && idProperty != "" && !extidFound) {
      statement += "      Q$item\t$idProperty\t\"$extid\"\n"
    }

    ui.append(qsFile, statement + "\n")
    
    // Recon stuff
    
    //statement = """
    //  Q$item\tP703\tQ15978631\tS248\tQ28601559"
    //"""
    //ui.append(qsFile, statement + "\n")
    //ui.append(reconFile, "$id Q$item\n")
      
    println "===================="
  } else {
    if (item == "LAST") {
      statement = """
      CREATE
      """
    } else statement = ""
   
    if (compoundClassQ != null) statement += "$item\tP31\t$compoundClassQ\n"
   
    statement += """
      $item\tP31\tQ11173$paperProv
      $item\tDen\t\"chemical compound\"$paperProv
      $item\t$smilesProp\t\"$smiles\"
      $item\tP274\t\"$formula\"
    """
    if (name.length() > 0) statement += "  $item\tLen\t\"${name}\"\n    "
    if (inchiShort.length() <= 400) statement += "  $item\tP234\t\"$inchiShort\""
    statement += """
      $item\tP235\t\"$key\"
      $pubchemLine
    """

    if (idProperty != null && idProperty != "") {
      statement += "  $item\t$idProperty\t\"$extid\""
    }

    println "===================="
    println "$formula is not yet in Wikidata"
    ui.append(qsFile, statement + "\n")
    println "===================="
  }

  mols.add(mol)
  return
}

// ui.open(mols)
// ui.open(qsFile)
