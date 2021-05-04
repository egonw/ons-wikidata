// import classes.ChemicalFormula
// import classes.WikiDataConnection
import classes.Compound
import classes.WikiDataProperty

@Grab(group='io.github.egonw.bacting', module='managers-excel', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-pubchem', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.12')

@Grab('com.xlson.groovycsv:groovycsv:1.0')
import com.xlson.groovycsv.CsvParser

workspaceRoot = '..'
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

// fileName = '/home/zjwar/fair/Data/dipole_moments_10071mols.csv'
// id_field = 'Molecule_ID'
// Molecule_ID  Dipole_Moment(D)

cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);

fileName = '/home/zjwar/fair/Data/BradleyMeltingPointDataset.csv'
id_field = 'smiles'

l = csv2List(fileName)
for (i in 900..1000) {
    if( i%100 == 0){
        println i + " Records Covered"
    }
    if (
        l[i]['donotuse'] != ''
        || l[i]['source'] != 'Alfa Aesar'
        // || l[i]['source'] != 'Sigma-Aldrich'
        // || l[i]['source'] != 'UsefulChem'
        // || l[i]['source'] != 'CRC Handbook'
        // || l[i]['source'] != 'CRC Handbook of Chemistry and Physics'
    ) {
        continue // Hard coded, not good -> add to WDP?
    }
    // Add in logic here to test multiple WDP
    try {
        smiles = l[i][id_field].trim()
        // compound = new Compound(workspaceRoot, null, smiles)
        // println compound.smiles
        // compound.updateInChIDetailsFromSmiles()
        mol = cdk.fromSMILES(smiles)
        inchiObj = inchi.generate(mol)
        inchiShort = inchiObj.value[6..-1]
        key = inchiObj.key
        formula = upgradeChemFormula(cdk.molecularFormula(mol))

        undefinedCenters = cdk.getAtomsWithUndefinedStereo(mol)
        fullChiralityIsDefined = undefinedCenters.size() == 0
        // ignoreBecauseStereoMissing =  options.s && !fullChiralityIsDefined
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
        pubchem_isomeric_smiles = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/property/IsomericSMILES/TXT").trim()
        pubchem_canonical_smiles = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/property/canonicalSMILES/TXT").trim()
        pubchem_inChI = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/property/InChI/TXT").trim()
        pubchem_cid = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/cids/TXT").trim()
        pubchem_compound = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${key}/Property/MolecularFormula/TXT").trim()
        pubchem_date = now.format("+yyyy-MM-dd'T'") + "00:00:00Z/11"
        println ""
        println ""
        println "TO BE ADDED:"
        println ""
        println l[i]['name']
        println key
        println ""
        println "Full Chirality is Defined: " + fullChiralityIsDefined
        println "Undefined centers: " + undefinedCenters + " if greater than 0, consider not adding"
        println ""
        println "InChI Comparison: "
        println "Generated: " + inchiObj.value
        println "Pubchem:   " + pubchem_inChI
        println ""
        println "Smiles Comparison: "
        println "Data:              " + l[i]['smiles'].toUpperCase()
        println "Pubchem Canonical: " + pubchem_canonical_smiles
        println "Pubchem Isomeric:  " + pubchem_isomeric_smiles
        println """
Quick Statement (Pubchem Values used when available):
${l[i]['name']}
${key}
\tP233\t\"${pubchem_canonical_smiles}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}
\tP234\t\"${pubchem_inChI}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}
\tP235\t\"${key}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}
\tP274\t\"${upgradeChemFormula(pubchem_compound)}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}
\tP662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}
\tP2017\t\"${pubchem_isomeric_smiles}\"\tS662\t\"${pubchem_cid}\"\tS813\t${pubchem_date}
\tP2101\t${l[i]['mpC'].trim()}U25267\tS248\tQ69644056
        """
    }
    else{
        // create the QuickStatements to add only the melting point
        for (j in 1..results.rowCount) {
            if (results.get(j, 'mp') != "") {
                mp = results.get(j, 'mp')
                cmp = results.get(j, 'cmp')
                // println "Melting Point Already Defined for ${cmp}, Melting Point: ${mp}" 
                // continue
            }
            inchikey = results.get(j, 'inchikey')
            cmp = results.get(j, 'cmp')
            melting_point = l[i]['mpC']
            // println "\t${cmp}\tP2101\t${melting_point}U25267\tS248\tQ69644056"
        }
    }
}
