package classes

/**@
* General compound object to store identifying characteristics of a compound, known properties,
* and functions to determine missing identification attributes.
*/
class Compound {

    String qID
    String inChI
    String inChiShort
    String inChIKey
    String inChIKeyGenerated
    String smiles
    String workspaceRoot
    String molecule
    String[] isomericSMILES
    HashMap bactingDetails
    HashMap compoundProperties

    GroovyShell shell = new GroovyShell()
    def tools = shell.parse(new File('tools/BactingUtils.groovy'))

    /**@
    * Create Compound object based on workspace Root (required for bacting) and InChIKey
    *
    * @param workspaceRoot String: A string for the root area of the workspace
    * @param inChIKey String: The known InChIKey for the compound
    * @return Compound Object
    */
    Compound(String workspaceRoot, String inChIKey) {
        this.workspaceRoot = workspaceRoot
        this.inChIKey = inChIKey
    }
    // TODO Create Constructors for different identifying characteristics

    /**@
    * Display all attributes for the compounds for debugging
    */
    void displayDetails() {
        println '\n\n All details of compound: \n'
        println "WorkspaceRoot: ${this.workspaceRoot}"
        println "InChI: ${this.inChI}"
        println "InChIKey: ${this.inChIKey}"
        println 'IsomericSMILES: '
        this.displaySmiles()
        println "Main smiles: ${this.smiles}"
        println "Molecule: ${this.molecule}"
    }

    /**@
    * Check if Smiles notation is disconnected
    *
    * @return boolean: Whether the smiles for the compound is disconnected
    */
    boolean checkSmilesDisconnected() {
        return this.inChI.contains('.')
    }

    /**@
    * Update the compound's smiles isomeres based on the compound inChIKey
    */
    void updateIsomeresFromInChI() {
        this.isomericSMILES = tools.getIsomeresFromInChI(this.workspaceRoot, this.inChIKey)
        if (this.isomericSMILES.length > 1) {
            println 'Multiple isomeric smiles, selecting first instance as compound smiles'
        }
        this.smiles = this.isomericSMILES[0].trim()
    }
    /**@
    * Return the isomeres list for the compound
    * @return String[]: List of isometric smiles for the compound
    */
    String[] smilesIsomeres() {
        return this.isomericSMILES
    }

    /**@
    * Print out all the isometric smiles for the compound
    */
    void displaySmiles() {
        for (smiles in this.isomericSMILES) {
            println smiles
        }
    }

    /**@
    * TODO: Not Tested
    * Update the molecule (Type undefined) for the compound
    */
    void updateMoleculeFromSmiles() {
        mol = tools.getMoleculeFromSMILES(this.workspaceRoot, this.smiles)
        println mol.getClass()
    // this.molecule = mol
    }

    /**@
    * Helper function to get an InChIManager object
    */
    def generateInChIObject() {
        return tools.generateInChIObject(this.workspaceRoot, this.molecule)
    }

    /**@
    * Determine missing details for the compound based on bacting libaries
    *
    * @return Map: Hash map of details for the compound generated from the bacting libaries
    */
    Map bactingDetails() {
        this.bactingDetails = tools.bactingDetails(this.workspaceRoot, this.inChIKey)
        this.inChIKeyGenerated = this.bactingDetails.inChIKeyGenerated
        this.inChI = this.bactingDetails.inChI
        this.inChiShort = this.bactingDetails.inChiShort
        return this.bactingDetails
    }

}
