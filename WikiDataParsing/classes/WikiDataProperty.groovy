package classes

/**@
* Define common wikiData properties for ease of reuse and consistent naming
*/
class WikiDataProperty {

    String instanceOf = 'P31'
    String casRegistryNumber = 'P231'
    String canonicalSMILES = 'P233'
    String inChI = 'P234'
    String inChIKey = 'P235'
    String chemicalFormula = 'P274'
    String chEmblID = 'P592'
    String chemSpiderID = 'P661'
    String pubChemCID = 'P662'
    String keggID = 'P665'
    String chEbiID = 'P683'
    String retrieved = 'P813'
    String isomericSMILES = 'P2017'
    String humanMetabolomeDB = 'P2057'
    String lipidMapsID = 'P2063'
    String knapsackID = 'P2064'
    String meltingPoint = 'P2101'
    String dipoleMoment = 'P2201'
    String dssToxSubtanceID = 'P3117'
    String pdbLigandID = 'P3636'


    def meltingPointFields = [
        'mpC'
    ]
    List getPropertyMapping(){

        l = []
        // meltingPointFields.each{ l.add([this.meltingPoint: it]) }
        ('c'..'a').each {
            println "Letter ${it}"
        }

        return l
    }
}
