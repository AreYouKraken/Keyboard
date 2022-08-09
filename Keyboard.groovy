// Created by Kraken at the Wall of Sheep for Defcon 2022
// This will work for the Password Lab Walk Through Workshop
// More work to be done, including multithreading!!

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

import groovy.io.FileType

class Keyboard {

	static main(args) {

		if(args.size() == 4) {
			// TO DO: This section will split the keyboard patterns parts of a string with the non-pattern parts. When it eventually works it will be useful.
			String wordListDir = args[0]
			int minPasswordLength = args[1].toInteger()
			double ratio = args[2].toDouble()
			String outputFile = args[3]

			findAdjacentWordsWithParts(wordListDir, minPasswordLength, ratio, outputFile)

		} else if(args.size() == 3){
			// This is the only section fully working right now.  Will work flawlessly with the lab.
			String wordListDir = args[0]
			int minPasswordLength = args[1].toInteger()
			String outputFile = args[2]
			findAdjacentWords(wordListDir, minPasswordLength, outputFile)


		} else if(args.size() == 2){
			// TO DO: Add support for displaying the Markov stats for a given wordlist.
		} else {

//			println "To filter a password list with a custom adjacencyRatio of 0.8: "
//
//			println "groovy Keyboard.groovy \"C:\\defcon\\password_lab_wtw\\wordlists\" 15 \"C:\\defcon\\password_lab_wtw\\patternPasswords.txt\" 0.8"

			println "\nTo filter a password list with the default adjacencyRatio 0.99)"

			println "groovy Keyboard.groovy <input File or Directory> <min password length> <output file location>"
			println "\nEx:"

			println "groovy /home/kali/password_lab_wtw/Keyboard.groovy /usr/home/wordlists/rockyou.txt 15 /home/kali/password_lab_wtw/patternPasswords.txt"

			println "The input file can be a folder to process many files at once or a single file."

//			println "\nTo generate markov stats for any given list:"
		}
	}

		def keys = [/`1234567890-=  /,
					/ qwertyuiop[]\ /,
					/ asdfghjkl;'   /,
					/ zxcvbnm,.\/    /]

	def shiftKeys = [/~!@#$%^&*()_+  /,
					 / QWERTYUIOP{}| /,
					 / ASDFGHJKL:"   /,
					 / ZXCVBNM<>?    /]


	def getOutlineKeys(){
		//		/~!@#$%^&*()_+/ + /ZXCVBNM<>?/ + /`1234567890-=/ + "zxcvbnm,./" + /QAqa']"|/
		/1!zZqQaA/
	}


	boolean[][] adjacencyMatrix = new boolean[94][94]
	def adjacencyList

	final static int numberOfRows = 3
	final static int numberOfColumns = 14
	final static double globalRatio = 0.99

	BigInteger total = 0

	Keyboard(){
		contructAdjacencyMatrix()
		String.metaClass.toAsciiInt = {
			->
			((int)((char)delegate))
		}

		String.metaClass.getHead = {
			->
			delegate.toList().head()
		}

		String.metaClass.getTail = {
			->
			delegate.toList().tail()
		}

		String.metaClass.getFirst = {
			->
			delegate.toList().first()
		}

		String.metaClass.getLast = {
			->
			delegate.toList().last()
		}

		List.metaClass.minus = {String string->
			delegate.minus(string.toList())
		}

		this.adjacencyList = contructAdjacencyList()
	}

	//  The adjacencyList is for convenience
	def contructAdjacencyList(){

		def adjacencyList = [:]
		for(row in 0..numberOfRows){

			for(column in 0..numberOfColumns-1){
				def currentList = []

				//the key we're on
				currentList << keys[row][column]
				currentList << shiftKeys[row][column]
				// to the left of the key we're on
				if(row-1>=0){
					currentList << keys[row-1][column]
					currentList << shiftKeys[row-1][column]
				}
				// to the right of the key we're on
				if(row+1<=numberOfRows){
					currentList << keys[row+1][column]
					currentList << shiftKeys[row+1][column]
				}

				// above the key we're on
				currentList << keys[row][(column+1)%numberOfColumns]
				currentList << shiftKeys[row][(column+1)%numberOfColumns]

				// below the key we're on
				currentList << keys[row][(column-1)%numberOfColumns]
				currentList << shiftKeys[row][(column-1)%numberOfColumns]

				String currentKey = keys[row][column]
				String currentShiftKey = shiftKeys[row][column]

				adjacencyList[currentKey] = currentList.findAll{it != ' ' && it!= currentKey }
				adjacencyList[currentShiftKey] = currentList.findAll{it != ' ' && it != currentShiftKey}
			}
		}
		adjacencyList
	}

	//  The adjacencyMatrix runs about 24 times faster than the adjacencyList, so is used more often.
	def contructAdjacencyMatrix(){
		for(row in 0..numberOfRows){

			for(column in 0..numberOfColumns-1){
				def currentList = []

				//the key we're on
				currentList << keys[row][column]
				currentList << shiftKeys[row][column]
				// to the left of the key we're on
				if(row-1>=0){
					currentList << keys[row-1][column]
					currentList << shiftKeys[row-1][column]
				}
				// to the right of the key we're on
				if(row+1<=numberOfRows){
					currentList << keys[row+1][column]
					currentList << shiftKeys[row+1][column]
				}

				// above the key we're on
				currentList << keys[row][(column+1)%numberOfColumns]
				currentList << shiftKeys[row][(column+1)%numberOfColumns]

				// below the key we're on
				currentList << keys[row][(column-1)%numberOfColumns]
				currentList << shiftKeys[row][(column-1)%numberOfColumns]

				int currentKey = ((int)keys[row][column])-33
				int currentShiftKey = ((int)shiftKeys[row][column])-33

				currentList.each{String nextKey->
					int nextKeyInt = ((int)nextKey)-33
					adjacencyMatrix[currentKey][nextKeyInt] = true
					adjacencyMatrix[currentShiftKey][nextKeyInt] = true
				}
			}
		}

	}

	def getAdjacentKeysFor(String string){
		if(string){
			adjacencyList[string.last]?.minus(string)
		} else {
			[]
		}
	}

	def getAdjacentListFor(String string){
		adjacencyList[string.last]
	}

	boolean isAdjacentList(String letter, String nextLetter){
		if(letter.last == nextLetter.last){
			true
		} else {
			getAdjacentKeysFor(letter)?.contains(nextLetter)
		}
	}

	boolean isAdjacent(String letter, String nextLetter){
		// Limit the keyspace to help with garbage data
		if((int)letter >= 33 && (int)letter <= 126 && (int)nextLetter >= 33 && (int)nextLetter <= 126){
			adjacencyMatrix[((int)letter)-33][((int)nextLetter)-33]
		} else {
			false
		}
	}

	def adjacencyRatioOf(String string){
		if(string.length() > 1 && string){
			int adjacencyHits = 0

			for(int i=0;i<string.length()-1;i++){
				if(isAdjacent(string[i], string[i+1])){
					adjacencyHits++
				}
			}
			adjacencyHits/(string.length()-1)
		} else{
			0
		}

	}

	//This will filter out passwords with repeating characters that are sort of useless.
	// It may miss some good passwords too, but helpful for some smart filtering.
	def adjacencyRatioOfUnique(String string){
		if(string.length() > 1 && string){
			int adjacencyHits = 0

			for(int i=0;i<string.length()-1;i++){
				
				if( !(string[i] ==  string[i+1]) && isAdjacent(string[i], string[i+1])){
					adjacencyHits++
				}
			}
			adjacencyHits/(string.length()-1)
		} else{
			0
		}

	}

	// Experimental feature that is only partly working.
	def getAdjacencyRatioAndParts(String string){
		def isAdjacentHops = [] as List<Boolean>
		double ratio = 0
		if(string.length() > 1 && string){

			int adjacencyHits = 0

			int adjacency = 0

			def currentRun = [start:0, end:0]
			int currentHits

			for(int i=0;i<string.length()-1;i++){


				if ( !(string[i] ==  string[i+1]) ){
					if (isAdjacent(string[i], string[i+1])){
						adjacencyHits++
						isAdjacentHops << true
					} else {
						isAdjacentHops << false
					}
				}


			}
			ratio = adjacencyHits/(string.length()-1)
		} else{
			ratio = 0
		}


		def adjacentRuns = []
		def notAdjacentRuns = []
		int currentRunStart = 0
		boolean inAdjacentRun = false

		def adjacentParts = []
		def notAdjacentParts = []

		isAdjacentHops.size().times{int i->
			if(i==0){
				inAdjacentRun = isAdjacentHops[0]
			}
			if(i>0){
				if(isAdjacentHops[i-1] == isAdjacentHops[i]){
					// do nothing.  if this never changes, the word was 100% adjacent and will be added at the end
					currentRunStart = i + 1
				} else {

					if(isAdjacentHops[i] == true && ((i-currentRunStart) >=4 )){
//						adjacentRuns << [start:currentRunStart, end:i]
						adjacentParts <<  string[currentRunStart..i]
						currentRunStart = i + 1
					} else if(isAdjacentHops[i] == false && ((i-currentRunStart) >=4 )){
//						println "DO WE EVER ADD ANY NOT ADJACENT WORDS BRO?"
//						notAdjacentRuns << [start:currentRunStart, end:i]
//						println "----"
						notAdjacentParts <<  string[currentRunStart..i]
//						println "hop part" + string[currentRunStart..i]

//                      WHY CAN I PRINT THIS WORD BUT NOT ADD IT TO AN ARRAY
//						println "whole word: " + string
						currentRunStart = i + 1
					}
				}
			}
		}

		//it was all adjacent, so just add the entire word
		if(currentRunStart == 0){
			adjacentRuns << string
		}

//		if(ratio>= 0.5) {
//			println "--------"
//			println "ratio: " + ratio
//			println "adjacentParts: $adjacentParts"
//			println "hop parts: " + notAdjacentParts
//		}
		[ratio:ratio, adjacentParts:adjacentParts, notAdjacentParts:notAdjacentParts]

	}

	def getListFromXWithLength(String result, int length, Closure closure){
		if(length>0){
			def nextKeys = getAdjacentKeysFor(result)
			nextKeys.each{
				getListFromXWithLength(result+it, length-1, closure)
			}
		} else {
			total++
			println "$result - $total"
			closure.call(result)

		}
	}

	static void findAdjacentWords(String wordListDir, int minPasswordLength, String outputFile){
		def patternPasswords = [] as Set
		eachAdjacentWord(wordListDir,minPasswordLength){String line->

			patternPasswords << line
		}

		new File(outputFile).withWriter {  writer ->
			patternPasswords.sort { p1, p2 -> (p1.length() <=> p2.length()) * -1 }.each{String patternPassword->
				writer.writeLine patternPassword
			}
		}
	}

	static void findAdjacentWordsWithParts(String wordListDir, int minPasswordLength, double ratio, String outputFile) {
		def adjacentWordList = [] as Set
		def hopWordList = [] as Set
//		double


		eachWordWithParts(wordListDir, minPasswordLength, ratio) { List<String> adjacentWords, List<String> notAdjacentWords ->

			adjacentWords.each { adjacentWordList << it }
			notAdjacentWords.each { hopWordList << it }

			new File(outputFile + "adjacent.txt").withWriter { writer ->
				adjacentWordList.sort { p1, p2 -> (p1.length() <=> p2.length()) * -1 }.each { String patternPassword ->
					writer.writeLine patternPassword
				}
			}

			new File(outputFile + "hops.txt").withWriter { writer ->
				hopWordList.sort { p1, p2 -> (p1.length() <=> p2.length()) * -1 }.each { String patternPassword ->
					writer.writeLine patternPassword
				}
			}
		}
	}

	static void eachAdjacentWord(String wordListDir, int minPasswordLength, Closure closure){
		Keyboard keyboard = new Keyboard()

		File inputFileOrDir = new File(wordListDir)
		if(inputFileOrDir.isDirectory()) {
			def files = []

			new File(wordListDir).eachFileRecurse(FileType.FILES) { File file ->
				files << file
			}

			println "${files.size} total dictionaries"
			int i = 0

			files.sort { f1, f2 -> (getFileSize(f1) <=> getFileSize(f2)) * -1 }.each { File file ->
				println "starting $file.name #${i++}"

				file.eachLine { String line ->


					if (line.size() >= minPasswordLength) {
						def ratio = keyboard.adjacencyRatioOfUnique(line)
						if (ratio >= globalRatio) {

//
							closure.call(line)


						}
					}
				}
			}
		} else if(inputFileOrDir.isFile()){
			inputFileOrDir.eachLine { String line ->


				if (line.size() >= minPasswordLength) {
					def ratio = keyboard.adjacencyRatioOfUnique(line)
					if (ratio >= globalRatio) {

//
						closure.call(line)


					}
				}
			}
		}


		}


	static void eachWordWithParts(String wordListDir, int minPasswordLength, double ratio, Closure closure){
		Keyboard keyboard = new Keyboard()

		def files = []

		new File(wordListDir).eachFileRecurse(FileType.FILES){File file->
			files << file
		}

		println "${files.size} total dictionaries"
		int i = 0

		files.sort{f1,f2->(getFileSize(f1) <=> getFileSize(f2))*-1}.each{File file->
			println "starting $file.name #${i++}"

			file.eachLine{String line->


				if(line.size() >= minPasswordLength){
					def result = keyboard.getAdjacencyRatioAndParts(line)
					if(result.ratio >= ratio){

//
						closure.call(result.adjacentParts, result.notAdjacentParts)


					}
				}
			}
		}
	}



	static def getFileSize(File file){
		BasicFileAttributes attributes = Files.readAttributes(FileSystems.getDefault().getPath(file.getParent(), file.name), BasicFileAttributes.class)
		attributes.size()
	}

	// If the next key isn't adjacent, what is it most likely to jump to?  Helpful in deconstructing passwords
	// that use a keyboard pattern for only part of the password.
	static void markovStats(String fileToAnalyze, String outputFile){
		Keyboard keyboard = new Keyboard()
		def keyboardMarkovStats = [:]
		new File(fileToAnalyze).eachLine{String line->
			
			
			if(line?.length() > 2){

				(line.length()-1).times{int i->
					if(!isAllSameKey(line)){
					String letter = line[i]
					String nextLetter = line[i+1]
					boolean isAdjacent = keyboard.isAdjacent(letter, nextLetter)

					if(!keyboardMarkovStats.containsKey(letter)){
						keyboardMarkovStats[letter] = [:]
					}


					if(isAdjacent){
						if(!keyboardMarkovStats[letter].containsKey(nextLetter)){
							keyboardMarkovStats[letter][nextLetter] = 0
						}
						keyboardMarkovStats[letter][nextLetter]++
					} else {
						if(!keyboardMarkovStats[letter].containsKey(">" + nextLetter)){
							keyboardMarkovStats[letter][">" + nextLetter] = 0
						}
						keyboardMarkovStats[letter][">" + nextLetter]++
					}
				}
				}

			}
		}

		new File(outputFile).withWriter{writer->
		keyboardMarkovStats.sort{k1,k2->k1.key.compareTo(k2.key)}.each{letter,nextLetters->
			int total = nextLetters.collect{it.value}.sum()
			writer.writeLine "-----------------"
			nextLetters.sort{k1,k2->(k1.value <=> k2.value)*-1}.each{nextLetter,nextLetterTotal->
				
				writer.writeLine "$letter-$nextLetter $nextLetterTotal (${nextLetterTotal/total})"
			}

		}
		}
	}
	static boolean isAllSameKey(String string){
				if(string.length() > 1 && string){
					int sameHits = 0
					(string.length()-1).times{int i->
						String letter = string[i]
						String nextLetter = string[i+1]
						if(letter == nextLetter){
							sameHits++
						}
					}
					sameHits == (string.length()-1) ? true : false
				} else{
					false
				}
		
	}
}

