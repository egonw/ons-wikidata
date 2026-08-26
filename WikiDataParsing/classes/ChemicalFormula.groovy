package classes

/**@
* Helper class to manage common modifications to chemical formulas
*/
class ChemicalFormula {

    String formula
    def subscript_map = [
        '0': '₀',
        '1': '₁',
        '2': '₂',
        '3': '₃',
        '4': '₄',
        '5': '₅',
        '6': '₆',
        '7': '₇',
        '8': '₈',
        '9': '₉'
    ]

    ChemicalFormula(formula) {
        this.formula = formula
    }

    /**@
    * Update chemical string with map of replacements.
    *
    * @param formula String: The chemical formula to be updated.
    * @param replaceMap Map: Pairs of strings to replace in the String.
    *
    * @return String: The updated formula
    */
    String modifyChemicalFormula(String formula, Map replaceMap) {
        for (entry in replaceMap) {
            formula = formula.replace(entry.key, entry.value)
        }
        this.formula = formula
        return formula
    }

    /**
    * Test function for modifyChemicalFormula.
    *
    * @return Boolean: True if all tests pass.
    */
    def testModifyChemicalFormula() {
        def subscript_map = [
            '0': '₀',
            '1': '₁'
        ]
        def x = modifyChemicalFormula('0-1', subscript_map)
        assert x == '₀-₁'
        return true
    }

}
