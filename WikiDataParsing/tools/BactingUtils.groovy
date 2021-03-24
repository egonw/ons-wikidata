package tools
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-pubchem', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.0.12')

/**@
* Download isometric smiles based on InChIKey from pubchem, include delay to prevent overuse
*
* @param workspaceRoot String: A string for the root area of the workspace
* @param inChIKey String: The InChIKey of the desired compound
* @return List: A list of the isometric smiles for the compound from pubChem
*/
def getIsomeresFromInChI(String workspaceRoot, String inChIKey){
    try {
        // Add pause to prevent too many calls to pubChem
        sleep(150)
        bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
        root_url = 'https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/'
        smiles = bioclipse.download("${root_url}${inChIKey}/property/IsomericSMILES/TXT")
        return smiles.split('\r\n|\r|\n')
    }
    catch (Exception e) {
        println "# ${inChIKey} exception: " + e.message
    }
}

/**@
* TODO: Untested
* Use bacting library to produce inChI manager object from a molecule
*
* @param workspaceRoot String: A string for the root area of the workspace
* @param molecule String: Molecule for a compound
* @return InChIManager Object
*/
def generateInChIObject(String workspaceRoot, String molecule) {
    inchi = new net.bioclipse.managers.InChIManager(workspaceRoot)
    return inchi.generate(molecule)
}

/**@
* TODO: Untested
* Use bacting library to produce an CDKManager Object from a compound's smiles
*
* @param workspaceRoot String: A string for the root area of the workspace
* @param smiles String: Smiles for a compound
* @return CDKManager Object
*/
def getMoleculeFromSMILES(String workspaceRoot, String smiles) {
    cdk = new net.bioclipse.managers.CDKManager(workspaceRoot)
    return cdk.fromSMILES(smiles)
}

/**@
* Use bacting library to produce compound details based on InChIKey 
*
* @param workspaceRoot String: A string for the root area of the workspace
* @param inChIKey String: The inChIKey for a compound
* @return Map: Hash map of key identifying details for the compound
*/
def bactingDetails(String workspaceRoot, String inChIKey) {
    smiles = getIsomeresFromInChI(workspaceRoot, inChIKey)[0]
    molecule = getMoleculeFromSMILES(workspaceRoot, smiles)
    atoms_with_undefined_stereo = cdk.getAtomsWithUndefinedStereo(molecule)

    if (atoms_with_undefined_stereo.size()) {
        println "# Molecule has ${atoms_with_undefined_stereo.size()} undefined stereo atoms: ${smiles}"
        return [
            inChIKey: inChIKey,
            smiles: smiles
        ]
    }
    bioclipse_object = new net.bioclipse.managers.InChIManager(workspaceRoot)
    bio_obj = bioclipse_object.generate(molecule)
    return [
        inChIKey: inChIKey,
        inChIKeyGenerated: bio_obj.key,
        inChI: bio_obj.value,
        inChIShort: bio_obj.value[6..-1],
        smiles: smiles.trim()
    ]
}
