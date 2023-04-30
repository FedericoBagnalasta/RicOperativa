package it.unibs.ricoperativa;

import java.io.*;
import gurobi.*;
import gurobi.GRB.DoubleAttr;
import gurobi.GRB.StringAttr;

public class Main {

	private static int m, n, mat[][], r_i[], s_j[], k, alfa_j[], h, x, y, variabiliBase;
	private static double c, valObj;
	
	public static void main(String[] args) throws Exception{
		readFile();

		
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);
		GRBVar[][] xij = aggiungiVariabili(model, s_j, r_i);
		aggiungiFunzioneObiettivo(model, xij, mat);
		aggiungiVincoliMagazzino(model, xij, s_j);
		aggiungiVincoliDomanda(model, xij, r_i);
		
		risolvi(model);
		//Stampa valore della funzione obbiettivo
		System.out.println(model.get(GRB.DoubleAttr.ObjVal));
		stampaSoluzione(model);
	}
	
	/*
	 * Metodo per stampare i risultati del problema
	 */
	private static void stampaSoluzione(GRBModel model) throws GRBException {
		System.out.println("GRUPPO 11");
		System.out.println("Componenti: Bagnalasta Federico, Kovaci Matteo");
		
		System.out.println("\nQUESITO I:");
		System.out.println("funzione obbiettivo = " + model.get(GRB.DoubleAttr.ObjVal));
		
		int variabiliNonAzzerate = 0;
		int ccrAzzerati = 0;
		for(GRBVar var : model.getVars()) {
			System.out.println(var.get(StringAttr.VarName) + ": "+ var.get(DoubleAttr.X));
			
			if((var.get(DoubleAttr.X) != 0)) {
				variabiliNonAzzerate++;
			}
			
			if((var.get(DoubleAttr.RC)) == 0) {
				ccrAzzerati++;
			}
		}
	//==========================================================================
		
		//PROVE
		//DA CANCELLARE
		
		for(GRBVar var : model.getVars()) {
			
			//if(var.get(DoubleAttr.RC) == 0)
			System.out.println(var.get(StringAttr.VarName) + ": "+ var.get(DoubleAttr.RC));
		}
		System.out.println(variabiliNonAzzerate);
		System.out.println(ccrAzzerati);    //DOVREBBERO ESSERE ALMENO 86
		
		//==========================================================================
		variabiliBase = m + n;
		if(variabiliNonAzzerate == variabiliBase) {
			System.out.println("Degenere: no");
		}
		//Soluzione degenere se ci sono delle variabili in base con valore nullo
		else {
			System.out.println("Degenere: sì");
		}
		if(ccrAzzerati == variabiliBase) {
			System.out.println("Multipla: no");
		}
		//Soluzione multipla se almeno una soluzione fuori base ha ccr nullo
		else {
			System.out.println("Multipla: sì");
		}
		
		
		
		
		System.out.println("\nQUESITO II:");
	
		/*
		System.out.println("funzione obbiettivo = " + model.get(GRB.DoubleAttr.ObjVal));
		for(GRBVar var : model.getVars()) {
			System.out.println(var.get(StringAttr.VarName)+ ": "+ var.get(DoubleAttr.X));
		}
		//Degenere e multipla?
		System.out.println("");
		System.out.println();
		
		System.out.println("\nQUESITO II:");
		*/
		
	}

	//Aggiunta variabili
	private static GRBVar[][] aggiungiVariabili(GRBModel model, int[] magazzino, int[] domanda) throws GRBException {
		GRBVar[][] xij = new GRBVar[magazzino.length][domanda.length];
		
		for(int i = 0; i < magazzino.length; i++) {
			for(int j = 0; j < domanda.length; j++) {
				xij[i][j] = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "xij_" + i + "_" + j);
			}
		}
		return xij;
	}
	
	//Aggaiunta funzione obiettivo
	private static void aggiungiFunzioneObiettivo(GRBModel model, GRBVar[][] xij, int[][] distanze) throws GRBException {
		GRBLinExpr obj = new GRBLinExpr();
		
		for(int i = 0; i < distanze.length; i++) {
			for(int j = 0; j < distanze[0].length; j++) {
				//MOLTIPLICARE LA DISTANZA PER IL COSTO UNA VOLTA SISTEMATE LE CIFRE DECIMALI ALTRIMENTI VA TUTTO A 0
				obj.addTerm((distanze[i][j]), xij[i][j]);
			}
		}
		model.setObjective(obj);
		model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
	}
	
	//Aggiunta vincoli di magazzino
	private static void aggiungiVincoliMagazzino(GRBModel model, GRBVar[][] xij, int[] magazzino) throws GRBException {
		for(int i = 0; i < magazzino.length; i++) {
			GRBLinExpr expr = new GRBLinExpr();
			
			for(int j = 0; j < xij[0].length; j++) {
				expr.addTerm(1, xij[i][j]);
			}
			model.addConstr(expr, GRB.LESS_EQUAL, magazzino[i], "vincolo_produzione_i_"+i);
		}
	}
	
	//Aggiunta vincoli di domanda
	private static void aggiungiVincoliDomanda(GRBModel model, GRBVar[][] xij, int[] domanda) throws GRBException {
		for(int j = 0; j < domanda.length; j++) {
			GRBLinExpr expr = new GRBLinExpr();
			
			for(int i = 0; i < xij.length; i++) {
				expr.addTerm(1, xij[i][j]);
			}
			model.addConstr(expr, GRB.GREATER_EQUAL, domanda[j], "vincolo_domanda_j_"+j);
		}
	}
	
	private static void risolvi(GRBModel model) throws GRBException {
		model.optimize();
		
		int status = model.get(GRB.IntAttr.Status);
		
		System.out.println("\n\n\nStato Ottimizzazione: "+ status); 
		// 2 soluzione ottima trovata
		// 3 non esiste soluzione ammissibile (infeasible)
		// 5 soluzione illimitata
		// 9 tempo limite raggiunto
		
		/*for(GRBVar var : model.getVars()) {
			System.out.println(var.get(StringAttr.VarName)+ ": "+ var.get(DoubleAttr.X));
		}*/
	}
	
	/**
	 * Metodo per impostare i parametri del problema
	 * @param env
	 * @throws GRBException
	 */
	private static void impostaParametri(GRBEnv env) throws GRBException 
	{
		//Metodo del simplesso duale
		env.set(GRB.IntParam.Method, 0);
		env.set(GRB.IntParam.Presolve, 0);
		env.set(GRB.DoubleParam.TimeLimit, 600);

	}
	
	/*
	 * Metodo per la lettura del file che contiene i dati del problema
	 */
	private static void readFile() throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader("coppia_11.txt"));
		
		String[] stArray;
		String st = reader.readLine();
		
		while(st!= null) {
			stArray = st.split(" ");
			
			//Switch con cui viene determinata la variabile dove salvare i dati del file
			switch (stArray[0]) {
			
				//Lettura numero di magazzini (RIGHE)
				case "m":
					m = Integer.parseInt(stArray[1]);
					System.out.println(m);
					break;
					
				//Lettura numero di clienti(COLONNE)
				case "n":
					n = Integer.parseInt(stArray[1]);
					System.out.println(n);
					break;
					
				//Lettura matrice
				case "matrice":
					mat = new int[m][n];
					for(int i = 0; i < m; i++) {
						st = reader.readLine();
						stArray = st.split(" ");
						for(int l = 0; l < n; l++) {
							mat[i][l] = Integer.parseInt(stArray[l]);
							System.out.print(mat[i][l]);
						}
					}

					break;
					
				//Lettura richieste dei clienti	
				case "r_i":
					r_i = new int[n];
					System.out.println();
					st = reader.readLine();
					stArray = st.split(" ");
					for(int i = 0; i < n; i++) {
						r_i[i] = Integer.parseInt(stArray[i]);
						System.out.print(r_i[i]);
					}
					break;
					
				//Lettura della capacità dei magazzini 	
				case "s_j":
					s_j = new int[m];
					System.out.println();
					st = reader.readLine();
					stArray = st.split(" ");
					for(int i = 0; i < m; i++) {	
						s_j[i] = Integer.parseInt(stArray[i]);
						System.out.print(s_j[i]);
					}
					break;	
					
				//Lettura costo in euro per trasportare 1kg di merce per 1km
				case "c":
					c = Double.parseDouble(stArray[1]);
					System.out.println("\n" + c);
					break;
					
				//Lettura raggio massima distanza
				case "k": 
					k = Integer.parseInt(stArray[1]);
					System.out.print(k);
					break;
					
				//Lettura dei dati relativi alla chiusura di un magazzino
				case "alfa_j":
					alfa_j = new int[m];
					System.out.println();
					st = reader.readLine();
					stArray = st.split(" ");
					for(int i = 0; i < m; i++) {	
						alfa_j[i] = Integer.parseInt(stArray[i]);
						System.out.print(alfa_j[i]);
					}
					break;
				
				//Lettura pedice del parametro s
				case "h":
					h = Integer.parseInt(stArray[1]);
					System.out.println();
					System.out.println(h);
					break;
				
				//Lettura pedici del parametro d
				case "x":
					x = Integer.parseInt(stArray[2]);
					y = Integer.parseInt(stArray[3]);
					System.out.println(x);
					System.out.println(y);
					break;
			}
			//Lettura della linea successiva
			st = reader.readLine();
		}
		reader.close();
	}
}