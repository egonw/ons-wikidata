import classes.Compound
import classes.WikiDataProperty
import com.xlson.groovycsv.CsvParser
import org.openscience.cdk.interfaces.IMolecularFormula;

@Grab(group='io.github.egonw.bacting', module='managers-excel', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-pubchem', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.12')
@Grab('com.xlson.groovycsv:groovycsv:1.0')

workspaceRoot = '..'
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
WDP = new WikiDataProperty()

List<Map> csv2List( String filePath, boolean shouldTrim = true, String charset = 'UTF-8' ) {
    new File( filePath ).withReader(charset) { r ->
        new CsvParser().parse( r ).with { csv ->
            return csv.collect { line ->
                line.columns.collectEntries { c, v ->
                    [ c, (shouldTrim ? line[ c ].trim() : line[ c ]) ]
                }
            }
        }
    }
}
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

TimeZone.setDefault(TimeZone.getTimeZone('UTC'))
def now = new Date()

fileName = '/home/zjwar/fair/Data/BradleyMeltingPointDataset.csv'
id_field = 'smiles'
l = csv2List(fileName)
File file = new File("QuickStatements.txt")

for (i in 0..30000) {
    if( i % 100 == 0){
        println i + " Records Covered"
    }
    if( l[i]['donotuse'] != '' || l[i]['source'] != 'Alfa Aesar') {
        continue
    }
    try {
        smiles = l[i][id_field].trim()
        mol = cdk.fromSMILES(smiles)
        inchiObj = inchi.generate(mol)
        inchiShort = inchiObj.value[6..-1]
        key = inchiObj.key
        formula = upgradeChemFormula(cdk.molecularFormula(mol))
        undefinedCenters = cdk.getAtomsWithUndefinedStereo(mol)
        mf = cdk.molecularFormulaObject(
            cdk.fromSMILES(smiles)
        )
        fullChiralityIsDefined = undefinedCenters.size() == 0
    }
    catch (ArrayIndexOutOfBoundsException exception) {
        println 'Index Error at line' + i + '...'
    }
    // query Wikidata for the detected InChIKeys
    // InChIKey should be unique for each compound
    sparql = """
        PREFIX wd: <http://www.wikidata.org/entity/>
        PREFIX wdt: <http://www.wikidata.org/prop/direct/>
        SELECT
            DISTINCT ?cmp ?inchikey ?mp
        WHERE {
            VALUES ?inchikey {  \"${key}\" }
            ?cmp wdt:P235 ?inchikey
            OPTIONAL { ?cmp wdt:P2101 ?mp }
        }
    """
    sleep(2000) // in milli-seconds
    results = rdf.sparqlRemote(
        'https://query.wikidata.org/sparql', sparql
    )

    // Can't find it in WikiData, creating quick statement
    if (results.rowCount == 0) {
        try{
            pubchem_isomeric_smiles = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/property/IsomericSMILES/TXT").trim()
            pubchem_canonical_smiles = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/property/canonicalSMILES/TXT").trim()
            pubchem_inChI = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/property/InChI/TXT").trim()
            pubchem_cid = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/cids/TXT").trim()
            pubchem_compound = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/Property/MolecularFormula/TXT").trim()
            pubchem_name = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/Property/IUPACName/TXT").trim()
            pubchem_date = now.format("+yyyy-MM-dd'T'") + "00:00:00Z/11"

            if (mf.getCharge() > 0) {
                println "Compound with Incorrect Charge:"
                println l[i]['name']
                println key
                println ""
                println "Full Chirality is Defined: " + fullChiralityIsDefined
                println "Undefined centers: " + undefinedCenters + " if greater than 0, consider not adding"
                println "Charge of Molecule: " + mf.getCharge()
                println ""
                println "InChI Comparison: "
                println "Generated: " + inchiObj.value
                println "Pubchem:   " + pubchem_inChI
                println ""
                println "Smiles Comparison: "
                println "Data:              " + l[i]['smiles'].toUpperCase()
                println "Pubchem Canonical: " + pubchem_canonical_smiles
                println "Pubchem Isomeric:  " + pubchem_isomeric_smiles
            }

            quickStatement = "CREATE\r\n"
            quickStatement += "LAST\tLen\t\"${pubchem_name}\"\r\n"
            quickStatement += "LAST\tP31\tQ11173\r\n"
            quickStatement += "LAST\tP233\t\"${pubchem_canonical_smiles}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}\r\n"
            quickStatement += "LAST\tP234\t\"${pubchem_inChI}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}\r\n"
            quickStatement += "LAST\tP235\t\"${key}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}\r\n"
            quickStatement += "LAST\tP274\t\"${upgradeChemFormula(pubchem_compound)}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}\r\n"
            quickStatement += "LAST\tP662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}\r\n"
            quickStatement += "LAST\tP2017\t\"${pubchem_isomeric_smiles}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}\r\n"
            quickStatement += "LAST\tP2101\t${l[i]['mpC'].trim()}U25267\tS248\tQ69644056\r\n\r\n"
            file.append(quickStatement)
        }
        catch(Exception ex){
            println "Row Number " + i + " with Error: " + ex
        }
    }
    else{
        if(results.rowCount > 1 || results.get(1, 'mp') != ''){
            println "More than one record with the same InChIKey or existing melting point. Row: " + i
            println results
        }
        else{
            // Compound Exists, Update existing information
            try{
                qid = results.get(1, 'cmp').toString()[3..-1]
                quickStatement = qid+"\tP2101\t${l[i]['mpC'].trim()}U25267\tS248\tQ69644056\r\n"
                file.append(quickStatement)
            }
            catch(Exception ex){
                println "Row Number " + i + " with Error: " + ex
            }
        }
    }
}
