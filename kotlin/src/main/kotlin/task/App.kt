package task
import java.io.InputStream
import java.util.LinkedList
import java.io.File

const val EOF_SYMBOL = -1
const val ERROR_STATE = 0
const val SKIP_VALUE = 0

const val NEWLINE = '\n'.code

interface Automaton {
    val states: Set<Int>
    val alphabet: IntRange
    fun next(state: Int, symbol: Int): Int
    fun value(state: Int): Int
    val startState: Int
    val finalStates: Set<Int>
}

object Example : Automaton {
    override val states = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30)
    override val alphabet = 0 .. 255
    override val startState = 1
    override val finalStates = setOf(2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30)

    private val numberOfStates = states.maxOrNull()!! + 1
    private val numberOfSymbols = alphabet.maxOrNull()!! + 1
    private val transitions = Array(numberOfStates) {IntArray(numberOfSymbols)}
    private val values: Array<Int> = Array(numberOfStates) {0}

    private fun setTransition(from: Int, symbol: Char, to: Int) {
        transitions[from][symbol.code] = to
    }

    private fun setValue(state: Int, terminal: Int) {
        values[state] = terminal
    }

    override fun next(state: Int, symbol: Int): Int =
        if (symbol == EOF_SYMBOL) ERROR_STATE
        else {
            assert(states.contains(state))
            assert(alphabet.contains(symbol))
            transitions[state][symbol]
        }

    override fun value(state: Int): Int {
        assert(states.contains(state))
        return values[state]
    }

    init {
        var i = 48
        var j = 65
        var n = 10
        while(i < 58){ //Števila
            setTransition(1, i.toChar(), 2)
            setTransition(2, i.toChar(), 2)
            setTransition(3, i.toChar(), 4)
            setTransition(4, i.toChar(), 4)
            setTransition(5, i.toChar(), 6)
            setTransition(6, i.toChar(), 6)
            i++
        }
        setTransition(2, '.', 3) // Pika
        while(j < 123){ //Črke
            setTransition(1, j.toChar(), 5)
            setTransition(5, j.toChar(), 5)
            j++
        }
        setTransition(1, '+', 7) //Plus
        setTransition(1, '-', 8) //Minus
        setTransition(1, '*', 9) //Znak za množenje
        setTransition(1, '/', 10) //Znak za deljenje
        setTransition(1, '^', 11) //Znak za potenco
        setTransition(1, '(', 12) //Oklepaj
        setTransition(1, ')', 13) //Zaklepaj
        setTransition(1,' ',14) //Presledek
        setTransition(1, n.toChar(),15) //Nova vrsta
        setTransition(1,':',16) //Dvopičje
        setTransition(16,'=',17) //Je enako
        setTransition(1,'W',18) //W
        setTransition(18,'R',19) //R
        setTransition(19,'I',20) //I
        setTransition(20,'T',21) //T
        setTransition(21,'E',22) //E
        setTransition(1,'d',23) //d
        setTransition(23,'o',24) //o
        setTransition(24,'n',25) //o
        setTransition(25,'e',26) //o
        setTransition(1,'f',27) //f
        setTransition(27,'o',28) //o
        setTransition(28,'r',29) //r
        setTransition(1,';',30) //;

        setValue(2,1) //Float
        setValue(4,1) //Float
        setValue(5,2) //Variable
        setValue(6,2) //Variable
        setValue(7,3) //Plus
        setValue(8,4) //Minus
        setValue(9,5) //Times
        setValue(10,6) //Divide
        setValue(11,7) //Power
        setValue(12,8) //Lparen
        setValue(13,9) //Rparen
        setValue(17,10) //:=
        setValue(22,11) //WRITE
        setValue(24,12) //do
        setValue(26,13) //done
        setValue(19,14) //for
        setValue(30,15) //;
    }
}

data class Token(val value: Int, val lexeme: String, val startRow: Int, val startColumn: Int)

class Scanner(private val automaton: Automaton, private val stream: InputStream) {
    private var state = automaton.startState
    private var last: Int? = null
    private var buffer = LinkedList<Byte>()
    private var row = 1
    private var column = 1

    private fun updatePosition(symbol: Int) {
        if (symbol == NEWLINE) {
            row += 1
            column = 1
        } else {
            column += 1
        }
    }

    private fun getValue(): Int {
        var symbol = last ?: stream.read()
        state = automaton.startState

        while (true) {
            updatePosition(symbol)

            val nextState = automaton.next(state, symbol)
            if (nextState == ERROR_STATE) {
                if (automaton.finalStates.contains(state)) {
                    last = symbol
                    return automaton.value(state)
                } else throw Error("Invalid pattern at ${row}:${column}")
            }
            state = nextState
            buffer.add(symbol.toByte())
            symbol = stream.read()
        }
    }

    fun eof(): Boolean =
        last == EOF_SYMBOL

    fun getToken(): Token? {
        if (eof()) return null

        val startRow = row
        val startColumn = column
        buffer.clear()

        val value = getValue()
        return if (value == SKIP_VALUE)
            getToken()
        else
            Token(value, String(buffer.toByteArray()), startRow, startColumn)
    }
}

fun name(value: Int) =
    when (value) {
        1 -> "float"
        2 -> "variable"
        3 -> "plus"
        4 -> "minus"
        5 -> "times"
        6 -> "divide"
        7 -> "pow"
        8 -> "lparen"
        9 -> "rparen"
        10 -> "assign"
        11 -> "write"
        12 -> "do"
        13 -> "done"
        14 -> "for"
        15 -> "semi"
        else -> throw Error("Invalid value")
    }

fun printTokens(scanner: Scanner) {
    val token = scanner.getToken()
    if (token != null) {
        print("${name(token.value)}(\"${token.lexeme}\") ")
        printTokens(scanner)
    }
}

fun main(args: Array<String>) {
    val inputStream: InputStream = File(args[0]).inputStream()
    val inputString = inputStream.bufferedReader().use { it.readText() }
    val scanner = Scanner(Example, inputString.byteInputStream())
    printTokens(scanner)
}