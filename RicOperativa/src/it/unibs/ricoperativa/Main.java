package it.unibs.ricoperativa;

import java.io.*;
import java.util.ArrayList;

import gurobi.*;
import gurobi.GRB.*;

public class Main {

		private static int m, n, mat[][], r_i[], s_j[], k, alfa_j[], h, x, y;
		private static double costo;

		public static void main(String[] args) throws Exception {
			readFile();

			GRBEnv env = new GRBEnv();
			impostaParametri(env);
			
			GRBModel model = new GRBModel(env);
			GRBVar[][] xij = aggiungiVariabili(model);
			aggiungiFunzioneObiettivo(model, xij);
			aggiungiVincoliMagazzino(model, xij);
			aggiungiVincoliDomanda(model, xij);
			
			risolvi(model);

			stampaSoluzione(model, xij, env);
		}
		
		private static void impostaParametri(GRBEnv env) throws GRBException {
			//Algoritmo impostato: Simplesso primale
			env.set(GRB.IntParam.Method, 0);
		}
		
		//Stampa soluzione
		private static void stampaSoluzione(GRBModel model, GRBVar[][] xij, GRBEnv env) throws Exception {
			System.out.println("GRUPPO 11");
			System.out.println("Componenti: Bagnalasta Federico, Kovacic Matteo");
			
			System.out.println("\nQUESITO I:");
			
			System.out.printf("funzione obiettivo = %.4f\n", model.get(GRB.DoubleAttr.ObjVal));
			
			controllaDegenere(model);
			controllaMultipla(model);
			
			//stampa valori
			/*
			for(GRBVar var : model.getVars()) {
				System.out.println(var.get(StringAttr.VarName) + ": "+ var.get(DoubleAttr.X));
			}
			
			
			//stampa RC
			/*
			for(GRBVar var : model.getVars()) {
				System.out.println(var.get(StringAttr.VarName) + ": "+ var.get(DoubleAttr.RC));
			}
			*/
			
			System.out.println("\nQUESITO II:");
			
			trovaVincoliInattivi(model);
			
			//k va da 0 al minimo valore delle distanze magazzino-clienti
			trovaIntervalloK(model, xij, env);
			
			//RIPORTO XIJ AI VALORI INIZIALI
			xij = aggiungiVariabili(model);
			model.optimize();
			
			ottimoDuale(model);
			
			sensitivitaMagazzino(model);
			
			sensitivitaDistanza(model);
			
			System.out.println("\n\nQUESITO III:");
			
			creaModulo3(env);
			
			//trovaRilassamento(model); E' DA FARE SUL NUOVO MODELLO
		}
		
		//Metodo per aggiungere le variabili
		private static GRBVar[][] aggiungiVariabili(GRBModel model) throws GRBException {
			GRBVar[][] xij = new GRBVar[s_j.length][r_i.length];
			
			for(int i = 0; i < s_j.length; i++) {
				for(int j = 0; j < r_i.length; j++) {
					double upperBound = GRB.INFINITY;
					if(mat[i][j] > k) {
						upperBound = 0;
					}
					xij[i][j] = model.addVar(0, upperBound, 0, GRB.CONTINUOUS, "xij_" + i + "_" + j);
				}
			}
			return xij;
		}
		
		//Metodo per trovare la funzione obiettivo
		private static void aggiungiFunzioneObiettivo(GRBModel model, GRBVar[][] xij) throws GRBException {
			GRBLinExpr obj = new GRBLinExpr();
			
			for(int i = 0; i < mat.length; i++) {
				for(int j = 0; j < mat[0].length; j++) {
					obj.addTerm(costo * mat[i][j], xij[i][j]);
				}
			}
			model.setObjective(obj);
			model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
		}
		
		//Metodo per aggiungere i vincoli di magazzino
		private static void aggiungiVincoliMagazzino(GRBModel model, GRBVar[][] xij) throws GRBException {
			for(int i = 0; i < s_j.length; i++) {
				GRBLinExpr expr = new GRBLinExpr();
				
				for(int j = 0; j < xij[0].length; j++) {
					expr.addTerm(1, xij[i][j]);
				}
				model.addConstr(expr, GRB.LESS_EQUAL, s_j[i], "vincolo_produzione_i_"+i);
			}
		}	
	
	//Metodo per aggiungere vincoli di domanda
	private static void aggiungiVincoliDomanda(GRBModel model, GRBVar[][] xij) throws GRBException {
		for(int j = 0; j < r_i.length; j++) {
			GRBLinExpr expr = new GRBLinExpr();
			
			for(int i = 0; i < xij.length; i++) {
				expr.addTerm(1, xij[i][j]);
			}
			model.addConstr(expr, GRB.GREATER_EQUAL, r_i[j], "vincolo_domanda_j_"+j);
		}
	}
	
	//Metodo per risolvere il problema 
	private static void risolvi(GRBModel model) throws GRBException {
		model.optimize();
	}
	
	//Metodo per leggere il file che contiene i dati del problema
	private static void readFile() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("coppia_11.txt"));
		
		String[] stArray;
		String st = reader.readLine();
		
		while(st!= null) {
			stArray = st.split(" ");
			
			//Switch con cui viene determinata la variabile in cui salvare i dati del file
			switch (stArray[0]) {
			
				//Lettura numero di magazzini (RIGHE)
				case "m":
					m = Integer.parseInt(stArray[1]);
					System.out.println(m);
					break;
					
				//Lettura numero di clienti (COLONNE)
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
						for(int j = 0; j < n; j++) {
							mat[i][j] = Integer.parseInt(stArray[j]);
							System.out.print(mat[i][j]);
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
					
				//Lettura della capacita' dei magazzini 	
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
					costo = Double.parseDouble(stArray[1]);
					System.out.println("\n" + costo);
					break;
					
				//Lettura raggio massima distanza
				case "k": 
					k = Integer.parseInt(stArray[1]);
					System.out.println(k);
					break;
					
				//Lettura dei dati relativi alla chiusura di un magazzino
				case "alfa_j":
					alfa_j = new int[m];
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
					System.out.println("\n" + h);
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
	
	
	
	//===========================================================================================
	
	// QUESITO I
	
	//Metodo per controllare se la soluzione e' degenere
	private static void controllaDegenere(GRBModel model) throws Exception {
		if(model.get(GRB.IntAttr.SolCount) > 0) {
            double[] solution = model.get(GRB.DoubleAttr.X, model.getVars());

            //Verifica se esistono variabili di base con valore 0
            boolean isDegenerate = false;
            for(GRBVar var : model.getVars()) {
                if(var.get(GRB.IntAttr.VBasis) == GRB.BASIC && solution[var.get(GRB.IntAttr.VBasis)] == 0.0) {
                    isDegenerate = true;
                    break;
                }
            }
            if(isDegenerate) {
            	 System.out.println("Degenere: sì");
            }
            else {
            	System.out.println("Degenere: no");
            }
            
        } else {
            System.out.println("Il problema non ha soluzione");
        }
	}
	
	//Metodo per controllare se la soluzione e' multipla
	private static void controllaMultipla(GRBModel model) throws Exception {
		int count = 0;
		for(GRBVar var: model.getVars()) {
			if(var.get(GRB.IntAttr.VBasis) != GRB.BASIC && var.get(GRB.DoubleAttr.RC) == 0) {
				count++;
			}
		}
		if(count != 0)
			System.out.println("Multipla: sì");
		else {
			int count2 = 0;
			for(GRBConstr constr: model.getConstrs()) {
				if(constr.get(GRB.IntAttr.VBasis) != GRB.BASIC && constr.get(DoubleAttr.Pi) == 0) {
					count2++;
				}
			}
			if(count2 != 0)
				System.out.println("Multipla: si");
			else
				System.out.println("Multipla: no");
		}
	}
	
	
	
	//===========================================================================================
	
	// QUESITO II
	
	//Metodo per trovare i vincoli inattivi
	private static void trovaVincoliInattivi(GRBModel model) throws GRBException {
		//Un vincolo è inattivo se l'attributo Slack è > 0
		System.out.print("lista vincoli non attivi =");
		GRBConstr[] constrains = model.getConstrs();
		for(int i = 0; i < constrains.length; i++) {
			double slack = constrains[i].get(GRB.DoubleAttr.Slack);
			if(slack > 0.0) {
				System.out.print(" " + constrains[i].get(GRB.StringAttr.ConstrName));
			}
		}
	}
	//Metodo per torvare l'intervallo di valori che, se assunti da k, rendono il problema impossibile
	private static void trovaIntervalloK(GRBModel model, GRBVar xij[][], GRBEnv env) throws GRBException{
		//Creazione nuovo problema
		GRBModel model2 = new GRBModel(env);
		GRBVar[][] xij2 = aggiungiVariabili(model2);
		aggiungiFunzioneObiettivo(model2, xij2);
		aggiungiVincoliMagazzino(model2, xij2);
		aggiungiVincoliDomanda(model2, xij2);
		int raggio = k - 1;
		while (raggio > 0) {
			for(int i = 0; i < m; i++) {
		 		for(int j = 0; j < n; j++) {
		 			if(mat[i][j] > raggio) {
		 				xij2[i][j].set(GRB.DoubleAttr.UB, 0.0);
		 			}
		 		}
			}
			model2.optimize();
			if(model2.get(GRB.IntAttr.Status) == GRB.Status.UNBOUNDED ||
						model2.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE){
				System.out.printf("intervallo k = (%.4f, %.4f)\n", 0.0, (double)raggio); 
				return;
			}
			raggio--;
		}	
		System.out.printf("intervallo k = (0.0000, %.4f)\n", (double)k);
	}
	
	//Metodo per trovare la soluzione ottima del duale
	private static void ottimoDuale(GRBModel model) throws Exception {
		System.out.println("\nsoluzione duale:");
		int i = 1;
		for(GRBConstr constr : model.getConstrs()) {
			System.out.printf("\nlambda%d = %.4f", i, constr.get(GRB.DoubleAttr.Pi));
			i++;
		}
	}
	
	//Metodo per trovare la sensitività della capacita' del magazzino
	private static void sensitivitaMagazzino(GRBModel model) throws Exception {
		System.out.printf("\nintervallo s_%d = [%.4f, %.4f]\n", h, model.getConstr(h).get(GRB.DoubleAttr.SARHSLow),
				model.getConstr(h).get(GRB.DoubleAttr.SARHSUp));
	}
	
	//Metodo per trovare la sensistivita' della distanza cliente-magazzino
	private static void sensitivitaDistanza(GRBModel model) throws Exception {
		GRBVar var = model.getVarByName("xij_" + y + "_" + x);
		double lowerBound = var.get(GRB.DoubleAttr.SAObjLow) / costo;
		double upperBound = var.get(GRB.DoubleAttr.SAObjUp) / costo;
		System.out.println(String.format("intervallo d_%d_%d = [%.4f, %.4f]\n", y, x, lowerBound, upperBound));
	}
	
	//===========================================================================================
	
	// QUESITO III
	
    private static GRBVar[] aggiungiY(GRBModel model) throws GRBException {
		GRBVar[] yi = new GRBVar[m];
		
		for(int i = 0; i < m; i++) {
			yi[i] = model.addVar(0, 1, 0, GRB.BINARY, "yi_" + i);
		}
		return yi;
	}
	
	//Metodo per trovare la funzione obiettivo
	private static void aggiungiFunzioneObiettivo3(GRBModel model, GRBVar[][] xij, GRBVar[] yi) throws GRBException {
		
		GRBLinExpr obj = new GRBLinExpr();
		/*
		for(int i = 0; i < mat.length; i++) {
			for(int j = 0; j < mat[0].length; j++) {
				obj.addTerm(c * mat[i][j], xij[i][j]);
			}
		}
		*/
		
		//-------------
		for(int i = 0; i < alfa_j.length; i++) {
			obj.addTerm(-alfa_j[i], yi[i]);  //alfaj (1-y) svolto
			obj.addConstant(alfa_j[i]);
		}
		//-------------
		model.setObjective(obj);
		model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
	}
	
	//Metodo per aggiungere i vincoli di magazzino
	private static void aggiungiVincoliMagazzino3(GRBModel model, GRBVar[][] xij, GRBVar[] yi) throws GRBException {
		
		for(int i = 0; i < s_j.length; i++) {
			GRBLinExpr exprX = new GRBLinExpr();
			for(int j = 0; j < xij[0].length; j++) {
				exprX.addTerm(1, xij[i][j]);
			}
			
			GRBLinExpr exprY = new GRBLinExpr();
			for(int j = 0; j < yi.length; j++) {
				exprY.addTerm(s_j[j], yi[j]);
			}
			model.addConstr(exprX, GRB.LESS_EQUAL, exprY, "vincolo_produzione_i_" + i);
		}	
	}
	
	private static void creaModulo3(GRBEnv env) throws GRBException {
		//Creazione nuovo problema
		GRBModel model3 = new GRBModel(env);
				
		GRBVar[][] xij3 = aggiungiVariabili(model3);
		GRBVar[] yi = aggiungiY(model3);
		aggiungiFunzioneObiettivo3(model3, xij3, yi);
		aggiungiVincoliMagazzino3(model3, xij3, yi);
		aggiungiVincoliDomanda(model3, xij3);
		
		model3.optimize();
		
		//Rilassamento continuo model3
		model3.relax().optimize();
		
		//Metodo per verificare il magazzino meno sfruttato
		sfruttamentoMagazzino(model3, xij3);
	}
	
	//Metodo per indiviuduare i magazzini meno utilizzati per inviare merci ai clienti
 private static void sfruttamentoMagazzino(GRBModel model3, GRBVar[][] xij3) throws GRBException {
	    int[] merciFornite = new int[m];
	    //Scopre quantita di merci inviate per ogni magazzino
		for(int i = 0; i < m; i++) {
			for(int j = 0; j < n; j++) {
				merciFornite[i] += xij3[i][j].get(GRB.DoubleAttr.X);;
			}
		}
		//Trova l'azienda che consegna meno merci
		ArrayList<Integer> listaMagazzini = new ArrayList<>();
		int min;
		min = merciFornite[0];
		for(int i = 0; i < m; i++) {
			if(merciFornite[i] == min){
				listaMagazzini.add(i);
			}
			if(merciFornite[i] <  min) {
				min = merciFornite[i];
				listaMagazzini.clear();
				listaMagazzini.add(i);
			}
		}	
		System.out.print("lista magazzini meno sfruttati = [ ");
		for(int i = 0; i < listaMagazzini.size(); i++) {
			System.out.printf("%d ", listaMagazzini.get(i));
		}
		System.out.println("]");
	}

	/*
	//Metodo che crea il nuovo problema, incluedendo i costi relativi ai singoli magazzini
	private static void creaProblema(GRBModel model, GRBVar[][] xij) throws GRBException {
		model.setObjective(aggiungiFunzioneObbiettivoIntera(xij), GRB.MINIMIZE);
		/*GRBEnv env = new GRBEnv();
		GRBModel model2 = new GRBModel(env);
		aggiungiVincoliDomanda(model2);
		aggiungiVincoliMagazzino(model2);
		model.optimize();
	}
	
	private static GRBExpr aggiungiFunzioneObbiettivoIntera(GRBVar[][] xij) {
		GRBLinExpr obj = new GRBLinExpr();
		for (int i = 0; i < m; i++) {
			for(int j = 0; j < n; j++) {
			//Se non vengono consegnati 
			if(xij[i][j].DoubleAttr.X == 0) {
				obj.addConstant(alfa_j[i]);
			}
			else {
				obj.addTerm(1, xij[i][j]);
			}
            
			}
        }
		return obj;
	}
*/
	//Metodo per trovare il rilassamento
	private static void trovaRilassamento(GRBModel model) throws Exception {
		GRBModel rilassamento = model.relax();
		rilassamento.optimize();
	}
}