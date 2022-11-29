// Copyright (C) 2016-2022  Egon Willighagen
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
//     http://quickstatements.toolforge.org/

// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-pubchem', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.1.2')

import groovy.cli.commons.CliBuilder

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
pubchem = new net.bioclipse.managers.PubChemManager(workspaceRoot);

smiFile = "/Wikidata/cas.smi"; 

propertyMappings = new HashMap<String,String>()
propertyMappings.put("P2057", "P2057") // hmdb
propertyMappings.put("P3117", "P3117") // comptox
propertyMappings.put("P2063", "P2063") // lipidmaps
propertyMappings.put("P231",  "P231")  // cas
propertyMappings.put("P592",  "P592")  // chembl
propertyMappings.put("P683",  "P683")  // chebi
propertyMappings.put("P665",  "P665")  // kegg
propertyMappings.put("P2064", "P2064") // knapsack
propertyMappings.put("P661",  "P661")  // chemspider
propertyMappings.put("P662",  "P662")  // pubchem
propertyMappings.put("P3636", "P3636") // pdb
propertyMappings.put("P9405", "P9405") // nmr

propertyMappings.put("P31",   "P31")   // instance of
propertyMappings.put("P233",  "P233")  // canonical SMILES
propertyMappings.put("P234",  "P234")  // InChI
propertyMappings.put("P235",  "P235")  // InChIKey
propertyMappings.put("P248",  "P248")  // stated in
propertyMappings.put("P274",  "P274")  // chemical formula
propertyMappings.put("P703",  "P703")  // found in taxon
propertyMappings.put("P887",  "P887")  // based on heuristic
propertyMappings.put("P2017", "P2017") // isomeric SMILES
propertyMappings.put("P2067", "P2067") // mass

propertyMappings.put("Q483261",    "Q483261")    // dalton
propertyMappings.put("Q11173",     "Q11173")     // chemical compound
propertyMappings.put("Q59199015",  "Q59199015")  // group of stereoisomers
propertyMappings.put("Q113907573", "Q113907573") // inferred from SMILES
propertyMappings.put("Q113993940", "Q113993940") // inferred from InChIKey

def cli = new CliBuilder(usage: 'createWDitemsFromSMILES.groovy')
cli.c(longOpt: 'compound-class', args:1, argName:'comp', 'QID of the class of which the compound is an instance')
cli.e(longOpt: 'existing-only', 'Only output statements for existing chemicals')
cli.f(longOpt: 'input-file', args:1, argName:'filename', 'Name of the file containing the SMILES and optionally identifiers and names')
cli.h(longOpt: 'help', 'print this message')
cli.i(longOpt: 'identifier', args:1, argName:'identifier', 'Name of the database for which the identifiers are given')
cli.l(longOpt: 'with-labels', 'Take the field after the SMILES as the label of the compound')
cli.n(longOpt: 'non-existing-only', 'Only output non-existing chemicals')
cli.o(longOpt: 'output-file', args:1, argName:'output', 'Name of the file where the quickstatements are stored')
cli.p(longOpt: 'paper', args:1, argName:'paper', 'QID of the article that backs up that this compound is a chemical')
cli.q(longOpt: 'exclude-charged-compounds', 'Exclude all charged compounds, like ions')
cli.s(longOpt: 'full-chirality', 'Only output statements for compounds with full stereochemistry defined')
cli.t(longOpt: 'taxon', args:1, argName:'taxon', 'QID of the taxon in which this compound is found')
cli.x(longOpt: 'exclude-disconnected-compounds', 'Exclude all disconnected compounds, like salts')
def options = cli.parse(args)

if (options.help) {
  cli.usage()
  System.exit(0)
}

if (options.f) {
  smiFile = options.f
}

outputLabel = false
if (options.l) {
  outputLabel = true
}

compoundClassQ = null
if (options.c) {
  compoundClassQ = options.c
}

idProperty = null
if (options.identifier) {
  switch (options.identifier.toLowerCase()) {
    case "hmdb": idProperty = propertyMappings.get("P2057"); break
    case "comptox": idProperty = propertyMappings.get("P3117"); break
    case "lipidmaps": idProperty = propertyMappings.get("P2063"); break
    case "cas": idProperty = propertyMappings.get("P231"); break
    case "chembl": idProperty = propertyMappings.get("P592"); break
    case "chebi": idProperty = propertyMappings.get("P683"); break
    case "kegg": idProperty = propertyMappings.get("P665"); break
    case "knapsack": idProperty = propertyMappings.get("P2064"); break
    case "chemspider": idProperty = propertyMappings.get("P661"); break
    case "pubchem": idProperty = propertyMappings.get("P662"); break
    case "pdb": idProperty = propertyMappings.get("P3636"); break
    case "nmr": idProperty = propertyMappings.get("P9405"); break // nmrshiftdb
    default: println "Unknown identifier database: ${options.identifier}"; System.exit(-1)
  }
  if (idProperty != null) println "ID found: ${idProperty}"
}

qsFile = "/Wikidata/output.quickstatements"
if (options.o) {
  qsFile = options.o
}

taxonQID = null
if (options.t) {
  taxonQID = options.t
}

// if all SMILES come from the same paper, enter the Wikidata item code
// on the next line, e.g. paperQ = "Q22570477". It will be used as reference
// to some of the information 
paperQ = null
if (options.p) {
  paperQ = options.p
}

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

// reset the output (SD file + QuickStatements)
ui.renewFile(qsFile)
mols = cdk.createMoleculeList()

instanceOfProp        = propertyMappings.get("P31")
canSmilesProp         = propertyMappings.get("P233")
inchiProp             = propertyMappings.get("P234")
inchikeyProp          = propertyMappings.get("P235")
provProp              = propertyMappings.get("P248").replace("P", "S")
chemFormulaProp       = propertyMappings.get("P274")
pubchemProp           = propertyMappings.get("P662")
foundInTaxonProp      = propertyMappings.get("P703")
basedOnHeuristicProp  = propertyMappings.get("P887").replace("P", "S")
isoSmilesProp         = propertyMappings.get("P2017")
massProp              = propertyMappings.get("P2067")
chemicalCompoundItem  = propertyMappings.get("Q11173")
daltonUnit            = propertyMappings.get("Q483261").replace("Q", "U")
stereoisomerGroupItem = propertyMappings.get("Q59199015")
inchikeyInferredItem  = propertyMappings.get("Q113993940")
smilesInferredItem    = propertyMappings.get("Q113907573")

new File(bioclipse.fullPath(smiFile)).eachLine { line ->
  if (line.startsWith("#")) return
  sleep(250) // keep PubChem happy
  smiles = ""; id = ""; inchikey = ""; extid = ""; name = ""
  existingQcode = ""; classInfo = ""
  if (line.contains("\t")) {
    fields = line.split("\t")
    if (fields.length == 2) {
      if (outputLabel) {
        (smiles, name) = fields
      } else {
        (smiles, extid) = fields
      }
    }
    // if (fields.length == 3) (inchikey, extid, smiles) = fields
    if (fields.length == 3) (smiles, extid, name) = fields
    if (fields.length == 1) smiles = fields[0]
  } else {
    smiles = line
    id = "LAST"
  }
  compoundQ = null
  
  mol = cdk.fromSMILES(smiles)
  println "Parsed $smiles into $mol"

  smilesProp = canSmilesProp
  if (smiles.contains("@") ||
      smiles.contains("/") ||
      smiles.contains("\\")) smilesProp = isoSmilesProp
  
  inchiObj = inchi.generate(mol)
  inchiShort = inchiObj.value.substring(6)
  key = inchiObj.key

  // check for duplicate based on the InChIKey
  sparql = """
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound WHERE {
    ?compound wdt:$inchikeyProp "$key" .
  }
  """
  if (bioclipse.isOnline()) {
    rawResults = bioclipse.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    results = rdf.processSPARQLXML(rawResults, sparql)
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
    rawResults = bioclipse.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    results = rdf.processSPARQLXML(rawResults, sparql)
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
  mass = cdk.calculateMass(mol)
  
  // Create the Wikidata QuickStatement, see https://tools.wmflabs.org/wikidata-todo/quick_statements.php
  
  item = "LAST" // set to Qxxxx if you need to append info, e.g. item = "Q22579236"
  
  pubchemLine = ""
  if (bioclipse.isOnline()) {
    try {
      pcResults = pubchem.search(key)
      sleep(250) // keep PubChem happy
      if (pcResults.size() == 1) {
        cid = pcResults[0]
        pubchemLine = "$item\t$pubchemProp\t\"$cid\"\t$basedOnHeuristicProp\t$inchikeyInferredItem"
  	  sparql = """
  	  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
    	  SELECT ?compound WHERE {
          ?compound wdt:$pubchemProp "$cid" .
  	  }
  	  """

        if (bioclipse.isOnline()) {
          rawResults = bioclipse.sparqlRemote(
            "https://query.wikidata.org/sparql", sparql
          )
          results = rdf.processSPARQLXML(rawResults, sparql)
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
      } else {
        println "PubChem hits: $pcResults"
      }
    } catch (Exception exception) {
      println "Error while accessing PubChem: ${exception.message}"
    }
  }

  paperProv = ""
  if (paperQ != null) paperProv = "\t$provProp\t$paperQ"

  undefinedCenters = cdk.getAtomsWithUndefinedStereo(mol)
  fullChiralityIsDefined = undefinedCenters.size() == 0
  ignoreBecauseStereoMissing =  options.s && !fullChiralityIsDefined
  
  totalFormalCharge = cdk.totalFormalCharge(mol)
  isCharged = (totalFormalCharge != 0)
  ignoreBecauseCharged = options.q && isCharged

  isDisconnected = (cdk.partition(mol).size() > 1)
  ignoreBecauseDisconnected = options.x && isDisconnected

  if (!missing && options.'non-existing-only') {
    println "===================="
    println (new String((char)27) + "[31m" + "$formula is already in Wikidata as " + existingQcode + new String((char)27) + "[37m")
    if (fullChiralityIsDefined) {
      println "Full stereochemistry is defined"
    } else {
      println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
    }
  } else if (!missing && ignoreBecauseStereoMissing) {
    println "===================="
    println (new String((char)27) + "[31m" + "$formula is already in Wikidata as " + existingQcode + new String((char)27) + "[37m")
    println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
  } else if (!missing) {
    println "===================="
    println (new String((char)27) + "[31m" + "$formula is already in Wikidata as " + existingQcode + new String((char)27) + "[37m")
    if (fullChiralityIsDefined) {
      println "Full stereochemistry is defined"
    } else {
      println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
    }

    item = existingQcode.substring(32)
    pubchemLine = pubchemLine.replace("LAST", "Q" + existingQcode.substring(32))

    if (compoundClassQ != null) classInfo = "Q$item\t$instanceOfProp\t$compoundClassQ"

    newInfo = false

    if (fullChiralityIsDefined) {
      typeInfo = "Q$item\t$instanceOfProp\t$chemicalCompoundItem" // chemical compound
    } else {
      typeInfo = "Q$item\t$instanceOfProp\t$stereoisomerGroupItem" // group of stereoisomers
    }

    if (classInfo != "") {
      statement = """
        $classInfo$paperProv\n"""
    } else {
      statement = ""
    }

    // check for missing properties
    sparql = """
      PREFIX wdt: <http://www.wikidata.org/prop/direct/>
      SELECT ?compound ?formula ?key ?inchi ?smiles ?pubchem ?mass WHERE {
        VALUES ?compound { <${existingQcode}> }
        OPTIONAL { ?compound wdt:$smilesProp ?smiles }
        OPTIONAL { ?compound wdt:$chemFormulaProp ?formula }
        OPTIONAL { ?compound wdt:$inchikeyProp ?key }
        OPTIONAL { ?compound wdt:$inchiProp ?inchi }
        OPTIONAL { ?compound wdt:$pubchemProp ?pubchem }
        OPTIONAL { ?compound wdt:$massProp ?mass }
      }
    """
    if (bioclipse.isOnline()) {
      rawResults = bioclipse.sparqlRemote(
        "https://query.wikidata.org/sparql", sparql
      )
      results = rdf.processSPARQLXML(rawResults, sparql)
      missing = results.rowCount == 0
      if (!missing) {
        if (results.get(1,"smiles") == null || results.get(1,"smiles").trim().length() == 0) {
           if (smiles.length() <= 400) {
             statement += "      Q$item\t$smilesProp\t\"$smiles\"\n"; newInfo = true
           }
        }
        if (results.get(1,"formula") == null || results.get(1,"formula").trim().length() == 0) {
          statement += "      Q$item\t$chemFormulaProp\t\"$formula\"\t$basedOnHeuristicProp\t$smilesInferredItem\n"
          newInfo = true
        }
        if (results.get(1,"mass") == null || results.get(1,"mass").trim().length() == 0) {
          statement += "      Q$item\t$massProp\t${mass}$daltonUnit\t$basedOnHeuristicProp\t$smilesInferredItem\n"
          newInfo = true
        }
        if (results.get(1,"key") == null || results.get(1,"key").trim().length() == 0) {
          statement += "      Q$item\t$inchikeyProp\t\"$key\"\t$basedOnHeuristicProp\t$smilesInferredItem\n"
          newInfo = true
        }
        if (results.get(1,"inchi") == null || results.get(1,"inchi").trim().length() == 0) {
          if (inchiShort.length() <= 400) {
            statement += "      Q$item\t$inchiProp\t\"InChI=$inchiShort\"\t$basedOnHeuristicProp\t$smilesInferredItem\n"
            newInfo = true
          }
        }
        if (results.get(1,"pubchem") == null || results.get(1,"pubchem").trim().length() == 0) {
          statement += "      $pubchemLine\n"
          newInfo = true
        }
      }
    }
  
    if (idProperty != null && idProperty != "" && idProperty != $pubchemProp && !extidFound) {
      statement += "      Q$item\t$idProperty\t\"$extid\"$paperProv\n"
      newInfo = true
    }
    
    if (taxonQID != null) {
      statement += "      Q$item\t$foundInTaxonProp\t$taxonQID$paperProv\n"
      newInfo = true
    }

    ui.append(qsFile, statement + "\n")
    
    // Recon stuff
    
    //statement = """
    //  Q$item\tP703\tQ15978631\tS248\tQ28601559"
    //"""
    //ui.append(qsFile, statement + "\n")
    //ui.append(reconFile, "$id Q$item\n")
      
    println "===================="
  } else if (ignoreBecauseStereoMissing) {
    println "===================="
    println (new String((char)27) + "[32m" + "$formula is not yet in Wikidata" + new String((char)27) + "[37m")
    println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
    println "===================="
  } else if (ignoreBecauseCharged) {
    println "===================="
    println (new String((char)27) + "[32m" + "$formula is not yet in Wikidata" + new String((char)27) + "[37m")
    println "Compound is charged. skipping"
    println "===================="
  } else if (ignoreBecauseDisconnected) {
    println "===================="
    println (new String((char)27) + "[32m" + "$formula is not yet in Wikidata" + new String((char)27) + "[37m")
    println "Compound is disconnected. skipping"
    println "===================="
  } else if (!ignoreBecauseStereoMissing && !ignoreBecauseCharged && !ignoreBecauseDisconnected && !options.e) {
    println "===================="
    println (new String((char)27) + "[32m" + "$formula is not yet in Wikidata" + new String((char)27) + "[37m")
    if (fullChiralityIsDefined) {
      println "Full stereochemistry is defined"
      typeInfo = "$item\t$instanceOfProp\t$chemicalCompoundItem" // chemical compound
    } else {
      println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
      typeInfo = "$item\t$instanceOfProp\t$stereoisomerGroupItem" // group of stereoisomers
    }

    if (item == "LAST") {
      statement = """
      CREATE
      """
    } else statement = ""

    if (compoundClassQ != null) statement += "$item\tP31\t$compoundClassQ$paperProv\n"
   
    statement += """
      $typeInfo
      $item\tDen\t\"chemical compound\"$paperProv
      $item\t$smilesProp\t\"$smiles\"\t$basedOnHeuristicProp\t$smilesInferredItem
      $item\tP274\t\"$formula\"\t$basedOnHeuristicProp\t$smilesInferredItem
      $item\tP2067\t${mass}$daltonUnit\t$basedOnHeuristicProp\t$smilesInferredItem
    """
    if (name.length() > 0) {
      if (name.length() < 200) statement += "  $item\tLen\t\"${name}\"\n    "
      else statement += "  $item\tLen\t\"${key}\"\n    "
    }
    if (inchiShort.length() <= 400) statement += "  $item\t$inchiProp\t\"InChI=$inchiShort\"\t$basedOnHeuristicProp\t$smilesInferredItem"
    statement += """
      $item\t$inchikeyProp\t\"$key\"
      $pubchemLine
    """

    if (idProperty != null && idProperty != "") {
      if (idProperty == pubchemProp && pubchemLine.contains(pubchemProp)) {} else
      statement += "  $item\t$idProperty\t\"$extid\"$paperProv"
    }

    ui.append(qsFile, statement + "\n")
    println "===================="
  }

  mols.add(mol)
  return
}

// ui.open(mols)
// ui.open(qsFile)
