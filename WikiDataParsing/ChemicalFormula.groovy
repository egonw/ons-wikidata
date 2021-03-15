class ChemicalFormula{
    /* 
    * Chemical formula class to handle repeated manipulations to
    * common for chemical formulas
    */


    String formula

    def ChemicalFormula(formula){
        this.formula = formula
    }

    def subscript_map = [
        "0": "₀",
        "1": "₁",
        "2": "₂",
        "3": "₃",
        "4": "₄",
        "5": "₅",
        "6": "₆",
        "7": "₇",
        "8": "₈",
        "9": "₉"
    ]
    /**
    * Update chemical string with map of replacements.
    *
    * @param formula: String the chemical formula to be updated.
    * @param replace_map: Map pairs of strings to replace in the String.
    * 
    * @return String the updated formula
    */
    def modifyChemicalFormula(formula, replace_map) {    
        for (entry in replace_map){
            formula = formula.replace(entry.key, entry.value)
        }
        return formula
    }

    /**
    * Test function for modifyChemicalFormula.
    *
    * @return Boolean: True if all tests pass.
    */
    def testModifyChemicalFormula(){
        def subscript_map = [
            "0": "₀",
            "1": "₁",
            "2": "₂",
            "3": "₃",
            "4": "₄",
            "5": "₅",
            "6": "₆",
            "7": "₇",
            "8": "₈",
            "9": "₉"
        ]
        def x = modifyChemicalFormula('1-2-3-4-5-6-7-8-9', subscript_map)
        assert x == '₁-₂-₃-₄-₅-₆-₇-₈-₉'
        return true
    }
}